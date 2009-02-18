package playground.yu.visum.filter;

import org.matsim.basic.v01.IdImpl;
import org.matsim.events.BasicEvent;
import org.matsim.events.PersonEvent;
import org.matsim.interfaces.basic.v01.Id;

public class EventIDFilter extends EventFilterA {
	private static final Id criterion = new IdImpl(38);

	@Override
	public boolean judge(BasicEvent event) {
		if (event instanceof PersonEvent) {
			return (((PersonEvent) event).agentId.equals(criterion));
		}
		return false;
	}
}