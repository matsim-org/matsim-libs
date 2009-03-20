package playground.ciarif.retailers;

import org.matsim.basic.v01.BasicLinkImpl;
import org.matsim.interfaces.basic.v01.Coord;
import org.matsim.interfaces.core.v01.Facility;
import org.matsim.interfaces.core.v01.Link;
import org.matsim.interfaces.core.v01.Person;
import org.matsim.utils.collections.QuadTree;
import org.matsim.utils.geometry.CoordImpl;

public abstract class Utils {
	private final static double EPSILON = 0.000000001;
	public static final void moveFacility(Facility f, BasicLinkImpl link) {
		double [] vector = new double[2];
		vector[0] = link.getToNode().getCoord().getY()-link.getFromNode().getCoord().getY();
		vector[1] = -(link.getToNode().getCoord().getX()-link.getFromNode().getCoord().getX());
		double length = Math.sqrt(vector[0]*vector[0]+vector[1]*vector[1]);
		Coord coord = new CoordImpl(link.getCoord().getX()+vector[0]*EPSILON*length,link.getCoord().getY()+vector[1]*EPSILON*length);
		System.out.println("Coord of the new link = " + coord);
		System.out.println("Coord of the old link = " + f.getLink().getCoord());
		f.moveTo(coord);
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
