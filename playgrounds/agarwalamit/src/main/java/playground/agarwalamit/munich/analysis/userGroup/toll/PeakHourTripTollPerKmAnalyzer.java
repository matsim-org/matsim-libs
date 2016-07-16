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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.utils.collections.Tuple;
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

public class PeakHourTripTollPerKmAnalyzer {
	
	private static final Logger LOG = Logger.getLogger(PeakHourTripTollPerKmAnalyzer.class);
	private final TripTollHandler tollHandler ;
	private final TripDistanceHandler distHandler;

	private final List<Double> pkHrs = new ArrayList<>(Arrays.asList(new Double []{8., 9., 10., 16., 17., 18.,})); // => 7-10 and 15-18
	private final ExtendedPersonFilter pf = new ExtendedPersonFilter();
	private final Map<Id<Person>,List<Double>> person2TollsPerKmPkHr = new HashMap<>();
	private final Map<Id<Person>,List<Double>> person2TollsPerKmOffPkHr = new HashMap<>();
	private final Map<Id<Person>,Integer> person2TripCountsPkHr = new HashMap<>();
	private final Map<Id<Person>,Integer> person2TripCountsOffPkHr = new HashMap<>();
	private final SortedMap<String, Tuple<Double,Double>> usrGrp2TollsPerKm = new TreeMap<>();
	private final SortedMap<String, Tuple<Integer,Integer>> usrGrp2TripCounts = new TreeMap<>();

	public PeakHourTripTollPerKmAnalyzer(final Network network, final double simulationEndTime, final int noOfTimeBins) {
		LOG.warn("Peak hours are assumed as 07:00-10:00 and 15:00-18:00 by looking on the travel demand for BAU scenario.");
		this.tollHandler = new TripTollHandler( simulationEndTime, noOfTimeBins );
		this.distHandler = new TripDistanceHandler(network, simulationEndTime, noOfTimeBins);
	} 

	public static void main(String[] args) {
		String [] pricingSchemes = new String [] {"ei","ci","eci"};
		for (String str :pricingSchemes) {
			String dir = "../../../../repos/runs-svn/detEval/emissionCongestionInternalization/iatbr/output/";
			String eventsFile = dir+str+"/ITERS/it.1500/1500.events.xml.gz";
			String networkFile = dir+str+"/output_network.xml.gz";
			String configFile = dir+str+"/output_config.xml.gz";
			Scenario sc = LoadMyScenarios.loadScenarioFromNetworkAndConfig(networkFile, configFile);
			
			PeakHourTripTollPerKmAnalyzer tda = new PeakHourTripTollPerKmAnalyzer(sc.getNetwork(),sc.getConfig().qsim().getEndTime(), 30);
			tda.run(eventsFile);
			tda.writeRBoxPlotData(dir+"/analysis/", str);
		}
	}

	public void run(final String eventsFile) {
		EventsManager events = EventsUtils.createEventsManager();
		MatsimEventsReader reader = new MatsimEventsReader(events);
		events.addHandler(this.tollHandler);
		events.addHandler(this.distHandler);
		reader.readFile(eventsFile);
		splitDataInPeakOffPeakHours();
		storeUserGroupData();
	}

	public void writeRBoxPlotData(final String outputFolder, final String pricingScheme) {
		if( ! new File(outputFolder+"/boxPlot/").exists()) new File(outputFolder+"/boxPlot/").mkdirs();

		BufferedWriter writer = IOUtils.getBufferedWriter(outputFolder+"/boxPlot/tripTollInEurCtPerKm_"+pricingScheme+"_pkHr"+".txt");
		try {
			for(Id<Person> p : person2TollsPerKmPkHr.keySet()){
				String ug = pf.getMyUserGroupFromPersonId(p);
				for(double d: person2TollsPerKmPkHr.get(p)){
					writer.write(pricingScheme.toUpperCase()+"\t"+ ug+"\t"+d*100+"\n");
				}
			}
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written in file. Reason: " + e);
		}

		//write off peak hour toll/trip
		writer = IOUtils.getBufferedWriter(outputFolder+"/boxPlot/tripTollInEurCtPerKm_"+pricingScheme+"_offPkHr"+".txt");
		try {
			for(Id<Person> p : person2TollsPerKmOffPkHr.keySet()){
				String ug = pf.getMyUserGroupFromPersonId(p);
				for(double d: person2TollsPerKmOffPkHr.get(p)){
					writer.write(pricingScheme.toUpperCase()+"\t"+ ug+"\t"+d*100+"\n");
				}
			}
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written in file. Reason: " + e);
		}
	}

