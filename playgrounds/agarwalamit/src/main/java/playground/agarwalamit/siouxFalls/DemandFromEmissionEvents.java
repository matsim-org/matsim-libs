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
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.utils.io.IOUtils;

import playground.benjamin.scenarios.munich.analysis.nectar.EmissionsPerLinkWarmEventHandler;
import playground.vsp.emissions.events.EmissionEventsReader;

/**
 * @author amit
 */
public class DemandFromEmissionEvents {
	private final Logger logger = Logger.getLogger(DemandFromEmissionEvents.class);

	private final String runDir = "/Users/aagarwal/Desktop/ils/agarwal/siouxFalls/outputMC/";
	private final int noOfTimeBins = 1;
	private double simulationEndTime;
	private String configFile =runDir+"run1"+"/output_config.xml.gz"; 

	private static String [] runNumber =  {"run1","run2","run3","run4"};


	public static void main(String[] args) {
		new DemandFromEmissionEvents().writeDemandData();
	}

	private void writeDemandData(){
		this.simulationEndTime = getEndTime(configFile);
		Map<Double, Map<Id, Double>> demandBAU = processEmissionsAndReturnDemand(runNumber[0]); 
		Map<Double, Map<Id, Double>> demandEI = processEmissionsAndReturnDemand(runNumber[1]);
		Map<Double, Map<Id, Double>> demandCI = processEmissionsAndReturnDemand(runNumber[2]);
		Map<Double, Map<Id, Double>> demandECI = processEmissionsAndReturnDemand(runNumber[3]);

		BufferedWriter writer = IOUtils.getBufferedWriter(runDir+"/analysis/demandPerLinkPerTimeInterval.txt");
		try {
			writer.write("time \t linkId \t linkCountBAU \t linkCountEI \t linkCountCI \t linkCountECI \n");

			for(double time :demandBAU.keySet()){
				for(Id id: demandBAU.get(time).keySet()){
					writer.write(time+"\t");
					writer.write(id.toString()+"\t");
					writer.write(demandBAU.get(time).get(id)+"\t");
					writer.write(demandEI.get(time).get(id)+"\t");
					writer.write(demandCI.get(time).get(id)+"\t");
					writer.write(demandECI.get(time).get(id)+"\t");
					writer.newLine();
				}
				writer.newLine();
			}
		} catch (Exception e) {
			throw new RuntimeException("Data is not written into file. Reason : "+e);
		}
		logger.info("Writing file(s) is finished.");
	}

	private Map<Double, Map<Id, Double>> processEmissionsAndReturnDemand(String runNumber){
		String emissionFileBAU = runDir+runNumber+"/ITERS/it.100/100.emission.events.xml.gz";

		EventsManager eventsManager = EventsUtils.createEventsManager();
		EmissionEventsReader emissionReader = new EmissionEventsReader(eventsManager);

		EmissionsPerLinkWarmEventHandler warmHandler = new EmissionsPerLinkWarmEventHandler(this.simulationEndTime, noOfTimeBins);
		eventsManager.addHandler(warmHandler);
		emissionReader.parse(emissionFileBAU);
		return warmHandler.getTime2linkIdLeaveCount();
	}

	private Double getEndTime(String configfile) {
		Config config = ConfigUtils.createConfig();
		MatsimConfigReader configReader = new MatsimConfigReader(config);
		configReader.readFile(configfile);
		Double endTime = config.qsim().getEndTime();
		logger.info("Simulation end time is: " + endTime / 3600 + " hours.");
		logger.info("Aggregating emissions for " + (int) (endTime / 3600 / noOfTimeBins) + " hour time bins.");
		return endTime;
	}
}
