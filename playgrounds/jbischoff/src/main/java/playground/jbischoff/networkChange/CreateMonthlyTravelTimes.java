/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.jbischoff.networkChange;

import java.io.BufferedWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.TravelTimeCalculatorConfigGroup;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;

import com.google.common.math.DoubleMath;

/**
 * @author jbischoff
 *
 */
public class CreateMonthlyTravelTimes {

	Logger log = Logger.getLogger(getClass());
	Map<Id<Link>, double[]> traveltimes = new HashMap<>();
	Map<Id<Link>, double[]> averageTraveltimes = new HashMap<>();
	Map<Id<Link>, double[]> standardDeviationTraveltimes = new HashMap<>();
	Map<Id<Link>, double[]> minTraveltimes = new HashMap<>();
	Map<Id<Link>, double[]> maxTraveltimes = new HashMap<>();


	Network network;
	String[] header;
	int TIMESTEP = 15 * 60;
	int firstday = 0;
	int lastday = 27;
	int timebins;
	int binsPerDay;
//	String inputfolder = "D:/runs-svn/incidents/output/2b_reroute1.0/";
	String inputfolder = "../../../runs-svn/incidents/output/2b_reroute1.0/";

	public static void main(String[] args) {

		CreateMonthlyTravelTimes cmtt = new CreateMonthlyTravelTimes();
		cmtt.run();
	}

	public CreateMonthlyTravelTimes() {
		timebins = ((lastday - firstday +1) * 24 * 3600) / TIMESTEP;
		header = new String[timebins];
		log.info(timebins + " timebins created.");
		binsPerDay = (24*3600)/TIMESTEP;


	}

	private void run() {
		prepareNetwork(inputfolder+"nce_0/output_network.xml.gz");

		for (int currentDay = firstday; currentDay <= lastday; currentDay++) {
			extractTraveltimes(currentDay);
		}
		calculateAverageTraveltimes();
		calculateStandardDeviationTraveltimes();
		calculateMinTraveltimes();
		calculateMaxTraveltimes();

		writeTravelTimes();
		writeAnalysisValues(this.averageTraveltimes, "tt_avg");
		writeAnalysisValues(this.standardDeviationTraveltimes, "tt_std");
		writeAnalysisValues(this.minTraveltimes, "tt_min");
		writeAnalysisValues(this.maxTraveltimes, "tt_max");

	}

