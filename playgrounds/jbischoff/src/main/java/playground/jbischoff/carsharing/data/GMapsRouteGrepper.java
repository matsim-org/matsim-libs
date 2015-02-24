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
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordImpl;
/**
 * @author jbischoff
 *
 */
public class GMapsRouteGrepper {

public static void main(String[] args)  {
	Coord c = new CoordImpl(52.519580833333,13.359681944444);
	Coord d = new CoordImpl(52.470137777778,13.335396944444);
	System.out.println(System.currentTimeMillis());
	GMapsRouteGrepper gr = new GMapsRouteGrepper(c, d);

}	

public GMapsRouteGrepper(Coord from, Coord to)  {
	String urlString = "http://maps.googleapis.com/maps/api/directions/json?origin="+new Double(from.getX()).toString().substring(0, 9)+","+new Double(from.getY()).toString().substring(0, 9)+"&destination="+new Double(to.getX()).toString().substring(0, 9)+","+new Double(to.getY()).toString().substring(0, 9)+"&departure_time="+System.currentTimeMillis()+"&sensor=false";
	try {
		System.out.println(urlString);
		URL url = new URL(urlString);
        BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
        JSONParser jp = new JSONParser();

        JSONObject jsonObject = (JSONObject)jp.parse(in);
        JSONArray routes = (JSONArray) jsonObject.get("routes");
        JSONObject route = (JSONObject) routes.get(0);
        JSONArray legs = (JSONArray) route.get("legs");
        JSONObject leg = (JSONObject) legs.get(0);
        JSONObject duration = (JSONObject) leg.get("duration");
        JSONObject distance = (JSONObject) leg.get("distance");
        long d = (long) duration.get("value");
        long di = (long) distance.get("value");
        System.out.println(d);
        
        System.out.println(di);
	} catch (MalformedURLException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (ParseException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	
}

}
