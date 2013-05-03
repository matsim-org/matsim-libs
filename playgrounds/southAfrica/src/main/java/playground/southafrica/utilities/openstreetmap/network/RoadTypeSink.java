/* *********************************************************************** *
 * project: org.matsim.*
 * RoadTypeSink.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.southafrica.utilities.openstreetmap.network;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.core.utils.misc.Counter;
import org.openstreetmap.osmosis.core.container.v0_6.BoundContainer;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.container.v0_6.EntityProcessor;
import org.openstreetmap.osmosis.core.container.v0_6.NodeContainer;
import org.openstreetmap.osmosis.core.container.v0_6.RelationContainer;
import org.openstreetmap.osmosis.core.container.v0_6.WayContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.TagCollectionImpl;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;

public class RoadTypeSink implements Sink {
	private final Logger log = Logger.getLogger(RoadTypeSink.class);
	private Map<Long, WayContainer> wayMap;
	private Map<Long, String> highwayTypeMap;
	private Counter wayCounter = new Counter("  ways # ");
	private Counter processCounter = new Counter("  processed # ");
	
	public RoadTypeSink() {
		this.wayMap = new HashMap<Long, WayContainer>();
		this.highwayTypeMap = new HashMap<Long, String>();
		log.info("RoadTypeSink instantiated.");
	}

	@Override
	public void complete() {
		wayCounter.printCounter();
		log.info("Finished reading ways. Processing...");
		Map<String, Integer> types = new HashMap<String, Integer>();
		for(long w : wayMap.keySet()){
			Way way = wayMap.get(w).getEntity();
			Map<String, String> tags = new TagCollectionImpl(way.getTags()).buildMap();
			String highwayType = tags.get("highway");
			if(highwayType != null){
				highwayTypeMap.put(w, highwayType);
				
				if(!types.containsKey(highwayType)){
					types.put(highwayType, 1);
				} else{
					int oldValue = types.get(highwayType);
					types.put(highwayType, oldValue+1);
				}
			}
			processCounter.incCounter();
		}
		processCounter.printCounter();
		
		wayMap = null;
		
		/* Report on the number of observed road types. */
		log.info("--- Summary of road types parsed ------------------------------");
		for(String s : types.keySet()){
			log.info("  " + s + ": " + types.get(s));
		}
		log.info("---------------------------------------------------------------");
	}

	@Override
	public void release() {
		// TODO Auto-generated method stub

	}

	@Override
	public void process(EntityContainer entityContainer) {
		entityContainer.process(new EntityProcessor() {
			
			@Override
			public void process(RelationContainer arg0) {
			}
			
			@Override
			public void process(WayContainer wayContainer) {
				wayMap.put(wayContainer.getEntity().getId(), wayContainer);
				wayCounter.incCounter();
			}
			
			@Override
			public void process(NodeContainer arg0) {
			}
			
			@Override
			public void process(BoundContainer arg0) {
			}
		});
	}
	
	public Map<Long, String> getHighwayTypeMap(){
		return new TreeMap<Long, String>(this.highwayTypeMap);
	}

	@Override
	public void initialize(Map<String, Object> metaData) {
		// TODO Auto-generated method stub
		
	}

//	@Override
//	public void initialize(Map<String, Object> metaData) {
//		// TODO Auto-generated method stub
//		
//	}
	// leads to compilation error.  I don't know why; probably inconsistent geotools versions.  Recommendation to 
	// load geotools directly in pom.xml, rather than via other peoples maven configurations. kai, may'13

}

