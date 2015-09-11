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
public class CombinedOsmSink implements Sink {
	private final Logger log = Logger.getLogger(CombinedOsmSink.class);
	private final CoordinateTransformation ct;
	private Map<Long, NodeContainer> nodeMap;
	private Map<Long, WayContainer> wayMap;
	private Map<Long, RelationContainer> relationMap;
	private ActivityFacilities facilities;
	private ObjectAttributes facilityAttributes;
	private Map<String, String> landUseTypeMap = new HashMap<>();
	private Map<String, String> buildingTypeMap = new HashMap<>();
	
	//
	private Map<String, String> amenityTypeMap = new HashMap<>();
	String toCRS = TransformationFactory.DHDN_GK4;
	//
	
	private Map<String, Integer> typeCount = new HashMap<>();
	
	private List <SimpleFeature> features = new ArrayList <SimpleFeature>();

	private double buildingTypeFromVicinityRange;
	private String[] tagsToIgnoreBuildings;

	private int featureErrorCounter = 0;
	private int buildingErrorCounter = 0;
	
	private PolygonFeatureFactory polygonFeatureFactory;

	
	public CombinedOsmSink(CoordinateTransformation ct, Map<String, String> osmLandUseToMatsimType, 
			Map<String, String> osmBuildingToMatsimType, 
			//
			Map<String, String> osmAmenityToMatsimType,
			//
			double buildingTypeFromVicinityRange,
			String[] tagsToIgnoreBuildings) {
		this.ct = ct;
		this.landUseTypeMap = osmLandUseToMatsimType;
		this.buildingTypeMap = osmBuildingToMatsimType;
		
		//
		this.amenityTypeMap = osmAmenityToMatsimType;
		//
		
		this.nodeMap = new HashMap<Long, NodeContainer>();
		this.wayMap = new HashMap<Long, WayContainer>();
		this.relationMap = new HashMap<Long, RelationContainer>();
		
		this.buildingTypeFromVicinityRange  = buildingTypeFromVicinityRange;
		this.tagsToIgnoreBuildings = tagsToIgnoreBuildings;
		
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
		
//		String landUseType = "type";
//		initLandUseFeatureType(landUseType, this.toCRS);
		initLandUseFeatureType(this.toCRS);

		/* First check all the ways for land use. */
		processLandUseAreas(aff, wayMap);
		
		/* Second, check all the ways for buildings. */
		processBuildings(aff, wayMap);
		
		//
		/* First check all the point features. */
		processFacilities(aff, nodeMap);
		
		/* Second, check for way features. */
		processFacilities(aff, wayMap);

		/* Thirdly, check for relation. */
		processFacilities(aff, relationMap);
		//
		
		log.info("featureErrorCounter = " + featureErrorCounter);
		log.info("buildingErrorCounter = " + buildingErrorCounter);
	}


