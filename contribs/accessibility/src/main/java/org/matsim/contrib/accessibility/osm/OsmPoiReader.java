/* *********************************************************************** *
 * project: org.matsim.*
 * MyShoppingReader.java
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

/**
 * 
 */
package org.matsim.contrib.accessibility.osm;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.collections.MapUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacilitiesFactory;
import org.matsim.facilities.ActivityFacilitiesFactoryImpl;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.FacilitiesWriter;
import org.openstreetmap.osmosis.core.container.v0_6.BoundContainer;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.container.v0_6.EntityProcessor;
import org.openstreetmap.osmosis.core.container.v0_6.NodeContainer;
import org.openstreetmap.osmosis.core.container.v0_6.RelationContainer;
import org.openstreetmap.osmosis.core.container.v0_6.WayContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.domain.v0_6.TagCollectionImpl;
import org.openstreetmap.osmosis.core.store.SimpleObjectStore;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;
import org.openstreetmap.osmosis.xml.common.CompressionMethod;
import org.openstreetmap.osmosis.xml.v0_6.XmlReader;

/**
 * @author dziemke
 */
public class OsmPoiReader {
	private final static Logger LOG = Logger.getLogger(OsmPoiReader.class);
	
	private File inputFile;
	private ActivityFacilities facilities;
	private final CoordinateTransformation ct;
	private boolean useGeneralTypeIsSpecificTypeUnknown = false;

	public OsmPoiReader(String osmInputFile, CoordinateTransformation ct) throws FileNotFoundException {
		LOG.info("Creating OSM POI reader");
		File file = new File(osmInputFile);
		if(!file.exists()) {
			throw new FileNotFoundException("Could not find " + osmInputFile);
		}
		
		this.inputFile = file;
		this.ct = ct;
		this.facilities = FacilitiesUtils.createActivityFacilities("OpenStreetMap facilities");
	}	
	
	/**
	 * Parses a given <i>OpenStreetMap</i> file for data in it that can be converted into MATSim facilities.
	 */
	public void parseOsmFileAndAddFacilities(Map<String, String> osmToMatsimTypeMap, String osmKey) {
		OsmPoiSink sink = new OsmPoiSink(this.ct, osmToMatsimTypeMap, osmKey, useGeneralTypeIsSpecificTypeUnknown);
		XmlReader xmlReader = new XmlReader(inputFile, false, CompressionMethod.None);
		xmlReader.setSink(sink);
		xmlReader.run();		
		
		for (ActivityFacility af : sink.getFacilities().getFacilities().values()) {
			if (!this.facilities.getFacilities().containsKey(af.getId())) {
				this.facilities.addActivityFacility(af);
			} else {
				for (ActivityOption activityOption : af.getActivityOptions().values()) {
					ActivityFacility activityFacility = this.facilities.getFacilities().get(af.getId());
					if (!activityFacility.getActivityOptions().containsKey(activityOption.getType())) {
						this.facilities.getFacilities().get(af.getId()).addActivityOption(activityOption);
					}
				}
			}
		}
	}

	public void writeFacilities(String outputFacilitiesFile){
		FacilitiesWriter facilitiesWriter = new FacilitiesWriter(this.facilities);
		facilitiesWriter.write(outputFacilitiesFile);
	}

	public ActivityFacilities getActivityFacilities(){
		return this.facilities;
	}
	
	public void setUseGeneralTypeIsSpecificTypeUnknown (boolean useGeneralTypeIsSpecificTypeUnknown) {
		this.useGeneralTypeIsSpecificTypeUnknown = useGeneralTypeIsSpecificTypeUnknown;
	}
	
	//------------------------------------------------------------------------------------------------------
	public class OsmPoiSink implements Sink {
		private final Logger LOG = Logger.getLogger(OsmPoiSink.class);
		
		private final CoordinateTransformation ct;
		private Map<String, String> typeMap = new HashMap<>();
		private final String osmKey;

		private Map<Long, NodeContainer> nodeMap;
		private Map<Long, WayContainer> wayMap;
		private Map<Long, RelationContainer> relationMap;
		
		private ActivityFacilities facilities;
		private Map<String, Integer> typeCount = new HashMap<>();
		
