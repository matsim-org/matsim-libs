/* *********************************************************************** *
 * project: org.matsim.*
 * MyShoppingSink.java
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

package org.matsim.contrib.accessibility.osm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.collections.MapUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.PolygonFeatureFactory;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacilitiesFactory;
import org.matsim.facilities.ActivityFacilitiesFactoryImpl;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.opengis.feature.simple.SimpleFeature;
import org.openstreetmap.osmosis.core.container.v0_6.BoundContainer;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.container.v0_6.EntityProcessor;
import org.openstreetmap.osmosis.core.container.v0_6.NodeContainer;
import org.openstreetmap.osmosis.core.container.v0_6.RelationContainer;
import org.openstreetmap.osmosis.core.container.v0_6.WayContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.domain.v0_6.TagCollectionImpl;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.store.SimpleObjectStore;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

/**
 * @author dziemke
 */
public class LandUseBuildingSink implements Sink {
	private final Logger log = Logger.getLogger(LandUseBuildingSink.class);
	private final CoordinateTransformation ct;
	private Map<Long, NodeContainer> nodeMap;
	private Map<Long, WayContainer> wayMap;
	private Map<Long, RelationContainer> relationMap;
	private ActivityFacilities facilities;
	private ObjectAttributes facilityAttributes;
	private Map<String, String> typeMap = new HashMap<>();
	private Map<String, Integer> typeCount = new HashMap<>();
	
	private List <SimpleFeature> features = new ArrayList <SimpleFeature>();
	
	
	private static PolygonFeatureFactory polygonFeatureFactory;

	
	public LandUseBuildingSink(CoordinateTransformation ct, Map<String, String> osmToMatsimType) {
		this.ct = ct;
		this.typeMap = osmToMatsimType;
		this.nodeMap = new HashMap<Long, NodeContainer>();
		this.wayMap = new HashMap<Long, WayContainer>();
		this.relationMap = new HashMap<Long, RelationContainer>();
		
		facilities = FacilitiesUtils.createActivityFacilities("Land Use");
		facilityAttributes = new ObjectAttributes();
	}

	
	@Override
	public void complete() {
		log.info("    nodes: " + nodeMap.size());
		log.info("     ways: " + wayMap.size());
		log.info("relations: " + relationMap.size());
		
		log.info("Creating facilities..");
		
		ActivityFacilitiesFactory aff = new ActivityFacilitiesFactoryImpl();
		
		String attributeLabel = "type";
		initFeatureType(attributeLabel);

		/* First check all the ways for land use. */
		processLandUseAreas(aff, wayMap);
		
		/* Second, check all the ways for buildings. */
		processBuildings(aff, wayMap);
	}


