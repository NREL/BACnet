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
public class BASender {
    String HOST_URL = "http://buildingagenttest.nrel.gov:5502/api/data_receiver.xml";
    
    public BASender() {
        
    }
    
    public BASender(String BAUrl) {
        this.HOST_URL = BAUrl;
    }
    
    public void sendData(String data) {
        InputStreamReader is = null;
        BufferedReader rd = null;
        OutputStreamWriter wr = null;

        try {
            //prepare data for sending
            data = data.replace("&", "&amp;");
            data = data.replace("<", "&lt;");
            data = data.replace(">", "&gt;");
            data = data.replace("<", "&lt;");
            data = data.replace("'", "&apos;");
            data = data.replace("\"", "&quot;");
            data = data.replace("%","Percent");
            data = "<data>\n" + data + "\n</data>";
          
            System.out.println(data);
            // Send data
            URL url = new URL(HOST_URL );
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setReadTimeout(120000);
            wr = new OutputStreamWriter(conn.getOutputStream());
            wr.write(data);
            wr.flush();

            // Get the response
            is = new InputStreamReader(conn.getInputStream());
            rd = new BufferedReader(is);
        } catch (Exception e) {
            System.out.println(this.getClass().getName()
                    + "SEND DATA ERROR " + e.getMessage());
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
