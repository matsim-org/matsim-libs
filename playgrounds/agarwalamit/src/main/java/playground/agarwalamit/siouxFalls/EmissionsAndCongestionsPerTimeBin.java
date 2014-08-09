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
import java.io.IOException;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.emissions.utils.EmissionUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.analysis.congestion.CongestionLinkAnalyzer;
import playground.agarwalamit.analysis.emission.EmissionLinkAnalyzer;
import playground.agarwalamit.siouxFalls.emissionAnalyzer.PerLinkEmissionData;

/**
 * @author amit
 */
public class EmissionsAndCongestionsPerTimeBin {

	private Logger logger = Logger.getLogger(PerLinkEmissionData.class);
	private String outputDir = "/Users/aagarwal/Desktop/ils4/agarwal/munich/output/1pct/";
	private String runCase = "ei";
	private String networkFile ="/Users/aagarwal/Desktop/ils4/agarwal/munich/input/network-86-85-87-84_simplifiedWithStrongLinkMerge---withLanes.xml";
	private String configFile = "/Users/aagarwal/Desktop/ils4/agarwal/munich/input/config_munich_1pct_baseCaseCtd.xml";
	private int lastIteration =1500  ;
	EmissionUtils emissionUtils;

	private Network network;
	final int noOfTimeBin = 30;

	public static void main(String[] args)  {
		EmissionsAndCongestionsPerTimeBin emissionsVsCongestionData = new EmissionsAndCongestionsPerTimeBin();
		emissionsVsCongestionData.run();
	}

	private void run()  {

		Scenario scenario = loadScenario(networkFile);
		String emissionEventFile = outputDir+runCase+"/ITERS/it."+lastIteration+"/"+lastIteration+".emission.events.xml.gz";
		String eventsFile = outputDir+runCase+"/ITERS/it."+lastIteration+"/"+lastIteration+".events.xml.gz";
		this.network = scenario.getNetwork();
		EmissionLinkAnalyzer eLinkAnalyzer = new EmissionLinkAnalyzer(configFile, emissionEventFile,noOfTimeBin);
		eLinkAnalyzer.init(null);
		eLinkAnalyzer.preProcessData();
		eLinkAnalyzer.postProcessData();

		emissionUtils = new EmissionUtils();

		SortedMap<Double, Map<Id, SortedMap<String, Double>>> time2EmissionsTotal = eLinkAnalyzer.getLink2TotalEmissions();
		SortedMap<Double, Map<Id, SortedMap<String, Double>>> time2EmissionsTotalFilled = setNonCalculatedEmissions(time2EmissionsTotal);

		CongestionLinkAnalyzer cLinkAnalyzer = new CongestionLinkAnalyzer(configFile, eventsFile, noOfTimeBin);
		cLinkAnalyzer.init(scenario);
		cLinkAnalyzer.preProcessData();
		cLinkAnalyzer.postProcessData();
		cLinkAnalyzer.checkTotalDelayUsingAlternativeMethod();

		Map<Double, Map<Id, Double>> time2linkIdDelays = cLinkAnalyzer.getCongestionPerLinkTimeInterval();
		BufferedWriter writer1 = IOUtils.getBufferedWriter(outputDir+"/analysis/emissionVsCongestion/"+runCase+".hourlyDelaysAndEmissionsPerLink.txt");


		try {
			writer1.write("time"+"\t"+"linkId"+"\t"+"delays_sec"+"\t"+"CO"+"\t"+"CO2_Total"+"\t"+"FC"+"\t"+"HC"+"\t"+"NMHC"+"\t"+"NO2"+"\t"+"NOX"+"\t"+"PM"+"\t"+"SO2"+"\n");

			for(double time : time2EmissionsTotalFilled.keySet()){
				for(Link link : network.getLinks().values()){

					double delay;
					if(time2linkIdDelays.get(time).get(link.getId())==null) delay = 0.0;
					else delay = time2linkIdDelays.get(time).get(link.getId());

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
		BufferedWriter writer2= IOUtils.getBufferedWriter(outputDir+"/analysis/emissionVsCongestion/"+runCase+".hourlyNetworkDelaysAndEmissions.txt");

		try{
			writer2.write("time"+"\t"+"delays_sec"+"\t"+"CO"+"\t"+"CO2_Total"+"\t"+"FC"+"\t"+"HC"+"\t"+"NMHC"+"\t"+"NO2"+"\t"+"NOX"+"\t"+"PM"+"\t"+"SO2"+"\n");
			for(double time : time2EmissionsTotalFilled.keySet()){
				double networkDelay = 0;
				SortedMap<String, Double> networkEmissions = new TreeMap<String, Double>();
				for(Link link : network.getLinks().values()){
					double delay;
					if(time2linkIdDelays.get(time).get(link.getId())==null) delay = 0.0;
					else delay = time2linkIdDelays.get(time).get(link.getId());
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
		logger.info("Finished Writing files.");
	}

	private Scenario loadScenario(String netFile) {
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(netFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		return scenario;
	}

	private SortedMap<Double, Map<Id, SortedMap<String, Double>>> setNonCalculatedEmissions(Map<Double, Map<Id, SortedMap<String, Double>>> time2EmissionsTotal) {
		SortedMap<Double, Map<Id, SortedMap<String, Double>>> time2EmissionsTotalFilled = new TreeMap<Double, Map<Id, SortedMap<String, Double>>>();

		for(double endOfTimeInterval : time2EmissionsTotal.keySet()){
			Map<Id, SortedMap<String, Double>> emissionsTotalFilled = emissionUtils.setNonCalculatedEmissionsForNetwork(network, time2EmissionsTotal.get(endOfTimeInterval));
			time2EmissionsTotalFilled.put(endOfTimeInterval, emissionsTotalFilled);
		}
		return time2EmissionsTotalFilled;
	}
}
