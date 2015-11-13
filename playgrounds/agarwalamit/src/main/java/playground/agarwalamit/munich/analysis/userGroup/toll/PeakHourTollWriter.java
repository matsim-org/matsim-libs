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
package playground.agarwalamit.munich.analysis.userGroup.toll;

import java.io.BufferedWriter;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.munich.utils.ExtendedPersonFilter;
import playground.benjamin.scenarios.munich.analysis.filter.UserGroup;

/**
 * @author amit
 */

public class PeakHourTollWriter {

	private PeakHourTollHandler pkHrToll;
	private final ExtendedPersonFilter pf = new ExtendedPersonFilter();

	public static void main(String[] args) {
		String [] pricingSchemes = new String [] {"ei","ci","eci"};

		for (String s :pricingSchemes) {
			new PeakHourTollWriter().run(s);
		}
	}

	public void run ( String pricingScheme) {
		String scenario = pricingScheme;
		String eventsFile = "../../../../repos/runs-svn/detEval/emissionCongestionInternalization/iatbr/output/"+scenario+"/ITERS/it.1500/1500.events.xml.gz";
		String outputFolder = "../../../../repos/runs-svn/detEval/emissionCongestionInternalization/iatbr/output/analysis/";

		EventsManager events = EventsUtils.createEventsManager();
		MatsimEventsReader reader = new MatsimEventsReader(events);
		pkHrToll = new PeakHourTollHandler();
		events.addHandler(pkHrToll);
		reader.readFile(eventsFile);

		writeRData(outputFolder, scenario);
		writeTollPerTrip(outputFolder, scenario);

	}

	private void writeTollPerTrip(String outputFolder, String pricingScheme) {
		SortedMap<UserGroup,Integer> pkHrTripCount = pkHrToll.getUserGroupToPeakHourTripCounts();
		SortedMap<UserGroup,Double> pkHrTotalToll = pkHrToll.getUserGroupToTotalPeakHourToll();

		SortedMap<UserGroup,Integer> offPkHrTripCount = pkHrToll.getUserGroupToOffPeakHourTripCounts();
		SortedMap<UserGroup,Double> offPkHrTotalToll = pkHrToll.getUserGroupToTotalOffPeakHourToll();


		BufferedWriter writer = IOUtils.getBufferedWriter(outputFolder+"/averageTollPerTrip_"+pricingScheme+".txt");
		try {
			writer.write("PricingScheme \t time \t userGroup \t averageTollPerTrip \n");
			writer.write(pricingScheme+"\t peakHours \t Urban \t"+pkHrTotalToll.get(UserGroup.URBAN)/pkHrTripCount.get(UserGroup.URBAN) + "\n");
			writer.write(pricingScheme+"\t peakHours \t (Rev)commuter \t"+ ( pkHrTotalToll.get(UserGroup.COMMUTER) + pkHrTotalToll.get(UserGroup.REV_COMMUTER) ) /
					( pkHrTripCount.get(UserGroup.COMMUTER) + pkHrTripCount.get(UserGroup.REV_COMMUTER) ) +"\n" );
			writer.write(pricingScheme+"\t peakHours \t Freight \t"+pkHrTotalToll.get(UserGroup.FREIGHT)/pkHrTripCount.get(UserGroup.FREIGHT) +"\n");

			writer.write(pricingScheme+"\t offPeakHours \t Urban \t"+offPkHrTotalToll.get(UserGroup.URBAN)/offPkHrTripCount.get(UserGroup.URBAN) + "\n");
			writer.write(pricingScheme+"\t offPeakHours \t (Rev)commuter \t"+ ( offPkHrTotalToll.get(UserGroup.COMMUTER) + offPkHrTotalToll.get(UserGroup.REV_COMMUTER) ) /
					( offPkHrTripCount.get(UserGroup.COMMUTER) + offPkHrTripCount.get(UserGroup.REV_COMMUTER) ) +"\n" );
			writer.write(pricingScheme+"\t offPeakHours \t Freight \t"+offPkHrTotalToll.get(UserGroup.FREIGHT)/offPkHrTripCount.get(UserGroup.FREIGHT) +"\n");
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written in file. Reason: " + e);
		}
	}
	
	private void writeRData(String outputFolder, String pricingScheme) {
		if( ! new File(outputFolder+"/boxPlot/").exists()) new File(outputFolder+"/boxPlot/").mkdirs();

		SortedMap<UserGroup, Map<Id<Person>,List<Double>>> userGrpTo_PkHrToll = pkHrToll.getUserGrpTo_PkHrToll();
		SortedMap<UserGroup, Map<Id<Person>,List<Double>>> userGrpTo_OffPkHrToll = pkHrToll.getUserGrpTo_OffPkHrToll();
		
		//write peak hour toll/trip
		BufferedWriter writer = IOUtils.getBufferedWriter(outputFolder+"/boxPlot/toll_"+pricingScheme+"_pkHr"+".txt");
		try {
			for( UserGroup ug : userGrpTo_PkHrToll.keySet() ) {
				for(Id<Person> p : userGrpTo_PkHrToll.get(ug).keySet()){
					for(double d: userGrpTo_PkHrToll.get(ug).get(p)){
						writer.write(pricingScheme.toUpperCase()+"\t"+ pf.geUserGroupMyWay(ug)+"\t"+d+"\n");
					}
				}
			}
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written in file. Reason: " + e);
		}

		//write off peak hour toll/trip
		writer = IOUtils.getBufferedWriter(outputFolder+"/boxPlot/toll_"+pricingScheme+"_offPkHr"+".txt");
		try {
			for( UserGroup ug : userGrpTo_OffPkHrToll.keySet() ) {
				for(Id<Person> p :userGrpTo_OffPkHrToll.get(ug).keySet()){
					for(double d: userGrpTo_OffPkHrToll.get(ug).get(p)){
						writer.write(pricingScheme.toUpperCase()+"\t"+ pf.geUserGroupMyWay(ug)+"\t"+d+"\n");
					}
				}
			}
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written in file. Reason: " + e);
		}
	}
}
