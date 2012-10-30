package gov.nrel.bacnet;

import gov.nrel.consumer.beans.Numbers;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.protocol.BasicHttpContext;
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
	public static final String HOST_URL = "https://databus.nrel.gov:5502";
	public static String GROUP_NAME = "bacnet";
	public static String REGISTER_KEY = "register:10768272987:b1:4814227944682770547";
	public static String STREAM_KEY = "post:10768273024:b1:5456969792682001521";
	public static String DEVICE_KEY = "post:10768273014:b1:2083447771105177062";
	private static HashMap<String, String> keyLookup;
	private static TreeSet<String> registeredDevices;
	private static DefaultHttpClient httpclient;

	static {
		PoolingClientConnectionManager mgr = new PoolingClientConnectionManager();
		mgr.setDefaultMaxPerRoute(30);
		mgr.setMaxTotal(30);
		if (HOST_URL.startsWith("https")) {
			httpclient = WebClientDevWrapper.wrapClient2(mgr);
		} else {
			httpclient = new DefaultHttpClient(mgr);
		}
	}
	
	private synchronized HashMap<String, String> getKeyLookup() {
		if (keyLookup == null) {
			keyLookup = new HashMap<String, String>();
		}
		return keyLookup;
	}

	private synchronized TreeSet<String> getRegDevices() {
		if (registeredDevices == null) {
			registeredDevices = new TreeSet<String>();
		}
		return registeredDevices;
	}

	public DatabusSender() {
	}

	public DatabusSender(String DatabusUrl) {
		this();
	}

	public String registerStream(String tableName, String key, String units,
			String device, String streamDescription, String deviceDescription,
			String address, Numbers times) {

		HttpPost httpPost = new HttpPost(HOST_URL + "/register/" + key);
		String postKey = null;

		try {
			long t1 = System.currentTimeMillis();
			postKey = registerNewStream(httpPost, tableName);
			long t2 = System.currentTimeMillis();
			long total = t2-t1;
			times.setRegisterTime(total);
			try {
				postNewStream(httpclient, units, "false", "raw", "raw", "raw",
						streamDescription, device, tableName);
				
			} catch (RuntimeException e) {
				//I think a stream is being posted every time but later we should keep track of whether it is
				//posted or not
				log.log(Level.FINE, "Exception expected alot", e);
			}

			long t3 = System.currentTimeMillis();
			times.setPostNewStreamTime(t3-t2);
			
			if (getRegDevices().contains(device) == false) {
				int spaceIndex = deviceDescription.indexOf(" ");
				int uscoreIndex = deviceDescription.indexOf("_");
				getRegDevices().add(device);
				postNewDevice(
						httpclient,
						"NREL",
						deviceDescription.startsWith("NWTC") ? "NWTC" : "STM",
						deviceDescription.startsWith("CP")
								|| deviceDescription.startsWith("FTU")
								|| deviceDescription.startsWith("1ST") ? "RSF"
								: (deviceDescription.startsWith("Garage") ? "Garage"
										: (spaceIndex < uscoreIndex
												&& spaceIndex != -1
												|| uscoreIndex == -1 ? deviceDescription
												.split(" ")[0]
												: deviceDescription.split("_")[0])),
						"unknown", "BACNet", deviceDescription, device, address);
				long t4 = System.currentTimeMillis();
				times.setPostDeviceTime(t4-t3);
			}
		} catch (RuntimeException e) {
			long t5 = System.currentTimeMillis();
			postKey = regenerateKey(tableName, key);
			long t6 = System.currentTimeMillis();
			times.setReregisterTime(t6-t5);
		}

		if (postKey != null) {
			log.info("Updated " + tableName + " post key");
			getKeyLookup().put(tableName, postKey);
		} else {
			log.info(tableName + " post key null!");
		}

		return postKey;
	}

	public String regenerateKey(String tableName, String key) {
		HttpPost httpPost = new HttpPost(HOST_URL + "/regenerate/" + tableName
				+ "/" + GROUP_NAME + "/" + key);
		return regenerateKey(httpPost, tableName);
	}

	public void sendData(String tableName, long time, double value,
			String units, String device, String streamDescription,
			String deviceDescription, String address, Numbers times) {
		String postKey = getKeyLookup().get(tableName);
		if (postKey == null) {
			long start = System.currentTimeMillis();
			postKey = registerStream(tableName, REGISTER_KEY, units, device,
					streamDescription, deviceDescription, address, times);
			long total = System.currentTimeMillis() - start;
			times.setFullRegTime(total);
		}

		long startPost = System.currentTimeMillis();
		HttpPost httpPost = new HttpPost(HOST_URL + "/postdata/" + postKey);
		postNewDataPoint(httpclient, time, value, postKey);
		long total = System.currentTimeMillis()-startPost;
		times.setPostTime(total);
	}

	private BasicHttpContext setupPreEmptiveBasicAuth(HttpHost targetHost) {
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

	public void postNewStream(HttpClient httpclient, String units,
			String virtual, String aggInterval, String aggType,
			String processed, String description, String device, String data) {
		HttpPost httpPost = new HttpPost(HOST_URL + "/postdata/" + STREAM_KEY);

		try {

			Map<String, String> result = new HashMap<String, String>();
			result.put("units", units + "");
			result.put("virtual", virtual + "");
			result.put("aggregationInterval", aggInterval + "");
			result.put("aggregationType", aggType + "");
			result.put("processed", processed + "");
			result.put("description", description + "");
			result.put("device", device + "");
			result.put("stream", data + "");
			ObjectMapper mapper = new ObjectMapper();
			String json = mapper.writeValueAsString(result);

			httpPost.setHeader("Content-Type", "application/json");
			httpPost.setEntity(new StringEntity(json));
			HttpResponse response2 = httpclient.execute(httpPost);

			log.info("" + response2.getStatusLine());
			if (response2.getStatusLine().getStatusCode() != 200) {
				log.severe(response2.getEntity().toString());
				throw new RuntimeException(response2.getEntity().toString());
			}
			HttpEntity entity = response2.getEntity();
			EntityUtils.consume(entity);

		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			httpPost.reset();
		}

	}

	public void postNewDevice(HttpClient httpclient, String owner, String site,
			String building, String endUse, String protocol,
			String description, String id, String address) {
		HttpPost httpPost = new HttpPost(HOST_URL + "/postdata/" + DEVICE_KEY);

		try {
			Map<String, String> result = new HashMap<String, String>();
			result.put("owner", owner + "");
			result.put("site", site + "");
			result.put("building", building + "");
			result.put("endUse", endUse + "");
			result.put("protocol", protocol + "");
			result.put("description", description + "");
			result.put("id", id + "");
			result.put("address", address + "");
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
		} finally {
			httpPost.reset();
		}
	}

	public String registerNewStream(HttpPost httpPost,
			String tableName) {
		String json = createJsonForRequest(tableName);
		httpPost.setHeader("Content-Type", "application/json");
		String statusLine = "";
		try {
			httpPost.setEntity(new StringEntity(json));
			HttpResponse response2 = httpclient.execute(httpPost);

			statusLine = ""+response2.getStatusLine();
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
				log.info("tableName="+tableName+" postkey="+postKey);
				return postKey;
			} else {
				log.severe(response2.getEntity().toString());
				EntityUtils.consume(entity);
				throw new RuntimeException("Databus Registration Key Error");
			}
		} catch (Exception e) {
			throw new RuntimeException("tableName="+tableName+" statusline="+statusLine, e);
		} finally {
			httpPost.reset();
		}
	}

	public String regenerateKey(HttpPost httpPost,
			String tableName) {
		httpPost.setHeader("Content-Type", "application/json");
		String statusLine = "";
		try {
			HttpResponse response2 = httpclient.execute(httpPost);

			statusLine = ""+response2.getStatusLine();
			// Assert.assertEquals(200,
			// response2.getStatusLine().getStatusCode());

			// read out the body so we can re-use the client
			HttpEntity entity = response2.getEntity();
			InputStream in = entity.getContent();
			ObjectMapper mapper = new ObjectMapper();
			RegisterResponseMessage resp = mapper.readValue(in,
					RegisterResponseMessage.class);
			String postKey = resp.getPostKey();
			log.info("tableName="+tableName+" retrieved postkey = "+postKey);
			return postKey;
		} catch (Exception e) {
			throw new RuntimeException("tableName="+tableName+" statusline="+statusLine, e);
		} finally {
			httpPost.reset();
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
		createColumn(cols, "time", "BigInteger", "oei:timestamp", true, true);
		createColumn(cols, "value", "BigDecimal", "oei:measured_value", true,
				false);
		msg.setIsSearchable(false);
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

		public static DefaultHttpClient wrapClient2(PoolingClientConnectionManager mgr) {
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
				sr.register(new Scheme("https", ssf, 443));
				return new DefaultHttpClient(mgr);
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		}
	}
}
