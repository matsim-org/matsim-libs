package org.matsim.contrib.analysis.vsp.traveltimedistance;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class ToBeDeleted {

	public static void main(String args[]) throws IOException, ParseException {
		System.out.println("Hello!");
		String urlString = "https://router.hereapi.com/v8/routes?&apiKey=vZ18hGtaNhxKFdLO5gxQpx4L9qKu8Hw8QhhJTsidEI4&transportmode=car&origin=51.33352,6.97941&destination=51.52072,6.92727&departureTime=2020-10-27T17:51:00&return=summary";

		try {
			URL url = new URL(urlString);
			BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
			JSONParser jp = new JSONParser();

			JSONObject jsonObject = (JSONObject) jp.parse(in);

			JSONArray routes = (JSONArray) jsonObject.get("routes");
			JSONObject route = (JSONObject) routes.get(0);
			JSONArray sections = (JSONArray) route.get("sections");
			JSONObject section = (JSONObject) sections.get(0);
			JSONObject summary = (JSONObject) section.get("summary");

			System.out.println("summary is: " + summary);
			long travelTime = (long) summary.get("duration");
			long distance = (long) summary.get("length");

			System.out.println("travel time is: " + travelTime);
			System.out.println("travel distance is " + distance);

		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
