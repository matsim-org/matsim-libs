package playground.ciarif.retailers;

import org.matsim.facilities.Facility;
import org.matsim.interfaces.basic.v01.Coord;
import org.matsim.network.Link;
import org.matsim.utils.geometry.CoordImpl;

public abstract class Utils {
	private final static double EPSILON = 0.0001;
	public static final void moveFacility(Facility f, Link link) {
		double [] vector = new double[2];
		vector[0] = link.getToNode().getCoord().getY()-link.getFromNode().getCoord().getY();
		vector[1] = -(link.getToNode().getCoord().getX()-link.getFromNode().getCoord().getX());
		double length = Math.sqrt(vector[0]*vector[0]+vector[1]*vector[0]);
		vector[0] = EPSILON*(vector[0]/length);
		vector[1] = EPSILON*(vector[1]/length);
		
		Coord coord = new CoordImpl(link.getCenter().getX()+vector[0],link.getCenter().getY()+vector[1]);
		f.moveTo(coord);
	}

}
