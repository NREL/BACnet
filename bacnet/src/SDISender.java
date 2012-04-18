package gov.nrel.bacnet;

import gov.nrel.bacnet.Scan;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author abeach
 */
public class SDISender {
    String HOST_URL = "http://sdi-prod-01.com:8080/SDI";
    
    public SDISender() {
        
    }
    
    public SDISender(String SDIUrl) {
        this.HOST_URL = SDIUrl;
    }
    
    public void sendData(String modName, String key, com.google.gson.JsonObject jObj) {
        InputStreamReader is = null;
        BufferedReader rd = null;
        OutputStreamWriter wr = null;

        try {

            // Send data
            URL url = new URL(HOST_URL + "/" + modName + "/" + key);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("PUT");
            conn.setReadTimeout(10000);
            wr = new OutputStreamWriter(conn.getOutputStream());
            wr.write(new com.google.gson.Gson().toJson(jObj));
            wr.flush();

            // Get the response
            is = new InputStreamReader(conn.getInputStream());
            rd = new BufferedReader(is);

        } catch (Exception e) {
            System.out.println(this.getClass().getName()
                    + "SEND DATA ERROR " + modName + " " + key
                    + " " + jObj.toString() + e.getMessage());
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ex) {
                    System.out.println(ex.getMessage());
                }
            }

            if (rd != null) {
                try {
                    rd.close();
                } catch (IOException ex) {
                    System.out.println(ex.getMessage());
                }
            }

            if (wr != null) {
                try {
                    wr.close();
                } catch (IOException ex) {
                    System.out.println(ex.getMessage());
                }
            }
        }
    }   
    
}
