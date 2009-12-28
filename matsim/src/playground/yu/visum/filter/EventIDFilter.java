package playground.yu.visum.filter;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.PersonEventImpl;

public class EventIDFilter extends EventFilterA {
	private static final Id criterion = new IdImpl(38);

	@Override
	public boolean judge(Event event) {
		if (event instanceof PersonEventImpl) {
			return (((PersonEventImpl) event).getPersonId().equals(criterion));
		}
		return false;
	}
}