	private void writeTravelTimes() {
		BufferedWriter bw = IOUtils.getBufferedWriter(inputfolder+"traveltimes.csv");
		BufferedWriter bwt = IOUtils.getBufferedWriter(inputfolder+"traveltimes.csvt");
		Locale.setDefault(Locale.US);
		DecimalFormat df = new DecimalFormat( "####0.00" );
		try {
			String l1 = "LinkID,FreeSpeedTravelTime";
			bw.append(l1);
			bwt.append("\"String\",\"Real\"");
			for (int currentDay = firstday; currentDay <= lastday; currentDay++) {
			for (int i = 0; i<24*3600;i=i+TIMESTEP ){
				double time = currentDay*24*3600+i;
				bw.append(","+Time.writeTime(time));
				bwt.append(",\"Real\"");
			}	
			}
			
			for (Entry<Id<Link>, double[]> e :  this.traveltimes.entrySet()){
				bw.newLine();
				bw.append(e.getKey().toString()+",");
				double freespeedTT = network.getLinks().get(e.getKey()).getLength()/network.getLinks().get(e.getKey()).getFreespeed();
				bw.append(df.format(freespeedTT));
				for (int i=0;i<e.getValue().length;i++){
					bw.append(","+df.format(e.getValue()[i]));
				}
				
			}
			bw.flush();
			bw.close();
			bwt.flush();
			bwt.close();
	
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		}
	
	private void writeAnalysisValues(Map<Id<Link>, double[]> linkId2analysisValue, String filename) {
		Locale.setDefault(Locale.US);
		DecimalFormat df = new DecimalFormat( "####0.00" );
		BufferedWriter bw = IOUtils.getBufferedWriter(inputfolder+filename+".csv");
		BufferedWriter bwt = IOUtils.getBufferedWriter(inputfolder+filename+".csvt");
		try {
			String l1 = "LinkID,FreeSpeedTravelTime";
			bw.append(l1);
			bwt.append("\"String\",\"Real\"");
			
			for (int i = 0; i<24*3600;i=i+TIMESTEP ){
				double time = i;
				bw.append(","+Time.writeTime(time));
				bwt.append(",\"Real\"");
			}	
			
			
			for (Entry<Id<Link>, double[]> e :  linkId2analysisValue.entrySet()){
				bw.newLine();
				bw.append(e.getKey().toString()+",");
				double freespeedTT = network.getLinks().get(e.getKey()).getLength()/network.getLinks().get(e.getKey()).getFreespeed();
				bw.append(df.format(freespeedTT));
				for (int i=0;i<e.getValue().length;i++){
					bw.append(","+df.format(e.getValue()[i]));
				}
				
			}
			bw.flush();
			bw.close();
			bwt.flush();
			bwt.close();
	
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		}

	private void extractTraveltimes(int currentDay) {
		String eventsFile = inputfolder + "nce_" + currentDay + "/output_events.xml.gz";
		TravelTimeCalculator ttc = readEvents(eventsFile);

		for (int time = 0; time < 24 * 3600; time = time + TIMESTEP) {
			int currentTimeBin = (currentDay * 24 * 3600 + time) / TIMESTEP;
			if (currentTimeBin >= timebins) {
				log.error("current: " + currentTimeBin + " size " + timebins);
				break;
			}
			log.info("bin no:" + currentTimeBin + "day: " + currentDay + " time: " + Time.writeTime(time));
			for (Id<Link> linkId : network.getLinks().keySet()) {
				double tt = ttc.getLinkTravelTime(linkId, time);
				this.traveltimes.get(linkId)[currentTimeBin] = tt;
			}

		}
	}

	private void prepareNetwork(String networkfile) {
		network = NetworkUtils.createNetwork();

		new MatsimNetworkReader(network).readFile(networkfile);
		for (Id<Link> linkId : network.getLinks().keySet()) {
			traveltimes.put(linkId, new double[timebins]);
			averageTraveltimes.put(linkId, new double[binsPerDay]);
			standardDeviationTraveltimes.put(linkId, new double[binsPerDay]);
			minTraveltimes.put(linkId, new double[binsPerDay]);
			maxTraveltimes.put(linkId, new double[binsPerDay]);
		}

	}

	private TravelTimeCalculator readEvents(String eventsFile) {
		EventsManager manager = EventsUtils.createEventsManager();
		TravelTimeCalculatorConfigGroup ttccg = new TravelTimeCalculatorConfigGroup();
		TravelTimeCalculator tc = new TravelTimeCalculator(network, ttccg);
		manager.addHandler(tc);
		new MatsimEventsReader(manager).readFile(eventsFile);
		return tc;
	}
	
	private void calculateAverageTraveltimes(){
		for (int i = 0;i<binsPerDay;i++){
			for (Id<Link> linkId : network.getLinks().keySet()){
			List<Double> aggregatedTT = new ArrayList<>();
			for (int currentDay = firstday; currentDay <= lastday; currentDay++) {
				int currentBin = i + currentDay*binsPerDay;
				double tt = this.traveltimes.get(linkId)[currentBin];
				aggregatedTT.add(tt);
				}
			double averageTT = DoubleMath.mean(aggregatedTT);
			this.averageTraveltimes.get(linkId)[i]=averageTT;
			}
			
		}
	}
	
	private void calculateStandardDeviationTraveltimes(){
		for (int i = 0;i<binsPerDay;i++){
			for (Id<Link> linkId : network.getLinks().keySet()){
			DescriptiveStatistics stats = new DescriptiveStatistics();
			for (int currentDay = firstday; currentDay <= lastday; currentDay++) {
				int currentBin = i + currentDay*binsPerDay;
				double tt = this.traveltimes.get(linkId)[currentBin];
				stats.addValue(tt);
				}
			double stdTT = stats.getStandardDeviation();
			this.standardDeviationTraveltimes.get(linkId)[i]=stdTT;
			}
			
		}
	}
	
	private void calculateMinTraveltimes(){
		for (int i = 0;i<binsPerDay;i++){
			for (Id<Link> linkId : network.getLinks().keySet()){
			DescriptiveStatistics stats = new DescriptiveStatistics();
			for (int currentDay = firstday; currentDay <= lastday; currentDay++) {
				int currentBin = i + currentDay*binsPerDay;
				double tt = this.traveltimes.get(linkId)[currentBin];
				stats.addValue(tt);
				}
			double min = stats.getMin();
			this.minTraveltimes.get(linkId)[i]=min;
			}
			
		}
	}
	
	private void calculateMaxTraveltimes(){
		for (int i = 0;i<binsPerDay;i++){
			for (Id<Link> linkId : network.getLinks().keySet()){
			DescriptiveStatistics stats = new DescriptiveStatistics();
			for (int currentDay = firstday; currentDay <= lastday; currentDay++) {
				int currentBin = i + currentDay*binsPerDay;
				double tt = this.traveltimes.get(linkId)[currentBin];
				stats.addValue(tt);
				}
			double max = stats.getMax();
			this.maxTraveltimes.get(linkId)[i]=max;
			}
			
		}
	}

}
