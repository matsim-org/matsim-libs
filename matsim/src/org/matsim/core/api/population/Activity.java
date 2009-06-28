package org.matsim.core.api.population;

import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.population.BasicActivity;

public interface Activity extends BasicActivity, PlanElement {

	public void setCoord(final Coord coord);
	// TODO kn not clear where this belongs.  Technically, the coordinate belongs to 
	// the facility, not the activity.


	
	
	// dealing with references _between_ top-level containers is currently not part of the 
	// interface. kn, jun09
//	public void setFacility(final ActivityFacility facility);
//
//	public void setLink(final BasicLink link);
//
//	public Link getLink();
//
//	public ActivityFacility getFacility();
	
}