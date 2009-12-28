package playground.ciarif.retailers.utils;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.world.World;

public abstract class Utils {
	
	
	private final static double EPSILON = 0.0001;
	public static final void moveFacility(ActivityFacilityImpl f, Link link, World world) {
		double [] vector = new double[2];
		vector[0] = link.getToNode().getCoord().getY()-link.getFromNode().getCoord().getY();
		vector[1] = -(link.getToNode().getCoord().getX()-link.getFromNode().getCoord().getX());
//		double length = Math.sqrt(vector[0]*vector[0]+vector[1]*vector[1]);
//		System.out.println("length = " + length);
		Coord coord = new CoordImpl(link.getCoord().getX()+vector[0]*EPSILON,link.getCoord().getY()+vector[1]*EPSILON);
		f.moveTo(coord);

		Link oldL = f.getLink();
		if (oldL != null) {
			world.removeMapping(f, (LinkImpl) oldL);
		}
		world.addMapping(f, (LinkImpl) link);

	}
	
	// BAD CODE STYLE but keep that anyway for the moment
	private static QuadTree<Person> personQuadTree = null;
	private static QuadTree<ActivityFacility> facilityQuadTree = null;
	
	public static final void setPersonQuadTree(QuadTree<Person> personQuadTree) {
		Utils.personQuadTree = personQuadTree;
	}
	
	public static final QuadTree<Person> getPersonQuadTree() {
		return Utils.personQuadTree;
	}
	
	public static final void setFacilityQuadTree(QuadTree<ActivityFacility> facilityQuadTree) {
		Utils.facilityQuadTree  = facilityQuadTree;
	}
	
	public static final QuadTree<ActivityFacility> getFacilityQuadTree() {
		return Utils.facilityQuadTree;
	}
}
