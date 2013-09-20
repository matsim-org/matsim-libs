/* *********************************************************************** *
 * project: org.matsim.*
 * DgFlightModalSplitAnalysis
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package air.scripts;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;

import air.analysis.DgFlightModalSplitEventHandler;


/**
 * @author dgrether
 *
 */
public class DgFlightModalSplitAnalysis {

	public static void main(String[] args) {
		String[] runNumbers ={
				//			"1836", 
				//			"1837",
				//			"1838",
				//			"1839",
				//			"1840",
				//			"1841"	
				//			"1848",
				//			"1849",
				//			"1850",
				//			"1851",
				//			"1852",
				//			"1853",

				//		"1854",
				//		"1855",
				//		"1856",
				//		"1857",
				//		"1858",
				//		"1859",
				//		
//				"1860",
//				"1861",
//				"1862",
//				"1863",
//				"1864"
				
				"1871",
				"1872",
				"1873",
				"1874",
				"1875"
		};
		String iteration = "600";
		List<String> results = new ArrayList<String>();

		for (String runNumber : runNumbers) {
			String events = "/home/dgrether/data/work/repos/runs-svn/run"+runNumber+"/ITERS/it."+iteration+"/"+runNumber+"."+iteration+".events.xml.gz";

			EventsManager eventsManager = EventsUtils.createEventsManager();
			DgFlightModalSplitEventHandler handler = new DgFlightModalSplitEventHandler();
			eventsManager.addHandler(handler);
			MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
			reader.readFile(events);
			int totalTrips = handler.getNumberOfPtTrips() + handler.getNumberOfTrainTrips() + handler.getNumberOfStuckTrips();
			
			String result = runNumber + " & " + iteration + 
					" & " + Integer.toString(handler.getNumberOfPtTrips()) + 
					" & " + Integer.toString(handler.getNumberOfTrainTrips()) + 
					" & " + Integer.toString(handler.getNumberOfStuckTrips()) + 
					" & " + Integer.toString(totalTrips) + 
					" & " + getPercent(handler.getNumberOfPtTrips(), totalTrips) +
					" & " + getPercent(handler.getNumberOfTrainTrips(), totalTrips) + 
					" & " + getPercent(handler.getNumberOfStuckTrips(), totalTrips) + 
					"\\\\";
			System.out.println(result);
			results.add(result);
		}

		System.out.println("runNumber & iteration & \\# pt trips  & \\# train & \\# stuck & \\# total & pt trips [\\%]  & train [\\%] & stuck [\\%] \\\\");
		for (String s : results){
			System.out.println(s);
		}
	}

	private static String getPercent(double count, double total) {
		return getStringFromDouble(count/total * 100.0, 2);
	}
	
	private static String getStringFromDouble(double d, int decimalPlaces){
		String pattern = "#00.";
		for (int i = 0; i  < decimalPlaces; i++) pattern+= "0";
		NumberFormat f = NumberFormat.getInstance();
		 if (f instanceof DecimalFormat) {
		     ((DecimalFormat) f).setRoundingMode(RoundingMode.HALF_UP);
		     ((DecimalFormat) f).applyPattern(pattern);
		 }
		 return f.format(d);
	}
}
