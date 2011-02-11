/* *********************************************************************** *
 * project: org.matsim.*
 * MyPatronComparator.java
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.roadpricing.RoadPricingReaderXMLv1;
import org.matsim.roadpricing.RoadPricingScheme;
import org.xml.sax.SAXException;

/**
 * A class to evaluate the patronage of agents on a given set of links. Agents
 * can be grouped into one or more different categories.
 * 
 * @author johanwjoubert
 */
public class MyPatronComparator {
	private final static Logger log = Logger.getLogger(MyPatronComparator.class);

	/**
	 * Implements the {@link MyPatronComparator}.
	 * 
	 * <h4>Note:</h4> Multiple breaks can be given to categorize agents, for 
	 * example to distinguish between commuter and commercial vehicle agents.
	 *  
	 * @param args a String array of arguments that must at least contain the
	 * 		following first arguments, and in the following order:
	 * <ol>
	 * 	<li> <b>baseline</b> {@code *.events.xml.gz} file that will be analyzed.
	 * 	<li> <b>comparison</b> {@code *.events.xml.gz} file that will be analyzed.
	 * 	<li> <b>linkIds</b> a text file containing linkIds that will be monitored
	 * 		in terms of agent arrival.
	 * 	<br><br>
	 * 	<li> <b>upper agent Id value</b> for the first category of agents that 
	 * 		will be monitored. All agents with an Id <i>smaller than</i> (not equal 
	 * 		to) will be added in this class.
	 *  <li>... subsequent category upper limits if more than one class is 
	 *  		required. These arguments will be parsed until the end of the
	 *  		String array.
	 * </ol>
	 * @author johanwjoubert
	 */
	public static void main(String[] args) {
		log.info("===============================================================================");
		log.info(" Comparing the agents using tolled links.");
		log.info("-------------------------------------------------------------------------------");
		String baseFilename = args[0];
		String compareFilename = args[1];
		String linksFilename = args[2];
		
		List<Id> breakList = new ArrayList<Id>();
		for(int i = 3; i < args.length; i++){
			breakList.add(new IdImpl(args[i]));
		}
		
		MyPatronComparator mpc = new MyPatronComparator();
		List<Id> linkList = mpc.readLinksFromRoadPicing(linksFilename);
		
		/* Read the baseline file and perform some analysis. */
		log.info("-------------------------------------------------------------------------------");
		List<Map<Id,Integer>> baseMaps= mpc.processEventsFile(baseFilename, linkList, breakList);
		mpc.reportOccurences(baseMaps);
		log.info("-------------------------------------------------------------------------------");
		List<Map<Id,Integer>> compareMaps= mpc.processEventsFile(compareFilename, linkList, breakList);
		mpc.reportOccurences(compareMaps);		
		
		log.info("-------------------------------------------------------------------------------");
		log.info("                                 Completed");
		log.info("===============================================================================");

	}
	
	/**
	 * Class to compare the patronage of agents on specific links. For example, 
	 * in the South African case of road pricing, this class is used to compare
	 * how many agents make use of a given set of links (cordon toll).
	 */
	public MyPatronComparator() {

	}
	
	
	/**
	 * Reports the patronage for each of the agent categories. The results are
	 * presented in a manner usable for a histogram: indicating the frequency
	 * of use by different patrons, i.e. how many people have entered observed
	 * links once, twice, etc.
	 * @param maps output from {@link MyPatronComparator#processEventsFile(String, List, List)}
	 */
	public void reportOccurences(List<Map<Id,Integer>> maps){
		for(int m = 0; m < maps.size(); m++){
			Map<Id,Integer> theMap = maps.get(m);
			log.info(" Map " + (m+1) + " (" + theMap.size() + " agents)");
			
			/* Check what the minimum and maximum usage was by any one agent. */
			Integer min = Integer.MAX_VALUE;
			Integer max = Integer.MIN_VALUE;
			for(Id id : theMap.keySet()){
				min = Math.min(min, theMap.get(id));
				max = Math.max(max, theMap.get(id));				
			}
			log.info("    Min usage: " + min + "; Max usage: " + max);
			
			/* Create a histogram of link usages. */
			List<Id> histList = new ArrayList<Id>();
			Map<Id,Integer> histMap = new HashMap<Id, Integer>();
			for(long h = min; h <= max; h++){
				Id id = new IdImpl(h);
				histList.add(id);
				histMap.put(id, new Integer(0));
			}
			for(Id id : theMap.keySet()){
				int value = theMap.get(id);
				histMap.put(new IdImpl(value), new Integer(histMap.get(new IdImpl(value))+1));
			}
			String s = "";
			for(Id id: histList){
				s += id.toString() + " (" + histMap.get(id) + "); ";
			}
			log.info("    Link usage histogram:");
			log.info("    " + s);			
		}	
	}
	
	
	/**
	 * Parses an events file, checking for {@link LinkEnterEvent}s on a given set
	 * of {@link Link}s. The events are handled in a way to provide a {@link Map}
	 * of people entering the observed links, as well as how many times each person
	 * has entered observed links.
	 * @param eventsFile
	 * @param linkList
	 * @param breakList a {@link List} of {@link Person} {@link Id}s that mark 
	 * 		the upper limit (not including) of the category of agents. (JJ, I 
	 * 		guess we assume the list is sorted in ascending order).  
	 * @return a {@link List} of {@link Map}s (one for each agent category) of 
	 * 		people that entered (on or more times) an observed link, as well as 
	 * 		the	<i>number</i> of times that agent entered observed links.
	 * @see MyPatronLinkEntryHandler
	 */
	public List<Map<Id,Integer>> processEventsFile(String eventsFile, List<Id> linkList, List<Id> breakList){
		log.info("Processing events from " + eventsFile);
		EventsManager em = new EventsManagerImpl();
		MyPatronLinkEntryHandler eh = new MyPatronLinkEntryHandler(linkList, breakList);
		em.addHandler(eh);
		MatsimEventsReader mer = new MatsimEventsReader(em);
		mer.readFile(eventsFile);
		
		return eh.getMaps();
	}
	
	
	/**
	 * Reads a {@link RoadPricingScheme} from file, and uses the tolled link
	 * Ids as links to observe.
	 * @param linksFile
	 * @return {@link List} of {@link Id}s of {@link Link}s to observe.
	 */
	public List<Id> readLinksFromRoadPicing(String linksFile){
		log.info("Reading tolled links to compare from " + linksFile);		
		List<Id> list = new ArrayList<Id>();
		RoadPricingScheme rps = new RoadPricingScheme();
		RoadPricingReaderXMLv1 rpr = new RoadPricingReaderXMLv1(rps);
		try {
			rpr.parse(linksFile);
		} catch (SAXException e1) {
			e1.printStackTrace();
		} catch (ParserConfigurationException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		for(Id i : rps.getLinkIdSet()){
			list.add(i);
		}
		log.info("Read " + list.size() + " tolled link Ids");
		return list;
	}

}

