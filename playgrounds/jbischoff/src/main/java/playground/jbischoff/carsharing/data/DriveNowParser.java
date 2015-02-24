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
public class DriveNowParser
{

    public static void main(String[] args)
    {
        DriveNowParser dnp = new DriveNowParser();
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
        url = new URL("https://www.drive-now.com/php/metropolis/json.vehicle_filter?cit=6099");
        BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
        JSONObject jsonObject = (JSONObject)jp.parse(in);
        JSONObject rec = (JSONObject)jsonObject.get("rec");    
        JSONObject veh = (JSONObject)rec.get("vehicles");    
        
        JSONArray msg = (JSONArray) veh.get("vehicles");
        Iterator<JSONObject> iterator = msg.iterator();
        while (iterator.hasNext()) {
            JSONObject car = iterator.next();
            String vin = (String)car.get("vin");
            String license = ((String)car.get("licensePlate")).replace(" ", "");
                      
            Id<CarsharingVehicleData> vid = Id.create(vin, CarsharingVehicleData.class);
            String mileage = car.get("mileage").toString();
            String fuel = car.get("fuelState").toString();
            
            JSONObject loc = (JSONObject)car.get("position");
            String latitude = loc.get("latitude").toString();
            String longitude = loc.get("longitude").toString();
            currentGrep.put(vid, new CarsharingVehicleData(vid,license, latitude, longitude, mileage, fuel, "DN"));
            
        }
        BufferedWriter bw = IOUtils.getBufferedWriter(outputfolder+"dn_"+System.currentTimeMillis()+".json.gz");
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

class CarsharingVehicleData {
    private Id<CarsharingVehicleData> vid;
    private Coord location;
    private long mileage;
    private double fuel;
    private long time;
    private String provider;
    private String license;
    
    CarsharingVehicleData(Id<CarsharingVehicleData> vid, String license, String lati, String longi, String mileage, String fuel, String provider){
        this.vid = vid;
        this.license = license;
        this.location = new CoordImpl(lati,longi);
        this.mileage = Long.parseLong(mileage);
        this.fuel = Double.parseDouble(fuel);
        this.time = System.currentTimeMillis();
        this.provider = provider;
    }

    public Id<CarsharingVehicleData> getVid()
    {
        return vid;
    }

    public Coord getLocation()
    {
        return location;
    }

    public long getMileage()
    {
        return mileage;
    }

    public double getFuel()
    {
        return fuel;
    }
    public long getTime()
    {
        return time;
    }
    
    public String getProvider() {
		return provider;
	}

	public String getLicense() {
		return license;
	}

	@Override
    public String toString()
    {
        return (license+"\t"+vid+"\t"+location+"\t"+mileage);
    }
    
}