		public OsmPoiSink(CoordinateTransformation ct, Map<String, String> osmToMatsimType, String osmKey, boolean useGeneralTypeIsSpecificTypeUnknown) {
			this.ct = ct;
			this.typeMap = osmToMatsimType;
			this.osmKey = osmKey;
			
			this.nodeMap = new HashMap<Long, NodeContainer>();
			this.wayMap = new HashMap<Long, WayContainer>();
			this.relationMap = new HashMap<Long, RelationContainer>();
			
			facilities = FacilitiesUtils.createActivityFacilities(osmKey + "_facilities");
		}

		@Override
		public void complete() {
			LOG.info("Nodes: " + nodeMap.size());
			LOG.info("Ways: " + wayMap.size());
			LOG.info("Relations: " + relationMap.size());
			
			LOG.info("Start creating facilities.");
			
			ActivityFacilitiesFactory aff = new ActivityFacilitiesFactoryImpl();

			processFacilities(aff, nodeMap, osmKey);
			processFacilities(aff, wayMap, osmKey);
			processFacilities(aff, relationMap, osmKey);
		}
		
		private void processFacilities(ActivityFacilitiesFactory aff, Map<Long,? extends EntityContainer> entityMap, String osmKey) {
			for (long n : entityMap.keySet()){
				Entity entity = entityMap.get(n).getEntity();
				Map<String, String> tags = new TagCollectionImpl(entity.getTags()).buildMap();
				
				String amenity = tags.get(osmKey);
				String matsimType = null;
				if (amenity != null) {
					matsimType = getActivityType(amenity, osmKey);
				}
				if (matsimType != null){
					String name = tags.get(OsmTags.NAME);
					name = AccessibilityOsmUtils.simplifyString(name);

					Coord coord = OSMCoordUtils.getCentroidCoord(entity, this.ct, this.nodeMap, this.wayMap, this.relationMap);
					Id<ActivityFacility> newId = Id.create(entity.getId(), ActivityFacility.class);
					ActivityFacility af;
					if(!facilities.getFacilities().containsKey(newId)){
						af = aff.createActivityFacility(newId, coord);
						((ActivityFacilityImpl)af).setDesc(name);
						facilities.addActivityFacility(af);
					} else{
						af = (ActivityFacilityImpl) facilities.getFacilities().get(newId);
					}
					ActivityOption ao = aff.createActivityOption(matsimType);
					af.addActivityOption(ao);
				}
			}
		}

		public ActivityFacilities getFacilities(){
			return facilities;
		}

		@Override
		public void release() {
		}

		/**
		 * Adds each <i>OpenStreetMap</i> element to an internal container.
		 * <ul>
		 * 	<li> <b>relation</b>s are add to a {@link SimpleObjectStore};
		 * 	<li> <b>way</b>s are added to a {@link Map};
		 * 	<li> <b>node</b>s are added to a {@link Map};
		 * </ul> 
		 */
		@Override
		public void process(EntityContainer entityContainer) {
			entityContainer.process(new EntityProcessor() {
				@Override
				public void process(RelationContainer relationContainer) {
					relationMap.put(relationContainer.getEntity().getId(), relationContainer);					
				}
				
				@Override
				public void process(WayContainer wayContainer) {
					wayMap.put(wayContainer.getEntity().getId(), wayContainer);
				}
				
				@Override
				public void process(NodeContainer nodeContainer) {
					nodeMap.put(nodeContainer.getEntity().getId(), nodeContainer);
				}
				
				@Override
				public void process(BoundContainer boundContainer) {
				}
			});
		}
		
		private String getActivityType(String osmValue, String osmKey){
			String matsimType = typeMap.get(osmValue);
			if (matsimType == null) {
				if (useGeneralTypeIsSpecificTypeUnknown) {
					return osmKey;
				} else {
					LOG.info("Do not have an activity type mapping for " + osmValue + "! Returning NULL.");
				}
			}
			MapUtils.addToInteger(matsimType, typeCount, 0, 1);
			return matsimType;
		}

		@Override
		public void initialize(Map<String, Object> metaData) {
		}
	}
}