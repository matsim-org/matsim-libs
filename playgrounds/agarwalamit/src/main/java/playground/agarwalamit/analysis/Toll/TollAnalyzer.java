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
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.munich.utils.ExtendedPersonFilter;
import playground.agarwalamit.utils.LoadMyScenarios;
import playground.agarwalamit.utils.MapUtils;
import playground.benjamin.scenarios.munich.analysis.filter.UserGroup;
import playground.vsp.analysis.modules.AbstractAnalysisModule;

/**
 * @author amit
 */

public class TollAnalyzer extends AbstractAnalysisModule {
	private static final Logger LOG = Logger.getLogger(TollAnalyzer.class);
	private final String eventsFile;
	private FilteredTollHandler handler;

	/**
	 * No filtering will be used, result will include all links, persons from all user groups.
	 */
	public TollAnalyzer (final String eventsFile, final double simulationEndTime, final int noOfTimeBins) {
		this(eventsFile, simulationEndTime, noOfTimeBins, null, null, null);
	}

	/**
	 * User group filtering will be used, result will include all links but persons from given user group only.
	 */
	public TollAnalyzer (final String eventsFile, final double simulationEndTime, final int noOfTimeBins, final String ug) {
		this(eventsFile, simulationEndTime, noOfTimeBins, null, null, ug);
	}

	/**
	 * Area filtering will be used, result will include links falls inside the given shape and persons from all user groups.
	 */
	public TollAnalyzer (final String eventsFile, final double simulationEndTime, final int noOfTimeBins, final String shapeFile, 
			final Network network) {
		this(eventsFile, simulationEndTime, noOfTimeBins, shapeFile, network, null);
	}

	/**
	 * Area and user group filtering will be used, links fall inside the given shape and persons belongs to the given user group will be considered.
	 */
	public TollAnalyzer (final String eventsFile, final double simulationEndTime, final int noOfTimeBins, final String shapeFile, 
			final Network network, final String userGroup ) {
		super(TollAnalyzer.class.getSimpleName());
		this.eventsFile = eventsFile;
		this.handler = new FilteredTollHandler(simulationEndTime, noOfTimeBins, shapeFile, network, userGroup);
	}

	public static void main(String[] args) {
		ExtendedPersonFilter pf = new ExtendedPersonFilter();

		String scenario = "ei";
		String eventsFile = "../../../../repos/runs-svn/detEval/emissionCongestionInternalization/hEART/output/"+scenario+"/ITERS/it.1500/1500.events.xml.gz";
		String configFile = "../../../../repos/runs-svn/detEval/emissionCongestionInternalization/hEART/output/"+scenario+"/output_config.xml.gz";
		String outputFolder = "../../../../repos/runs-svn/detEval/emissionCongestionInternalization/hEART/output/"+scenario+"/analysis/";
		String networkFile = "../../../../repos/runs-svn/detEval/emissionCongestionInternalization/hEART/output/"+scenario+"/output_network.xml.gz";

		String shapeFileCity = "../../../../repos/shared-svn/projects/detailedEval/Net/shapeFromVISUM/urbanSuburban/cityArea.shp";
		String shapeFileMMA = "../../../../repos/shared-svn/projects/detailedEval/Net/boundaryArea/munichMetroArea_correctedCRS_simplified.shp";

		String [] areas = {shapeFileCity, shapeFileMMA, null};
		String [] areasName = {"city","MMA","wholeArea"};
		double simEndTime = LoadMyScenarios.getSimulationEndTime(configFile);
		Network network = LoadMyScenarios.loadScenarioFromNetwork(networkFile).getNetwork();

		SortedMap<String, SortedMap<String, Double>> area2usrGrp2Toll = new TreeMap<>();

		for(int ii=0; ii<areas.length;ii++) {
			SortedMap<String, Double> usrGrpToToll = new TreeMap<>();
			for( UserGroup ug : UserGroup.values() ) {
				String myUg = pf.getMyUserGroup(ug);
				if(usrGrpToToll.containsKey(myUg)) continue;

				TollAnalyzer ata = new TollAnalyzer(eventsFile, simEndTime, 30, areas[ii], network, myUg);
				ata.preProcessData();
				ata.postProcessData();
				usrGrpToToll.put(myUg, MapUtils.doubleValueSum( ata.getTimeBin2Toll() ) );
			}
			usrGrpToToll.put("allPersons", MapUtils.doubleValueSum(usrGrpToToll));
			area2usrGrp2Toll.put(areasName[ii], usrGrpToToll);
		}

		BufferedWriter writer = IOUtils.getBufferedWriter(outputFolder+"/areaToUserGroupToToll.txt");
		try {
			writer.write("area \t userGroup \t tollInEUR \n");
			for(String a : area2usrGrp2Toll.keySet()) {
				for(String s : area2usrGrp2Toll.get(a).keySet()) {
					writer.write(a+"\t"+s+"\t"+area2usrGrp2Toll.get(a).get(s)+"\n");
				}
			}
			writer.close();
		} catch (IOException e) {
			throw new RuntimeException("Data is not written. Reason "+e);
		}
	}

