/**
 * 
 */
package playground.yu.visum.filter;

import org.matsim.api.basic.v01.events.BasicEvent;
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
	public boolean judge(BasicEvent event) {
		if (event.getClass().equals(AgentDepartureEventImpl.class)) {
			return (event.getTime()<criterionMAX)&&(event.getTime()>criterionMIN);
		}
		return isResult();
	}

}
