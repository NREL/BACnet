package gov.nrel.bacnet;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import gov.nrel.bacnet.DatasetColumnModel;
import gov.nrel.bacnet.DatasetType;
import gov.nrel.bacnet.RegisterMessage;
import gov.nrel.bacnet.RegisterResponseMessage;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.util.EntityUtils;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
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
public class DatabusSender {
	private static final Logger log = Logger.getLogger(DatabusSender.class
			.getName());
	String HOST_URL = "https://databus.nrel.gov:5502";
	public static String GROUP_NAME = "bacnet";
	public static String REGISTER_KEY = "register:7729155626:b1:7884682400432616546";
	HashMap<String, String> keyLookup;

	public DatabusSender() {
		keyLookup = new HashMap<String, String>();
	}

	public DatabusSender(String DatabusUrl) {
		this();
		this.HOST_URL = DatabusUrl;
	}

	public String registerStream(String tableName, String key) {
		HttpClient httpclient = new DefaultHttpClient();
		if (HOST_URL.startsWith("https")) {
			httpclient = WebClientDevWrapper.wrapClient(httpclient);
		}

		HttpPost httpPost = new HttpPost(HOST_URL + "/register/" + key);
		String postKey = null;
		try {
			postKey = registerNewStream(httpclient, httpPost, tableName);
		} catch (RuntimeException e) {
			postKey = regenerateKey(tableName, key);
		}

		if (postKey != null) {
			keyLookup.put(tableName, postKey);
		}

		return postKey;

	}

	public String regenerateKey(String tableName, String key) {
		HttpClient httpclient = new DefaultHttpClient();
		if (HOST_URL.startsWith("https")) {
			httpclient = WebClientDevWrapper.wrapClient(httpclient);
		}
		HttpPost httpPost = new HttpPost(HOST_URL + "/regenerate/" + tableName
				+ "/" + GROUP_NAME + "/" + key);
		return regenerateKey(httpclient, httpPost, tableName);
	}

	public void sendData(String tableName, long time, double value) {
		String postKey = keyLookup.get(tableName);
		if (postKey == null) {
			postKey = registerStream(tableName, REGISTER_KEY);
		}

		HttpClient httpclient = new DefaultHttpClient();
		if(HOST_URL.startsWith("https")) {
			httpclient = WebClientDevWrapper.wrapClient(httpclient);
		}
		HttpPost httpPost = new HttpPost(HOST_URL + "/postdata/" + postKey);
		postNewDataPoint(httpclient, time, value, postKey);
	}

	private BasicHttpContext setupPreEmptiveBasicAuth(HttpHost targetHost,
			DefaultHttpClient httpclient) {
		httpclient.getCredentialsProvider().setCredentials(
				new AuthScope(targetHost.getHostName(), targetHost.getPort()),
				new UsernamePasswordCredentials("dhiller2", "password"));

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

	public void postNewDataPoint(HttpClient httpclient, long time,
			double value, String postKey) {
		HttpPost httpPost = new HttpPost(HOST_URL + "/postdata/" + postKey);

		try {
			Map<String, String> result = new HashMap<String, String>();
			result.put("time", time + "");
			result.put("value", value + "");
			ObjectMapper mapper = new ObjectMapper();
			String json = mapper.writeValueAsString(result);

			httpPost.setHeader("Content-Type", "application/json");
			httpPost.setEntity(new StringEntity(json));
			HttpResponse response2 = httpclient.execute(httpPost);

			log.info("" + response2.getStatusLine());
			if (response2.getStatusLine().getStatusCode() != 200) {
				log.severe(response2.getEntity().toString());
			}
			HttpEntity entity = response2.getEntity();
			EntityUtils.consume(entity);

		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	public String registerNewStream(HttpClient httpclient,
			HttpPost httpPost, String tableName) {
		String json = createJsonForRequest(tableName);
		httpPost.setHeader("Content-Type", "application/json");
		try {
			httpPost.setEntity(new StringEntity(json));
			HttpResponse response2 = httpclient.execute(httpPost);

			System.out.println(response2.getStatusLine());
			// Assert.assertEquals(200,
			// response2.getStatusLine().getStatusCode());

			// read out the body so we can re-use the client
			HttpEntity entity = response2.getEntity();

			if (response2.getStatusLine().getStatusCode() == 200) {
				InputStream in = entity.getContent();
				ObjectMapper mapper = new ObjectMapper();
				RegisterResponseMessage resp = mapper.readValue(in,
						RegisterResponseMessage.class);
				String postKey = resp.getPostKey();
				return postKey;
			} else {
				log.severe(response2.getEntity().toString());
				EntityUtils.consume(entity);
				throw new RuntimeException("Databus Registration Key Error");
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public String regenerateKey(HttpClient httpclient,
			HttpPost httpPost, String tableName) {
		httpPost.setHeader("Content-Type", "application/json");
		try {
			HttpResponse response2 = httpclient.execute(httpPost);

			System.out.println(response2.getStatusLine());
			// Assert.assertEquals(200,
			// response2.getStatusLine().getStatusCode());

			// read out the body so we can re-use the client
			HttpEntity entity = response2.getEntity();
			InputStream in = entity.getContent();
			ObjectMapper mapper = new ObjectMapper();
			RegisterResponseMessage resp = mapper.readValue(in,
					RegisterResponseMessage.class);
			String postKey = resp.getPostKey();
			return postKey;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private String createJsonForRequest(String tableName) {
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
		groups.add(GROUP_NAME);
		msg.setGroups(groups);
		msg.setModelName(tableName);

		List<DatasetColumnModel> cols = new ArrayList<DatasetColumnModel>();
		createColumn(cols, "time", "BigInteger", "oei:timestamp", false, true);
		createColumn(cols, "value", "BigDecimal", "oei:measured_value", false,
				false);
		msg.setColumns(cols);

		ObjectMapper mapper = new ObjectMapper();
		StringWriter out = new StringWriter();
		try {
			mapper.writeValue(out, msg);
		} catch (JsonGenerationException e) {
			throw new RuntimeException(e);
		} catch (JsonMappingException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		String json = out.toString();
		return json;
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

	public static class WebClientDevWrapper {

		public static HttpClient wrapClient(HttpClient base) {
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
				ClientConnectionManager ccm = base.getConnectionManager();
				SchemeRegistry sr = ccm.getSchemeRegistry();
				sr.register(new Scheme("https", ssf, 443));
				return new DefaultHttpClient(ccm, base.getParams());
			} catch (Exception ex) {
				ex.printStackTrace();
				return null;
			}
		}
	}
}
