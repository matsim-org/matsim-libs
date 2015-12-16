/* *********************************************************************** *
 * project: org.matsim.*
 * MyTollPotentialCalculator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.southafrica.gauteng.analysis;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.roadpricing.RoadPricingConfigGroup;
import org.matsim.roadpricing.RoadPricingReaderXMLv1;
import org.matsim.roadpricing.RoadPricingSchemeImpl;
import org.matsim.roadpricing.RoadPricingSchemeUsingTollFactor;
import org.matsim.vehicles.Vehicle;

import playground.southafrica.utilities.Header;

public class MyTollPotentialCalculator {
	private final static Logger log = Logger.getLogger(MyTollPotentialCalculator.class);
	private final RoadPricingSchemeImpl scheme;
	private Scenario sc;
	private List<Map<Id<Vehicle>, Double>> valueMaps;
	private List<Map<Id<Vehicle>, Integer>> countMaps;

	/**
	 * Implementing the class to calculate the potential toll. 
	 * @param args the following arguments are required, and in the following order:
	 * <ol>
	 * 	<li> events file from which agent plans are analyzed;
	 * 	<li> file road pricing file (should be in the format according to
	 * 		 {@link http://www.matsim.org/files/dtd/roadpricing_v1.dtd}
	 * 	<li> the output folder to which the maps are written;
	 * 	<li> list of agent Ids that indicate the upper limit of a subgroup. A separate
	 * 		 map will be written for each subgroup of agents.
	 * </ol>
	 */
	public static void main(String[] args) {
		Header.printHeader(MyTollPotentialCalculator.class.toString(), args);
		String baseFilename = args[0];
		String linksFilename = args[1];
		String populationFilename = args[2];
		String networkFilename = args[3];
		String outputFolder = args[4];
		
		List<Id<Link>> breakList = new ArrayList<>();
		for(int i = 5; i < args.length; i++){
			breakList.add(Id.create(args[i], Link.class));
		}

		MyTollPotentialCalculator mtpc = new MyTollPotentialCalculator();
		List<Id<Link>> linkList = mtpc.readLinkIdsFromRoadPricingScheme(linksFilename);
		mtpc.readNetwork(networkFilename);
		mtpc.readPopulation(populationFilename);

        ConfigUtils.addOrGetModule(mtpc.getScenario().getConfig(), RoadPricingConfigGroup.GROUP_NAME, RoadPricingConfigGroup.class).setTollLinksFile(linksFilename);
		
		/* Read the baseline file and perform some analysis. */
		log.info("-------------------------------------------------------------------------------");
        String tollLinksFileName = ConfigUtils.addOrGetModule(mtpc.getScenario().getConfig(), RoadPricingConfigGroup.GROUP_NAME, RoadPricingConfigGroup.class).getTollLinksFile() ;
        
        
//		GautengRoadPricingScheme scheme = new GautengRoadPricingScheme(tollLinksFileName, mtpc.getScenario().getNetwork(), 
//				mtpc.getScenario().getPopulation(), new SanralTollFactor());
        RoadPricingSchemeUsingTollFactor scheme = null ;
        System.err.println("SanralTollFactor does no longer exist; only exists with subpopulations.  Aborting ...");
        System.exit(-1) ;
		
		mtpc.processEventsFile(baseFilename, linkList, breakList, scheme);
		mtpc.writeMaps(outputFolder);
		
		log.info("-------------------------------------------------------------------------------");
		log.info("                                 Completed");
		log.info("===============================================================================");
	}
	
	private void readNetwork(String filename){
		MatsimNetworkReader mnr = new MatsimNetworkReader(sc);
		mnr.parse(filename);
	}
	
	private void readPopulation(String filename){
		MatsimPopulationReader mpr = new MatsimPopulationReader(sc);
		mpr.parse(filename);
	}
	
	public Scenario getScenario(){
		return this.sc;
	}
	
	private void writeMaps(String outputFolder) {
		File folder = new File(outputFolder);
		if(!folder.isDirectory() || !folder.canWrite()){
			throw new RuntimeException("Cannot write output maps to " + outputFolder);
		}
		
		log.info("Writing value maps to " + outputFolder);
		BufferedWriter bw = null;
		for(int i = 0; i < this.valueMaps.size(); i++){
			try {
				bw = IOUtils.getBufferedWriter(String.format("%sValueMaps_%02d.txt", outputFolder, (i+1)));
				try{
					for(Id<Vehicle> id : this.valueMaps.get(i).keySet()){
						bw.write(id.toString());
						bw.write(",");
						bw.write(String.valueOf(this.valueMaps.get(i).get(id)));
						bw.newLine();
					}
				} finally{
					bw.close();
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	
		log.info("Writing count maps to " + outputFolder);
		bw = null;
		for(int i = 0; i < this.countMaps.size(); i++){
			try {
				bw = IOUtils.getBufferedWriter(String.format("%sCountMaps_%02d.txt", outputFolder, (i+1)));
				try{
					for(Id<Vehicle> id : this.countMaps.get(i).keySet()){
						bw.write(id.toString());
						bw.write(",");
						bw.write(String.valueOf(this.countMaps.get(i).get(id)));
						bw.newLine();
					}
				} finally{
					bw.close();
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	
	public MyTollPotentialCalculator() {
		scheme = new RoadPricingSchemeImpl();
		sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
	}
	
	
	private List<Id<Link>> readLinkIdsFromRoadPricingScheme(String roadpricingFilename){
		log.info("Reading tolled links from " + roadpricingFilename);		
		RoadPricingReaderXMLv1 rpr = new RoadPricingReaderXMLv1(scheme);
		rpr.parse(roadpricingFilename);		
		List<Id<Link>> list = new ArrayList<>();
		for(Id<Link> i : this.scheme.getTolledLinkIds()){
			list.add(i);
		}
		log.info("Read " + list.size() + " tolled link Ids");
		return list;
	}
	

	/**
	 * Parses an events file, checking for {@link LinkEnterEvent}s on a given set
	 * of {@link Link}s. The events are handled in a way to provide a {@link Map}
	 * of people entering the observed links, as well as their accumulated toll;
	 * @param eventsFile
	 * @param linkList
	 * @param breakList a {@link List} of {@link Person} {@link Id}s that mark 
	 * 		the upper limit (not including) of the category of agents. (JJ, I 
	 * 		guess we assume the list is sorted in ascending order).  
	 * @param scheme the {@link RoadPricingSchemeImpl} 
	 * @return a {@link List} of {@link Map}s (one for each agent category) of 
	 * 		people that entered (one or more times) an observed link, as well as 
	 * 		the	<i>number</i> of times that agent entered observed links.
	 * @see MyTollPotentialEventHandler
	 */
	public void processEventsFile(String eventsFile, List<Id<Link>> linkList, List<Id<Link>> breakList, RoadPricingSchemeUsingTollFactor scheme){
		log.info("Processing events from " + eventsFile);
		EventsManager em = EventsUtils.createEventsManager();
		MyTollPotentialEventHandler eh = new MyTollPotentialEventHandler(linkList, breakList, scheme);
		em.addHandler(eh);
		MatsimEventsReader mer = new MatsimEventsReader(em);
		mer.readFile(eventsFile);
		
		this.countMaps = eh.getCountMaps();
		this.valueMaps = eh.getValueMaps();
	}

}

