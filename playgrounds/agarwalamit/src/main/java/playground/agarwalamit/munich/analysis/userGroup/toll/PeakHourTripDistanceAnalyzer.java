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
import java.util.*;
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
import playground.agarwalamit.analysis.tripDistance.ModeFilterTripDistanceHandler;
import playground.agarwalamit.munich.utils.MunichPersonFilter;
import playground.agarwalamit.munich.utils.MunichPersonFilter.MunichUserGroup;
import playground.agarwalamit.utils.FileUtils;
import playground.agarwalamit.utils.ListUtils;
import playground.agarwalamit.utils.LoadMyScenarios;

/**
 * @author amit
 */

public class PeakHourTripDistanceAnalyzer  {
	private final ModeFilterTripDistanceHandler tripDistHandler;
	private final List<Double> pkHrs = new ArrayList<>(Arrays.asList(new Double []{8., 9., 10., 16., 17., 18.,})); // => 7-10 and 15-18
	private final MunichPersonFilter pf = new MunichPersonFilter();
	private final Map<Id<Person>,List<Double>> person2DistsPkHr = new HashMap<>();
	private final Map<Id<Person>,List<Double>> person2DistsOffPkHr = new HashMap<>();
	private final Map<Id<Person>,Integer> person2TripCountsPkHr = new HashMap<>();
	private final Map<Id<Person>,Integer> person2TripCountsOffPkHr = new HashMap<>();
	private final SortedMap<String, Tuple<Double,Double>> usrGrp2Dists = new TreeMap<>();
	private final SortedMap<String, Tuple<Integer,Integer>> usrGrp2TripCounts = new TreeMap<>();
	private static final Logger LOG = Logger.getLogger(PeakHourTripDistanceAnalyzer.class);

	public PeakHourTripDistanceAnalyzer(Network network, double simulationEndTime, int noOfTimeBins) {
		LOG.warn("Peak hours are assumed as 07:00-10:00 and 15:00-18:00 by looking on the travel demand for BAU scenario.");
		this.tripDistHandler = new ModeFilterTripDistanceHandler(network, simulationEndTime, noOfTimeBins);
		throw new RuntimeException("looks, there is some problem somewhere, cant reproduce the results (Oct 2016).");
	}
	
	public static void main(String[] args) {
		String [] pricingSchemes = new String [] {"ei","ci","eci"};
		for (String str :pricingSchemes) {
			String dir = FileUtils.RUNS_SVN+"/detEval/emissionCongestionInternalization/iatbr/output/";
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
		for(MunichUserGroup ug : MunichUserGroup.values()){
			usrGrp2Dists.put(ug.toString(), new Tuple<>(0., 0.));
			usrGrp2TripCounts.put(ug.toString(), new Tuple<>(0, 0));
		}
		//first store peak hour data
		for (Id<Person> personId : this.person2DistsPkHr.keySet()) {
			String ug = pf.getUserGroupAsStringFromPersonId(personId);
			double pkDist = usrGrp2Dists.get(ug).getFirst() + ListUtils.doubleSum(this.person2DistsPkHr.get(personId));
			int pkTripCount = usrGrp2TripCounts.get(ug).getFirst() + this.person2TripCountsPkHr.get(personId);
			usrGrp2Dists.put(ug, new Tuple<>(pkDist, 0.));
			usrGrp2TripCounts.put(ug, new Tuple<>(pkTripCount, 0) );
		}

		//now store off-peak hour data
		for (Id<Person> personId : this.person2DistsOffPkHr.keySet()) {
			String ug = pf.getUserGroupAsStringFromPersonId(personId);
			double offpkDist = usrGrp2Dists.get(ug).getSecond() + ListUtils.doubleSum(this.person2DistsOffPkHr.get(personId));
			int offpkTripCount = usrGrp2TripCounts.get(ug).getSecond() + this.person2TripCountsOffPkHr.get(personId);
			usrGrp2Dists.put(ug, new Tuple<>(usrGrp2Dists.get(ug).getFirst(), offpkDist));
			usrGrp2TripCounts.put(ug, new Tuple<>(usrGrp2TripCounts.get(ug).getFirst(), offpkTripCount) );
		}
	}

	private void splitDataInPeakOffPeakHours() {
		SortedMap<Double, Map<Id<Person>, List<Double>>> timebin2person2tripDists = tripDistHandler.getTimeBin2Person2TripsDistance();
		SortedMap<Double, Map<Id<Person>, Integer>> timebin2person2tripCounts = tripDistHandler.getTimeBin2Person2TripsCount();

		for(double d :timebin2person2tripDists.keySet()) {
			for (Id<Person> person : timebin2person2tripDists.get(d).keySet()) {
				if(pkHrs.contains(d)) {
					if (person2DistsPkHr.containsKey(person) ) {
						List<Double> dists =  person2DistsPkHr.get(person);
						dists.addAll(timebin2person2tripDists.get(d).get(person));
						person2TripCountsPkHr.put(person, timebin2person2tripCounts.get(d).get(person) + person2TripCountsPkHr.get(person));
					} else {
						person2DistsPkHr.put(person,  timebin2person2tripDists.get(d).get(person));
						person2TripCountsPkHr.put(person, timebin2person2tripCounts.get(d).get(person));
					}
				} else {
					if (person2DistsOffPkHr.containsKey(person) ) {
						List<Double> dists =  person2DistsOffPkHr.get(person);
						dists.addAll(timebin2person2tripDists.get(d).get(person));
						person2TripCountsOffPkHr.put(person, timebin2person2tripCounts.get(d).get(person) + person2TripCountsOffPkHr.get(person));
					} else {
						person2DistsOffPkHr.put(person,  timebin2person2tripDists.get(d).get(person));
						person2TripCountsOffPkHr.put(person, timebin2person2tripCounts.get(d).get(person));
					}
				}
			}
		}
	}
}
