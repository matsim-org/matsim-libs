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
package playground.agarwalamit.analysis.Toll;

import java.io.BufferedWriter;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.utils.LoadMyScenarios;
import playground.benjamin.scenarios.munich.analysis.filter.UserGroup;
import playground.vsp.analysis.modules.AbstractAnalysisModule;

/**
 * @author amit
 */

public class AverageTollAnalyzer extends AbstractAnalysisModule {

	private static final Logger log = Logger.getLogger(AverageTollAnalyzer.class);

	public static void main(String[] args) {
		String scenario = "eci";
		String eventsFile = "../../../../repos/runs-svn/detEval/emissionCongestionInternalization/iatbr/output/"+scenario+"/ITERS/it.1500/1500.events.xml.gz";
		String configFile = "../../../../repos/runs-svn/detEval/emissionCongestionInternalization/iatbr/output/"+scenario+"/output_config.xml.gz";
		String outputFolder = "../../../../repos/runs-svn/detEval/emissionCongestionInternalization/iatbr/output/"+scenario+"/analysis/";
		AverageTollAnalyzer ata = new AverageTollAnalyzer(eventsFile, configFile, 30);
		ata.preProcessData();
		ata.postProcessData();
		//		ata.writeResults(outputFolder);
		ata.writeRDataForBoxPlot(outputFolder,true);
	}

	public AverageTollAnalyzer (String eventsFile, String configFile) {
		super(AverageTollAnalyzer.class.getSimpleName());
		this.eventsFile = eventsFile;
		this.configFile = configFile;
		this.noOfTimeBin = 1;
	}
	
	public AverageTollAnalyzer (String eventsFile, String configFile, int noOfTimeBins) {
		super(AverageTollAnalyzer.class.getSimpleName());
		this.eventsFile = eventsFile;
		this.configFile = configFile;
		this.noOfTimeBin = noOfTimeBins;
	}

	private String eventsFile;
	private String configFile;
	private TollInfoHandler handler;
	private double simulationEndTime;
	private int noOfTimeBin;

	@Override
	public List<EventHandler> getEventHandler() {
		return null;
	}

	@Override
	public void preProcessData() {

		this.simulationEndTime = LoadMyScenarios.getSimulationEndTime(this.configFile);
		this.handler = new TollInfoHandler(this.simulationEndTime, noOfTimeBin);

		EventsManager events = EventsUtils.createEventsManager();
		MatsimEventsReader reader = new MatsimEventsReader(events);
		events.addHandler(handler);
		reader.readFile(eventsFile);
	}

	@Override
	public void postProcessData() {
		//nothing to do
	}

	@Override
	public void writeResults(String outputFolder) {
		SortedMap<Double,Double> timeBin2Toll = this.handler.getTimeBin2Toll();

		BufferedWriter writer = IOUtils.getBufferedWriter(outputFolder+"/timeBin2TollValues.txt");
		try {
			writer.write("timeBin \t toll[EUR] \n");
			for(double d : timeBin2Toll.keySet()){
				writer.write(d+"\t"+timeBin2Toll.get(d)+"\n");
			}
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written in file. Reason: " + e);
		}
	}

	public void writeRDataForBoxPlot(String outputFolder, boolean isWritingDataForEachTimeInterval){
		if( ! new File(outputFolder+"/boxPlot/").exists()) new File(outputFolder+"/boxPlot/").mkdirs();

		SortedMap<UserGroup, SortedMap<Double, Map<Id<Person>,Double>>> userGrp2PersonToll = handler.getUserGrp2TimeBin2Person2Toll();

		for(UserGroup ug : userGrp2PersonToll.keySet()){

			if(! isWritingDataForEachTimeInterval) {
				log.info("Writing toll/trip for whole day for each user group. This data is likely to be suitable for box plot in R.");
				BufferedWriter writer = IOUtils.getBufferedWriter(outputFolder+"/boxPlot/toll_"+ug.toString()+".txt");
				try {
					// sum all the values for different time bins
					Map<Id<Person>,Double> personToll  = new HashMap<Id<Person>, Double>();
					for (double d : userGrp2PersonToll.get(ug).keySet()){
						for( Id<Person> person : userGrp2PersonToll.get(ug).get(d).keySet() ) {
							if(personToll.containsKey(person)) personToll.put(person, personToll.get(person) + userGrp2PersonToll.get(ug).get(d).get(person) );
							else personToll.put(person, userGrp2PersonToll.get(ug).get(d).get(person) );
						}
					}

					for(Id<Person> id : personToll.keySet()){
						writer.write(personToll.get(id)+"\n");
					}
					writer.close();
				} catch (Exception e) {
					throw new RuntimeException("Data is not written in file. Reason: " + e);
				}
			} else {
				log.warn("Writing toll/trip for each time bin and for each user group. Thus, this will write many files for each user group. This data is likely to be suitable for box plot in R. ");
				try {
					for (double d : userGrp2PersonToll.get(ug).keySet()){
						BufferedWriter writer = IOUtils.getBufferedWriter(outputFolder+"/boxPlot/toll_"+ug.toString()+"_"+((int) d/3600 +1)+"h.txt");
						for( Id<Person> person : userGrp2PersonToll.get(ug).get(d).keySet() ) {
							writer.write(userGrp2PersonToll.get(ug).get(d).get(person)+"\n");
						}
						writer.close();
					}
				} catch (Exception e) {
					throw new RuntimeException("Data is not written in file. Reason: " + e);
				}
			}
		}
	}
}
