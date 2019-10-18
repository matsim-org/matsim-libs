/* *********************************************************************** *
 * project: org.matsim.*
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
package playground.vsp.analysis.modules.transitScheduleAnalyser;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.TransitScheduleReaderV1;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;


/**
 * @author mkillat
 */
public class TransitScheduleAnalyserToCSVandTEX {
	
	private static final Logger log = Logger.getLogger(TransitScheduleAnalyserToCSVandTEX.class);
	
	public static void transitScheduleAnalyser (TransitSchedule ts, Network net, String outputDirectory){
		
		Date date = new Date();
		String filenameTEX = outputDirectory + "\\transitScheduleAnalyser_" + date.getHours() + "_" + date.getMinutes() +  ".tex";
		String filenameCSV = outputDirectory + "\\transitScheduleAnalyser_" + date.getHours() + "_" + date.getMinutes() +  ".csv";
		String filenameTexForAutomaticAnalyzer = outputDirectory + "\\transitScheduleAnalyser.tex";
		
		List <TransitScheduleAnalyserElement> allInformations = new ArrayList<TransitScheduleAnalyserElement>();
		
		Map <Id<TransitLine>, TransitLine> transitLinesMap = ts.getTransitLines();
		
		for (Entry <Id<TransitLine>, TransitLine> lineEntry : transitLinesMap.entrySet()) { 
	
			
		String lineId = String.valueOf(lineEntry.getKey());
		String routeId;
			Map <Id<TransitRoute>, TransitRoute> transitRoutesMap = transitLinesMap.get(lineEntry.getKey()).getRoutes();
			for (Entry <Id<TransitRoute>, TransitRoute> routeEntry : transitRoutesMap.entrySet()){
				
				TransitRoute route = transitRoutesMap.get(routeEntry.getKey());
				routeId = String.valueOf(route.getId());

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//				Get Departures
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
				Map <Id<Departure>, Departure> departuresMap = route.getDepartures();
				String firstDeparture;
				String lastDeparture;
				List <Double> departureTimes = new ArrayList<Double>();
				for (Entry <Id<Departure>, Departure> departuresEntry : departuresMap.entrySet()){
					departureTimes.add(departuresEntry.getValue().getDepartureTime());
				}
				Collections.sort(departureTimes);
				firstDeparture = Time.writeTime(departureTimes.get(0));
				lastDeparture = Time.writeTime(departureTimes.get(departureTimes.size()-1));
				

				
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////	
//				Get tourlength time
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
				List<TransitRouteStop> transitstopsList = route.getStops();
				Double tourlengthTimeDouble = transitstopsList.get(transitstopsList.size()-1).getArrivalOffset()- transitstopsList.get(0).getDepartureOffset();
				String tourlengthTime = Time.writeTime(tourlengthTimeDouble);
				
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////	
//				Get tourlength distance
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
				double routeLength=0.0;
				for (int i = 0; i < transitstopsList.size(); i++) {
					routeLength = routeLength + net.getLinks().get(transitstopsList.get(i).getStopFacility().getLinkId()).getLength();
				}
				routeLength = routeLength/1000;
				
				String tourLengthDistance = String.valueOf(routeLength);
				
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////	
//				number of Vehicles
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////	
				String numberOfVehicles;
				Map <Id, String> vehiclesIdMap = new HashMap<Id, String>();
				for (Entry <Id<Departure>, Departure> departuresEntry : departuresMap.entrySet()){
					vehiclesIdMap.put(departuresMap.get(departuresEntry.getKey()).getVehicleId(), "bla" );
				}
				int vehicleMapSize = vehiclesIdMap.size();
				numberOfVehicles = String.valueOf(vehicleMapSize);
				
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////	
//				average headway
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////	
				String averageHeadway;
				Collections.reverse(departureTimes);
				Double sum = 0.0;
				for (int i = 0; i < departureTimes.size(); i++) {
					if (i!=departureTimes.size()-1){
						sum = sum + departureTimes.get(i) - departureTimes.get(i+1);
					}
				}
				double averageHeadwayDouble = sum/(departureTimes.size()-1);
				averageHeadway = String.valueOf(Time.writeTime(averageHeadwayDouble));

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////	
//				printing the results
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////				
				
				TransitScheduleAnalyserElement temp = new TransitScheduleAnalyserElement(lineId, routeId, firstDeparture, lastDeparture, tourlengthTime, tourLengthDistance, averageHeadway, numberOfVehicles);
				
				allInformations.add(temp);

//				for *.csv File
			}}

				try {
					BufferedWriter writer = new BufferedWriter(new FileWriter(new File(filenameCSV),false));
						writer.write("# lineID; routeID; first Departure; last Departure; tourlength time; tourlength distance; average headway; number of vehicles" );
						writer.newLine();
						for (int i = 0; i < allInformations.size(); i++) {
							writer.write(allInformations.get(i).lineId + "; " + allInformations.get(i).routeId + "; " + allInformations.get(i).firstDeparture + "; " + allInformations.get(i).lastDeparture + "; " + allInformations.get(i).tourLengthTime + "; " + allInformations.get(i).tourLengthDistance + "; " +   allInformations.get(i).averageHeadway + "; " + allInformations.get(i).numberOfVehicles);
							writer.newLine();
						}
	
					
					
					writer.newLine();
					writer.flush();
					writer.close();	
				}catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
//				for *.tex File
				
				try {
					BufferedWriter writer = new BufferedWriter(new FileWriter(new File(filenameTEX),false));
						writer.write("\\documentclass[a4paper, 12pt]{article}" );
						writer.newLine();
						writer.write("\\usepackage[utf8]{inputenc}" );
						writer.newLine();
						writer.write("\\usepackage{booktabs}" );
						writer.newLine();
						writer.write("\\usepackage{textcomp}" );
						writer.newLine();
						writer.write("\\usepackage{array}" );
						writer.newLine();
						writer.write("\\usepackage{longtable}" );
						writer.newLine();
						writer.write("\\begin{document}" );
						writer.newLine();
						writer.newLine();
						writer.write("\\begin{longtable}{>{\\raggedleft}p{15mm} >{\\raggedleft}p{15mm} >{\\raggedleft}p{18mm} >{\\raggedleft}p{18mm} >{\\raggedleft}p{18mm} >{\\raggedleft}p{10mm} >{\\raggedleft}p{15mm} >{\\raggedleft}p{10mm}}" );
						writer.newLine();
						writer.write("\\caption{\\label{tab:LABEL}Results of Transitschedule Analyser} \\tabularnewline \\addlinespace[1.0em]" );
						writer.newLine();
						writer.write("\\toprule" );
						writer.newLine();
						writer.write("lineID & routeID & first Departure &  last Departure &  tourlength time & tourlength distance &  average headway &  number of vehicles \\tabularnewline" );
						writer.newLine();
						writer.write("\\midrule \\endhead" );
						writer.newLine();
						writer.newLine();
						writer.write("\\caption{(continued)} \\tabularnewline \\addlinespace[1.0em]" );
						writer.newLine();
						writer.write("\\toprule" );
						writer.newLine();
						writer.write(" lineID & routeID & first Departure &  last Departure &  tourlength time & tourlength distance &  average headway &  number of vehicles \\tabularnewline" );
						writer.newLine();
						writer.write("\\midrule \\endhead" );
						writer.newLine();
						writer.newLine();
						
						for (int i = 0; i < allInformations.size(); i++) {
							String line = allInformations.get(i).lineId;
							line = line.replace("_", "\\_");
							String route = allInformations.get(i).routeId;
							route = route.replace("_", "\\_");
							writer.write(line + " & " + route + " & " + allInformations.get(i).firstDeparture + " & " + allInformations.get(i).lastDeparture + " & " + allInformations.get(i).tourLengthTime + " & " + allInformations.get(i).tourLengthDistance +  " & " +  allInformations.get(i).averageHeadway + " & " + allInformations.get(i).numberOfVehicles + " \\tabularnewline "  );
							writer.newLine();
						}
						writer.newLine();
						writer.write("% \\tabularnewline");
						writer.newLine();
						writer.write("%\\bottomrule");
						writer.newLine();
						writer.write("\\end{longtable}");
						writer.newLine();
						writer.newLine();
						writer.write("\\end{document}");
						writer.newLine();
						writer.flush();
						writer.close();
					
					
					
					

				}catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				/*
				 * .tex for Automatic analyzer:
				 * without begin{document} and usepackage declarations
				 * added by gleich
				 */
				
				try {
					BufferedWriter writer = new BufferedWriter(new FileWriter(new File(filenameTexForAutomaticAnalyzer),false));
						writer.write("\\begin{longtable}{>{\\raggedleft}p{15mm} >{\\raggedleft}p{15mm} >{\\raggedleft}p{18mm} >{\\raggedleft}p{18mm} >{\\raggedleft}p{18mm} >{\\raggedleft}p{10mm} >{\\raggedleft}p{15mm} >{\\raggedleft}p{10mm}}" );
						writer.newLine();
						writer.write("\\caption{\\label{tab:LABEL}Results of Transitschedule Analyser} \\tabularnewline \\addlinespace[1.0em]" );
						writer.newLine();
						writer.write("\\toprule" );
						writer.newLine();
						writer.write("lineID & routeID & first Departure &  last Departure &  tourlength time & tourlength distance &  average headway &  number of vehicles \\tabularnewline" );
						writer.newLine();
						writer.write("\\midrule \\endhead" );
						writer.newLine();
						writer.newLine();
						writer.write("\\caption{(continued)} \\tabularnewline \\addlinespace[1.0em]" );
						writer.newLine();
						writer.write("\\toprule" );
						writer.newLine();
						writer.write(" lineID & routeID & first Departure &  last Departure &  tourlength time & tourlength distance &  average headway &  number of vehicles \\tabularnewline" );
						writer.newLine();
						writer.write("\\midrule \\endhead" );
						writer.newLine();
						writer.newLine();
						
						for (int i = 0; i < allInformations.size(); i++) {
							String line = allInformations.get(i).lineId;
							line = line.replace("_", "\\_");
							String route = allInformations.get(i).routeId;
							route = route.replace("_", "\\_");
							writer.write(line + " & " + route + " & " + allInformations.get(i).firstDeparture + " & " + allInformations.get(i).lastDeparture + " & " + allInformations.get(i).tourLengthTime + " & " + allInformations.get(i).tourLengthDistance +  " & " +  allInformations.get(i).averageHeadway + " & " + allInformations.get(i).numberOfVehicles + " \\tabularnewline "  );
							writer.newLine();
						}
						writer.newLine();
						writer.write("% \\tabularnewline");
						writer.newLine();
						writer.write("%\\bottomrule");
						writer.newLine();
						writer.write("\\end{longtable}");
						writer.newLine();
						writer.flush();
						writer.close();
					
					
					
					

				}catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
		log.info("Die Datei wurde nach " + filenameTEX + " geschrieben.");
		log.info("Die Datei wurde nach " + filenameCSV + " geschrieben.");
		log.info("Die Datei wurde nach " + filenameTexForAutomaticAnalyzer + " geschrieben.");
		
		}
	
	public static void main(String[] args) {
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		sc.getConfig().transit().setUseTransit(true);
		(new MatsimNetworkReader(sc.getNetwork())).readFile("F:/temp/network.final.xml.gz");
		(new TransitScheduleReaderV1(sc)).readFile("F:/temp/transitSchedule_basecase.xml.gz");
		
		TransitScheduleAnalyserToCSVandTEX.transitScheduleAnalyser(sc.getTransitSchedule(), sc.getNetwork(), "F:/temp");
	}
}