	private void storeUserGroupData(){
		for(UserGroup ug : UserGroup.values()){
			usrGrp2TollsPerKm.put(pf.getMyUserGroup(ug), new Tuple<Double, Double>(0., 0.));
			usrGrp2TripCounts.put(pf.getMyUserGroup(ug), new Tuple<Integer, Integer>(0, 0));
		}
		//first store peak hour data
		for (Id<Person> personId : this.person2TollsPerKmPkHr.keySet()) {
			String ug = pf.getMyUserGroupFromPersonId(personId);
			double tollInMeter = ListUtils.doubleSum(this.person2TollsPerKmPkHr.get(personId));
			double pkTollInKm = usrGrp2TollsPerKm.get(ug).getFirst() + 1000*tollInMeter;
			int pkTripCount = usrGrp2TripCounts.get(ug).getFirst() + this.person2TripCountsPkHr.get(personId);
			usrGrp2TollsPerKm.put(ug, new Tuple<Double, Double>(pkTollInKm, 0.));
			usrGrp2TripCounts.put(ug, new Tuple<Integer,Integer>(pkTripCount,0) );
		}

		//now store off-peak hour data
		for (Id<Person> personId : this.person2TollsPerKmOffPkHr.keySet()) {
			String ug = pf.getMyUserGroupFromPersonId(personId);
			double tollInMeter = ListUtils.doubleSum(this.person2TollsPerKmOffPkHr.get(personId));
			double offpkToll = usrGrp2TollsPerKm.get(ug).getSecond() + 1000*tollInMeter;
			int offpkTripCount = usrGrp2TripCounts.get(ug).getSecond() + this.person2TripCountsOffPkHr.get(personId);
			usrGrp2TollsPerKm.put(ug, new Tuple<Double, Double>(usrGrp2TollsPerKm.get(ug).getFirst(), offpkToll));
			usrGrp2TripCounts.put(ug, new Tuple<Integer,Integer>(usrGrp2TripCounts.get(ug).getFirst(),offpkTripCount) );
		}
	}

	private void splitDataInPeakOffPeakHours() {
		SortedMap<Double, Map<Id<Person>, List<Double>>> timebin2person2tripTolls = this.tollHandler.getTimeBin2Person2TripToll();
		SortedMap<Double, Map<Id<Person>, Integer>> timebin2person2tripCounts = this.tollHandler.getTimeBin2Person2TripsCount();
		
		SortedMap<Double, Map<Id<Person>, List<Double>>> timebin2person2tripDists = this.distHandler.getTimeBin2Person2TripsDistance();
		SortedMap<Double, Map<Id<Person>, Integer>> timebin2person2tripDistCounts = this.distHandler.getTimeBin2Person2TripsCount();

		for(double d :timebin2person2tripTolls.keySet()) {
			for (Id<Person> person : timebin2person2tripTolls.get(d).keySet()) {
				if(pkHrs.contains(d)) {
					if (person2TollsPerKmPkHr.containsKey(person) ) {
						List<Double> existingTollsPerMeter = person2TollsPerKmPkHr.get(person);
						List<Double> additionalTollsPerMeter = ListUtils.divide(timebin2person2tripTolls.get(d).get(person), timebin2person2tripDists.get(d).get(person));
						existingTollsPerMeter.addAll(additionalTollsPerMeter);
						if(! (timebin2person2tripCounts.get(d).get(person)).equals(timebin2person2tripDistCounts.get(d).get(person)) ) {
							throw new RuntimeException("Trip count should be equal in both lists. Aborting ...");
						}
						person2TripCountsPkHr.put(person, timebin2person2tripCounts.get(d).get(person) + person2TripCountsPkHr.get(person));
					} else {
						List<Double> tolls =  timebin2person2tripTolls.get(d).get(person);
						List<Double> dists = timebin2person2tripDists.get(d).get(person);
						List<Double> tollsPerMeter = ListUtils.divide(tolls, dists); 
						person2TollsPerKmPkHr.put(person, tollsPerMeter);
						// just a check
						if(! (timebin2person2tripCounts.get(d).get(person)).equals(timebin2person2tripDistCounts.get(d).get(person)) ) {
							throw new RuntimeException("Trip count should be equal in both lists. Aborting ...");
						}
						person2TripCountsPkHr.put(person, timebin2person2tripCounts.get(d).get(person));
					}
				} else {
					if (person2TollsPerKmOffPkHr.containsKey(person) ) {
						List<Double> existingTollsPerMeter = person2TollsPerKmOffPkHr.get(person);
						List<Double> additionalTollsPerMeter = ListUtils.divide(timebin2person2tripTolls.get(d).get(person), timebin2person2tripDists.get(d).get(person));
						if(additionalTollsPerMeter==null){
							System.out.println("problem.");
						}
						existingTollsPerMeter.addAll(additionalTollsPerMeter);
						if(! (timebin2person2tripCounts.get(d).get(person)).equals(timebin2person2tripDistCounts.get(d).get(person)) ) {
							throw new RuntimeException("Trip count should be equal in both lists. Aborting ...");
						}
						person2TripCountsOffPkHr.put(person, timebin2person2tripCounts.get(d).get(person) + person2TripCountsOffPkHr.get(person));
					} else {
						List<Double> tolls =  timebin2person2tripTolls.get(d).get(person);
						List<Double> dists = timebin2person2tripDists.get(d).get(person);
						List<Double> tollsPerMeter = ListUtils.divide(tolls, dists); 
						person2TollsPerKmOffPkHr.put(person,  tollsPerMeter);
						if(! (timebin2person2tripCounts.get(d).get(person)).equals(timebin2person2tripDistCounts.get(d).get(person)) ) {
							throw new RuntimeException("Trip count should be equal in both lists. Aborting ...");
						}
						person2TripCountsOffPkHr.put(person, timebin2person2tripCounts.get(d).get(person));
					}
				}
			}
		}
	}
}
