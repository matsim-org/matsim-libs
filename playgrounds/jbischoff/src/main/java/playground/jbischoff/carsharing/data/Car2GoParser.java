package playground.jbischoff.carsharing.data;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.io.IOUtils;

public class Car2GoParser
{
  public static void main(String[] args)
  {
    Car2GoParser dnp = new Car2GoParser();
    Map<Id<CarsharingVehicleData>, CarsharingVehicleData> currentGrep = dnp.grepAndDumpOnlineDatabase("./");
    for (CarsharingVehicleData cv : currentGrep.values()) {
      System.out.println(cv.toString());
    }
  }
  
  public Map<Id<CarsharingVehicleData>, CarsharingVehicleData> grepAndDumpOnlineDatabase(String outputfolder)
  {
    JSONParser jp = new JSONParser();
    
    Map<Id<CarsharingVehicleData>, CarsharingVehicleData> currentGrep = new HashMap();
    try
    {
      disableCertificates();
      
      System.setProperty("https.protocols", "TLSv1");
      
      URL url = new URL("https://www.car2go.com/api/v2.1/vehicles?loc=berlin&oauth_consumer_key=BerlinMultimodal&format=json");
      BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
      
      JSONObject jsonObject = (JSONObject)jp.parse(in);
      
      JSONArray msg = (JSONArray)jsonObject.get("placemarks");
      Iterator<JSONObject> iterator = msg.iterator();
      while (iterator.hasNext())
      {
        JSONObject car = (JSONObject)iterator.next();
        String vin = (String)car.get("vin");
        String license = (String)car.get("name");
        Id<CarsharingVehicleData> vid = Id.create(vin, CarsharingVehicleData.class);
        
        String fuel = car.get("fuel").toString();
        
        JSONArray loc = (JSONArray)car.get("coordinates");
        String longi = loc.get(0).toString();
        String lat = loc.get(1).toString();
        currentGrep.put(vid, new CarsharingVehicleData(vid, license, lat, longi, "0", fuel, "CG"));
      }
      BufferedWriter bw = IOUtils.getBufferedWriter(outputfolder + "c2g_" + System.currentTimeMillis() + ".json.gz");
      bw.write(jsonObject.toString());
      bw.flush();
      bw.close();
    }
    catch (IOException e)
    {
      e.printStackTrace();
    }
    catch (ParseException e)
    {
      e.printStackTrace();
    }
    return currentGrep;
  }
  
  public static void disableCertificates()
  {
    TrustManager[] trustAllCerts = {
      new X509TrustManager()
      {
        public X509Certificate[] getAcceptedIssuers()
        {
          return null;
        }
        
        public void checkClientTrusted(X509Certificate[] certs, String authType) {}
        
        public void checkServerTrusted(X509Certificate[] certs, String authType) {}
      } };
    try
    {
      SSLContext sc = SSLContext.getInstance("SSL");
      sc.init(null, trustAllCerts, new SecureRandom());
      HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
    }
    catch (Exception localException) {}
  }
}
