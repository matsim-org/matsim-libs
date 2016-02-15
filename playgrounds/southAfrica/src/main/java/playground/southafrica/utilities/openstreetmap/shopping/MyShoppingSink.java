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

package playground.southafrica.utilities.openstreetmap.shopping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
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
import org.openstreetmap.osmosis.core.domain.v0_6.Relation;
import org.openstreetmap.osmosis.core.domain.v0_6.RelationMember;
import org.openstreetmap.osmosis.core.domain.v0_6.TagCollectionImpl;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;
import org.openstreetmap.osmosis.core.store.SimpleObjectStore;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;

public class MyShoppingSink implements Sink {
	private final Logger log = Logger.getLogger(MyShoppingSink.class);
	private final CoordinateTransformation ct;
	private Map<Long, NodeContainer> nodeMap;
	private Map<Long, WayContainer> wayMap;
	private Map<Long, RelationContainer> relationMap;
	private ActivityFacilitiesImpl facilities;
	private ObjectAttributes facilityAttributes;
	
	private int warningCounter = 0;
	private int errorCounter = 0;
	
	public MyShoppingSink(CoordinateTransformation ct) {
		this.ct = ct;
		this.nodeMap = new HashMap<Long, NodeContainer>();
		this.wayMap = new HashMap<Long, WayContainer>();
		this.relationMap = new HashMap<Long, RelationContainer>();
		
		facilities = new ActivityFacilitiesImpl("SACSC Centre");
		facilityAttributes = new ObjectAttributes();
	}

	
	@Override
	public void complete() {
		log.info("    nodes: " + nodeMap.size());
		log.info("     ways: " + wayMap.size());
		log.info("relations: " + relationMap.size());
		
		log.info("Creating facilities..");
				
		for(long r : relationMap.keySet()){
			Relation relation = relationMap.get(r).getEntity();
			
			/* Find the SACSC-captured relations. */ 
			Map<String, String> tags = new TagCollectionImpl(relation.getTags()).buildMap();
			String site = tags.get("site");
			String type = tags.get("type");
			if(site != null && type != null){
				if(type.equalsIgnoreCase("site") && site.equalsIgnoreCase("mall")){
					String gla = tags.get("lettable_surface_area");
					String name = tags.get("name");
					if(gla == null || name == null){
						log.warn("Shopping mall relations without name or GLA: OSM Id " + r + ". Shopping mall ignored.");
						warningCounter++;
					}else{
						/* Shopping mall identified. Now get the centroid of all members. */ 
						Coord coord = getRelationCentroid(relationMap.get(r).getEntity());
						ActivityFacilityImpl mall = facilities.createAndAddFacility(Id.create(relation.getId(), ActivityFacility.class), coord);
						mall.setDesc(name);
						/* Shopping */
						ActivityOptionImpl shopping = mall.createAndAddActivityOption("s");
						shopping.addOpeningTime(new OpeningTimeImpl(32400, 61200)); // 09:00 - 17:00
						shopping.setCapacity(Double.parseDouble(gla) / 10);

						/* Work */
						ActivityOptionImpl work = mall.createAndAddActivityOption("w");
						work.addOpeningTime(new OpeningTimeImpl(28800, 72000)); // 08:00 - 20:00
						work.setCapacity(Double.parseDouble(gla) / 20);
						
						ActivityOptionImpl leisure = mall.createAndAddActivityOption("l");
						leisure.addOpeningTime(new OpeningTimeImpl(28800, 72000)); // 08:00 - 20:00
						leisure.setCapacity(Double.parseDouble(gla) / 10);

						ActivityOptionImpl minor = mall.createAndAddActivityOption("minor");
						
						facilityAttributes.putAttribute(mall.getId().toString(), "gla", gla);
					}					
				}
			}
		}
		log.info("------------------------------------------------");
		log.info("Done creating facilities.");
		log.info(" Warnings: " + warningCounter);
		log.info(" Errors  : " + errorCounter);
		log.info("------------------------------------------------");
	}
	
	
	/**
	 * Return the SACSC facilities parsed.
	 * @return
	 */
	public ActivityFacilitiesImpl getFacilities(){
		return facilities;
	}
	
	
	/**
	 * Return the SACSC facility attributes.
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
		Coord bottomLeft = new Coord(xmin, ymin);
		Coord topRight = new Coord(xmax, ymax);
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
		Coord bottomLeft = new Coord(xmin, ymin);
		Coord topRight = new Coord(xmax, ymax);
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
		Coord c = new Coord(x, y);
		
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
		Coord c = new Coord(x, y);
		
		/* This should be in the transformed CRS. */
		return ct.transform(c);
	}
	

	@Override
	public void release() {
		// TODO Auto-generated method stub

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