	@Override
	public List<EventHandler> getEventHandler() {
		return null;
	}

	@Override
	public void preProcessData() {
		EventsManager events = EventsUtils.createEventsManager();
		MatsimEventsReader reader = new MatsimEventsReader(events);
		events.addHandler(handler);
		reader.readFile(eventsFile);
	}

	@Override
	public void postProcessData() {
		//nothing to do
	}

	/**
	 * @return time bin to person id to toll value after filtering if any
	 */
	public SortedMap<Double,Map<Id<Person>,Double>> getTimeBin2Person2Toll() {
		return this.handler.getTimeBin2Person2Toll();
	}

	/**
	 * @return timeBin to toll values after filtering if any
	 */
	public SortedMap<Double,Double> getTimeBin2Toll(){
		return this.handler.getTimeBin2Toll();
	}

	@Override
	public void writeResults(String outputFolder) {
		writeResults(outputFolder, "");
	}

	public void writeResults(final String outputFolder, final String prefix){
		SortedMap<Double,Double> timeBin2Toll = this.handler.getTimeBin2Toll();

		BufferedWriter writer = IOUtils.getBufferedWriter(outputFolder+"/timeBin2TollValues"+prefix+".txt");
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

	public void writeRDataForBoxPlot(final String outputFolder, final String prefix, final boolean isWritingDataForEachTimeInterval){
		if( ! new File(outputFolder+"/boxPlot/").exists()) new File(outputFolder+"/boxPlot/").mkdirs();

		SortedMap<Double, Map<Id<Person>,Double>> time2person2toll = handler.getTimeBin2Person2Toll();

		if(! isWritingDataForEachTimeInterval) {
			LOG.info("Writing toll/trip for whole day for each user group. This data is likely to be suitable for box plot in R.");
			BufferedWriter writer = IOUtils.getBufferedWriter(outputFolder+"/boxPlot/toll_"+prefix+".txt");
			try {
				// sum all the values for different time bins
				Map<Id<Person>,Double> personToll  = new HashMap<Id<Person>, Double>();
				for (double d : time2person2toll.keySet()){
					for( Id<Person> person : time2person2toll.get(d).keySet() ) {
						if(personToll.containsKey(person)) personToll.put(person, personToll.get(person) + time2person2toll.get(d).get(person) );
						else personToll.put(person, time2person2toll.get(d).get(person) );
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
			LOG.warn("Writing toll/trip for each time bin and for each user group. Thus, this will write many files for each user group. This data is likely to be suitable for box plot in R. ");
			try {
				for (double d : time2person2toll.keySet()){
					BufferedWriter writer = IOUtils.getBufferedWriter(outputFolder+"/boxPlot/toll_"+prefix+"_"+((int) d/3600 +1)+"h.txt");
					for( Id<Person> person : time2person2toll.get(d).keySet() ) {
						writer.write(time2person2toll.get(d).get(person)+"\n");
					}
					writer.close();
				}
			} catch (Exception e) {
				throw new RuntimeException("Data is not written in file. Reason: " + e);
			}
		}
	}
}