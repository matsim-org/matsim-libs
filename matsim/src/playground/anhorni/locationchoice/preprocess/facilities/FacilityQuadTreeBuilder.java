package playground.anhorni.locationchoice.preprocess.facilities;

import java.util.List;
import java.util.TreeMap;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.locationchoice.LocationMutator;
import org.matsim.locationchoice.utils.QuadTreeRing;


public class FacilityQuadTreeBuilder {
	
	private static final Logger log = Logger.getLogger(LocationMutator.class);
	
	public QuadTreeRing<ActivityFacilityImpl> buildFacilityQuadTree(String type, List<ActivityFacilityImpl> facilities) {
		
		TreeMap<Id, ActivityFacilityImpl> treeMap = new TreeMap<Id, ActivityFacilityImpl>();
		// get all types of activities
		for (ActivityFacilityImpl f : facilities) {		
			if (!treeMap.containsKey(f.getId())) {
				treeMap.put(f.getId(), f);
			}	
		}
		return this.builFacQuadTree(type, treeMap);
	}
	
	
	public QuadTreeRing<ActivityFacilityImpl> buildFacilityQuadTree(String type, ActivityFacilitiesImpl facilities) {
		TreeMap<Id, ActivityFacilityImpl> treeMap = new TreeMap<Id, ActivityFacilityImpl>();
		// get all types of activities
		for (ActivityFacilityImpl f : facilities.getFacilitiesForActivityType(type).values()) {		
			if (!treeMap.containsKey(f.getId())) {
				treeMap.put(f.getId(), f);
			}	
		}
		return this.builFacQuadTree(type, treeMap);
	}
	
	
	public QuadTreeRing<ActivityFacilityImpl> builFacQuadTree(String type, TreeMap<Id,ActivityFacilityImpl> facilities_of_type) {
		Gbl.startMeasurement();
		log.info(" building " + type + " facility quad tree");
		double minx = Double.POSITIVE_INFINITY;
		double miny = Double.POSITIVE_INFINITY;
		double maxx = Double.NEGATIVE_INFINITY;
		double maxy = Double.NEGATIVE_INFINITY;

		for (final ActivityFacilityImpl f : facilities_of_type.values()) {
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
		QuadTreeRing<ActivityFacilityImpl> quadtree = new QuadTreeRing<ActivityFacilityImpl>(minx, miny, maxx, maxy);
		for (final ActivityFacilityImpl f : facilities_of_type.values()) {
			quadtree.put(f.getCoord().getX(),f.getCoord().getY(),f);
		}
		log.info("Number of facilities: " + quadtree.size());
		Gbl.printRoundTime();
		return quadtree;
	}
}
