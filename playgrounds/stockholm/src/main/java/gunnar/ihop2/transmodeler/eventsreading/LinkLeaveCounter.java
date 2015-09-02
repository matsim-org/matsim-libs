package gunnar.ihop2.transmodeler.eventsreading;

import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;

class LinkLeaveCounter implements LinkLeaveEventHandler {

	final Map<Id<Link>, Long> id2cnt = new LinkedHashMap<Id<Link>, Long>();

	long totalLeaves = 0;

	LinkLeaveCounter() {
	}

	@Override
	public void reset(int iteration) {
	}

	@Override
	public void handleEvent(final LinkLeaveEvent event) {
		if (this.id2cnt.containsKey(event.getLinkId())) {
			this.id2cnt.put(event.getLinkId(),
					this.id2cnt.get(event.getLinkId()) + 1);
		} else {
			this.id2cnt.put(event.getLinkId(), 1l);
		}
		this.totalLeaves++;
	}

}
