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
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.collections.MapUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacilitiesFactory;
import org.matsim.facilities.ActivityFacilitiesFactoryImpl;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.OpeningTimeImpl;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.openstreetmap.osmosis.core.container.v0_6.BoundContainer;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.container.v0_6.EntityProcessor;
import org.openstreetmap.osmosis.core.container.v0_6.NodeContainer;
import org.openstreetmap.osmosis.core.container.v0_6.RelationContainer;
import org.openstreetmap.osmosis.core.container.v0_6.WayContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.domain.v0_6.EntityType;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Relation;
import org.openstreetmap.osmosis.core.domain.v0_6.RelationMember;
import org.openstreetmap.osmosis.core.domain.v0_6.TagCollectionImpl;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;
import org.openstreetmap.osmosis.core.store.SimpleObjectStore;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;

public class LandUseSink implements Sink {
	private final Logger log = Logger.getLogger(LandUseSink.class);
	private final CoordinateTransformation ct;
	private Map<Long, NodeContainer> nodeMap;
	private Map<Long, WayContainer> wayMap;
	private Map<Long, RelationContainer> relationMap;
	private ActivityFacilities facilities;
	private ObjectAttributes facilityAttributes;
//	private Map<String,Integer> educationLevelMap;
	private Map<String, String> typeMap = new HashMap<>();
	private Map<String, Integer> typeCount = new HashMap<>();
	
	private int errorCounter = 0;
	private int warningCounter = 0;
	private int educationCounter = 0;
	private int leisureCounter = 0;
	private int shoppingCounter = 0;
	private int otherCounter = 0;
	private int policeCounter = 0;
	private int healthcareCounter = 0;
	
	public LandUseSink(CoordinateTransformation ct, Map<String, String> osmToMatsimType) {
		this.ct = ct;
		this.typeMap = osmToMatsimType;
		this.nodeMap = new HashMap<Long, NodeContainer>();
		this.wayMap = new HashMap<Long, WayContainer>();
		this.relationMap = new HashMap<Long, RelationContainer>();
		
		facilities = FacilitiesUtils.createActivityFacilities("Land Use");
		facilityAttributes = new ObjectAttributes();
		
		/* Keep track of the different education level facilities. */
//		educationLevelMap = new HashMap<>();
//		educationLevelMap.put("primary", 0);
//		educationLevelMap.put("secondary", 0);
//		educationLevelMap.put("tertiary", 0);
//		educationLevelMap.put("unknown", 0);
	}

	
	@Override
	public void complete() {
		log.info("    nodes: " + nodeMap.size());
		log.info("     ways: " + wayMap.size());
		log.info("relations: " + relationMap.size());
		
		log.info("Creating facilities..");
		
		ActivityFacilitiesFactory aff = new ActivityFacilitiesFactoryImpl();

//		/* First check all the point features. */
//		processFacilities(aff, nodeMap);
//		
//		/* Second, check for way features. */
//		processFacilities(aff, wayMap);
//
//		/* Thirdly, check for relation. */
//		processFacilities(aff, relationMap);
		
		/* First check all the relations for land use. */
		processFacilities(aff, relationMap);
		
		/* Second, check for way features. */
		processFacilities(aff, wayMap);

		/* Thirdly, check for relation. */
		processFacilities(aff, relationMap);
		
		/*TODO Report the final counts of different amenity types. */
	}

//	private Coord getCoord(Entity entity){
//		if(entity instanceof Node){
//			return getNodeCoord((Node)entity);
//		} else if(entity instanceof Way){
//			return getWayCentroid((Way)entity);
//		} else if(entity instanceof Relation){
//			return getRelationCentroid((Relation)entity);
//		}
//		
//		return null;
//	}

