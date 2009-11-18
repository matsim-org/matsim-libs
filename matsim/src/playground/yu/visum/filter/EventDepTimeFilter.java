/**
 * 
 */
package playground.yu.visum.filter;

import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.events.AgentDepartureEventImpl;
import org.matsim.core.utils.misc.Time;

/**
 * @author ychen
 * 
 */
public class EventDepTimeFilter extends EventFilterA {
	private static double criterionMAX = Time.parseTime("08:00");

	private static double criterionMIN = Time.parseTime("06:00");

	@Override
	public boolean judge(Event event) {
		if (event.getClass().equals(AgentDepartureEventImpl.class)) {
			return (event.getTime()<criterionMAX)&&(event.getTime()>criterionMIN);
		}
		return isResult();
	}

}
