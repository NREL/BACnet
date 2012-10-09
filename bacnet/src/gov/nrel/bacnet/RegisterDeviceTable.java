package gov.nrel.bacnet;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import gov.nrel.bacnet.DatabusSender.WebClientDevWrapper;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

public class RegisterDeviceTable {
	static String HOST_URL = "https://databus.nrel.gov";
	public static String GROUP_NAME = "bacnet";
	public static String STREAM_TABLE_NAME = "streamMeta";
	public static String DEVICE_TABLE_NAME = "deviceMeta";
	public static String REGISTER_KEY = "register:10768272987:b1:4814227944682770547";

	public static void main(String[] args) {
		new RegisterDeviceTable().registerDeviceStreamTables();
	}

	public void registerDeviceStreamTables() {
		HttpClient httpclient = new DefaultHttpClient();
		if (HOST_URL.startsWith("https")) {
			httpclient = WebClientDevWrapper.wrapClient(httpclient);
		}

		HttpPost httpPost = new HttpPost(HOST_URL + "/register/" + REGISTER_KEY);
		String postKey = null;
		registerNewStream(httpclient, httpPost);
	}

	public void registerNewStream(HttpClient httpclient, HttpPost httpPost) {
		String json = createJsonForDeviceRegister();
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
			} else {
				EntityUtils.consume(entity);
				throw new RuntimeException("Databus Registration Key Error");
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		json = createJsonForStreamRegister();
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
			} else {
				EntityUtils.consume(entity);
				throw new RuntimeException("Databus Registration Key Error");
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public String regenerateKey(HttpClient httpclient, HttpPost httpPost,
			String tableName) {
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

	private String createJsonForStreamRegister() {
		RegisterMessage msg = new RegisterMessage();
		msg.setDatasetType(DatasetType.RELATIONAL_TABLE);
		List<String> groups = new ArrayList<String>();
		groups.add(GROUP_NAME);
		msg.setGroups(groups);
		msg.setModelName(GROUP_NAME + STREAM_TABLE_NAME);
		
		
		List<DatasetColumnModel> cols = new ArrayList<DatasetColumnModel>();
		createColumn(cols, "units", "string", "oei:unit", true, false);
		createColumn(cols, "virtual", "string", "oei:virtuality", true, false);
		createColumn(cols, "aggregationInterval", "string", "oei:interval",
				true, false);
		createColumn(cols, "aggregationType", "string", "oei:aggregation",
				true, false);
		createColumn(cols, "processed", "string", "oei:dataProcess", true,
				false);
		createColumn(cols, "description", "string", "oei:description", true,
				false);
		createColumn(cols, "device", "string", "oei:name", true, false);
		createColumn(cols, "stream", "string", "oei:timeSeriesData", true, true);
		msg.setIsSearchable(true);
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

	private String createJsonForDeviceRegister() {
		RegisterMessage msg = new RegisterMessage();
		msg.setDatasetType(DatasetType.RELATIONAL_TABLE);
		List<String> groups = new ArrayList<String>();
		groups.add(GROUP_NAME);
		msg.setGroups(groups);
		msg.setModelName(GROUP_NAME + DEVICE_TABLE_NAME);

		List<DatasetColumnModel> cols = new ArrayList<DatasetColumnModel>();
		createColumn(cols, "owner", "string", "oei:owner", true, false);
		createColumn(cols, "site", "string", "oei:name", true, false);
		createColumn(cols, "building", "string", "oei:name", true, false);
		createColumn(cols, "endUse", "string", "oei:energyUse", true, false);
		createColumn(cols, "protocol", "string", "oei:dataProtocol", true,
				false);
		createColumn(cols, "description", "string", "oei:description", true,
				false);
		createColumn(cols, "id", "string", "oei:name", true, true);
		createColumn(cols, "address", "string", "oei:address", true, false);
		msg.setIsSearchable(true);
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
}
