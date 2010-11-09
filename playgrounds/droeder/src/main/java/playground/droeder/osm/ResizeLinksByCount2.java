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
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsReaderMatsimV1;
import org.matsim.counts.CountsWriter;
import org.matsim.counts.Volume;
import org.xml.sax.SAXException;

import playground.andreas.osmBB.osm2counts.Osm2Counts;

/**
 * @author droeder
 */
public class ResizeLinksByCount2 extends AbstractResizeLinksByCount{
	
	private static final Logger log = Logger.getLogger(ResizeLinksByCount2.class);
	private Map<String, List<Id>> origin2counts;
	
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
		
		ResizeLinksByCount2 r = new ResizeLinksByCount2(networkFile, counts, shortNameMap, 1.1);
		r.run("d:/VSP/output/osm_bb/network_resized.xml");
	}
	
	/**
	 * use this contructor if the counts loc_Ids are NOT matched to the linkIds. The shortNameMap 
	 * consists of  toNodeIds mapped to counts cs_Ids!  
	 * @param networkFile
	 * @param counts
	 * @param shortNameMap
	 */
	public ResizeLinksByCount2(String networkFile, Counts counts, Map<String, String> shortNameMap, double scaleFactor){
		super(networkFile, counts, shortNameMap, scaleFactor);
	}
	
	/**
	 * use this constructor if counts loc_Ids and linkIds are matched!
	 * @param networkFile
	 * @param counts
	 */
	public ResizeLinksByCount2(String networkFile, Counts counts, Double scaleFactor){
		super(networkFile, counts, scaleFactor);
	}
		
	public void run(String outFile){
		super.run(outFile);
		
	}
	
	private void getCountsOnOrigLink() {
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

	@Override
	protected void resize() {
		log.info("Start resizing...");
		this.getCountsOnOrigLink();
		List<Id> sortedLinks = null;
		for(Entry<String, List<Id>> e : this.origin2counts.entrySet()){
			sortedLinks = sortLinks(e.getKey(), e.getValue().get(0));
			if(e.getValue().size() == 1){
				this.processOneCountOnOrigLink(sortedLinks, e.getValue().get(0));
			}else if(e.getValue().size() > 1){
//				to sort the links it is unimportant how many counts are one the originLink
				this.processMultipleCountsOnOrigLink(sortedLinks, (ArrayList<Id>) e.getValue());
			}else{
				log.error("no count registered for origId " + e.getKey());
			}
			
			sortedLinks = null;
		}
		log.info("resizing finished...");
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
