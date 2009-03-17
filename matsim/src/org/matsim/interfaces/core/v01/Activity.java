package org.matsim.interfaces.core.v01;

import org.matsim.interfaces.basic.v01.network.BasicLink;
import org.matsim.interfaces.basic.v01.population.BasicActivity;

public interface Activity extends BasicActivity, PlanElement {

	public void setFacility(final Facility facility);

	public void setLink(final BasicLink link);

	// here to return correct link type
	public Link getLink();

	public Facility getFacility();
	
//	public Id getLinkId(); // already in BasicAct

//	public Id getFacilityId(); // already in BasicAct

	/**
	 * This method calculates the duration of the activity from the start and endtimes if set.
	 * If neither end nor starttime is set, but the duration is stored in the attribute of the
	 * class the duration is returned.
	 * If only start time is set, assume this is the last activity of the day.
	 * If only the end time is set, assume this is the first activity of the day.
	 * If the duration could neither be calculated nor the act.dur attribute is set to a value
	 * not equal to Time.UNDEFINED_TIME an exception is thrown.
	 * @return the duration in seconds
	 * 
	 * @deprecated duration is deprecated.  kn, mar09
	 */
	@Deprecated // duration is deprecated.  kn, mar09
	public double calculateDuration();

	/**
	 * @deprecated
	 */
	@Deprecated
	public double getDuration();
	/**
	 * @deprecated
	 */
	@Deprecated
	public void setDuration(double duration);

}