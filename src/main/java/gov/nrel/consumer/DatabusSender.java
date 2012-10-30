package gov.nrel.consumer;

import gov.nrel.bacnet.DatasetColumnModel;
import gov.nrel.bacnet.DatasetType;
import gov.nrel.bacnet.RegisterMessage;
import gov.nrel.consumer.beans.DatabusBean;
import gov.nrel.consumer.beans.Device;
import gov.nrel.consumer.beans.Stream;

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
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.util.EntityUtils;
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
public class DatabusSender {
	private static final Logger log = Logger.getLogger(DatabusSender.class
			.getName());

	public static final int PORT = 5502;
	public static final String HOST_URL = "https://databus.nrel.gov:"+PORT;
	public static String REGISTER_KEY = "register:10768272987:b1:4814227944682770547";

	private DefaultHttpClient httpclient;
	private String groupPostKey;
	private ObjectMapper mapper = new ObjectMapper();
	private String deviceTable;
	private String streamTable;
	private MetaLoader meta = new MetaLoader();

	private int counter = 0;
	private double maxAve = 0;
	private double minAve = Integer.MAX_VALUE;

	private Long initialStart;
	
	public DatabusSender(String groupPostKey, String groupGetKey, String deviceTable, String streamTable, ExecutorService recorderSvc) {
		this.groupPostKey = groupPostKey;
		this.deviceTable = deviceTable;
		this.streamTable = streamTable;
		PoolingClientConnectionManager mgr = new PoolingClientConnectionManager();
		mgr.setDefaultMaxPerRoute(30);
		mgr.setMaxTotal(30);
		if (HOST_URL.startsWith("https")) {
			httpclient = createSecureOne(mgr);
		} else {
			httpclient = new DefaultHttpClient(mgr);
		}

		meta.initialize(httpclient, deviceTable, streamTable, groupGetKey, recorderSvc);
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
		
		post("/postdata", json);
		
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
		result.put("_postKey", groupPostKey);
		result.put("_tableName", b.getTableName());
		result.put("time", b.getTime() + "");
		result.put("value", b.getValue() + "");
		return result;
	}

	public void postNewStream(Stream str, Device dev, String group, String id) {
		if(!meta.addStream(str))
			return;
		
		log.info(id+"posting new stream="+str.getTableName());
		postNewDevice(id, dev);
		
		long reg = registerNewStream(str.getTableName(), group);
		
		Map<String, String> result = new HashMap<String, String>();
		result.put("_postKey", groupPostKey);
		result.put("_tableName", streamTable);
		result.put("units", str.getUnits());
		result.put("virtual", str.getVirtual());
		result.put("aggregationInterval", str.getAggInterval());
		result.put("aggregationType", str.getAggType());
		result.put("processed", str.getProcessed());
		result.put("description", str.getStreamDescription());
		result.put("device", str.getDevice());
		result.put("stream", str.getTableName());
		String json = writeValueAsString(result);
		
		long post2 = post("/postdata", json);
		log.info(id+"registered time="+reg+" and posted time="+post2+" stream="+str.getTableName());
	}
	
	private long registerNewStream(String tableName, String group) {
		String json = createJsonForRequest(tableName, group);
		return post("/register/" + REGISTER_KEY, json);
	}

	private synchronized void postNewDevice(String id, Device d) {
		if(!meta.addDevice(d))
			return;
		
		log.info(id+"posting new device="+d.getDeviceId());
		
		Map<String, String> result = new HashMap<String, String>();
		result.put("_postKey", groupPostKey);
		result.put("_tableName", deviceTable);
		result.put("owner", d.getOwner());
		result.put("site", d.getSite());
		result.put("building", d.getBldg());
		result.put("endUse", d.getEndUse());
		result.put("protocol", d.getProtocol());
		result.put("description", d.getDeviceDescription());
		result.put("id", d.getDeviceId());
		result.put("address", d.getAddress());
		String json = writeValueAsString(result);

		long total = post("/postdata", json);
		log.info(id+"device="+d.getDeviceId()+" posted time="+total);
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
		HttpPost httpPost = new HttpPost(HOST_URL+url);

		String statusLine = null;

		try {
			httpPost.setHeader("Content-Type", "application/json");
			httpPost.setEntity(new StringEntity(json));
			long t1 = System.currentTimeMillis();
			HttpResponse response2 = httpclient.execute(httpPost);
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
			throw new RuntimeException(e);
		} finally {
			httpPost.reset();
		}
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

	public static DefaultHttpClient createSecureOne(PoolingClientConnectionManager mgr) {
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
			sr.register(new Scheme("https", ssf, PORT));
			return new DefaultHttpClient(mgr);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
}
