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
package playground.agarwalamit.siouxFalls;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.emissions.utils.EmissionUtils;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.analysis.congestion.ExperiencedDelayAnalyzer;
import playground.agarwalamit.analysis.emission.EmissionLinkAnalyzer;
import playground.agarwalamit.utils.LoadMyScenarios;

/**
 * @author amit
 */
public class EmissionsAndCongestionsPerTimeBin {

	private final Logger logger = Logger.getLogger(EmissionsAndCongestionsPerTimeBin.class);
	private final String outputDir = "/Users/aagarwal/Desktop/ils4/agarwal/munich/output/1pct/";
	private final String runCase = "eci";
	private final String networkFile =outputDir+runCase+"/output_network.xml.gz";
	private final String configFile = outputDir+runCase+"/output_config.xml";
	private EmissionUtils emissionUtils;

	private Network network;
	private final int noOfTimeBins = 30;
	private final double simulationEndTime = LoadMyScenarios.getSimulationEndTime(this.configFile) ;
	private final Scenario scenario = LoadMyScenarios.loadScenarioFromNetworkAndConfig(this.networkFile,this.configFile);
	private final int lastIteration = LoadMyScenarios.getLastIteration(this.configFile);
	private final List<Double> allTimeBins = new ArrayList<>();

	public static void main(String[] args)  {
		EmissionsAndCongestionsPerTimeBin emissionsVsCongestionData = new EmissionsAndCongestionsPerTimeBin();
		emissionsVsCongestionData.run();
	}

