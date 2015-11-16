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
package playground.agarwalamit.munich.analysis.userGroup.pkHr;

import java.io.BufferedWriter;
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

import playground.agarwalamit.analysis.trip.TripDistanceHandler;
import playground.agarwalamit.munich.utils.ExtendedPersonFilter;
import playground.agarwalamit.utils.ListUtils;
import playground.agarwalamit.utils.LoadMyScenarios;
import playground.benjamin.scenarios.munich.analysis.filter.UserGroup;

/**
 * @author amit
 */

public class PeakHourTripDistanceAnalyzer  {

	public PeakHourTripDistanceAnalyzer(Network network, double simulationEndTime, int noOfTimeBins) {
		log.warn("Peak hours are assumed as 07:00-10:00 and 15:00-18:00 by looking on the travel demand for BAU scenario.");
		this.tripDistHandler = new TripDistanceHandler(network, simulationEndTime, noOfTimeBins);
	}

	private TripDistanceHandler tripDistHandler;
	private final List<Double> pkHrs = new ArrayList<>(Arrays.asList(new Double []{8., 9., 10., 16., 17., 18.,})); // => 7-10 and 15-18
	private final ExtendedPersonFilter pf = new ExtendedPersonFilter();
	private Map<Id<Person>,List<Double>> person2Dists_pkHr = new HashMap<>();
	private Map<Id<Person>,List<Double>> person2Dists_offPkHr = new HashMap<>();
	private Map<Id<Person>,Integer> person2TripCounts_pkHr = new HashMap<>();
	private Map<Id<Person>,Integer> person2TripCounts_offPkHr = new HashMap<>();
	private SortedMap<String, Tuple<Double,Double>> usrGrp2Dists = new TreeMap<>();
	private SortedMap<String, Tuple<Integer,Integer>> usrGrp2TripCounts = new TreeMap<>();
	private static final Logger log = Logger.getLogger(PeakHourTripDistanceAnalyzer.class);

	public static void main(String[] args) {
		String [] pricingSchemes = new String [] {"ei","ci","eci"};
		for (String str :pricingSchemes) {
			String dir = "../../../../repos/runs-svn/detEval/emissionCongestionInternalization/iatbr/output/";
			String eventsFile = dir+str+"/ITERS/it.1500/1500.events.xml.gz";
			String networkFile = dir+str+"/output_network.xml.gz";
			String configFile = dir+str+"/output_config.xml.gz";
			Scenario sc = LoadMyScenarios.loadScenarioFromNetworkAndConfig(networkFile, configFile);

			PeakHourTripDistanceAnalyzer tda = new PeakHourTripDistanceAnalyzer(sc.getNetwork(), sc.getConfig().qsim().getEndTime(), 30);
			tda.run(eventsFile);
			tda.writeTripData(dir+"/analysis/", str);
		}
	}

	public void run(String eventsFile) {
		EventsManager events = EventsUtils.createEventsManager();
		MatsimEventsReader reader = new MatsimEventsReader(events);
		events.addHandler(tripDistHandler);
		reader.readFile(eventsFile);
		splitDataInPeakOffPeakHours();
		storeUserGroupData();
	}

	public void writeTripData(String outputFolder, String pricingScheme){
		BufferedWriter writer = IOUtils.getBufferedWriter(outputFolder+"/userGrp_tripDist_"+pricingScheme+".txt");
		try {
			writer.write("userGroup \t peakHrTotalDistanceInKm \t offPeakHrTotalDistanceInKm \t peakHrAvgTripDist \t offPeakHrAvgTripDist \t peakHrTripCount \t offPeakHrTripCount \n");
			for(String ug:this.usrGrp2Dists.keySet()){
				writer.write(ug+"\t"+this.usrGrp2Dists.get(ug).getFirst()/1000.+"\t"+this.usrGrp2Dists.get(ug).getSecond()/1000.+"\t"
						+this.usrGrp2Dists.get(ug).getFirst()/(1000.*this.usrGrp2TripCounts.get(ug).getFirst())+"\t"+this.usrGrp2Dists.get(ug).getSecond()/(1000.*this.usrGrp2TripCounts.get(ug).getSecond())+"\t"
						+this.usrGrp2TripCounts.get(ug).getFirst()+"\t"+this.usrGrp2TripCounts.get(ug).getSecond()+"\n");
			}
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written. Reason "+e);
		}
	}

