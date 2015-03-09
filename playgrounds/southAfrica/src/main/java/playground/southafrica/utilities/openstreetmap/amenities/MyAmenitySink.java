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

package playground.southafrica.utilities.openstreetmap.amenities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.facilities.ActivityFacilitiesImpl;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;
import org.matsim.facilities.ActivityOptionImpl;
import org.matsim.facilities.OpeningTimeImpl;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.openstreetmap.osmosis.core.container.v0_6.BoundContainer;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.container.v0_6.EntityProcessor;
import org.openstreetmap.osmosis.core.container.v0_6.NodeContainer;
import org.openstreetmap.osmosis.core.container.v0_6.RelationContainer;
import org.openstreetmap.osmosis.core.container.v0_6.WayContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.EntityType;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Relation;
import org.openstreetmap.osmosis.core.domain.v0_6.RelationMember;
import org.openstreetmap.osmosis.core.domain.v0_6.TagCollectionImpl;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;
import org.openstreetmap.osmosis.core.store.SimpleObjectStore;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;

public class MyAmenitySink implements Sink {
	private final Logger log = Logger.getLogger(MyAmenitySink.class);
	private final CoordinateTransformation ct;
	private Map<Long, NodeContainer> nodeMap;
	private Map<Long, WayContainer> wayMap;
	private Map<Long, RelationContainer> relationMap;
	private ActivityFacilitiesImpl facilities;
	private ObjectAttributes facilityAttributes;
	private Map<String,Integer> educationLevelMap;
	
	private int errorCounter = 0;
	private int warningCounter = 0;
	private int educationCounter = 0;
	private int leisureCounter = 0;
	private int shoppingCounter = 0;
	private int otherCounter = 0;
	private int policeCounter = 0;
	private int healthcareCounter = 0;
	
