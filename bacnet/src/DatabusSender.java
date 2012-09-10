package gov.nrel.bacnet;

import gov.nrel.bacnet.Scan;
import gov.nrel.util.StartupBean;

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

import models.message.DatasetColumnModel;
import models.message.DatasetType;
import models.message.RegisterMessage;
import models.message.RegisterResponseMessage;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.DefaultHttpClient;
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
    String HOST_URL = "https://databus.nrel.gov:5502";
    String GROUP_NAME = "bacnet";
    String REGISTER_KEY = "";
    HashMap<String, String> keyLookup;
    
    public DatabusSender() {
        keyLookup = new HashMap<String, String>();
    }
    
    public DatabusSender(String DatabusUrl) {
    	this();
        this.HOST_URL = "https://" + DatabusUrl;
    }
    

    public String registerStream(String tableName, String key) {
		DefaultHttpClient httpclient = new DefaultHttpClient();
		HttpPost httpPost = new HttpPost(HOST_URL + "/register/" + key);
		String postKey = registerNewStream(httpclient, httpPost, tableName);
		if(postKey == null) {
			postKey = regenerateKey(tableName, key);
		} 
		
		keyLookup.put(tableName, postKey);
		return postKey;
		
    }   
    
    public String regenerateKey(String tableName, String key) {
		DefaultHttpClient httpclient = new DefaultHttpClient();
		HttpPost httpPost = new HttpPost(HOST_URL + "/regenerate/" 
		+ tableName + "/" + GROUP_NAME + "/" + key);
		return regenerateKey(httpclient, httpPost, tableName);
    }   
    
    public void sendData(String tableName, long time, double value) {
    	String postKey = keyLookup.get(tableName);
    	if(postKey == null) {
    		postKey = registerStream(tableName, REGISTER_KEY);
    	}
    	    	
    	DefaultHttpClient httpclient = new DefaultHttpClient();
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
	
	public void postNewDataPoint(DefaultHttpClient httpclient, long time,
			double value, String postKey) throws UnsupportedEncodingException,
			IOException, ClientProtocolException {
		HttpPost httpPost = new HttpPost(HOST_URL + "/postdata/" + postKey);
		
		Map<String, String> result = new HashMap<String, String>();
		result.put("time", time+"");
		result.put("value", value+"");
		ObjectMapper mapper = new ObjectMapper();
		String json = mapper.writeValueAsString(result);
		
		httpPost.setHeader("Content-Type", "application/json");
		httpPost.setEntity(new StringEntity(json));
		HttpResponse response2 = httpclient.execute(httpPost);

	    System.out.println(response2.getStatusLine());
		// Assert.assertEquals(200, response2.getStatusLine().getStatusCode());
	    
		//read out the body so we can re-use the client
		HttpEntity entity = response2.getEntity();
		EntityUtils.consume(entity);	    	
		
	}
	
	public String registerNewStream(DefaultHttpClient httpclient,
			HttpPost httpPost, String tableName) throws IOException, 
			UnsupportedEncodingException,
			ClientProtocolException {
		String json = createJsonForRequest(tableName);
		httpPost.setHeader("Content-Type", "application/json");
		httpPost.setEntity(new StringEntity(json));
		HttpResponse response2 = httpclient.execute(httpPost);

	    System.out.println(response2.getStatusLine());
		//Assert.assertEquals(200, response2.getStatusLine().getStatusCode());
		
		//read out the body so we can re-use the client
		HttpEntity entity = response2.getEntity();
		InputStream in = entity.getContent();
		ObjectMapper mapper = new ObjectMapper();
		RegisterResponseMessage resp = mapper.readValue(in, RegisterResponseMessage.class);
		
		if(response2.getStatusLine().getStatusCode() == 200) {
			String postKey = resp.getPostKey();
			return postKey;
		} else {
			return null;
		}
	}
	
	public String regenerateKey(DefaultHttpClient httpclient,
			HttpPost httpPost, String tableName) throws IOException, 
			UnsupportedEncodingException,
			ClientProtocolException {
		httpPost.setHeader("Content-Type", "application/json");
		HttpResponse response2 = httpclient.execute(httpPost);

	    System.out.println(response2.getStatusLine());
		//Assert.assertEquals(200, response2.getStatusLine().getStatusCode());
		
		//read out the body so we can re-use the client
		HttpEntity entity = response2.getEntity();
		InputStream in = entity.getContent();
		ObjectMapper mapper = new ObjectMapper();
		RegisterResponseMessage resp = mapper.readValue(in, RegisterResponseMessage.class);
		String postKey = resp.getPostKey();
		return postKey;
	}

	private String createJsonForRequest(String tableName) throws IOException {
//		{"datasetType":"STREAM",
//		 "modelName":"timeSeriesForPinkBlobZ",
//		 "groups":["supa"],
//		 "columns":[
//		      {"name":"time","dataType":"BigInteger","semanticType":"oei:timestamp","isIndex":false,"isPrimaryKey":true,"semantics":[]},
//		      {"name":"color","dataType":"string","semanticType":"oei:color","isIndex":false,"isPrimaryKey":false,"semantics":[]},
//		      {"name":"volume","dataType":"BigDecimal","semanticType":"oei:volume","isIndex":false,"isPrimaryKey":false,"semantics":[]}
//		      ]
//		}
		RegisterMessage msg = new RegisterMessage();
		msg.setDatasetType(DatasetType.STREAM);
		List<String> groups = new ArrayList<String>();
		groups.add(GROUP_NAME);
		msg.setGroups(groups);
		msg.setModelName(tableName);
		
		List<DatasetColumnModel> cols = new ArrayList<DatasetColumnModel>();
		createColumn(cols, "time", "BigInteger", "oei:timestamp", false, true);
		createColumn(cols, "value", "BigDecimal", "oei:measured_value", false, false);
		msg.setColumns(cols);

		ObjectMapper mapper = new ObjectMapper();
		StringWriter out = new StringWriter();
		mapper.writeValue(out, msg);
		String json = out.toString();
		return json;
	}

	//{"name":"time","dataType":"BigInteger","semanticType":"oei:timestamp","isIndex":false,"isPrimaryKey":true,"semantics":[]},

	private void createColumn(List<DatasetColumnModel> cols, String name,
			String dataType, String semanticType, boolean isIndex, boolean isPrimaryKey) {
		DatasetColumnModel col = new DatasetColumnModel();
		col.setName(name);
		col.setDataType(dataType);
		col.setSemanticType(semanticType);
		col.setIsIndex(isIndex);
		col.setIsPrimaryKey(isPrimaryKey);
		cols.add(col);
	}

    
}