	private void run()  {
		String emissionEventFile = this.outputDir+this.runCase+"/ITERS/it."+this.lastIteration+"/"+this.lastIteration+".emission.events.xml.gz";
		String eventsFile = this.outputDir+this.runCase+"/ITERS/it."+this.lastIteration+"/"+this.lastIteration+".events.xml.gz";
		this.network = this.scenario.getNetwork();
		EmissionLinkAnalyzer eLinkAnalyzer = new EmissionLinkAnalyzer(this.simulationEndTime, emissionEventFile, this.noOfTimeBins);
		eLinkAnalyzer.preProcessData();
		eLinkAnalyzer.postProcessData();

		this.emissionUtils = new EmissionUtils();

		SortedMap<Double, Map<Id<Link>, SortedMap<String, Double>>> time2EmissionsTotal = eLinkAnalyzer.getLink2TotalEmissions();
		SortedMap<Double, Map<Id<Link>, SortedMap<String, Double>>> time2EmissionsTotalFilled = setNonCalculatedEmissions(time2EmissionsTotal);

		ExperiencedDelayAnalyzer cLinkAnalyzer = new ExperiencedDelayAnalyzer(eventsFile, this.scenario, noOfTimeBins);
		cLinkAnalyzer.run();
		cLinkAnalyzer.checkTotalDelayUsingAlternativeMethod();

		Map<Double, Map<Id<Link>, Double>> time2linkIdDelays = cLinkAnalyzer.getTimeBin2LinkId2Delay();
		new File(outputDir+runCase+"/analysis/emissionVsCongestion/").mkdirs();
		BufferedWriter writer1 = IOUtils.getBufferedWriter(outputDir+runCase+"/analysis/emissionVsCongestion/"+runCase+".hourlyDelaysAndEmissionsPerLink.txt");


		try {
			writer1.write("time"+"\t"+"linkId"+"\t"+"delays_sec"+"\t"+"CO"+"\t"+"CO2_Total"+"\t"+"FC"+"\t"+"HC"+"\t"+"NMHC"+"\t"+"NO2"+"\t"+"NOX"+"\t"+"PM"+"\t"+"SO2"+"\n");

			for(double time : this.allTimeBins){
				for(Link link : this.network.getLinks().values()){

					double delay;
					if(time2linkIdDelays.get(time)!=null) {
						if(time2linkIdDelays.get(time).get(link.getId())==null)	delay = 0.0;
						else delay = time2linkIdDelays.get(time).get(link.getId());
					}
					else delay =0.0;

					writer1.write(time+"\t"+link.getId().toString()+"\t"+delay+"\t");

					for(String str : (time2EmissionsTotalFilled.get(time)).get(link.getId()).keySet()){
						writer1.write(time2EmissionsTotalFilled.get(time).get(link.getId()).get(str)+"\t");
					} 
					writer1.write("\n");
				}
			} 	
			writer1.close();
		} catch (IOException e) {
			throw new RuntimeException("Data is not written in a file. Reason "+e);
		}

		//writing network delays and emissions per time bin
		BufferedWriter writer2= IOUtils.getBufferedWriter(this.outputDir+this.runCase+"/analysis/emissionVsCongestion/"+this.runCase+".hourlyNetworkDelaysAndEmissions.txt");
		try{
			writer2.write("time"+"\t"+"delays_sec"+"\t"+"CO"+"\t"+"CO2_Total"+"\t"+"FC"+"\t"+"HC"+"\t"+"NMHC"+"\t"+"NO2"+"\t"+"NOX"+"\t"+"PM"+"\t"+"SO2"+"\n");
			for(double time : time2EmissionsTotalFilled.keySet()){
				double networkDelay = 0;
				SortedMap<String, Double> networkEmissions = new TreeMap<>();
				for(Link link : this.network.getLinks().values()){
					double delay;
					if(time2linkIdDelays.get(time)!=null) {
						if(time2linkIdDelays.get(time).get(link.getId())==null)	delay = 0.0;
						else delay = time2linkIdDelays.get(time).get(link.getId());
					}
					else delay =0.0;
					networkDelay+=delay;

					for(String str : (time2EmissionsTotalFilled.get(time)).get(link.getId()).keySet()){
						double  emissionSoFar;

						if(networkEmissions.get(str)!=null){
							emissionSoFar = networkEmissions.get(str);
						} else {
							emissionSoFar =0.;
						}

						double emissionNow=time2EmissionsTotalFilled.get(time).get(link.getId()).get(str);
						networkEmissions.put(str, emissionSoFar+emissionNow);
					}
				}
				writer2.write(time+"\t"+networkDelay+"\t");
				for(String str : networkEmissions.keySet()){
					writer2.write(networkEmissions.get(str)+"\t");
				}
				writer2.newLine();
			}
			writer2.close();
		}catch (IOException e) {
			throw new RuntimeException("Data is not written in a file. Reason "+e);
		}
		this.logger.info("Finished Writing files.");
	}

	private SortedMap<Double, Map<Id<Link>, SortedMap<String, Double>>> setNonCalculatedEmissions(Map<Double, Map<Id<Link>, SortedMap<String, Double>>> time2EmissionsTotal) {
		SortedMap<Double, Map<Id<Link>, SortedMap<String, Double>>> time2EmissionsTotalFilled = new TreeMap<>();

		double timeBinSize = this.simulationEndTime/this.noOfTimeBins;

		for(double t=0;t<=this.simulationEndTime;){
			this.allTimeBins.add(t);
			t=t+timeBinSize;
		}

		for(double d:time2EmissionsTotal.keySet()){
			if(!this.allTimeBins.contains(d)) throw new RuntimeException("Time bin "+d+" not found in list of all time bins. Aborting...");
		}

		for(double endOfTimeInterval : this.allTimeBins){
			if(!time2EmissionsTotal.containsKey(endOfTimeInterval)){
				time2EmissionsTotal.put(endOfTimeInterval, new HashMap<>());
			}

			Map<Id<Link>, SortedMap<String, Double>> emissionsTotalFilled = this.emissionUtils.setNonCalculatedEmissionsForNetwork(this.network, time2EmissionsTotal.get(endOfTimeInterval));
			time2EmissionsTotalFilled.put(endOfTimeInterval, emissionsTotalFilled);
		}
		return time2EmissionsTotalFilled;
	}
}
