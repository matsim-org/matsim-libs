/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.jbischoff.carsharing.data;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.IOUtils;
/**
 * @author jbischoff
 *
 */
public class Car2GoParser
{

    public static void main(String[] args)
    {
        Car2GoParser dnp = new Car2GoParser();
        Map<Id<CarsharingVehicleData>,CarsharingVehicleData> currentGrep = dnp.grepAndDumpOnlineDatabase("./");
        for (CarsharingVehicleData cv : currentGrep.values()){
            System.out.println(cv.toString());
        }
    }
    

    public Map<Id<CarsharingVehicleData>,CarsharingVehicleData> grepAndDumpOnlineDatabase(String outputfolder)
    
{
    JSONParser jp = new JSONParser();
    URL url;
    Map<Id<CarsharingVehicleData>,CarsharingVehicleData> currentGrep = new HashMap<>();
    
    
    try {
    	System.setProperty("https.protocols", "TLSv1");
//    	url = new URL("https", "www.car2go.com", 443, "/api/v2.1/vehicles?loc=berlin&oauth_consumer_key=BerlinMultimodal&format=json");
        url = new URL("https://www.car2go.com/api/v2.1/vehicles?loc=berlin&oauth_consumer_key=BerlinMultimodal&format=json");
        
        BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
        JSONObject jsonObject = (JSONObject)jp.parse(in);
 
        
        JSONArray msg = (JSONArray) jsonObject.get("placemarks");
        Iterator<JSONObject> iterator = msg.iterator();
        while (iterator.hasNext()) {
            JSONObject car = iterator.next();
            String vin = (String)car.get("vin");
            String license = (String)car.get("name");
            Id<CarsharingVehicleData> vid = Id.create(vin, CarsharingVehicleData.class);

            String fuel = car.get("fuel").toString();
            
            JSONArray loc = (JSONArray)car.get("coordinates");
            String longi = loc.get(0).toString();
            String lat = loc.get(1).toString();
            currentGrep.put(vid, new CarsharingVehicleData(vid, license, lat, longi,"0" , fuel, "CG"));
                        
        }
        BufferedWriter bw = IOUtils.getBufferedWriter(outputfolder+"c2g_"+System.currentTimeMillis()+".json.gz");
        bw.write(jsonObject.toString());
        bw.flush();
        bw.close();
        
    }
    catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
    catch (ParseException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
    
    return currentGrep;
    
}



}

    

