package playground.jbischoff.carsharing.data;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.io.IOUtils;

public class DriveNowParser
{
  public static void main(String[] args)
  {
    DriveNowParser dnp = new DriveNowParser();
    Map<Id<CarsharingVehicleData>, CarsharingVehicleData> currentGrep = dnp.grepAndDumpOnlineDatabase("./");
    for (CarsharingVehicleData cv : currentGrep.values()) {
      System.out.println(cv.toString());
    }
  }
  
  public DriveNowParser()
  {
    VBBRouteCatcher.initiate();
  }
  
  public Map<Id<CarsharingVehicleData>, CarsharingVehicleData> grepAndDumpOnlineDatabase(String outputfolder)
  {
    JSONParser jp = new JSONParser();
    
    Map<Id<CarsharingVehicleData>, CarsharingVehicleData> currentGrep = new HashMap();
    try
    {
      Car2GoParser.disableCertificates();
      GetMethod get = new GetMethod("https://api2.drive-now.com/cities/6099/cars");
      
      get.setRequestHeader("X-Api-Key", "adf51226795afbc4e7575ccc124face7");
      
      HttpClient httpclient = new HttpClient();
      httpclient.executeMethod(get);
      
      BufferedReader in = new BufferedReader(new InputStreamReader(get.getResponseBodyAsStream()));
      JSONObject jsonObject = (JSONObject)jp.parse(in);
      
      BufferedWriter bw = IOUtils.getBufferedWriter(outputfolder + "dn_" + System.currentTimeMillis() + ".json.gz");
      bw.write(jsonObject.toString());
      bw.flush();
      bw.close();
      
      JSONArray items = (JSONArray)jsonObject.get("items");
      
      Iterator<JSONObject> iterator = items.iterator();
      while (iterator.hasNext())
      {
        JSONObject car = (JSONObject)iterator.next();
        String vin = (String)car.get("id");
        String license = ((String)car.get("licensePlate")).replace(" ", "");
        
        Id<CarsharingVehicleData> vid = Id.create(vin, CarsharingVehicleData.class);
        String mileage = "0";
        String fuel = car.get("fuelLevel").toString();
        String latitude = car.get("latitude").toString();
        String longitude = car.get("longitude").toString();
        currentGrep.put(vid, new CarsharingVehicleData(vid, license, latitude, longitude, mileage, fuel, "DN"));
      }
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
}