	public MyAmenitySink(CoordinateTransformation ct) {
		this.ct = ct;
		this.nodeMap = new HashMap<Long, NodeContainer>();
		this.wayMap = new HashMap<Long, WayContainer>();
		this.relationMap = new HashMap<Long, RelationContainer>();
		
		facilities = new ActivityFacilitiesImpl("Amenities");
		facilityAttributes = new ObjectAttributes();
		
		/* Keep track of the different education level facilities. */
		educationLevelMap = new HashMap<>();
		educationLevelMap.put("primary", 0);
		educationLevelMap.put("secondary", 0);
		educationLevelMap.put("tertiary", 0);
		educationLevelMap.put("unknown", 0);
	}

	
	@Override
	public void complete() {
		log.info("    nodes: " + nodeMap.size());
		log.info("     ways: " + wayMap.size());
		log.info("relations: " + relationMap.size());
		
		log.info("Creating facilities..");

		/* First check all the point features. */
		int nodeFacilities = 0;
		for(long n : nodeMap.keySet()){
			Node node = nodeMap.get(n).getEntity();
			Map<String, String> tags = new TagCollectionImpl(node.getTags()).buildMap();
			/* Check amenities */
			String amenity = tags.get("amenity");
			if(amenity != null){
				String activityType = getActivityType(amenity);
				String name = tags.get("name");
				if(name != null){
					/* Check education level. */
					if(activityType.equalsIgnoreCase("e")){
						getEducationLevel(name);
					}					
				} else{
					log.warn("      ---> Amenity " + n + " without a name.");
				}

				/* Facility identified. Now get the centroid of all members. */ 
				Coord coord = ct.transform(new CoordImpl(node.getLongitude(), node.getLatitude()));
				Id<ActivityFacility> newId = Id.create(node.getId(), ActivityFacility.class);
				ActivityFacilityImpl af;
				if(!facilities.getFacilities().containsKey(newId)){
					af = facilities.createAndAddFacility(newId, coord);					
					af.setDesc(name);
				} else{
					af = (ActivityFacilityImpl) facilities.getFacilities().get(newId);
				}
				ActivityOptionImpl ao = af.createActivityOption(activityType);
				setFacilityDetails(ao);
				nodeFacilities++;
			}
			/* Check shops */
			String shops = tags.get("shop");
			if(shops != null){
				String name = tags.get("name");
				if(name == null){
					log.warn("      ---> Shop " + n + " without a name.");
				}

				/* Facility identified. Now get the centroid of all members. */ 
				Coord coord = ct.transform(new CoordImpl(node.getLongitude(), node.getLatitude()));
				Id<ActivityFacility> newId = Id.create(node.getId(), ActivityFacility.class);
				ActivityFacilityImpl af;
				if(!facilities.getFacilities().containsKey(newId)){
					af = facilities.createAndAddFacility(newId, coord);					
					af.setDesc(name);
				} else{
					af = (ActivityFacilityImpl) facilities.getFacilities().get(newId);
				}
				ActivityOptionImpl ao = af.createActivityOption("s");
				setFacilityDetails(ao);
				shoppingCounter++;
				nodeFacilities++;
			}
			
		}
		
		/* Second, check for way features. */
		int wayFacilities = 0;
		for(long w : wayMap.keySet()){
			Way way = wayMap.get(w).getEntity();
			Map<String, String> tags = new TagCollectionImpl(way.getTags()).buildMap();
			String amenity = tags.get("amenity");
			if(amenity != null){
				String activityType = getActivityType(amenity);
				String name = tags.get("name");
				if(name != null){
					/* Check education level. */
					if(activityType.equalsIgnoreCase("education")){
						getEducationLevel(name);
					}					
				} else{
					log.warn("      ---> Amenity " + w + " without a name.");
				}

				/* Facility identified. Now get the centroid of all members. */ 
				Coord coord = getWayCentroid(way);
				Id<ActivityFacility> newId = Id.create(way.getId(), ActivityFacility.class);
				ActivityFacilityImpl af;
				if(!facilities.getFacilities().containsKey(newId)){
					af = facilities.createAndAddFacility(newId, coord);					
					af.setDesc(name);
				} else{
					af = (ActivityFacilityImpl) facilities.getFacilities().get(newId);
				}
				ActivityOptionImpl ao = af.createActivityOption(activityType);
				setFacilityDetails(ao);
				wayFacilities++;
			}					
			/* Check shops */
			String shops = tags.get("shop");
			if(shops != null){
				String name = tags.get("name");
				if(name == null){
					log.warn("      ---> Shop " + w + " without a name.");
				}

				/* Facility identified. Now get the centroid of all members. */ 
				Coord coord = getWayCentroid(way);
				Id<ActivityFacility> newId = Id.create(way.getId(), ActivityFacility.class);
				ActivityFacilityImpl af;
				if(!facilities.getFacilities().containsKey(newId)){
					af = facilities.createAndAddFacility(newId, coord);					
					af.setDesc(name);
				} else{
					af = (ActivityFacilityImpl) facilities.getFacilities().get(newId);
				}
				ActivityOptionImpl ao = af.createActivityOption("s");
				setFacilityDetails(ao);
				shoppingCounter++;
				nodeFacilities++;
			}
		}
		
		/* Thirdly, check for relation. */
		int relationFacilities = 0;
		for(long r : relationMap.keySet()){
			Relation relation = relationMap.get(r).getEntity();
			Map<String, String> tags = new TagCollectionImpl(relation.getTags()).buildMap();
			String amenity = tags.get("amenity");
			if(amenity != null){
				String activityType = getActivityType(amenity);
				String name = tags.get("name");
				if(name != null){
					/* Check education level. */
					if(activityType.equalsIgnoreCase("e")){
						getEducationLevel(name);
					}					
				} else{
					log.warn("      ---> Amenity " + r + " without a name.");
				}

				/* Facility identified. Now get the centroid of all members. */ 
				Coord coord = getRelationCentroid(relationMap.get(r).getEntity());
				Id<ActivityFacility> newId = Id.create(relation.getId(), ActivityFacility.class);
				ActivityFacilityImpl af;
				if(!facilities.getFacilities().containsKey(newId)){
					af = facilities.createAndAddFacility(newId, coord);					
					af.setDesc(name);
				} else{
					af = (ActivityFacilityImpl) facilities.getFacilities().get(newId);
				}
				ActivityOptionImpl ao = af.createActivityOption(activityType);
				setFacilityDetails(ao);
				relationFacilities++;
			}					
			/* Check shops */
			String shops = tags.get("shop");
			if(shops != null){
				String name = tags.get("name");
				if(name == null){
					log.warn("      ---> Shop " + r + " without a name.");
				}

				/* Facility identified. Now get the centroid of all members. */ 
				Coord coord = getRelationCentroid(relationMap.get(r).getEntity());
				Id<ActivityFacility> newId = Id.create(relation.getId(), ActivityFacility.class);
				ActivityFacilityImpl af;
				if(!facilities.getFacilities().containsKey(newId)){
					af = facilities.createAndAddFacility(newId, coord);					
					af.setDesc(name);
				} else{
					af = (ActivityFacilityImpl) facilities.getFacilities().get(newId);
				}
				ActivityOptionImpl ao = af.createActivityOption("s");
				setFacilityDetails(ao);
				shoppingCounter++;
				nodeFacilities++;
			}
		}
		log.info("------------------------------------------------");
		log.info("Facilities parsed:");
		log.info("  nodes    : " + nodeFacilities);
		log.info("  ways     : " + wayFacilities);
		log.info("  relations: " + relationFacilities);
		log.info("------------------------------------------------");
		log.info("Done creating facilities.");
		log.info("  education  : " + educationCounter);
		log.info("  leisure    : " + leisureCounter);
		log.info("  shopping   : " + shoppingCounter);		
		log.info("  healthcare : " + healthcareCounter);
		log.info("  police     : " + policeCounter);
		log.info("  other      : " + otherCounter);
		log.info("------------------------------------------------");
		log.info("Level of education:");
		log.info(" primary  : " + educationLevelMap.get("primary") );
		log.info(" secondary: " + educationLevelMap.get("secondary") );
		log.info(" tertiary : " + educationLevelMap.get("tertiary"));
		log.info(" unknown  : " + educationLevelMap.get("unknown"));
		log.info("------------------------------------------------");
		log.info("Errors and warnings:");
		log.info("  errors  : " + errorCounter);
		log.info("  warnings: " + warningCounter);
	}
	
	
	private void setFacilityDetails(ActivityOptionImpl ao){
		if(ao.getType().equalsIgnoreCase("s")){
			double r1 = MatsimRandom.getRandom().nextDouble();
			double starttime = Math.round(r1)*28800 + (1-Math.round(r1))*32400; // either 08:00 or 09:00
			double r2 = MatsimRandom.getRandom().nextDouble();
			double closingtime = Math.round(r2)*54000 + (1-Math.round(r2))*68400; // either 17:00 or 19:00
			ao.addOpeningTime(new OpeningTimeImpl(starttime, closingtime));
			
			double r3 = MatsimRandom.getRandom().nextDouble();
			double capacity = (200 + r3*800) / 10; // random surface between 200 and 1000 m^2.
			ao.setCapacity(capacity);
		} else if(ao.getType().equalsIgnoreCase("l")){
			double r1 = MatsimRandom.getRandom().nextDouble();
			double starttime = Math.round(r1)*28800 + (1-Math.round(r1))*32400; // either 08:00 or 09:00
			double r2 = MatsimRandom.getRandom().nextDouble();
			double closingtime = Math.round(r2)*64800 + (1-Math.round(r2))*79200; // either 18:00 or 22:00
			ao.addOpeningTime(new OpeningTimeImpl(starttime, closingtime));
			
			double r3 = MatsimRandom.getRandom().nextDouble();
			double capacity = (50 + r3*1950) / 10; // random surface between 50 and 2000 m^2.
			ao.setCapacity(capacity);
		} else{
			double r1 = MatsimRandom.getRandom().nextDouble();
			double starttime = Math.round(r1)*28800 + (1-Math.round(r1))*32400; // either 08:00 or 09:00
			double r2 = MatsimRandom.getRandom().nextDouble();
			double closingtime = Math.round(r2)*54000 + (1-Math.round(r2))*68400; // either 17:00 or 19:00
			ao.addOpeningTime(new OpeningTimeImpl(starttime, closingtime));
			
			double r3 = MatsimRandom.getRandom().nextDouble();
			double capacity = (200 + r3*800) / 10; // random number between 200 and 1000 m^2.
			ao.setCapacity(capacity);
		}
	}
	
	
	/**
	 * Return the facilities parsed.
	 * @return
	 */
	public ActivityFacilitiesImpl getFacilities(){
		return facilities;
	}
	
	
	/**
	 * Return the facility attributes.
	 * @return
	 */
	public ObjectAttributes getFacilityAttributes(){
		return this.facilityAttributes;
	}
	
	
	/**
	 * Determine the bounding box of a closed way.
	 * @param way
	 * @return the {@link List} of {@link Coord}s: the first is the bottom-left
	 * 		of the bounding box, and the second is the upper-right. 
	 */
	private List<Coord> getWayBox(Way way){
		List<Coord> list = new ArrayList<Coord>();
		Double xmin = Double.POSITIVE_INFINITY;
		Double ymin = Double.POSITIVE_INFINITY;
		Double xmax = Double.NEGATIVE_INFINITY;
		Double ymax = Double.NEGATIVE_INFINITY;
		for(WayNode n : way.getWayNodes()){
			double xNode = nodeMap.get(n.getNodeId()).getEntity().getLongitude();
			double yNode = nodeMap.get(n.getNodeId()).getEntity().getLatitude();
			if(xNode < xmin){ xmin = xNode; }
			if(yNode < ymin){ ymin = yNode; }
			if(xNode > xmax){ xmax = xNode; }
			if(yNode > ymax){ ymax = yNode; }
		}

		/* Create the bounding coordinates, and add them to the result list. */
		Coord bottomLeft = new CoordImpl(xmin, ymin);
		Coord topRight = new CoordImpl(xmax, ymax);
		list.add(bottomLeft);
		list.add(topRight);
		
		return list;
	}
	
	
	/**
	 * Determine the bounding box of a relation.
	 * @param relation
	 * @return the {@link List} of {@link Coord}s: the first is the bottom-left
	 * 		of the bounding box, and the second is the upper-right. 
	 */
	private List<Coord> getRelationBox(Relation relation){
		List<Coord> list = new ArrayList<Coord>(); 
		Double xmin = Double.POSITIVE_INFINITY;
		Double ymin = Double.POSITIVE_INFINITY;
		Double xmax = Double.NEGATIVE_INFINITY;
		Double ymax = Double.NEGATIVE_INFINITY;

		for(RelationMember rm : relation.getMembers()){
			if(rm.getMemberType() == EntityType.Node){
				if(nodeMap.containsKey(rm.getMemberId())){
					double xNode = nodeMap.get(rm.getMemberId()).getEntity().getLongitude();
					double yNode = nodeMap.get(rm.getMemberId()).getEntity().getLatitude();
					if(xNode < xmin){ xmin = xNode; }
					if(yNode < ymin){ ymin = yNode; }
					if(xNode > xmax){ xmax = xNode; }
					if(yNode > ymax){ ymax = yNode; }					
				} else{
					log.warn("Node " + rm.getMemberId() + " was not found in nodeMap, and will be ignored.");
				}
			} else if(rm.getMemberType() == EntityType.Way){
				if(wayMap.containsKey(rm.getMemberId())){
					Way way = wayMap.get(rm.getMemberId()).getEntity();
					List<Coord> box = this.getWayBox(way);
					if(box.get(0).getX() < xmin){ xmin = box.get(0).getX(); }
					if(box.get(0).getY() < ymin){ ymin = box.get(0).getY(); }
					if(box.get(1).getX() > xmax){ xmax = box.get(1).getX(); }
					if(box.get(1).getY() > ymax){ ymax = box.get(1).getY(); }									
				} else{
					log.warn("Way " + rm.getMemberId() + " was not found in wayMap, and will be ignored.");
				}
			} else if(rm.getMemberType() == EntityType.Relation){
//				log.info("                                                                              ----> " + rm.getMemberId());
				try{
					if(relationMap.containsKey(rm.getMemberId())){
						Relation r = relationMap.get(rm.getMemberId()).getEntity();
						List<Coord> box = this.getRelationBox(r);
						if(box.get(0).getX() < xmin){ xmin = box.get(0).getX(); }
						if(box.get(0).getY() < ymin){ ymin = box.get(0).getY(); }
						if(box.get(1).getX() > xmax){ xmax = box.get(1).getX(); }
						if(box.get(1).getY() > ymax){ ymax = box.get(1).getY(); }									
					} else{
						log.warn("Relation " + rm.getMemberId() + " was not found in relationMap, and will be ignored.");
					}					
				} catch(StackOverflowError e){
					log.error("Circular reference: Relation " + rm.getMemberId());
					errorCounter++;
				}
			} else{
				log.warn("Could not get the bounding box for EntityType " + rm.getMemberType().toString());
			}
		}

		/* Create the bounding coordinates, and add them to the result list. */
		Coord bottomLeft = new CoordImpl(xmin, ymin);
		Coord topRight = new CoordImpl(xmax, ymax);
		list.add(bottomLeft);
		list.add(topRight);
		
		return list;
	}
	
		
	/**
	 * Calculate the centre of the way as the centroid of the bounding box
	 * of the facility;
	 * @param way
	 * @return
	 */
	private Coord getWayCentroid(Way way){
		List<Coord> box = getWayBox(way);
		double xmin = box.get(0).getX();
		double ymin = box.get(0).getY();
		double xmax = box.get(1).getX();
		double ymax = box.get(1).getY();
		
		Double x = xmin + (xmax - xmin)/2;
		Double y = ymin + (ymax - ymin)/2;
		
		/* This should be in WGS84. */
		Coord c = new CoordImpl(x, y);
		
		/* This should be returned in the transformed CRS. */
		return ct.transform(c);
	}
	
