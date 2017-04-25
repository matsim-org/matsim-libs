/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.andreas.mzilske.osm;

import org.apache.log4j.Logger;
import org.openstreetmap.osmosis.core.filter.common.IdTrackerType;
import org.openstreetmap.osmosis.core.merge.common.ConflictResolutionMethod;
import org.openstreetmap.osmosis.core.progress.v0_6.EntityProgressLogger;
import org.openstreetmap.osmosis.set.v0_6.EntityMerger;
import org.openstreetmap.osmosis.tagfilter.v0_6.TagFilter;
import org.openstreetmap.osmosis.tagfilter.v0_6.UsedNodeFilter;
import org.openstreetmap.osmosis.xml.common.CompressionMethod;
import org.openstreetmap.osmosis.xml.v0_6.XmlWriter;

import java.io.File;
import java.util.*;

public class OsmPrepare {
	
	private final static Logger log = Logger.getLogger(OsmPrepare.class);
	
	private final String infFile;
	private final String outFile;
	
	private final String[] streetFilter;
	private final String[] transitFilter;
	
	public OsmPrepare(String infFile, String outFile, String[] streetFilter, String[] transitFilter){
		this.infFile = infFile;
		this.outFile = outFile;
		this.streetFilter = streetFilter;
		this.transitFilter = transitFilter;
	}

	public static void main(String[] args) {
		new OsmPrepare("/Users/michaelzilske/sotm-paper/osm-work/berlin.osm", "/Users/michaelzilske/sotm-paper/osm-work/berlin-filtered.osm", new String[]{"motorway","motorway_link","trunk","trunk_link","primary","primary_link","secondary","tertiary","minor","unclassified","residential","living_street"}, new String[]{"tram", "train", "bus"}).prepareOsm();		
	}
	
	public void prepareOsm(){
		log.info("Start...");
		log.info("StreetFilter: " + this.streetFilter.toString());
		log.info("TransitFilter: " + this.transitFilter.toString());
		String filename = this.infFile;
		String targetFilename = this.outFile;
		
		log.info("Reading " + this.infFile);
		
		JOSMTolerantFastXMLReader reader = new JOSMTolerantFastXMLReader(new File(filename), true, CompressionMethod.None);
		UsedNodeFilter usedNodeFilter = new UsedNodeFilter(IdTrackerType.BitSet);
		EntityProgressLogger logger= new EntityProgressLogger(10, null);
		
		JOSMTolerantFastXMLReader reader2 = new JOSMTolerantFastXMLReader(new File(filename), true, CompressionMethod.None);		
		EntityProgressLogger logger2= new EntityProgressLogger(10, null);
				
		TagFilter streetTagFilter = createStreetFilter(this.streetFilter);
		TagFilter transitTagFilter = createTransitFilter(this.transitFilter);
		UsedNodeAndWayFilter usedFilter = new UsedNodeAndWayFilter(IdTrackerType.BitSet);
		
		EntityMerger entityMerger = new EntityMerger(ConflictResolutionMethod.LatestSource, 20, null);
		
		XmlWriter writer = new XmlWriter(new File(targetFilename), CompressionMethod.None);
		
		reader.setSink(logger);
		logger.setSink(streetTagFilter);
		streetTagFilter.setSink(usedNodeFilter);
		reader2.setSink(logger2);
		logger2.setSink(transitTagFilter);
		transitTagFilter.setSink(usedFilter);
		usedNodeFilter.setSink(entityMerger.getSink(0));
		usedFilter.setSink(entityMerger.getSink(1));
		entityMerger.setSink(writer);
		
		Thread t1 = new Thread(reader);
		t1.start();
		Thread t2 = new Thread(reader2);
		t2.start();
		Thread t3 = new Thread(entityMerger);
		t3.start();
		
		try {
			t1.join();
			t2.join();
			t3.join();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		log.info(this.outFile + " written");
		log.info("Done...");
	}

	private static TagFilter createStreetFilter(String[] filter) {
		Map<String, Set<String>> tagKeyValues = new HashMap<String, Set<String>>();
		tagKeyValues.put("highway", new HashSet<String>(Arrays.asList(filter)));
		Set<String> tagKeys = Collections.emptySet();
		TagFilter tagFilter = new TagFilter("accept-way", tagKeys, tagKeyValues);
		return tagFilter;
	}

	private static TagFilter createTransitFilter(String[] filter) {
		Map<String, Set<String>> tagKeyValues = new HashMap<String, Set<String>>();
		tagKeyValues.put("route", new HashSet<String>(Arrays.asList(filter))); 
		Set<String> tagKeys = Collections.emptySet();
		TagFilter transitFilter = new TagFilter("accept-relation", tagKeys, tagKeyValues);
		return transitFilter;
	}
	
}
