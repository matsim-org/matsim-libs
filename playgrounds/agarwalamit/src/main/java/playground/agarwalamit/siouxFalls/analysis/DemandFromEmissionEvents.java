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
package playground.agarwalamit.siouxFalls.analysis;

import java.io.BufferedWriter;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.emissions.events.EmissionEventsReader;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.utils.LoadMyScenarios;
import playground.agarwalamit.utils.MapUtils;
import playground.benjamin.scenarios.munich.analysis.nectar.EmissionsPerLinkWarmEventHandler;

/**
 * @author amit
 */
public class DemandFromEmissionEvents {
	private final Logger logger = Logger.getLogger(DemandFromEmissionEvents.class);

	private final String runDir = "/Users/aagarwal/Desktop/ils4/agarwal/munich/output/1pct/";
    private double simulationEndTime;

    private final String [] runNumber =  {"baseCaseCtd","ei"};
    private Network network;

	public static void main(String[] args) {
		new DemandFromEmissionEvents().writeDemandData();
	}

	private void writeDemandData(){

        String netFile1 = "/Users/aagarwal/Desktop/ils4/agarwal/munich/input/network-86-85-87-84_simplifiedWithStrongLinkMerge---withLanes.xml";
        this.network = LoadMyScenarios.loadScenarioFromNetwork(netFile1).getNetwork();

        String configFile = "/Users/aagarwal/Desktop/ils4/agarwal/munich/input/config_munich_1pct_baseCaseCtd.xml";
        this.simulationEndTime = LoadMyScenarios.getSimulationEndTime(configFile);

		Map<Double, Map<Id<Link>, Double>> demandBAU = filterLinks(processEmissionsAndReturnDemand(runNumber[0])); 
		//		Map<Double, Map<Id, Double>> demandPolicy = filterLinks(processEmissionsAndReturnDemand(runNumber[1]));

		writeAbsoluteDemand(this.runDir+this.runNumber[0]+"/analysis/emissionVsCongestion/hourlyNetworkDemand.txt", demandBAU);
		//		writeAbsoluteDemand(runDir+runNumber[1]+"/analysis/emissionVsCongestion/hourlyNetworkDemand.txt", demandPolicy);

		//		writeChangeInDemand(runDir+runNumber[1]+"/analysis/emissionVsCongestion/hourlyChangeInNetworkDemandWRTBAU.txt", demandBAU, demandPolicy);

		this.logger.info("Writing file(s) is finished.");
	}

	private void writeAbsoluteDemand(String outputFolder,Map<Double, Map<Id<Link>, Double>> linkCountMap ){
		BufferedWriter writer = IOUtils.getBufferedWriter(outputFolder);
		try {
			writer.write("time \t linkCounts  \n");

			for(double time :linkCountMap.keySet()){
				double hrDemand =0;
				writer.write(time+"\t");
				hrDemand = MapUtils.doubleValueSum(linkCountMap.get(time));
				writer.write(hrDemand+"\n");
			}
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written into file. Reason : "+e);
		}
	}

	private void writeChangeInDemand(String outputFolder,Map<Double, Map<Id<Link>, Double>> linkCountMapBAU, Map<Double, Map<Id<Link>, Double>> linkCountMapPolicy ){
		BufferedWriter writer = IOUtils.getBufferedWriter(outputFolder);
		try {
			writer.write("time \t %ChangeInlinkCounts  \n");

			for(double time :linkCountMapBAU.keySet()){
				double hrDemandBAU =0;
				double hrDemandPolicy=0;
				writer.write(time+"\t");
				for(Id<Link> id:linkCountMapBAU.get(time).keySet()){
					hrDemandBAU += linkCountMapBAU.get(time).get(id);
					double tempPolicyDemand =0;
					if(linkCountMapPolicy.get(time).get(id)!=null){
						tempPolicyDemand = linkCountMapPolicy.get(time).get(id);
					} else tempPolicyDemand =0;
					hrDemandPolicy += tempPolicyDemand; 

				}
				writer.write(percentageChange(hrDemandBAU, hrDemandPolicy)+"\n");
			}
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written into file. Reason : "+e);
		}
	}

	private Map<Double, Map<Id<Link>,  Double>> filterLinks (Map<Double, Map<Id<Link>, Double>> time2LinksData) {
		Map<Double, Map<Id<Link>, Double>> time2LinksDataFiltered = new HashMap<>();

		for(Double endOfTimeInterval : time2LinksData.keySet()){
			Map<Id<Link>,  Double> linksData = time2LinksData.get(endOfTimeInterval);
			Map<Id<Link>, Double> linksDataFiltered = new HashMap<>();

			for(Link link : this.network.getLinks().values()){
				Id<Link> linkId = link.getId();

				if(linksData.get(linkId) == null){
					linksDataFiltered.put(linkId, 0.);
				} else {
					linksDataFiltered.put(linkId, linksData.get(linkId));
				}
			}
			time2LinksDataFiltered.put(endOfTimeInterval, linksDataFiltered);
		}
		return time2LinksDataFiltered;
	}
	private Map<Double, Map<Id<Link>, Double>> processEmissionsAndReturnDemand(String runNumber){
		String emissionFileBAU = this.runDir+runNumber+"/ITERS/it.1500/1500.emission.events.xml.gz";

		EventsManager eventsManager = EventsUtils.createEventsManager();
		EmissionEventsReader emissionReader = new EmissionEventsReader(eventsManager);

        int noOfTimeBins = 30;
        EmissionsPerLinkWarmEventHandler warmHandler = new EmissionsPerLinkWarmEventHandler(this.simulationEndTime,
                noOfTimeBins);
		eventsManager.addHandler(warmHandler);
		emissionReader.readFile(emissionFileBAU);
		return warmHandler.getTime2linkIdLeaveCount();
	}

	private double percentageChange(double firstNr, double secondNr){
		if(firstNr!=0) return (secondNr-firstNr)*100/firstNr;
		else return 0.;
	}
}
