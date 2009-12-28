package playground.yu.visum.filter;

import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.events.LinkEnterEventImpl;
import org.matsim.core.events.LinkLeaveEventImpl;
import org.matsim.core.utils.misc.Time;

/**
 * @author ychen
 * 
 */
public class LinkAveCalEventTimeFilter extends EventFilterA {
	private static final double criterionMAX = Time.parseTime("08:00");

	private static final double criterionMIN = Time.parseTime("06:00");

	private boolean judgeTime(double time) {
		return (time < criterionMAX) && (time > criterionMIN);
	}

	@Override
	public boolean judge(Event event) {
		if ((event.getClass() == (LinkEnterEventImpl.class))
				|| (event.getClass() == (LinkLeaveEventImpl.class))) {
			return judgeTime(event.getTime());
		}
		return isResult();
	}
}
