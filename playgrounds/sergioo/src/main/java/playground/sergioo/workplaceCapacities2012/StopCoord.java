package playground.sergioo.workplaceCapacities2012;

import java.util.Collection;

import org.apache.commons.math.stat.clustering.Clusterable;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

public class StopCoord extends CoordImpl implements Clusterable<StopCoord> {

	private static final long serialVersionUID = 1L;

	//Attributes
	private Id<TransitStopFacility> id;

	//Constructors
	public StopCoord(double x, double y, Id<TransitStopFacility> id) {
		super(x, y);
		this.id = id;
	}

	//Methods
	public Id<TransitStopFacility> getId(){
		return id;
	}
	@Override
	public double distanceFrom(StopCoord p) {
		return CoordUtils.calcDistance(this, p);
	}
	@Override
	public StopCoord centroidOf(Collection<StopCoord> ps) {
		double x=0, y=0;
		for(StopCoord p:ps) {
			x+=p.getX();
			y+=p.getY();
		}
		return new StopCoord(x/ps.size(), y/ps.size(), null);
	}

}
