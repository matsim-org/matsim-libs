package playground.sergioo.workplaceCapacities2012;

import java.util.Collection;

import org.apache.commons.math3.ml.clustering.Clusterable;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

public class StopCoord implements Clusterable {

	private static final long serialVersionUID = 1L;

	//Attributes
	private Id<TransitStopFacility> id;
	private Coord coord ;

	//Constructors
	public StopCoord(double x, double y, Id<TransitStopFacility> id) {
		coord = new Coord(x,y) ;
		this.id = id;
	}

	//Methods
	public Id<TransitStopFacility> getId(){
		return id;
	}
	public double distanceFrom(StopCoord p) {
		return CoordUtils.calcEuclideanDistance(this.getCoord(), p.getCoord());
	}
	public static StopCoord centroidOf(Collection<StopCoord> ps) {
		double x=0, y=0;
		for(StopCoord p:ps) {
			x+=p.getCoord().getX();
			y+=p.getCoord().getY();
		}
		return new StopCoord(x/ps.size(), y/ps.size(), null);
	}

	@Override
	public double[] getPoint() {
		return new double[] {coord.getX(), coord.getY()};
	}

	final Coord getCoord() {
		return this.coord;
	}

}
