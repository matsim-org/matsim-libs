package vrp.api;

import org.matsim.api.core.v01.Id;

import vrp.basics.Relation;
import vrp.basics.TimeWindow;


/**
 * 
 * @author stefan schroeder
 *
 */

public interface Customer {
	
	public abstract Id getId();
	
	public abstract Node getLocation();
	
	public abstract Relation getRelation();

	public abstract void setRelation(Relation relationship);
	
	public abstract void removeRelation();
	
	public abstract boolean hasRelation();

	public abstract int getDemand();

	public abstract void setDemand(int demand);
	
	public abstract void setServiceTime(double serviceTime);

	public abstract double getServiceTime();

	public abstract void setTheoreticalTimeWindow(TimeWindow timeWindow);
	
	public abstract void setTheoreticalTimeWindow(double start, double end);

	public abstract TimeWindow getTheoreticalTimeWindow();
	
}