	private void processLandUseAreas(ActivityFacilitiesFactory aff,	Map<Long,? extends EntityContainer> entityMap) {
		
		// TODO process historic=memorial and leisure=park and amenity=xy and tourism=zoo the same way
		
		for(long entityKey : entityMap.keySet()){
			Entity entity = entityMap.get(entityKey).getEntity();
			Map<String, String> tags = new TagCollectionImpl(entity.getTags()).buildMap();
			
			String landuseType = tags.get("landuse");
			String matsimActivityType = null;
			if(landuseType != null) {
				matsimActivityType = getActivityType(landuseType, this.landUseTypeMap);
			}
			if(matsimActivityType != null){
				Coord[] coords = CoordUtils.getWayCoords((Way) entity, this.ct, this.nodeMap);
				SimpleFeature feature = createLandUseFeature(coords, matsimActivityType);
				
				// TODO check if the following is needed
				if (feature == null) {
					continue;
				}
				//
				
				this.features.add(feature);
			}
		}
	}
	
	
	// private void initLandUseFeatureType(String landUseType, String toCRS) {
	private void initLandUseFeatureType(String toCRS) {
		
		// TODO make CRS adjustable
		
		this.polygonFeatureFactory = new PolygonFeatureFactory.Builder().
		//setCrs(MGC.getCRS(TransformationFactory.DHDN_GK4)).
		setCrs(MGC.getCRS(toCRS)).
		//setName("buildings").
		setName("land_use").
		addAttribute("type", String.class).
		create();
	}	
	
	
	// creates a feature in form of a polygon that is defined by its vertices, which are defined by its coordinates
	private SimpleFeature createLandUseFeature(Coord[] coords, String matsimActivityType) {
		Object[] attributes = new Object[]{matsimActivityType};

		Coordinate[] vividCoordinates = new Coordinate[coords.length];
		for (int i = 0; i < coords.length; i++) {
			vividCoordinates[i] = new Coordinate(coords[i].getX(), coords[i].getY());
		}
		
		SimpleFeature feature = null;
		
		try {
			feature = this.polygonFeatureFactory.createPolygon(vividCoordinates, attributes, null);
		} catch (IllegalArgumentException e) {
			log.error("IllegalArgumentException: " + e.getMessage());
			this.featureErrorCounter++;
		}
		
		return feature;
	}	
	
	
	private void processBuildings(ActivityFacilitiesFactory activityFacilityFactory,
			Map<Long,? extends EntityContainer> entityMap) {
		
		GeometryFactory geometryFactory = new GeometryFactory();
		
		for(long entityKey : entityMap.keySet()){
			boolean ignoreBecauseOfTag = false;
			
			Entity entity = entityMap.get(entityKey).getEntity();
			Map<String, String> tags = new TagCollectionImpl(entity.getTags()).buildMap();
			
			String buildingType = tags.get("building");
//			String amenityType = tags.get("amenity");
			
			for (String tag : tagsToIgnoreBuildings) {
				String tagValue = tags.get(tag);
				if (tagValue != null) {
					ignoreBecauseOfTag = true; 
				}
			}

			
			// Entities that possess an "amenity" tag need to be excluded here, because they are already considered via the AmenityReader
//			if(buildingType != null && amenityType == null) {
			if(buildingType != null && ignoreBecauseOfTag == false) {
				
				
				
				String name = tags.get("name");
				if(name == null){
//					log.warn("Building " + entityKey + " does not have a name.");
				} else{
//					log.warn("Building " + entityKey + " has the name " + name + ".");
					// & and " need to be replaced to avoid problems with parsing the facilities file later
					if (name.contains("&")) {							
						name = name.replaceAll("&", "u");
					}
					if (name.contains("\"")) {							
						name = name.replaceAll("\"", "");
					}
				}
								
				Coord coord = CoordUtils.getCoord(entity, ct, nodeMap, wayMap, relationMap);
				Coord[] buildingCoords = CoordUtils.getWayCoords((Way) entity, this.ct, this.nodeMap);
				SimpleFeature buildingAsFeature = createLandUseFeature(buildingCoords, null);
				// TODO should not be called land use feature
				
				if (buildingAsFeature == null) {
					log.error("The feature of building " + entityKey + " is null!");
					this.buildingErrorCounter++;
					continue;
				}

				// Get facility type
				String activityType = getActivityType(buildingType, this.buildingTypeMap);
				
				if (activityType == null) {
					activityType = getActivityTypeFromLandUseArea(geometryFactory, buildingType, coord);
				}
				
				if (activityType == "ignore") {
					// System.out.println("activityType == ignore");
					continue;
				}

				// Get number of levels/storeys
				String buildingLevelsAsString = tags.get("building:levels");
				Integer buildingLevels = getBuildingLevels(entityKey, buildingLevelsAsString);
//				System.out.println("Number of building levels = " + buildingLevels);

				// Get area of building under consideration
				Geometry buildingGeometry = (Geometry) buildingAsFeature.getDefaultGeometry();
				double buildingArea = buildingGeometry.getArea();
//				System.out.println("Building area = " + buildingArea);

				Double buildingFloorSpace = Double.NaN;
				if (buildingLevels != null) {
					buildingFloorSpace = buildingArea * buildingLevels;
				}
//				System.out.println("building floor space = " + buildingFloorSpace);

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
				}
			}
		}
	}
	
	
	//////////////////////////////////////
	private void processFacilities(ActivityFacilitiesFactory aff,
			Map<Long,? extends EntityContainer> entityMap) {
		for(long n : entityMap.keySet()){
			Entity entity = entityMap.get(n).getEntity();
			Map<String, String> tags = new TagCollectionImpl(entity.getTags()).buildMap();
			
			/* Check amenities */
			String amenity = tags.get("amenity");
			String matsimType = null;
			if(amenity != null) {
				matsimType = getActivityType(amenity, this.amenityTypeMap);
			}
			if(matsimType != null){
				String activityType = getActivityType(amenity, this.amenityTypeMap);
				String name = tags.get("name");
				if(name != null){
					/* Check education level. */
					if(activityType.equalsIgnoreCase("e")){
//						getEducationLevel(name);
					}					
				} else{
					log.warn("      ---> Amenity " + n + " without a name.");
				}

				/* Facility identified. Now get the centroid of all members. */ 
				//Coord coord = getCoord(entity);
				Coord coord = CoordUtils.getCoord(entity, this.ct, this.nodeMap, this.wayMap, this.relationMap);
				Id<ActivityFacility> newId = Id.create(entity.getId(), ActivityFacility.class);
				ActivityFacility af;
				if(!facilities.getFacilities().containsKey(newId)){
					af = aff.createActivityFacility(newId, coord);
					((ActivityFacilityImpl)af).setDesc(name);
					facilities.addActivityFacility(af);
				} else{
					af = (ActivityFacilityImpl) facilities.getFacilities().get(newId);
				}
				ActivityOption ao = aff.createActivityOption(activityType);
				af.addActivityOption(ao);
//				setFacilityDetails(ao);
//				nodeFacilities++;
			}
			
			/* Check shops */
			String shops = tags.get("shop");
			if(shops != null){
				String name = tags.get("name");
				if(name == null){
					log.warn("      ---> Shop " + n + " without a name.");
				}

				/* Facility identified. Now get the centroid of all members. */ 
//				Coord coord = getCoord(entity);
				Coord coord = CoordUtils.getCoord(entity, this.ct, this.nodeMap, this.wayMap, this.relationMap);
				Id<ActivityFacility> newId = Id.create(entity.getId(), ActivityFacility.class);
				ActivityFacility af;
				if(!facilities.getFacilities().containsKey(newId)){
					af = aff.createActivityFacility(newId, coord);					
					((ActivityFacilityImpl)af).setDesc(name);
					facilities.addActivityFacility(af);
				} else{
					af = (ActivityFacilityImpl) facilities.getFacilities().get(newId);
				}
				ActivityOption ao = aff.createActivityOption("s");
				af.addActivityOption(ao);
//				setFacilityDetails(ao);
//				shoppingCounter++;
//				nodeFacilities++;
			}
		}
	}
	//////////////////////////////////////////

	
	private String getActivityTypeFromLandUseArea(GeometryFactory geometryFactory,	String buildingType, Coord coord) {
		String activityType = null;
		Coordinate coordinate = new Coordinate(coord.getX(), coord.getY());
		Geometry coordAsGeometry = geometryFactory.createPoint(coordinate);

		boolean buildingContainedInLandUseArea = false;
		Geometry geometry;

		for (SimpleFeature feature : this.features) {
//			System.out.println("feature.toString()" + feature.toString());
//			System.out.println(feature);
//			System.out.println(feature.getAttribute("type"));
			geometry = (Geometry) feature.getDefaultGeometry();

			if (geometry.contains(coordAsGeometry)) {
				activityType = (String) feature.getAttribute("type");
				buildingContainedInLandUseArea = true;
			}
		}
		
		if (activityType == "ignore") {
			return null;
		}

		if (buildingContainedInLandUseArea == false) {
			double minDistanceToLandUseArea = Double.POSITIVE_INFINITY;

			for (SimpleFeature feature : this.features) {
				geometry = (Geometry) feature.getDefaultGeometry();

				double distanceToLandUseArea = coordAsGeometry.distance(geometry);

				if (distanceToLandUseArea < minDistanceToLandUseArea) {
					minDistanceToLandUseArea = distanceToLandUseArea;
					activityType = (String) feature.getAttribute("type");
				}
			}
			if (minDistanceToLandUseArea >= this.buildingTypeFromVicinityRange) {
				activityType = null;
			}

		}
		return activityType;
	}


	private Integer getBuildingLevels(long entityKey, String buildingLevelsAsString) {
		Integer buildingLevels = null;
		if (buildingLevelsAsString != null) {
			
			if (!Character.isDigit(buildingLevelsAsString.charAt(0))) {
				log.warn("No meaningful level number given for building " + entityKey + ".");
				
			} else if (buildingLevelsAsString.contains("-")) {
				log.info("Level string of building " + entityKey + " is " + buildingLevelsAsString + ".");
				String[] subString = buildingLevelsAsString.split("-");
				buildingLevels = (Integer.parseInt(subString[0].trim()) + Integer.parseInt(subString[1].trim())) / 2;
				log.warn("Simplified level number of building " + entityKey + " by averaging different values.");
				
			} else if (buildingLevelsAsString.contains(",")) {
				log.info("Level string of building " + entityKey + " is " + buildingLevelsAsString + ".");
				String[] subString = buildingLevelsAsString.split(",");
				buildingLevels = (Integer.parseInt(subString[0].trim()) + Integer.parseInt(subString[1].trim())) / 2;
				log.warn("Simplified level number of building " + entityKey + " by averaging different values.");
				
			} else if (buildingLevelsAsString.contains("/")) {
				log.info("Level string of building " + entityKey + " is " + buildingLevelsAsString + ".");
				String[] subString = buildingLevelsAsString.split("/");
				buildingLevels = (Integer.parseInt(subString[0].trim()) + Integer.parseInt(subString[1].trim())) / 2;
				log.warn("Simplified level number of building " + entityKey + " by averaging different values.");
				
			} else if (buildingLevelsAsString.contains(".")) {
				log.info("Level string of building " + entityKey + " is " + buildingLevelsAsString + ".");
				String[] subString = buildingLevelsAsString.split("\\.");
				buildingLevels = (Integer.parseInt(subString[0]));
				log.warn("Simplified level number of building " + entityKey + " by truncating decimal places.");
				
			} else {
				buildingLevels = Integer.parseInt(buildingLevelsAsString);
			}
		}
		return buildingLevels;
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
	
	
	private String getActivityType(String osmType, Map<String, String> typeMap){
		String matsimType = typeMap.get(osmType);
		if(osmType == null){
			log.warn("Do not have an activity type mapping for " + osmType + "! Returning NULL.");
		}
		MapUtils.addToInteger(osmType, typeCount, 0, 1);
		return matsimType;
	}
	
	
	
	/////////////////////////////////////////////////
//	private String getActivityType(String amenity){
//		String type = typeMap.get(amenity);
//		if(type == null){
//			log.warn("Do not have an activity type mapping for " + amenity + "! Returning NULL.");
//		} else{
//		}
//		MapUtils.addToInteger(type, typeCount, 0, 1);
//		return type;
//	}
	//////////////////////////////////////


	@Override
	public void initialize(Map<String, Object> metaData) {
		// TODO Auto-generated method stub
	}
}