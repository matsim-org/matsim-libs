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
import java.util.Arrays;
import java.util.HashMap;
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
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.munich.utils.ExtendedPersonFilter;
import playground.agarwalamit.munich.utils.ListUitls;
import playground.agarwalamit.utils.LoadMyScenarios;
import playground.benjamin.scenarios.munich.analysis.filter.UserGroup;

/**
 * @author amit
 */

public class TripDistanceAnalyzer  {

	public TripDistanceAnalyzer(Network network, double simulationEndTime, int noOfTimeBins) {
		this.tripDistHandler = new TripDistanceHandler(network, simulationEndTime, noOfTimeBins);
	}

	private TripDistanceHandler tripDistHandler;
	private final List<Double> pkHrs = new ArrayList<>(Arrays.asList(new Double []{7., 8., 9., 15., 16.,17.}));
	private final ExtendedPersonFilter pf = new ExtendedPersonFilter();
	private Map<Id<Person>,List<Double>> person2Dists_pkHr = new HashMap<>();
	private Map<Id<Person>,List<Double>> person2Dists_offPkHr = new HashMap<>();
	private Map<Id<Person>,Integer> person2TripCounts_pkHr = new HashMap<>();
	private Map<Id<Person>,Integer> person2TripCounts_offPkHr = new HashMap<>();
	private SortedMap<String, Tuple<Double,Double>> usrGrp2Dists = new TreeMap<>();
	private SortedMap<String, Tuple<Integer,Integer>> usrGrp2TripCounts = new TreeMap<>();

	public static void main(String[] args) {
		String scenario = "ei";
		String dir = "../../../../repos/runs-svn/detEval/emissionCongestionInternalization/iatbr/output/"+scenario;
		String eventsFile = dir+"/ITERS/it.1500/1500.events.xml.gz";
		String networkFile = dir+"/output_network.xml.gz";
		String configFile = dir+"/output_config.xml.gz";
		Scenario sc = LoadMyScenarios.loadScenarioFromNetworkAndConfig(networkFile, configFile);
		
		TripDistanceAnalyzer tda = new TripDistanceAnalyzer(sc.getNetwork(), sc.getConfig().qsim().getEndTime(), 30);
		tda.run(eventsFile);
		tda.writeTripData(dir+"/analysis/");
		
	}

	public void run(String eventsFile) {
		EventsManager events = EventsUtils.createEventsManager();
		MatsimEventsReader reader = new MatsimEventsReader(events);
		events.addHandler(tripDistHandler);
		reader.readFile(eventsFile);
		splitDataInPeakOffPeakHours();
		storeUserGroupData();
	}

	public Map<Id<Person>,List<Double>> getPersonToTripDistancesInPeakHours() {
		return this.person2Dists_pkHr;
	}

	public Map<Id<Person>,List<Double>> getPersonToTripDistancesInOffPeakHours() {
		return this.person2Dists_offPkHr;
	}

	public Map<Id<Person>, Integer> getPerson2TripCountsInPeakHours() {
		return person2TripCounts_pkHr;
	}

	public Map<Id<Person>, Integer> getPerson2TripCountsInOffPeakHours() {
		return person2TripCounts_offPkHr;
	}

	/**
	 * @return usergroup to trip total trip distance tuple of which first object is for peak hour and second is for off peak hour.
	 */
	public SortedMap<String, Tuple<Double,Double>> getUsrGrp2TotalDistance() {
		return usrGrp2Dists;
	}

	/**
	 * @return usergroup to trip count tuple of which first object is for peak hour and second is for off peak hour.
	 */
	public SortedMap<String, Tuple<Integer,Integer>> getUsrGrp2TripCounts() {
		return usrGrp2TripCounts;
	}

	public void writeTripData(String outputFolder){
		BufferedWriter writer = IOUtils.getBufferedWriter(outputFolder+"/userGrp_tripDist.txt");
		try {
			writer.write("userGroup \t peakHrTotalDistanceInKm \t offPeakHrTotalDistanceInKm \t peakHrTripCount \t offPeakHrTripCount \n");
			for(String ug:this.usrGrp2Dists.keySet()){
				writer.write(ug+"\t"+this.usrGrp2Dists.get(ug).getFirst()/1000.+"\t"+this.usrGrp2Dists.get(ug).getSecond()/1000.+"\t"
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
			double pkDist = usrGrp2Dists.get(ug).getFirst() + ListUitls.doubleSum(this.person2Dists_pkHr.get(personId));
			int pkTripCount = usrGrp2TripCounts.get(ug).getFirst() + this.person2TripCounts_pkHr.get(personId);
			usrGrp2Dists.put(ug, new Tuple<Double, Double>(pkDist, 0.));
			usrGrp2TripCounts.put(ug, new Tuple<Integer,Integer>(pkTripCount,0) );
		}

		//now store off-peak hour data
		for (Id<Person> personId : this.person2Dists_offPkHr.keySet()) {
			String ug = pf.getMyUserGroupFromPersonId(personId);
			double offpkDist = usrGrp2Dists.get(ug).getSecond() + ListUitls.doubleSum(this.person2Dists_offPkHr.get(personId));
			int offpkTripCount = usrGrp2TripCounts.get(ug).getSecond() + this.person2TripCounts_offPkHr.get(personId);
			usrGrp2Dists.put(ug, new Tuple<Double, Double>(usrGrp2Dists.get(ug).getFirst(), offpkDist));
			usrGrp2TripCounts.put(ug, new Tuple<Integer,Integer>(usrGrp2TripCounts.get(ug).getFirst(),offpkTripCount) );
		}
	}

	private void splitDataInPeakOffPeakHours() {
		SortedMap<Double, Map<Id<Person>, List<Double>>> timebin2person2tripDists = tripDistHandler.getTimeBin2Person2TripsDistance();
		SortedMap<Double, Map<Id<Person>, Integer>> timebin2person2tripCounts = tripDistHandler.getTimeBin2Person2TripsCount();

		for(double d :timebin2person2tripDists.keySet()) {
			// iterate through time bins, sum number of trips and total trip dists for each person in pk and off pk hours..
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
