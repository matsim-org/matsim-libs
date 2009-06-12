package playground.ciarif.retailers;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.api.facilities.ActivityFacility;
import org.matsim.core.api.population.Person;
import org.matsim.core.utils.collections.QuadTree;

public class RetailZone {
	private final Id id;
	private static QuadTree<Person> personsQuadTree; // TODO Check if it does make more sense private, or if static does make sense at all  
	private static QuadTree<ActivityFacility> shopsQuadTree;
	
	public RetailZone(final Id id,Double minx, Double miny,Double maxx,Double maxy) { 
		this.id = id;
		QuadTree<Person> personsQuadTree = new QuadTree<Person>(minx, miny, maxx, maxy);
		QuadTree<ActivityFacility> shopsQuadTree = new QuadTree<ActivityFacility>(minx, miny, maxx, maxy);
	}
}	

