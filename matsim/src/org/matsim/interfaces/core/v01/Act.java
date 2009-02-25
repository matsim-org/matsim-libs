package org.matsim.interfaces.core.v01;

import org.matsim.facilities.Facility;
import org.matsim.interfaces.basic.v01.BasicAct;
import org.matsim.interfaces.basic.v01.BasicLink;
import org.matsim.interfaces.basic.v01.Id;
import org.matsim.network.Link;

public interface Act extends BasicAct {

	public void setFacility(final Facility facility);

	public void setLink(final BasicLink link);

	// here to return correct link type
	public Link getLink();

	public Facility getFacility();

	public Id getLinkId();

	public Id getFacilityId();

	/**
	 * This method calculates the duration of the activity from the start and endtimes if set.
	 * If neither end nor starttime is set, but the duration is stored in the attribute of the
	 * class the duration is returned.
	 * If only start time is set, assume this is the last activity of the day.
	 * If only the end time is set, assume this is the first activity of the day.
	 * If the duration could neither be calculated nor the act.dur attribute is set to a value
	 * not equal to Time.UNDEFINED_TIME an exception is thrown.
	 * @return the duration in seconds
	 */
	public double calculateDuration();

}