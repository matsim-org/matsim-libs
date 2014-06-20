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
import java.util.HashMap;
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

import playground.agarwalamit.siouxFalls.congestionAnalyzer.CongestionLinkAnalyzer;
import playground.agarwalamit.siouxFalls.emissionAnalyzer.EmissionLinkAnalyzer;
import playground.agarwalamit.siouxFalls.emissionAnalyzer.PerLinkEmissionData;

/**
 * @author amit
 */
public class EmissionsAndCongestionsPerTimeBin {

	private Logger logger = Logger.getLogger(PerLinkEmissionData.class);
	private static String outputDir = "/Users/aagarwal/Desktop/ils4/agarwal/siouxFalls/outputMCOff/run112/";
	private static String networkFile =outputDir+ "/output_network.xml.gz";
	private static String configFile = outputDir+"/output_config.xml";
	private static String emissionEventFile = outputDir+"/ITERS/it.100/100.emission.events.xml.gz";
	private static String eventsFile = outputDir+"/ITERS/it.100/100.events.xml.gz";
	EmissionUtils emissionUtils;

	Network network;
	final int noOfTimeBin = 30;
	public static void main(String[] args)  {

		EmissionsAndCongestionsPerTimeBin emissionsVsCongestionData = new EmissionsAndCongestionsPerTimeBin();
		emissionsVsCongestionData.run();
	}

	private void run()  {

		BufferedWriter writer1 = IOUtils.getBufferedWriter(outputDir+"/analysis/emissionVsCongestion/100.hourlyDelaysAndEmissionsPerLink.txt");

		Scenario scenario = loadScenario(networkFile);
		this.network = scenario.getNetwork();
		EmissionLinkAnalyzer eLinkAnalyzer = new EmissionLinkAnalyzer(configFile, emissionEventFile,noOfTimeBin);
		eLinkAnalyzer.init(null);
		eLinkAnalyzer.preProcessData();
		eLinkAnalyzer.postProcessData();

		emissionUtils = new EmissionUtils();

		Map<Double, Map<Id, SortedMap<String, Double>>> time2EmissionsTotal = eLinkAnalyzer.getLink2TotalEmissions();
		Map<Double, Map<Id, SortedMap<String, Double>>> time2EmissionsTotalFilled = setNonCalculatedEmissions(time2EmissionsTotal);

		CongestionLinkAnalyzer cLinkAnalyzer = new CongestionLinkAnalyzer(configFile, eventsFile,noOfTimeBin);
		cLinkAnalyzer.init(scenario);
		cLinkAnalyzer.preProcessData();
		cLinkAnalyzer.postProcessData();
		cLinkAnalyzer.checkTotalDelayUsingAlternativeMethod();

		Map<Double, Map<Id, Double>> time2linkIdDelays = cLinkAnalyzer.getCongestionPerLinkTimeInterval();



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
		BufferedWriter writer2= IOUtils.getBufferedWriter(outputDir+"/analysis/emissionVsCongestion/100.hourlyNetworkDelaysAndEmissions.txt");

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

	private Map<Double, Map<Id, SortedMap<String, Double>>> setNonCalculatedEmissions(Map<Double, Map<Id, SortedMap<String, Double>>> time2EmissionsTotal) {
		Map<Double, Map<Id, SortedMap<String, Double>>> time2EmissionsTotalFilled = new HashMap<Double, Map<Id, SortedMap<String, Double>>>();

		for(double endOfTimeInterval : time2EmissionsTotal.keySet()){
			Map<Id, SortedMap<String, Double>> emissionsTotalFilled = emissionUtils.setNonCalculatedEmissionsForNetwork(network, time2EmissionsTotal.get(endOfTimeInterval));
			time2EmissionsTotalFilled.put(endOfTimeInterval, emissionsTotalFilled);
		}
		return time2EmissionsTotalFilled;
	}
}
