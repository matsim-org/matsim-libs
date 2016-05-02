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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.analysis.Toll.TripTollHandler;
import playground.agarwalamit.analysis.trip.TripDistanceHandler;
import playground.agarwalamit.munich.utils.ExtendedPersonFilter;
import playground.agarwalamit.utils.ListUtils;
import playground.agarwalamit.utils.LoadMyScenarios;
import playground.benjamin.scenarios.munich.analysis.filter.UserGroup;

/**
 * @author amit
 */

public class HourlyTripTollPerKmWriter {
	
	private final TripTollHandler tollHandler ;
	private final TripDistanceHandler distHandler;
	private final ExtendedPersonFilter pf = new ExtendedPersonFilter();
	
	public HourlyTripTollPerKmWriter(final Network network, final double simulationEndTime, final int noOfTimeBins) {
		this.tollHandler = new TripTollHandler( simulationEndTime, noOfTimeBins );
		this.distHandler = new TripDistanceHandler(network, simulationEndTime, noOfTimeBins);
	}
	
	public static void main(String[] args) {
//		String [] pricingSchemes = new String [] {"eci"};
		String [] pricingSchemes = new String [] {"ei","ci","eci"};
		for (String str :pricingSchemes) {
			String dir = "../../../../repos/runs-svn/detEval/emissionCongestionInternalization/iatbr/output/";
			String eventsFile = dir+str+"/ITERS/it.1500/1500.events.xml.gz";
//			String eventsFile = dir+str+"/ITERS/it.1500/1500.events_congestionAndMoneyEvent.xml.gz";
			String networkFile = dir+str+"/output_network.xml.gz";
			String configFile = dir+str+"/output_config.xml.gz";
			Scenario sc = LoadMyScenarios.loadScenarioFromNetworkAndConfig(networkFile, configFile);
			
			HourlyTripTollPerKmWriter tda = new HourlyTripTollPerKmWriter(sc.getNetwork(),sc.getConfig().qsim().getEndTime(), 30);
			tda.run(eventsFile);
			tda.writeUserGroupTollValuesOverTime(dir+"/analysis/", str);
		}
	}

	public void run(final String eventsFile) {
		EventsManager events = EventsUtils.createEventsManager();
		MatsimEventsReader reader = new MatsimEventsReader(events);
		events.addHandler(this.tollHandler);
		events.addHandler(this.distHandler);
		reader.readFile(eventsFile);
	}

	private void writeUserGroupTollValuesOverTime(final String outputFolder, final String pricingScheme){
		SortedMap<Double, Map<Id<Person>, List<Double>>> timebin2persontoll = tollHandler.getTimeBin2Person2TripToll();
		SortedMap<Double, Map<Id<Person>, List<Double>>> timebin2persondist = distHandler.getTimeBin2Person2TripsDistance();

		// convert above into userGroup
		SortedMap<Double,SortedMap<String, List<Double>> > userGroup2timebin2tolls = new TreeMap<>();

		//initialize
		for(double d : timebin2persontoll.keySet()){
			userGroup2timebin2tolls.put(d, new TreeMap<String, List<Double>>());
				SortedMap<String, List<Double>> time2totalToll = new TreeMap<>();
				for(UserGroup ug : UserGroup.values()){
					String userGroup = pf.getMyUserGroup(ug);
				time2totalToll.put(userGroup, new ArrayList<Double>());
			}
				userGroup2timebin2tolls.put(d, time2totalToll);
		}

		// fill map
		for(double d : timebin2persontoll.keySet()){
			SortedMap<String, List<Double>> usrGrp2tolls = userGroup2timebin2tolls.get(d); 
			for(Id<Person> p : timebin2persontoll.get(d).keySet()){
				String ug = pf.getMyUserGroupFromPersonId(p);
				List<Double> tollsInEurCt = ListUtils.scalerProduct( timebin2persontoll.get(d).get(p), 100.);
				List<Double> distInKm =  ListUtils.scalerProduct( timebin2persondist.get(d).get(p), 1./1000.);
				List<Double> tollPerKm = ListUtils.divide(tollsInEurCt, distInKm);
				usrGrp2tolls.get(ug).addAll(tollPerKm);
			}
		}

		BufferedWriter writer = IOUtils.getBufferedWriter(outputFolder+"/timeBin2TripTollPerKm_"+pricingScheme+".txt");
//		BufferedWriter writer = IOUtils.getBufferedWriter(outputFolder+"/timeBin2TripTollPerKm_"+pricingScheme+"_onlyCongestionMoneyEvents.txt");
		try {
			writer.write("pricingScheme \t userGroup \t timeBin \t totalToll_EURCtPerKm \t avgTripToll_EURCtPerKm \n");
			for(double d : userGroup2timebin2tolls.keySet()){
				for(String ug : userGroup2timebin2tolls.get(d).keySet()){
					writer.write(pricingScheme+"\t"+ug+"\t"+d+"\t"+ ListUtils.doubleSum(userGroup2timebin2tolls.get(d).get(ug)) +"\t"+ ListUtils.doubleMean(userGroup2timebin2tolls.get(d).get(ug)) +"\n");
				}
			}
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written in file. Reason: " + e);
		}
	}
}