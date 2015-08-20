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

package playground.johannes.gsv.synPop.mid2002;

import java.io.IOException;
import java.util.Map;

import playground.johannes.synpop.source.mid2008.generator.RowHandler;
import playground.johannes.sna.math.DescriptivePiStatistics;
import playground.johannes.socialnetworks.snowball2.analysis.WSMStatsFactory;

/**
 * @author johannes
 *
 */
public class AvrTripDist {

	private static DescriptivePiStatistics stats;
	
	public static void main(String[] args) throws IOException {
		stats = new WSMStatsFactory().newInstance();
		
		new Handler().read("/home/johannes/gsv/mid2002/wege.txt");
		
		System.out.println(String.format("Average trip length > 100 KM: %s", stats.getMean()));

	}

	private static class Handler extends RowHandler {

		@Override
		protected void handleRow(Map<String, String> attributes) {
			String dist = attributes.get("W08");
			String weight = attributes.get("GEW_WB");
			
			if(dist != null && weight != null) {
				double d = Double.parseDouble(dist);
				double w = Double.parseDouble(weight);
				
				if(d > 100 && d < 1000) {
					stats.addValue(d, 1/w);
				}
			}
			
		}
		
	}
}
