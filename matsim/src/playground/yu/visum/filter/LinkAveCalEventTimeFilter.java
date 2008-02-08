package playground.yu.visum.filter;

import org.matsim.events.BasicEvent;
import org.matsim.events.EventLinkEnter;
import org.matsim.events.EventLinkLeave;
import org.matsim.utils.misc.Time;

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
	public boolean judge(BasicEvent event) {
		if ((event.getClass() == (EventLinkEnter.class))
				|| (event.getClass() == (EventLinkLeave.class))) {
			return judgeTime(event.time);
		}
		return isResult();
	}
}
