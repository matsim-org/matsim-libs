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
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.IOUtils;
/**
 * @author jbischoff
 * this class requests travel times and distances between two coordinates from HERE Maps. 
 * Please replace the API code with your own and obey Nokia's usage policy
 *
 */
public class HereMapsRouteGrepper {

	private long distance = 0;
	private long baseTime = 0;
	private long travelTime = 0;
	private Coord from;
	private Coord to;
	private boolean secondround = false;
	private String filename;
	
public static void main(String[] args)  {
	// main class exists for testing purposes only
	Coord c = new CoordImpl(52.519580833333,13.359681944444);
	Coord d = new CoordImpl(52.470137777778,13.335396944444);
	System.out.println(System.currentTimeMillis());
	HereMapsRouteGrepper gr = new HereMapsRouteGrepper(c, d,"testHere.json.gz");

}	

public HereMapsRouteGrepper(Coord from, Coord to, String filename)  {
	this.from = from;
	this.to = to;
	this.filename = filename;
	calculate();
}

private void calculate(){
	Locale locale  = new Locale("en", "UK");
	String pattern = "###.#####";

	DecimalFormat df = (DecimalFormat)    NumberFormat.getNumberInstance(locale);
	df.applyPattern(pattern);
	
	String urlString = "http://route.cit.api.here.com/routing/7.2/calculateroute.json?app_id=JGeDcl8WDVcBZO7ymAhm"
			+ "&app_code=ZeWjVxSBgD8VEodu0X7Wmg"
			+ "&waypoint0=geo!"+df.format(from.getX())+","+df.format(from.getY())
			+ "&waypoint1=geo!"+df.format(to.getX())+","+df.format(to.getY())
			+ "&mode=fastest;car;traffic:enabled";
			
	try {
		System.out.println(urlString);
		URL url = new URL(urlString);
        BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
        JSONParser jp = new JSONParser();

        JSONObject jsonObject = (JSONObject)jp.parse(in);
        JSONObject route = (JSONObject) ((JSONArray) ((JSONObject)jsonObject.get("response") ).get("route")).get(0);
        JSONObject summary = (JSONObject) route.get("summary");
        travelTime =  (long) summary.get("travelTime");
        baseTime = (long) summary.get("baseTime");
        distance = (long) summary.get("distance");
        
        System.out.println(travelTime + " "+ baseTime + " "+distance);
        
        BufferedWriter bw = IOUtils.getBufferedWriter(filename);
        bw.write(jsonObject.toString());
        bw.flush();
        bw.close();
	} catch (MalformedURLException e) {
		exceptionHandler(e);
	} catch (IOException e) {
		exceptionHandler(e);
	} catch (ParseException e) {
		exceptionHandler(e);
	}
	
}
private void exceptionHandler (Exception e){
	if (travelTime == 0) travelTime = -1; 
	if (baseTime == 0) baseTime = -1; 
	if (distance == 0) distance = -1; 
	if (!secondround){
		try {
			Thread.sleep(2000);
			secondround = true;
			this.calculate();
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	
}

public long getDistance() {
	return distance;
}

public long getBaseTime() {
	return baseTime;
}

public long getTravelTime() {
	return travelTime;
}


}