	private void processLandUseAreas(ActivityFacilitiesFactory aff,	Map<Long,? extends EntityContainer> entityMap) {
		for(long entityKey : entityMap.keySet()){
			Entity entity = entityMap.get(entityKey).getEntity();
			Map<String, String> tags = new TagCollectionImpl(entity.getTags()).buildMap();
			
			String landuseType = tags.get("landuse");
			String activityType = null;
			if(landuseType != null) {
				activityType = getActivityType(landuseType);
			}
			if(activityType != null){
				String name = tags.get("name");
				if(name != null){
					log.warn("      ---> Land use " + entityKey + " has name " + name + ".");
				} else{
					log.warn("      ---> Land use " + entityKey + " does not have a name.");
				}

				Coord[] coords = CoordUtils.getWayCoords((Way) entity, this.ct, this.nodeMap);
				
				SimpleFeature feature = createFeature(coords, activityType);

				this.features.add(feature);
			}
		}
	}
	
	
	private static void initFeatureType(String attributeLabel) {
		polygonFeatureFactory = new PolygonFeatureFactory.Builder().
		setCrs(MGC.getCRS(TransformationFactory.DHDN_GK4)).
		setName("buildings").
		addAttribute(attributeLabel, String.class).
		create();
	}	
	
	
	private static SimpleFeature createFeature(Coord[] coords, String activityType) {
		Object[] attributes = new Object[]{activityType};

		Coordinate[] coordinates = new Coordinate[coords.length];
		for (int i = 0; i < coords.length; i++) {
			coordinates[i] = new Coordinate(coords[i].getX(), coords[i].getY());
		}
			
		SimpleFeature feature = polygonFeatureFactory.createPolygon(coordinates, attributes, null);
		return feature;
	}	
	
	
	private void processBuildings(ActivityFacilitiesFactory activityFacilityFactory,
			Map<Long,? extends EntityContainer> entityMap) {
		
		GeometryFactory geometryFactory = new GeometryFactory();
		
		for(long entityKey : entityMap.keySet()){
			Entity entity = entityMap.get(entityKey).getEntity();
			Map<String, String> tags = new TagCollectionImpl(entity.getTags()).buildMap();
			
			String buildingType = tags.get("building");
			String amenityType = tags.get("amenity");
		
			
			// Entities that possess an "amenity" tag need to be excluded here, because they are already considered via the AmenityReader
			if(buildingType != null && amenityType == null) {
				String name = tags.get("name");
				if(name != null){
					log.warn("      ---> Land use " + entityKey + " has name " + name + ".");
				} else{
					log.warn("      ---> Land use " + entityKey + " does not have a name.");
				}
				
				
				// Get type of land use for the building under consideration
				Coord coord = CoordUtils.getCoord(entity, ct, nodeMap, wayMap, relationMap);
				Coordinate coordinate = new Coordinate(coord.getX(), coord.getY());
				Geometry coordAsGeometry = geometryFactory.createPoint(coordinate);
				
				String activityType = null;
								
				for (SimpleFeature feature : this.features) {
					Geometry geometry = (Geometry) feature.getDefaultGeometry();

					if (geometry.contains(coordAsGeometry)) {
						activityType = (String) feature.getAttribute("type");
					}
				}
				
				
				// Get number of levels/storeys
				Integer buildingLevels = null;
				String buildingLevelsAsString = tags.get("building:levels");
				if (buildingLevelsAsString != null) {
					buildingLevels = Integer.parseInt(buildingLevelsAsString);
				}
				System.out.println("building levels = " + buildingLevels);
				
				
				// Get area of building under consideration
				Coord[] buildingCoords = CoordUtils.getWayCoords((Way) entity, this.ct, this.nodeMap);
				SimpleFeature buildingAsFeature = createFeature(buildingCoords, null);
				Geometry buildingGeometry = (Geometry) buildingAsFeature.getDefaultGeometry();
				double buildingArea = buildingGeometry.getArea();
				System.out.println("building area = " + buildingArea);
				
				Double buildingFloorSpace = Double.NaN;
				if (buildingLevels != null) {
					buildingFloorSpace = buildingArea * buildingLevels;
				}
				System.out.println("building floor space = " + buildingFloorSpace);
				

				// Create facility for building
				if (activityType != null) {
					Id<ActivityFacility> facilityId = Id.create(entity.getId(), ActivityFacility.class);
					ActivityFacility activityFacility;
					
					if(!facilities.getFacilities().containsKey(facilityId)){
						activityFacility = activityFacilityFactory.createActivityFacility(facilityId, coord);
						((ActivityFacilityImpl)activityFacility).setDesc(name);
						facilities.addActivityFacility(activityFacility);
					} else{
						activityFacility = (ActivityFacilityImpl) facilities.getFacilities().get(facilityId);
					}
					ActivityOption aactivityOption = activityFacilityFactory.createActivityOption(activityType);
					
					activityFacility.addActivityOption(aactivityOption);
//				setFacilityDetails(ao);
//				nodeFacilities++;
				}
			}

		}
	}
	
	
	/**
	 * Return the facilities parsed.
	 * @return
	 */
	public ActivityFacilities getFacilities(){
		return facilities;
	}
	
	
	/**
	 * Return the facility attributes.
	 * @return
	 */
	public ObjectAttributes getFacilityAttributes(){
		return this.facilityAttributes;
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
	
	
	private String getActivityType(String landuseType){
		String type = typeMap.get(landuseType);
		if(type == null){
			log.warn("Do not have an activity type mapping for " + landuseType + "! Returning NULL.");
		} else{
		}
		MapUtils.addToInteger(type, typeCount, 0, 1);
		return type;
	}


	@Override
	public void initialize(Map<String, Object> metaData) {
		// TODO Auto-generated method stub
	}
}