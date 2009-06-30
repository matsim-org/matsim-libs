package org.matsim.core.api.experimental.population;

import org.matsim.api.basic.v01.population.BasicActivity;
import org.matsim.core.api.population.PlanElement;

public interface Activity extends BasicActivity, PlanElement
//, BasicLocation 
{

//	public void setCoord(final Coord coord);
	// FIXME kn I think this should go into the BasicActivity and be solved along the lines
	// suggested in an email.  kai, jun09


	
	
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