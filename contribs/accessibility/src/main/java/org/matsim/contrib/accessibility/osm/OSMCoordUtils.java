package org.matsim.contrib.accessibility.osm;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.openstreetmap.osmosis.core.container.v0_6.NodeContainer;
import org.openstreetmap.osmosis.core.container.v0_6.RelationContainer;
import org.openstreetmap.osmosis.core.container.v0_6.WayContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.domain.v0_6.EntityType;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Relation;
import org.openstreetmap.osmosis.core.domain.v0_6.RelationMember;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;

/**
 * @author dziemke
 */
class CoordUtils {
	private final static Logger log = Logger.getLogger(CoordUtils.class);


	public static Coord getCentroidCoord(
			Entity entity,
			CoordinateTransformation ct,			
			Map<Long, NodeContainer> nodeMap,
			Map<Long, WayContainer> wayMap,
			Map<Long, RelationContainer> relationMap){
		if(entity instanceof Node){
			return getNodeCoord((Node)entity, ct);
		} else if(entity instanceof Way){
			return getWayCentroid((Way)entity, ct, nodeMap);
		} else if(entity instanceof Relation){
			return getRelationCentroid((Relation)entity, ct, nodeMap, wayMap, relationMap);
		}
		
		return null;
	}
	
	
	private static Coord getNodeCoord(Node node, CoordinateTransformation ct){
		return ct.transform(new Coord(node.getLongitude(), node.getLatitude()));
	}
	
		
	/**
	 * Calculate the centre of the way as the centroid of the bounding box
	 * of the facility;
	 * @param way
	 * @return
	 */
	private static Coord getWayCentroid(Way way, CoordinateTransformation ct, Map<Long, NodeContainer> nodeMap){
		List<Coord> box = getWayBox(way, nodeMap);
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
	private static Coord getRelationCentroid(Relation relation, CoordinateTransformation ct, Map<Long, NodeContainer> nodeMap,
			Map<Long, WayContainer> wayMap, Map<Long, RelationContainer> relationMap){
		List<Coord> box = getRelationBox(relation, nodeMap, wayMap, relationMap);
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
	
	
	/**
	 * Determine the bounding box of a closed way.
	 * @param way
	 * @return the {@link List} of {@link Coord}s: the first is the bottom-left
	 * 		of the bounding box, and the second is the upper-right. 
	 */
	private static List<Coord> getWayBox(Way way, Map<Long, NodeContainer> nodeMap){
		List<Coord> list = new ArrayList<Coord>();
		Double xmin = Double.POSITIVE_INFINITY;
		Double ymin = Double.POSITIVE_INFINITY;
		Double xmax = Double.NEGATIVE_INFINITY;
		Double ymax = Double.NEGATIVE_INFINITY;
		for(WayNode wayNode : way.getWayNodes()){
			double xNode = nodeMap.get(wayNode.getNodeId()).getEntity().getLongitude();
			double yNode = nodeMap.get(wayNode.getNodeId()).getEntity().getLatitude();
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
	
	
	public static Coord[] getAllWayCoords(Way way, CoordinateTransformation ct, Map<Long, NodeContainer> nodeMap){
		List<Coord> list = new ArrayList<Coord>(); 
		
		for(WayNode wayNode : way.getWayNodes()){
			NodeContainer nc = nodeMap.get(wayNode.getNodeId());
			if(nc == null){
				log.error("Oops... some way coords are missing.");
				throw new RuntimeException("Ensure you pass the 'completeWays=yes' argument when executing osmosis commands.");
			}
			Node node = nc.getEntity();
			double xNode = node.getLongitude();
			double yNode = node.getLatitude();

			Coord coord = new Coord(xNode, yNode);
			
			list.add(ct.transform(coord));
		}		
				
		Coord[] coords = new Coord[list.size()];
		for (int i=0; i < list.size(); i++) {
			coords[i] = list.get(i);
		}
		
		return coords;
	}
		
	
	/**
	 * Determine the bounding box of a relation.
	 * @param relation
	 * @return the {@link List} of {@link Coord}s: the first is the bottom-left
	 * 		of the bounding box, and the second is the upper-right. 
	 */
	private static List<Coord> getRelationBox(Relation relation, Map<Long, NodeContainer> nodeMap, Map<Long, WayContainer> wayMap, Map<Long, RelationContainer> relationMap){
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
//					List<Coord> box = this.getWayBox(way);
					List<Coord> box = getWayBox(way, nodeMap);
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
//						List<Coord> box = this.getRelationBox(r);
						List<Coord> box = getRelationBox(r, nodeMap, wayMap, relationMap);
						if(box.get(0).getX() < xmin){ xmin = box.get(0).getX(); }
						if(box.get(0).getY() < ymin){ ymin = box.get(0).getY(); }
						if(box.get(1).getX() > xmax){ xmax = box.get(1).getX(); }
						if(box.get(1).getY() > ymax){ ymax = box.get(1).getY(); }									
					} else{
						log.warn("Relation " + rm.getMemberId() + " was not found in relationMap, and will be ignored.");
					}					
				} catch(StackOverflowError e){
					log.error("Circular reference: Relation " + rm.getMemberId());
//					errorCounter++;
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
}