	private void processFacilities(ActivityFacilitiesFactory aff,
			Map<Long,? extends EntityContainer> entityMap) {
		for(long entityKey : entityMap.keySet()){
			Entity entity = entityMap.get(entityKey).getEntity();
			Map<String, String> tags = new TagCollectionImpl(entity.getTags()).buildMap();
			/* Check land use */
			//System.out.println("tags.get(landuse) = " + tags.get("landuse"));
			String landuseType = tags.get("landuse");
			String activityType = null;
			if(landuseType != null) {
				activityType = getActivityType(landuseType);
			}
			if(activityType != null){
				String name = tags.get("name");
				if(name != null){
					/* Check education level. */
//					if(activityType.equalsIgnoreCase("e")){
//						getEducationLevel(name);
//					}
					log.warn("      ---> Land use " + entityKey + " has name " + name + ".");
				} else{
					log.warn("      ---> Land use " + entityKey + " does not have a name.");
				}

				/* Facility identified. Now get the centroid of all members. */ 
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
//			String shops = tags.get("shop");
//			if(shops != null){
//				String name = tags.get("name");
//				if(name == null){
//					log.warn("      ---> Shop " + n + " without a name.");
//				}
//
//				/* Facility identified. Now get the centroid of all members. */ 
//				Coord coord = getCoord(entity);
//				Id<ActivityFacility> newId = Id.create(entity.getId(), ActivityFacility.class);
//				ActivityFacility af;
//				if(!facilities.getFacilities().containsKey(newId)){
//					af = aff.createActivityFacility(newId, coord);					
//					((ActivityFacilityImpl)af).setDesc(name);
//					facilities.addActivityFacility(af);
//				} else{
//					af = (ActivityFacilityImpl) facilities.getFacilities().get(newId);
//				}
//				ActivityOption ao = aff.createActivityOption("s");
//				af.addActivityOption(ao);
//				setFacilityDetails(ao);
//				shoppingCounter++;
//				nodeFacilities++;
//			}
		}
	}
	
	private void processFacilities2(ActivityFacilitiesFactory aff,
			Map<Long,? extends EntityContainer> entityMap) {
		for(long entityKey : entityMap.keySet()){
			Entity entity = entityMap.get(entityKey).getEntity();
			Map<String, String> tags = new TagCollectionImpl(entity.getTags()).buildMap();
			/* Check land use */
			//System.out.println("tags.get(landuse) = " + tags.get("landuse"));
			String landuseType = tags.get("landuse");
			String activityType = null;
			if(landuseType != null) {
				activityType = getActivityType(landuseType);
			}
			if(activityType != null){
				String name = tags.get("name");
				if(name != null){
					/* Check education level. */
//					if(activityType.equalsIgnoreCase("e")){
//						getEducationLevel(name);
//					}
					log.warn("      ---> Land use " + entityKey + " has name " + name + ".");
				} else{
					log.warn("      ---> Land use " + entityKey + " does not have a name.");
				}

				/* Facility identified. Now get the centroid of all members. */ 
				Coord coord = CoordUtils.getCoord(entity, ct, nodeMap, wayMap, relationMap);
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
	
	
//	/**
//	 * Determine the bounding box of a closed way.
//	 * @param way
//	 * @return the {@link List} of {@link Coord}s: the first is the bottom-left
//	 * 		of the bounding box, and the second is the upper-right. 
//	 */
//	private List<Coord> getWayBox(Way way){
//		List<Coord> list = new ArrayList<Coord>();
//		Double xmin = Double.POSITIVE_INFINITY;
//		Double ymin = Double.POSITIVE_INFINITY;
//		Double xmax = Double.NEGATIVE_INFINITY;
//		Double ymax = Double.NEGATIVE_INFINITY;
//		for(WayNode n : way.getWayNodes()){
//			double xNode = nodeMap.get(n.getNodeId()).getEntity().getLongitude();
//			double yNode = nodeMap.get(n.getNodeId()).getEntity().getLatitude();
//			if(xNode < xmin){ xmin = xNode; }
//			if(yNode < ymin){ ymin = yNode; }
//			if(xNode > xmax){ xmax = xNode; }
//			if(yNode > ymax){ ymax = yNode; }
//		}
//
//		/* Create the bounding coordinates, and add them to the result list. */
//		Coord bottomLeft = new CoordImpl(xmin, ymin);
//		Coord topRight = new CoordImpl(xmax, ymax);
//		list.add(bottomLeft);
//		list.add(topRight);
//		
//		return list;
//	}
	
	
//	/**
//	 * Determine the bounding box of a relation.
//	 * @param relation
//	 * @return the {@link List} of {@link Coord}s: the first is the bottom-left
//	 * 		of the bounding box, and the second is the upper-right. 
//	 */
//	private List<Coord> getRelationBox(Relation relation){
//		List<Coord> list = new ArrayList<Coord>(); 
//		Double xmin = Double.POSITIVE_INFINITY;
//		Double ymin = Double.POSITIVE_INFINITY;
//		Double xmax = Double.NEGATIVE_INFINITY;
//		Double ymax = Double.NEGATIVE_INFINITY;
//
//		for(RelationMember rm : relation.getMembers()){
//			if(rm.getMemberType() == EntityType.Node){
//				if(nodeMap.containsKey(rm.getMemberId())){
//					double xNode = nodeMap.get(rm.getMemberId()).getEntity().getLongitude();
//					double yNode = nodeMap.get(rm.getMemberId()).getEntity().getLatitude();
//					if(xNode < xmin){ xmin = xNode; }
//					if(yNode < ymin){ ymin = yNode; }
//					if(xNode > xmax){ xmax = xNode; }
//					if(yNode > ymax){ ymax = yNode; }					
//				} else{
//					log.warn("Node " + rm.getMemberId() + " was not found in nodeMap, and will be ignored.");
//				}
//			} else if(rm.getMemberType() == EntityType.Way){
//				if(wayMap.containsKey(rm.getMemberId())){
//					Way way = wayMap.get(rm.getMemberId()).getEntity();
//					List<Coord> box = this.getWayBox(way);
//					if(box.get(0).getX() < xmin){ xmin = box.get(0).getX(); }
//					if(box.get(0).getY() < ymin){ ymin = box.get(0).getY(); }
//					if(box.get(1).getX() > xmax){ xmax = box.get(1).getX(); }
//					if(box.get(1).getY() > ymax){ ymax = box.get(1).getY(); }									
//				} else{
//					log.warn("Way " + rm.getMemberId() + " was not found in wayMap, and will be ignored.");
//				}
//			} else if(rm.getMemberType() == EntityType.Relation){
////				log.info("                                                                              ----> " + rm.getMemberId());
//				try{
//					if(relationMap.containsKey(rm.getMemberId())){
//						Relation r = relationMap.get(rm.getMemberId()).getEntity();
//						List<Coord> box = this.getRelationBox(r);
//						if(box.get(0).getX() < xmin){ xmin = box.get(0).getX(); }
//						if(box.get(0).getY() < ymin){ ymin = box.get(0).getY(); }
//						if(box.get(1).getX() > xmax){ xmax = box.get(1).getX(); }
//						if(box.get(1).getY() > ymax){ ymax = box.get(1).getY(); }									
//					} else{
//						log.warn("Relation " + rm.getMemberId() + " was not found in relationMap, and will be ignored.");
//					}					
//				} catch(StackOverflowError e){
//					log.error("Circular reference: Relation " + rm.getMemberId());
//					errorCounter++;
//				}
//			} else{
//				log.warn("Could not get the bounding box for EntityType " + rm.getMemberType().toString());
//			}
//		}
//
//		/* Create the bounding coordinates, and add them to the result list. */
//		Coord bottomLeft = new CoordImpl(xmin, ymin);
//		Coord topRight = new CoordImpl(xmax, ymax);
//		list.add(bottomLeft);
//		list.add(topRight);
//		
//		return list;
//	}
	
//	private Coord getNodeCoord(Node node){
//		return ct.transform(new CoordImpl(node.getLongitude(), node.getLatitude()));
//	}
//	
//		
//	/**
//	 * Calculate the centre of the way as the centroid of the bounding box
//	 * of the facility;
//	 * @param way
//	 * @return
//	 */
//	private Coord getWayCentroid(Way way){
//		List<Coord> box = getWayBox(way);
//		double xmin = box.get(0).getX();
//		double ymin = box.get(0).getY();
//		double xmax = box.get(1).getX();
//		double ymax = box.get(1).getY();
//		
//		Double x = xmin + (xmax - xmin)/2;
//		Double y = ymin + (ymax - ymin)/2;
//		
//		/* This should be in WGS84. */
//		Coord c = new CoordImpl(x, y);
//		
//		/* This should be returned in the transformed CRS. */
//		return ct.transform(c);
//	}
//	
//	/**
//	 * Calculate the centre of the relation as the centroid of the bounding box
//	 * of the facility;
//	 * @param relation
//	 * @return
//	 */	
//	private Coord getRelationCentroid(Relation relation){
//		List<Coord> box = getRelationBox(relation);
//		double xmin = box.get(0).getX();
//		double ymin = box.get(0).getY();
//		double xmax = box.get(1).getX();
//		double ymax = box.get(1).getY();
//
//		Double x = xmin + (xmax - xmin)/2;
//		Double y = ymin + (ymax - ymin)/2;
//		
//		/* This should be in WGS84. */
//		Coord c = new CoordImpl(x, y);
//		
//		/* This should be in the transformed CRS. */
//		return ct.transform(c);
//	}
	

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
	
	
//	private void getEducationLevel(String facilityName){
//		/* Try and figure out the type/level of school. */
//		String level = "unknown";
//		if(facilityName.contains("Primary") || facilityName.contains("Laerskool")){
//			level="primary";
//		} else if(facilityName.contains("Secondary") || 
//				  facilityName.contains("High") || 
//				  facilityName.contains("Hoerskool") || 
//				  facilityName.contains("Intermediate") || 
//				  facilityName.contains("College")){
//			level="secondary";
//		} else if(facilityName.contains("University")){
//			level = "tertiary";
//		} 
//
//		educationLevelMap.put(level, educationLevelMap.get(level)+1);
//	}


	@Override
	public void initialize(Map<String, Object> metaData) {
		// TODO Auto-generated method stub
	}

}

