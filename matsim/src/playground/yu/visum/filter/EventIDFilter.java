package playground.yu.visum.filter;

import org.matsim.events.BasicEvent;

public class EventIDFilter extends EventFilterA {
	private static final int criterion = 38;

	@Override
	public boolean judge(BasicEvent event) {
		return (Integer.parseInt(event.agentId) % criterion == 0);
	}
}