/* *********************************************************************** *
 * project: org.matsim.*
 * Plansgenerator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.droeder.osm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsReaderMatsimV1;
import org.xml.sax.SAXException;

import playground.andreas.osmBB.osm2counts.Osm2Counts;

/**
 * @author droeder
 *
 */
public class ResizeLinksByCount4 extends AbstractResizeLinksByCount {
	private static final Logger log = Logger.getLogger(ResizeLinksByCount4.class);
	
	public static void main(String[] args){
		String countsFile = "d:/VSP/output/osm_bb/Di-Do_counts.xml";
		String filteredOsmFile = "d:/VSP/output/osm_bb/counts.osm";
		String networkFile = "d:/VSP/output/osm_bb/counts_network.xml";
		
		Osm2Counts osm2Counts = new Osm2Counts(filteredOsmFile);
		osm2Counts.prepareOsm();
		HashMap<String, String> shortNameMap = osm2Counts.getShortNameMap();
		Counts counts = new Counts();
		CountsReaderMatsimV1 countsReader = new CountsReaderMatsimV1(counts);
		try {
			countsReader.parse(countsFile);
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		ResizeLinksByCount4 r = new ResizeLinksByCount4(networkFile, counts, shortNameMap, 1.1);
		r.run("d:/VSP/output/osm_bb/network_resized.xml");
	}
	
	private Counts counts;
	private Map<String, List<Id>> origin2counts;
	private Map<String, List<Id>> origin2sortedLinks;
	private Map<Integer, Tuple<List<Id>, List<Id>>> newLinkLines2Counts;

	/**
	 * @param networkFile
	 * @param counts
	 * @param shortNameMap
	 * @param scaleFactor
	 */
	public ResizeLinksByCount4(String networkFile, Counts counts, Map<String, String> shortNameMap, Double scaleFactor) {
		super(networkFile, counts, shortNameMap, scaleFactor);
		this.origin2sortedLinks = new HashMap<String, List<Id>>();
	}

	/**
	 * @param networkFile
	 * @param counts
	 * @param scaleFactor
	 */
	public ResizeLinksByCount4(String networkFile, Counts counts, Double scaleFactor) {
		super(networkFile, counts, scaleFactor);
		this.origin2sortedLinks = new HashMap<String, List<Id>>();
	}
	
	public void run(String outFile){
		super.run(outFile);
	}

	@Override
	protected void resize() {
		this.counts = this.getOriginalCounts();
		this.findCountsOnOrigLink();
		this.splitOriginLinks();
		List<Id> sortedLinks = null;
		
		log.info("Start resizing...");
		for(Entry<Integer, Tuple<List<Id>, List<Id>>> e: this.newLinkLines2Counts.entrySet()){
			sortedLinks = e.getValue().getFirst();
			if(e.getValue().getSecond().size() == 1){
				this.processOneCountOnOrigLink(sortedLinks, e.getValue().getSecond().get(0));
			}else if(e.getValue().getSecond().size() > 1){
//				to sort the links it is unimportant how many counts are one the originLink
				this.processMultipleCountsOnOrigLink(sortedLinks, (ArrayList<Id>) e.getValue().getSecond());
			}else{
				log.error("no count registered for origId " + e.getKey());
			}
			sortedLinks = null;
		}
		log.info("resizing finished...");
		
	}
	
	private void splitOriginLinks() {
		//preprocess
		for(Entry<String, List<Id>> e : this.origin2counts.entrySet()){
			this.origin2sortedLinks.put(e.getKey(), sortLinks(e.getKey(), e.getValue().get(0)));
		}
		// end
		
		Integer i = 0;
		this.newLinkLines2Counts = new HashMap<Integer, Tuple<List<Id>,List<Id>>>();
		List<Id> tempNewSortedLinks;
		List<Id> countsOnNewSortedLinks;
		
		for(Entry<String, List<Id>> e: this.origin2sortedLinks.entrySet()){
			tempNewSortedLinks = new ArrayList<Id>();
			countsOnNewSortedLinks = new ArrayList<Id>();
			for(Id link : e.getValue()){
				//add link
				tempNewSortedLinks.add(link);
				
				//add count if exists
				if(this.counts.getCounts().containsKey(link)){
					countsOnNewSortedLinks.add(link);
				}
				
				// check if toNode is intersection and add links if there is a count
				if(this.getOriginalLink(link).getToNode().getOutLinks().size() > 1){
					
					//check if at least one count is located on the actual linkLine
					if(countsOnNewSortedLinks.size() > 0){
						this.newLinkLines2Counts.put(i, new Tuple<List<Id>, List<Id>>(tempNewSortedLinks, countsOnNewSortedLinks));
						i++;
					}
					tempNewSortedLinks = new ArrayList<Id>();
					countsOnNewSortedLinks = new ArrayList<Id>();
				}
				// if last link and at least one count on linkLine add anyway
				else if((e.getValue().indexOf(link) == (e.getValue().size() - 1)) && (countsOnNewSortedLinks.size() > 0)){
					this.newLinkLines2Counts.put(i, new Tuple<List<Id>, List<Id>>(tempNewSortedLinks, countsOnNewSortedLinks));
					i++;
				}
				
			}
		}
	}

	private void findCountsOnOrigLink() {
		this.origin2counts = new HashMap<String, List<Id>>();
		String origId;
		for(Id id : this.getOriginalCounts().getCounts().keySet()){
			origId = this.getOriginalLink(id).getOrigId();
			if(this.origin2counts.containsKey(origId)){
				this.origin2counts.get(origId).add(id);
			}else{
				this.origin2counts.put(origId, new ArrayList<Id>());
				this.origin2counts.get(origId).add(id);
			}
		}
	}
	
	private List<Id> sortLinks(String origId, Id countLoc){
		LinkImpl countLocation = this.getOriginalLink(countLoc);
		LinkImpl cursor = countLocation;
		String cursorOrigId = origId;
		List<Id> sortedLinks = new ArrayList<Id>();
		
		//walk backwards
		while(origId.equals(cursorOrigId) ){
			if(sortedLinks.contains(cursor.getId())) break;
			
			sortedLinks.add(0, cursor.getId());
			
			for(Link l : cursor.getFromNode().getInLinks().values()){
				if(((LinkImpl) l).getOrigId().equals(origId) &&
						(!(cursor.getToNode().getId().equals(l.getFromNode().getId())))){
					
					cursor = (LinkImpl) l;
					cursorOrigId = cursor.getOrigId();
					
					break;
				}

			}
		}
		
		cursor = countLocation;
		cursorOrigId = origId;
		
		//walk forward
		while(origId.equals(cursorOrigId)){
			
			for(Link l : cursor.getToNode().getOutLinks().values()){
				if ( (!l.getToNode().getId().equals(cursor.getFromNode().getId())) && 
						((LinkImpl) l).getOrigId().equals(origId)){
					
					cursor = (LinkImpl) l;
					cursorOrigId = cursor.getOrigId();
					break;
				}
			}
			
			if(sortedLinks.contains(cursor.getId())){
				break;
			}else{
				sortedLinks.add(sortedLinks.size(), cursor.getId());
			}
		}
		
		return sortedLinks;
	}
	
	private void processOneCountOnOrigLink(List<Id> sortedLinks, Id countLoc) {
		this.constantChanges(sortedLinks, countLoc);
	}
	
	private void processMultipleCountsOnOrigLink(List<Id> sortedLinks, ArrayList<Id> counts) {
		int temp = 0;
		Id previousCount = null;
		
		for(Id id : sortedLinks){
			if(counts.contains(id) && (temp == 0) ){
				temp++;
				previousCount = id;
				this.constantChanges(sortedLinks.subList(0, sortedLinks.indexOf(id) + 1), id);
			}else if(counts.contains(id) && (temp < counts.size())){
				temp++;
				this.proportionalChanges(sortedLinks.subList(sortedLinks.indexOf(previousCount) + 1, sortedLinks.indexOf(id) ), previousCount, id);
				previousCount = id;
			}else if(temp == counts.size()){
				temp++;
				this.constantChanges(sortedLinks.subList(sortedLinks.indexOf(previousCount), sortedLinks.size() ), previousCount);
			}
		}
		
	}
	
	private void proportionalChanges(List<Id> sortedLinks, Id previousCount, Id actualCount) {
		
		if(sortedLinks.size() > 0){
			double prevCountVal = this.getRescaledCount(previousCount).getMaxVolume().getValue();
			double countDifference = (this.getRescaledCount(actualCount).getMaxVolume().getValue() - 
					this.getRescaledCount(previousCount).getMaxVolume().getValue())/
					(sortedLinks.size() + 1);
			
			
			for(Id id : sortedLinks){
				prevCountVal = prevCountVal + countDifference;
				Link l = getOriginalLink(id);
				double capPerLane = l.getCapacity() / l.getNumberOfLanes();
				int nrOfNewLanes;

				
				// if maxCount < cap set cap to maxCount and keep nrOfLanes
				if(prevCountVal < l.getCapacity()){
					nrOfNewLanes = (int) l.getNumberOfLanes();
				}
				// else set nrOfNewLanes to int(maxCount/capPerLane) and cap to maxCount
				else{
					nrOfNewLanes = (int) (prevCountVal/capPerLane);
				}
				this.setNewLinkData(id, prevCountVal, nrOfNewLanes);
				this.addLink2shp(id);
			}
			
		}
		
	}

	private void constantChanges(List<Id> sortedLinks, Id count){
		double maxCount = this.getRescaledCount(count).getMaxVolume().getValue();
		Link l = this.getOriginalLink(count);
		double capPerLane = l.getCapacity() / l.getNumberOfLanes();
		int nrOfNewLanes;

		
		// if maxCount < cap set cap to maxCount and keep nrOfLanes
		if(maxCount < l.getCapacity()){
			nrOfNewLanes = (int) l.getNumberOfLanes();
		}
		// else set nrOfNewLanes to int(maxCount/capPerLane) and cap to maxCount
		else{
			nrOfNewLanes = (int) (maxCount/capPerLane);
		}
		
		for(Id id : sortedLinks){
			this.setNewLinkData(id, maxCount, nrOfNewLanes);
			this.addLink2shp(id);
		}
	}

}
