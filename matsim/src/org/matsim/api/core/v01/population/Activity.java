package org.matsim.api.core.v01.population;

import org.matsim.api.basic.v01.population.BasicActivity;
import org.matsim.api.basic.v01.population.PlanElement;

/**
 * <p> Regarding "duration vs. end time":
 * <li> check if run693 (w/o duration) is similar to other runs </li>
 * <li> ask (per email) if moving to a design where we use <em> either </em> duration <em> or </em> end time
 *      would be ok (if both are encountered, duration would be ignored, and also not written) </li>
 * <li> only after these things are fixed, "duration" could be added to interface </li>
 * </p>
 *
 */
public interface Activity extends BasicActivity, PlanElement
//, BasicLocation 
{

	
}