package org.matsim.api.core.v01.population;

import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.Id;

/**
 * <p> Regarding "duration vs. end time":
 * <li> check if run693 (w/o duration) is similar to other runs </li>
 * <li> ask (per email) if moving to a design where we use <em> either </em> duration <em> or </em> end time
 *      would be ok (if both are encountered, duration would be ignored, and also not written) </li>
 * <li> only after these things are fixed, "duration" could be added to interface </li>
 * </p>
 *
 */
public interface Activity extends PlanElement {
// NOTE: Activity is NOT a location, since there is no consistent way to set the coord directly.  
// If we need an interface for getCoord funktionality (e.g. for quadtree entries), it needs to have a different name.
// kai, jul09

public double getEndTime();

public void setEndTime(final double seconds);

/**
 * Activity type is, until further notice, defined via the config file.
 * 
 * @return
 */
public String getType();

public void setType(final String type);

public Coord getCoord();

public double getStartTime();
// TODO kn not clear what this means.  I guess that in the same sense as "expected travel time" in leg I should accept it
// as "expected activity start time".  --> remove for next version?

public void setStartTime(double seconds);
// TODO kn not clear what this means (see above). --> remove for next version?

public Id getLinkId();


public Id getFacilityId();

// the following should not come back since they cause headaches without end.  use builder methods instead.

//	public void setLinkId(final Id id);
//	public void setFacilityId(final Id id);

}