package playground.ciarif.retailers;

import org.matsim.basic.v01.BasicLinkImpl;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.basic.v01.Coord;
import org.matsim.interfaces.core.v01.Facility;
import org.matsim.interfaces.core.v01.Link;
import org.matsim.interfaces.core.v01.Person;
import org.matsim.utils.collections.QuadTree;
import org.matsim.utils.geometry.CoordImpl;

public abstract class Utils {
	private final static double EPSILON = 0.0001;
	public static final void moveFacility(Facility f, BasicLinkImpl link) {
		double [] vector = new double[2];
		vector[0] = link.getToNode().getCoord().getY()-link.getFromNode().getCoord().getY();
		vector[1] = -(link.getToNode().getCoord().getX()-link.getFromNode().getCoord().getX());
//		double length = Math.sqrt(vector[0]*vector[0]+vector[1]*vector[1]);
//		System.out.println("length = " + length);
		Coord coord = new CoordImpl(link.getCoord().getX()+vector[0]*EPSILON,link.getCoord().getY()+vector[1]*EPSILON);
		f.moveTo(coord);
//		
//		Link oldL = f.getLink();
//		if (oldL != null) {
//			Gbl.getWorld().removeMapping(f, oldL);
//		}
//		Gbl.getWorld().addMapping(f, link);
//	
	}
	
	// BAD CODE STYLE but keep that anyway for the moment
	private static QuadTree<Person> personQuadTree = null;
	
	public static final void setPersonQuadTree(QuadTree<Person> personQuadTree) {
		Utils.personQuadTree = personQuadTree;
	}
	
	public static final QuadTree<Person> getPersonQuadTree() {
		return Utils.personQuadTree;
	}
}
