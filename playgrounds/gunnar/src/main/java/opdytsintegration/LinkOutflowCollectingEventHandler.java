package opdytsintegration;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.network.Link;

import floetteroed.utilities.DynamicData;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class LinkOutflowCollectingEventHandler implements LinkEnterEventHandler {

	private DynamicData<Id<Link>> data = null;

	LinkOutflowCollectingEventHandler(final DynamicData<Id<Link>> data) {
		this.data = data;
	}

	void setDynamicData(final DynamicData<Id<Link>> data) {
		this.data = data;
	}

	@Override
	public void reset(final int iteration) {
	}

	@Override
	public void handleEvent(final LinkEnterEvent event) {
		// TODO Instead of counting LinkEnterEvents, it may make more sense
		// to estimate per time bin the average number of vehicles on the link.
		final int bin = Math.min(this.data.bin((int) event.getTime()),
				this.data.getBinCnt() - 1);
		this.data.add(event.getLinkId(), bin, 1.0);
	}

}
