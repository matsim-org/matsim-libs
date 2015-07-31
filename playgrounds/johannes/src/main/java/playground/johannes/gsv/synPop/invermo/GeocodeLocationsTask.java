/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.synPop.invermo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import playground.johannes.synpop.data.Element;
import playground.johannes.gsv.synPop.ProxyPerson;
import playground.johannes.gsv.synPop.ProxyPersonTask;
import playground.johannes.gsv.synPop.ProxyPlan;
import playground.johannes.socialnetworks.survey.ivt2009.util.GoogleGeoCoder;

import com.google.code.geocoder.model.LatLng;

/**
 * @author johannes
 * 
 */
public class GeocodeLocationsTask implements ProxyPersonTask {

	private final GoogleGeoCoder geoCoder;

	private Map<String, String> cache = new HashMap<String, String>();

	private String cacheFile;

	public GeocodeLocationsTask() {
		geoCoder = new GoogleGeoCoder();
	}

	public GeocodeLocationsTask(String host, int port) {
		geoCoder = new GoogleGeoCoder(host, port, 50);
	}

	public void setCacheFile(String file) {
		this.cacheFile = file;
		try {
			File theFile = new File(file);
			if (theFile.exists()) {
				BufferedReader reader = new BufferedReader(new FileReader(file));
				String line;
				while ((line = reader.readLine()) != null) {
					String tokens[] = line.split("\t", -1);
					cache.put(tokens[0], tokens[1]);
				}

				reader.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void apply(ProxyPerson person) {
		geocodeField(InvermoKeys.HOME_LOCATION, InvermoKeys.HOME_COORD, person);
		for (ProxyPlan plan : person.getPlans()) {
			for (Element act : plan.getActivities()) {
				geocodeField("location", "coord", act);
			}
		}

	}

	private void geocodeField(String key1, String key2, Element obj) {
		String start = obj.getAttribute(key1);
		if (start != null && !start.equalsIgnoreCase("home") && !start.equalsIgnoreCase("work") && !start.equalsIgnoreCase("prev")) {
			String str = cache.get(start);
//			if (str == null) {
//				LatLng coord = geoCoder.requestCoordinate(start);
//				if (coord != null) {
//					str = makeCoodinateString(coord);
//					cache.put(start, str);
//				}
//			}
			obj.setAttribute(key2, str);
		}
	}

	private String makeCoodinateString(LatLng coord) {
		StringBuilder builder = new StringBuilder(100);
		builder.append(String.valueOf(coord.getLng()));
		builder.append(",");
		builder.append(String.valueOf(coord.getLat()));

		return builder.toString();
	}

	public void writeCache() {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(cacheFile));
			for (Entry<String, String> entry : cache.entrySet()) {
				writer.write(entry.getKey());
				writer.write("\t");
				writer.write(entry.getValue());
				writer.newLine();
			}

			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