	private void storeUserGroupData(){
		for(UserGroup ug : UserGroup.values()){
			usrGrp2Dists.put(pf.getMyUserGroup(ug), new Tuple<Double, Double>(0., 0.));
			usrGrp2TripCounts.put(pf.getMyUserGroup(ug), new Tuple<Integer, Integer>(0, 0));
		}
		//first store peak hour data
		for (Id<Person> personId : this.person2Dists_pkHr.keySet()) {
			String ug = pf.getMyUserGroupFromPersonId(personId);
			double pkDist = usrGrp2Dists.get(ug).getFirst() + ListUtils.doubleSum(this.person2Dists_pkHr.get(personId));
			int pkTripCount = usrGrp2TripCounts.get(ug).getFirst() + this.person2TripCounts_pkHr.get(personId);
			usrGrp2Dists.put(ug, new Tuple<Double, Double>(pkDist, 0.));
			usrGrp2TripCounts.put(ug, new Tuple<Integer,Integer>(pkTripCount,0) );
		}

		//now store off-peak hour data
		for (Id<Person> personId : this.person2Dists_offPkHr.keySet()) {
			String ug = pf.getMyUserGroupFromPersonId(personId);
			double offpkDist = usrGrp2Dists.get(ug).getSecond() + ListUtils.doubleSum(this.person2Dists_offPkHr.get(personId));
			int offpkTripCount = usrGrp2TripCounts.get(ug).getSecond() + this.person2TripCounts_offPkHr.get(personId);
			usrGrp2Dists.put(ug, new Tuple<Double, Double>(usrGrp2Dists.get(ug).getFirst(), offpkDist));
			usrGrp2TripCounts.put(ug, new Tuple<Integer,Integer>(usrGrp2TripCounts.get(ug).getFirst(),offpkTripCount) );
		}
	}

	private void splitDataInPeakOffPeakHours() {
		SortedMap<Double, Map<Id<Person>, List<Double>>> timebin2person2tripDists = tripDistHandler.getTimeBin2Person2TripsDistance();
		SortedMap<Double, Map<Id<Person>, Integer>> timebin2person2tripCounts = tripDistHandler.getTimeBin2Person2TripsCount();

		for(double d :timebin2person2tripDists.keySet()) {
			for (Id<Person> person : timebin2person2tripDists.get(d).keySet()) {
				if(pkHrs.contains(d)) {
					if (person2Dists_pkHr.containsKey(person) ) {
						List<Double> dists =  person2Dists_pkHr.get(person);
						dists.addAll(timebin2person2tripDists.get(d).get(person));
						person2TripCounts_pkHr.put(person, timebin2person2tripCounts.get(d).get(person) + person2TripCounts_pkHr.get(person));
					} else {
						person2Dists_pkHr.put(person,  timebin2person2tripDists.get(d).get(person));
						person2TripCounts_pkHr.put(person, timebin2person2tripCounts.get(d).get(person));
					}
				} else {
					if (person2Dists_offPkHr.containsKey(person) ) {
						List<Double> dists =  person2Dists_offPkHr.get(person);
						dists.addAll(timebin2person2tripDists.get(d).get(person));
						person2TripCounts_offPkHr.put(person, timebin2person2tripCounts.get(d).get(person) + person2TripCounts_offPkHr.get(person));
					} else {
						person2Dists_offPkHr.put(person,  timebin2person2tripDists.get(d).get(person));
						person2TripCounts_offPkHr.put(person, timebin2person2tripCounts.get(d).get(person));
					}
				}
			}
		}
	}
}
