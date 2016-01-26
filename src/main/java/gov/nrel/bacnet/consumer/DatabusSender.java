/*
 * Copyright (C) 2013, Alliance for Sustainable Energy
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package gov.nrel.bacnet.consumer;

import gov.nrel.bacnet.DatasetColumnModel;
import gov.nrel.bacnet.DatasetType;
import gov.nrel.bacnet.RegisterMessage;
import gov.nrel.bacnet.consumer.beans.DatabusBean;
import gov.nrel.bacnet.consumer.beans.Device;
import gov.nrel.bacnet.consumer.beans.Stream;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.HttpHost;
import org.apache.http.client.AuthCache;
import org.apache.http.auth.AuthScope;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.util.EntityUtils;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 * 
 * @author abeach
 */
class DatabusSender {
	private static final Logger log = Logger.getLogger(DatabusSender.class.getName());

	private DefaultHttpClient httpclient;
	private ObjectMapper mapper = new ObjectMapper();

	private int counter = 0;
	private double maxAve = 0;
	private double minAve = Integer.MAX_VALUE;

	private Long initialStart;

	private String hostUrl;
	private String host;
	private int port;
	private String mode;
	private String username;
	private String key;

	
	public DatabusSender(String username, String key, ExecutorService recorderSvc, String host, int port, boolean isSecure) {
		log.info("username=" + username + " key=" + key + " port=" + port);
		this.port = port;

		PoolingClientConnectionManager mgr = new PoolingClientConnectionManager();
		mgr.setDefaultMaxPerRoute(30);
		mgr.setMaxTotal(30);
		this.username = username;
		this.key = key;
		this.host = host;
		this.port = port;
		this.mode = "https";

		if(!isSecure)
			mode = "http";
		this.hostUrl = mode+"://"+host+":"+port;
		if (isSecure) {
			httpclient = createSecureOne(mgr);
		} else {
			httpclient = new DefaultHttpClient(mgr);
		}
		HttpParams params = httpclient.getParams();
		HttpConnectionParams.setConnectionTimeout(params, 60000);
		HttpConnectionParams.setSoTimeout(params, 60000);
		log.info("hostUrl="+hostUrl);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void postData(List<DatabusBean> data) {
		synchronized(this) {
			if(initialStart == null) {
				initialStart = System.currentTimeMillis();
			}
		}
		
		long startPost = System.currentTimeMillis();
		
		List list = new ArrayList();
		Map<String, Object> jsonObj = new HashMap<String, Object>();
		jsonObj.put("_dataset", list);

		for(DatabusBean b : data) {
			Map<String, String> result = create(b);
			list.add(result);
		}

		String json = writeValueAsString(jsonObj);
		log.info("posting datasetsize="+data.size());
		post("/api/postdataV1", json);
		if(log.isLoggable(Level.FINE))
			log.fine("posted dataset="+json);
		
		logInfo(data.size(), startPost);
	}

	private synchronized void logInfo(int size, long start) {
		counter+=size;
		long time = System.currentTimeMillis();
		long total = time-start;
		double timeSecs = ((double)(time - initialStart)) / 1000;
		double postsPerSec = ((double)counter) / timeSecs;
		
		maxAve = Math.max(postsPerSec, maxAve);
		minAve = Math.min(postsPerSec, minAve);
		
		log.info("posting datasetsize="+size+" took="+total+"ms rate="+postsPerSec+" postsPerSec(last 100k records) max/min="+maxAve+"/"+minAve+" timecoll="+timeSecs+" secs counter="+counter);
		
		if(counter > 100000) {
			initialStart = System.currentTimeMillis();
			counter = 0;
		}
	}

	private Map<String, String> create(DatabusBean b) {
		Map<String, String> result = new HashMap<String, String>();
		result.put("_tableName", b.getTableName());
		result.put("time", b.getTime() + "");
		result.put("value", b.getValue() + "");
		return result;
	}

	public void postNewStream(Stream str, Device dev, String group, String id) {
		log.info(id+"posting new stream="+str.getTableName());
		long reg = -1;
		try {
			String json = createJsonForRequest(str.getTableName(), group);
			reg = post("/api/registerV1", json);
		}
		catch (Exception e) {
			log.log(Level.WARNING,"failed to post new stream",e);
		}
	}

	private String writeValueAsString(Object obj) {
		try {
			return mapper.writeValueAsString(obj);
		} catch (JsonGenerationException e) {
			throw new RuntimeException(e);
		} catch (JsonMappingException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private long post(String url, String json) {
		HttpPost httpPost = new HttpPost(hostUrl+url);

		String statusLine = null;

		try {
			httpPost.setHeader("Content-Type", "application/json");
			httpPost.setEntity(new StringEntity(json));
			long t1 = System.currentTimeMillis();
			this.setupPreEmptiveBasicAuth(httpclient);
			BasicHttpContext ctx = setupPreEmptiveBasicAuth(httpclient);
			HttpResponse response2 = httpclient.execute(httpPost, ctx);
			long t2 = System.currentTimeMillis();
			
			statusLine = ""+response2.getStatusLine();
			if (response2.getStatusLine().getStatusCode() != 200) {
				HttpEntity entity = response2.getEntity();
				InputStream instream = entity.getContent();
				StringWriter writer = new StringWriter();
				IOUtils.copy(instream, writer);
				String theString = writer.toString();
				throw new RuntimeException("Failure="+theString+" statusline="+statusLine+" on json="+json);
			}
			
			HttpEntity entity = response2.getEntity();
			EntityUtils.consume(entity);
			return t2-t1;
		} catch (Exception e) {
			log.log(Level.WARNING,"failed to post, continuing on to next request",e);
			throw new RuntimeException(e);
		} finally {
			httpPost.reset();
		}
	}
	
	BasicHttpContext setupPreEmptiveBasicAuth(DefaultHttpClient httpclient) {
		HttpHost targetHost = new HttpHost(host, port, mode); 
		httpclient.getCredentialsProvider().setCredentials(
		        new AuthScope(targetHost.getHostName(), targetHost.getPort()), 
		        new UsernamePasswordCredentials(username, key));

		// Create AuthCache instance
		AuthCache authCache = new BasicAuthCache();
		// Generate BASIC scheme object and add it to the local auth cache
		BasicScheme basicAuth = new BasicScheme();
		authCache.put(targetHost, basicAuth);

		// Add AuthCache to the execution context
		BasicHttpContext localcontext = new BasicHttpContext();
		localcontext.setAttribute(ClientContext.AUTH_CACHE, authCache);
		return localcontext;
	}

	private String createJsonForRequest(String tableName, String group) {
		// {"datasetType":"STREAM",
		// "modelName":"timeSeriesForPinkBlobZ",
		// "groups":["supa"],
		// "columns":[
		// {"name":"time","dataType":"BigInteger","semanticType":"oei:timestamp","isIndex":false,"isPrimaryKey":true,"semantics":[]},
		// {"name":"color","dataType":"string","semanticType":"oei:color","isIndex":false,"isPrimaryKey":false,"semantics":[]},
		// {"name":"volume","dataType":"BigDecimal","semanticType":"oei:volume","isIndex":false,"isPrimaryKey":false,"semantics":[]}
		// ]
		// }
		RegisterMessage msg = new RegisterMessage();
		msg.setDatasetType(DatasetType.STREAM);
		List<String> groups = new ArrayList<String>();
		groups.add(group);
		msg.setGroups(groups);
		msg.setModelName(tableName);

		List<DatasetColumnModel> cols = new ArrayList<DatasetColumnModel>();
		createColumn(cols, "time", "BigInteger", "oei:timestamp", true, true);
		createColumn(cols, "value", "BigDecimal", "oei:measured_value", true,
				false);
		msg.setIsSearchable(false);
		msg.setColumns(cols);

		return writeValueAsString(msg);
	}

	// {"name":"time","dataType":"BigInteger","semanticType":"oei:timestamp","isIndex":false,"isPrimaryKey":true,"semantics":[]},

	private void createColumn(List<DatasetColumnModel> cols, String name,
			String dataType, String semanticType, boolean isIndex,
			boolean isPrimaryKey) {
		DatasetColumnModel col = new DatasetColumnModel();
		col.setName(name);
		col.setDataType(dataType);
		col.setSemanticType(semanticType);
		col.setIsIndex(isIndex);
		col.setIsPrimaryKey(isPrimaryKey);
		cols.add(col);
	}

	public DefaultHttpClient createSecureOne(PoolingClientConnectionManager mgr) {
		try {
			SSLContext ctx = SSLContext.getInstance("TLS");
			X509TrustManager tm = new X509TrustManager() {

				public void checkClientTrusted(X509Certificate[] xcs,
						String string) throws CertificateException {
				}

				public void checkServerTrusted(X509Certificate[] xcs,
						String string) throws CertificateException {
				}

				public X509Certificate[] getAcceptedIssuers() {
					return null;
				}
			};
			X509HostnameVerifier verifier = new X509HostnameVerifier() {

				@Override
				public void verify(String string, X509Certificate xc)
						throws SSLException {
				}

				@Override
				public void verify(String string, String[] strings,
						String[] strings1) throws SSLException {
				}

				@Override
				public boolean verify(String hostname, SSLSession session) {
					// TODO Auto-generated method stub
					return false;
				}

				@Override
				public void verify(String arg0, SSLSocket arg1)
						throws IOException {
					// TODO Auto-generated method stub

				}
			};
			ctx.init(null, new TrustManager[] { tm }, null);
			SSLSocketFactory ssf = new SSLSocketFactory(ctx);
			ssf.setHostnameVerifier(verifier);
			SchemeRegistry sr = mgr.getSchemeRegistry();
			sr.register(new Scheme("https", ssf, port));
			return new DefaultHttpClient(mgr);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
}