	/**
	 * Calculate the centre of the relation as the centroid of the bounding box
	 * of the facility;
	 * @param relation
	 * @return
	 */	
	private Coord getRelationCentroid(Relation relation){
		List<Coord> box = getRelationBox(relation);
		double xmin = box.get(0).getX();
		double ymin = box.get(0).getY();
		double xmax = box.get(1).getX();
		double ymax = box.get(1).getY();

		Double x = xmin + (xmax - xmin)/2;
		Double y = ymin + (ymax - ymin)/2;
		
		/* This should be in WGS84. */
		Coord c = new CoordImpl(x, y);
		
		/* This should be in the transformed CRS. */
		return ct.transform(c);
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
	
	
	private String getActivityType(String amenity){
		/* EDUCATION */
		if(
					amenity.equalsIgnoreCase("school") ||
					amenity.equalsIgnoreCase("kindergarten") ||
					amenity.equalsIgnoreCase("college") ||
					amenity.equalsIgnoreCase("university")){
			educationCounter++;
			return "e";
		/* LEISURE */
		} else if(
				/* Sustenance */
					amenity.equalsIgnoreCase("bar") ||
					amenity.equalsIgnoreCase("cafe") ||
					amenity.equalsIgnoreCase("fast_food") ||
					amenity.equalsIgnoreCase("food_court") ||
					amenity.equalsIgnoreCase("ice_cream") ||
					amenity.equalsIgnoreCase("pub") ||
					amenity.equalsIgnoreCase("restaurant") ||
					/* Entertainment, arts and culture */
					amenity.equalsIgnoreCase("arts_centre") ||
					amenity.equalsIgnoreCase("cinema") ||
					amenity.equalsIgnoreCase("nightclub") ||
					amenity.equalsIgnoreCase("stripclub") ||
					amenity.equalsIgnoreCase("theatre") ||
					/* Other */
					amenity.equalsIgnoreCase("brothel")){
			leisureCounter++;
			return "l";
		}else if(
				amenity.equalsIgnoreCase("clinic") ||
				amenity.equalsIgnoreCase("dentist") ||
				amenity.equalsIgnoreCase("doctors") ||
				amenity.equalsIgnoreCase("hospital") ||
				amenity.equalsIgnoreCase("nursing_home") ||
				amenity.equalsIgnoreCase("pharmacy")
				){
			healthcareCounter++;
			return "m";
		} else if(
				amenity.equalsIgnoreCase("police")
				){
			policeCounter++;
			return "p";
		/* OTHER */
		} else if(
				amenity.equalsIgnoreCase("library") ||
				amenity.equalsIgnoreCase("car_wash") ||
				amenity.equalsIgnoreCase("fuel") ||
				amenity.equalsIgnoreCase("atm") ||
				amenity.equalsIgnoreCase("bank") ||
				amenity.equalsIgnoreCase("bureau_de_change") ||
				amenity.equalsIgnoreCase("social_centre") ||
				amenity.equalsIgnoreCase("marketplace") ||
				amenity.equalsIgnoreCase("place_of_worship") ||
				amenity.equalsIgnoreCase("post_office") ||
				amenity.equalsIgnoreCase("townhall")						
				){
			otherCounter++;
			return "t";
		}
		return "t";
	}
	
	private void getEducationLevel(String facilityName){
		/* Try and figure out the type/level of school. */
		String level = "unknown";
		if(facilityName.contains("Primary") || facilityName.contains("Laerskool")){
			level="primary";
		} else if(facilityName.contains("Secondary") || 
				  facilityName.contains("High") || 
				  facilityName.contains("Hoerskool") || 
				  facilityName.contains("Intermediate") || 
				  facilityName.contains("College")){
			level="secondary";
		} else if(facilityName.contains("University")){
			level = "tertiary";
		} 

		educationLevelMap.put(level, educationLevelMap.get(level)+1);
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

