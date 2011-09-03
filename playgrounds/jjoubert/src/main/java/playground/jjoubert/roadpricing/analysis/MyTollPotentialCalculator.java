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

package playground.jjoubert.roadpricing.analysis;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.roadpricing.RoadPricingReaderXMLv1;
import org.matsim.roadpricing.RoadPricingScheme;

public class MyTollPotentialCalculator {
	private final static Logger log = Logger.getLogger(MyTollPotentialCalculator.class);
	private final RoadPricingScheme scheme;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		log.info("===============================================================================");
		log.info(" Determining the toll potential of agents.");
		log.info("-------------------------------------------------------------------------------");
		String baseFilename = args[0];
		String linksFilename = args[1];
		String outputFolder = args[2];
		
		List<Id> breakList = new ArrayList<Id>();
		for(int i = 3; i < args.length; i++){
			breakList.add(new IdImpl(args[i]));
		}

		MyTollPotentialCalculator mtpc = new MyTollPotentialCalculator(linksFilename);
		List<Id> linkList = mtpc.readLinkIdsFromRoadPricingScheme();
		
		/* Read the baseline file and perform some analysis. */
		log.info("-------------------------------------------------------------------------------");
		List<Map<Id,Double>> baseMaps = mtpc.processEventsFile(baseFilename, linkList, breakList, mtpc.scheme);
		mtpc.writeMaps(outputFolder, baseMaps);
		
		log.info("-------------------------------------------------------------------------------");
		log.info("                                 Completed");
		log.info("===============================================================================");
	}
	
	
	private void writeMaps(String outputFolder, List<Map<Id, Double>> maps) {
		File folder = new File(outputFolder);
		if(!folder.isDirectory() || !folder.canWrite()){
			throw new RuntimeException("Cannot write output maps to " + outputFolder);
		}
		log.info("Writing maps to " + outputFolder);
		
		BufferedWriter bw = null;
		for(int i = 0; i < maps.size(); i++){
			try {
				bw = IOUtils.getBufferedWriter(String.format("%sMaps_%02d.txt", outputFolder, (i+1)));
				try{
					for(Id id : maps.get(i).keySet()){
						bw.write(id.toString());
						bw.write(",");
						bw.write(String.valueOf(maps.get(i).get(id)));
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

	
	public MyTollPotentialCalculator(String roadpricingFilename) {
		log.info("Reading tolled links from " + roadpricingFilename);		
		scheme = new RoadPricingScheme();
		RoadPricingReaderXMLv1 rpr = new RoadPricingReaderXMLv1(scheme);
		rpr.parse(roadpricingFilename);		
	}
	
	private List<Id> readLinkIdsFromRoadPricingScheme(){
		List<Id> list = new ArrayList<Id>();
		for(Id i : this.scheme.getLinkIdSet()){
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
	 * @param scheme the {@link RoadPricingScheme} 
	 * @return a {@link List} of {@link Map}s (one for each agent category) of 
	 * 		people that entered (on or more times) an observed link, as well as 
	 * 		the	<i>number</i> of times that agent entered observed links.
	 * @see MyTollPotentialEventHandler
	 */
	public List<Map<Id,Double>> processEventsFile(String eventsFile, List<Id> linkList, List<Id> breakList, RoadPricingScheme scheme){
		log.info("Processing events from " + eventsFile);
		EventsManager em = EventsUtils.createEventsManager();
		MyTollPotentialEventHandler eh = new MyTollPotentialEventHandler(linkList, breakList, scheme);
		em.addHandler(eh);
		MatsimEventsReader mer = new MatsimEventsReader(em);
		mer.readFile(eventsFile);
		
		return eh.getMaps();
	}

}

