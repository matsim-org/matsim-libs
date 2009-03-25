/**
 * 
 */
package playground.yu.visum.filter;

import org.matsim.core.events.AgentDepartureEvent;
import org.matsim.core.events.BasicEventImpl;
import org.matsim.core.utils.misc.Time;

/**
 * @author ychen
 * 
 */
public class EventDepTimeFilter extends EventFilterA {
	private static double criterionMAX = Time.parseTime("08:00");

	private static double criterionMIN = Time.parseTime("06:00");

	@Override
	public boolean judge(BasicEventImpl event) {
		if (event.getClass().equals(AgentDepartureEvent.class)) {
			return (event.getTime()<criterionMAX)&&(event.getTime()>criterionMIN);
		}
		return isResult();
	}

}
