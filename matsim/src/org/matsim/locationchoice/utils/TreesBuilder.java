package org.matsim.locationchoice.utils;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Id;
import org.matsim.core.api.facilities.ActivityFacilities;
import org.matsim.core.api.facilities.ActivityFacility;
import org.matsim.core.api.facilities.ActivityOption;
import org.matsim.core.api.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.locationchoice.LocationChoice;

public class TreesBuilder {
	
	private NetworkLayer network = null;
	private static final Logger log = Logger.getLogger(LocationChoice.class);
	private HashSet<String> flexibleTypes = new HashSet<String>();
	
	protected TreeMap<String, QuadTree<ActivityFacility>> quadTreesOfType = new TreeMap<String, QuadTree<ActivityFacility>>();
	protected TreeMap<String, ActivityFacility []> facilitiesOfType = new TreeMap<String, ActivityFacility []>();
	
	
	public TreesBuilder(HashSet<String> flexibleTypes, NetworkLayer network) {
		this.flexibleTypes = flexibleTypes;
		this.network = network;
	}
	
	public TreesBuilder(NetworkLayer network) {
		this.initFlexibleTypes();
		this.network = network;
	}
	
	private void initFlexibleTypes() {
		String types = Gbl.getConfig().locationchoice().getFlexibleTypes();
		
		if (!types.equals("null")) {
			log.info("Doing location choice for activity types: " + types);
			String[] entries = types.split(",", -1);
			for (int i = 0; i < entries.length; i++) {
				if (!entries[i].trim().equals("null")) {
					this.flexibleTypes.add(entries[i].trim());
				}
			}
		}
	}
	
	public void createTrees(ActivityFacilities facilities) {
		TreeMap<String, TreeMap<Id, ActivityFacility>> treesForTypes = this.createTreesForTypes(facilities); 
		this.createQuadTreesAndArrays(treesForTypes);
	}
	
	private TreeMap<String, TreeMap<Id, ActivityFacility>> createTreesForTypes(ActivityFacilities facilities) {
		
		boolean regionalScenario = false;
		double radius = 0.0;
		Node centerNode = null; 
		
		if (!Gbl.getConfig().locationchoice().getCenterNode().equals("null") &&
				!Gbl.getConfig().locationchoice().getRadius().equals("null")) {
			regionalScenario = true;
			centerNode = this.network.getNode(new IdImpl(Gbl.getConfig().locationchoice().getCenterNode()));
			radius = Double.parseDouble(Gbl.getConfig().locationchoice().getRadius());
		}
		
		TreeMap<String, TreeMap<Id, ActivityFacility>> trees = new TreeMap<String, TreeMap<Id, ActivityFacility>>();
		// get all types of activities
		for (ActivityFacility f : facilities.getFacilities().values()) {
			Map<String, ActivityOption> activities = f.getActivityOptions();
			
			// do not add facility if it is not in region of interest ------------------------
			if (regionalScenario && f.calcDistance(centerNode.getCoord()) > radius) continue;
			// -------------------------------------------------------------------------------
			
			Iterator<ActivityOption> act_it = activities.values().iterator();
			while (act_it.hasNext()) {
				ActivityOption act = act_it.next();
				
				// do only add activities of flexibleTypes if flexibleTypes != null
				if (this.flexibleTypes.size() == 0 ||  this.flexibleTypes.contains(act.getType())) {
					if (!trees.containsKey(act.getType())) {
						trees.put(act.getType(), new TreeMap<Id, ActivityFacility>());
					}
					trees.get(act.getType()).put(f.getId(), f);
				}
			}	
		}
		return trees;
	}
	
	private void createQuadTreesAndArrays(TreeMap<String, TreeMap<Id, ActivityFacility>> trees) {
		Iterator<TreeMap<Id, ActivityFacility>> tree_it = trees.values().iterator();
		Iterator<String> type_it = trees.keySet().iterator();
			
		while (tree_it.hasNext()) {
			TreeMap<Id, ActivityFacility> tree_of_type = tree_it.next();
			String type = type_it.next();
			
			// do not construct tree for home and tta act
			if (type.startsWith("h") || type.startsWith("tta")) continue;
			
			this.quadTreesOfType.put(type, this.builFacQuadTree(type, tree_of_type));
			this.facilitiesOfType.put(type, tree_of_type.values().toArray(new ActivityFacility[tree_of_type.size()]));
		}
	}
	
	private QuadTree<ActivityFacility> builFacQuadTree(String type, TreeMap<Id,ActivityFacility> facilities_of_type) {
		Gbl.startMeasurement();
		log.info(" building " + type + " facility quad tree");
		double minx = Double.POSITIVE_INFINITY;
		double miny = Double.POSITIVE_INFINITY;
		double maxx = Double.NEGATIVE_INFINITY;
		double maxy = Double.NEGATIVE_INFINITY;

		for (final ActivityFacility f : facilities_of_type.values()) {
			if (f.getCoord().getX() < minx) { minx = f.getCoord().getX(); }
			if (f.getCoord().getY() < miny) { miny = f.getCoord().getY(); }
			if (f.getCoord().getX() > maxx) { maxx = f.getCoord().getX(); }
			if (f.getCoord().getY() > maxy) { maxy = f.getCoord().getY(); }
		}
		minx -= 1.0;
		miny -= 1.0;
		maxx += 1.0;
		maxy += 1.0;
		System.out.println("        xrange(" + minx + "," + maxx + "); yrange(" + miny + "," + maxy + ")");
		QuadTree<ActivityFacility> quadtree = new QuadTree<ActivityFacility>(minx, miny, maxx, maxy);
		for (final ActivityFacility f : facilities_of_type.values()) {
			quadtree.put(f.getCoord().getX(),f.getCoord().getY(),f);
		}
		log.info("    done");
		Gbl.printRoundTime();
		return quadtree;
	}

	public TreeMap<String, QuadTree<ActivityFacility>> getQuadTreesOfType() {
		return quadTreesOfType;
	}
	public TreeMap<String, ActivityFacility[]> getFacilitiesOfType() {
		return facilitiesOfType;
	}
}
