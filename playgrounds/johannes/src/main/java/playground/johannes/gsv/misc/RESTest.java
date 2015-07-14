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

/**
 * 
 */
package playground.johannes.gsv.misc;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;

import playground.johannes.gsv.analysis.RailCounts;
import playground.johannes.gsv.analysis.TransitLineAttributes;
import playground.johannes.gsv.visum.LineRouteCountsHandler;
import playground.johannes.gsv.visum.LineRouteCountsHandler.CountsContainer;
import playground.johannes.gsv.visum.NetFileReader;
import playground.johannes.gsv.visum.NetFileReader.TableHandler;

/**
 * @author johannes
 *
 */
public class RESTest {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		Map<String, TableHandler> handlers = new HashMap<String, NetFileReader.TableHandler>();
		Handler handler = new Handler();
		
		handler.attribs = TransitLineAttributes.createFromFile("/home/johannes/gsv/matsim/studies/netz2030/data/transitLineAttributes.xml");
		
		handlers.put("FAHRPLANFAHRT", handler);
		NetFileReader netReader = new NetFileReader(handlers);
		netReader.read("/home/johannes/gsv/matsim/studies/netz2030/data/raw/network.net");
		
		Config config = ConfigUtils.createConfig();
		config.transit().setUseTransit(true);
		Scenario scenario = ScenarioUtils.createScenario(config);
		
		MatsimNetworkReader reader = new MatsimNetworkReader(scenario);
		reader.readFile("/home/johannes/gsv/matsim/studies/netz2030/data/network.gk3.xml");
		
		Map<String, TableHandler> tableHandlers = new HashMap<String, NetFileReader.TableHandler>();
		LineRouteCountsHandler countsHandler = new LineRouteCountsHandler(scenario.getNetwork());
		tableHandlers.put("LINIENROUTENELEMENT", countsHandler);
		netReader = new NetFileReader(tableHandlers);
		NetFileReader.FIELD_SEPARATOR = "\t";
		netReader.read("/home/johannes/gsv/matsim/studies/netz2030/data/raw/counts.att");
		
		TransitScheduleReader scheduleReader = new TransitScheduleReader(scenario);
		scheduleReader.readFile("/home/johannes/gsv/matsim/studies/netz2030/data/transitSchedule.gk3.xml");
		TransitSchedule schedule = scenario.getTransitSchedule();
		
		int match = 0;
		int total = 0;
		Set<String> unmatchedTrains = new HashSet<String>();
		Set<String> matchedTrains = new HashSet<String>();
		
		RailCounts railCounts = new RailCounts(handler.attribs);
		
		for(CountsContainer container : countsHandler.getCounts2()) {
			String trainId = container.trainId;
			
			String[] tokens = trainId.split("_");
			trainId = tokens[1];
			
			Tuple<String, String> tuple = handler.train2LineRoute.get(trainId);
			if(tuple != null) {
				Link link = scenario.getNetwork().getLinks().get(Id.create(container.linkeId, Link.class));
				TransitLine line = schedule.getTransitLines().get(Id.create(tuple.getSecond(), TransitLine.class));
				railCounts.addCounts(link.getId(), line.getId(), container.count);
				match++;
				matchedTrains.add(trainId);
			} else {
				unmatchedTrains.add(trainId);
			}
			total++;
		}
		
		System.out.println(String.format("Matched %s of %s counts", match, total));
		System.out.println(matchedTrains.size() + " matched trains");
		System.out.println(unmatchedTrains.size() + " unmatched trains");
//		for(String train : unmatchedTrains) {
//			System.out.println(train);
//		}
		
		railCounts.writeToFile("/home/johannes/gsv/matsim/studies/netz2030/data/railCounts.xml");
	}
	
	private static class Handler extends TableHandler {

		private static final String NAME = "NAME";
		
		private static final String LINE_ROUTE = "LINROUTENAME";
		
		private static final String LINE = "LINNAME";
		
		Map<String,Tuple<String,String>> train2LineRoute = new HashMap<String, Tuple<String, String>>();
		
		private TransitLineAttributes attribs;
		
		/* (non-Javadoc)
		 * @see playground.johannes.gsv.visum.NetFileReader.TableHandler#handleRow(java.util.Map)
		 */
		@Override
		public void handleRow(Map<String, String> record) {
			String train = record.get("ZUGNR");
			String lineRoute = record.get(LINE_ROUTE);
			String line = record.get(LINE);
			
			if(train != null && lineRoute != null && line != null) {
				String tSys = attribs.getTransportSystem(line);
				boolean isDB = false;
				if(tSys != null) {
				if(tSys.equalsIgnoreCase("IC/EC/D-Tag"))
					isDB = true;
				else if(tSys.equalsIgnoreCase("RB"))
					isDB = true;
				else if(tSys.equalsIgnoreCase("RE/IRE"))
					isDB = true;
				else if(tSys.equalsIgnoreCase("ICE/ICE-T"))
					isDB = true;
				else if(tSys.equalsIgnoreCase("S-Bahn"))
					isDB = true;
				else if(tSys.equalsIgnoreCase("NZ/CNL/D-Nacht/UEx/AZ"))
					isDB = true;
				}
				if(isDB) {
					Object obj = train2LineRoute.put(train, new Tuple<String, String>(lineRoute, line));
					if(obj != null) {
						if(!train.equalsIgnoreCase("0"))
							System.err.println(String.format("Overwriting value for %s", train));
					}
				}
			} else {
				throw new RuntimeException("Field = null");
			}
		}
		
	}

}
