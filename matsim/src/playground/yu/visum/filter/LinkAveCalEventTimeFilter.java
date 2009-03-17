package playground.yu.visum.filter;

import org.matsim.events.BasicEventImpl;
import org.matsim.events.LinkEnterEvent;
import org.matsim.events.LinkLeaveEvent;
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
	public boolean judge(BasicEventImpl event) {
		if ((event.getClass() == (LinkEnterEvent.class))
				|| (event.getClass() == (LinkLeaveEvent.class))) {
			return judgeTime(event.getTime());
		}
		return isResult();
	}
}
