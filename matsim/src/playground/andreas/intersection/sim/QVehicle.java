package playground.andreas.intersection.sim;

import org.matsim.mobsim.queuesim.QueueLink;
import org.matsim.mobsim.queuesim.Vehicle;
import org.matsim.network.Link;

@SuppressWarnings("serial")
public class QVehicle extends Vehicle {

	@Override
	protected void reachActivity(final double now, QueueLink currentQueueLink) {
		super.reachActivity(now, currentQueueLink);
	}

	@Override
	protected Link chooseNextLink() {
		return super.chooseNextLink();
	}

	@Override
	protected void leaveActivity(final double now) {
		super.leaveActivity(now);
	}

}
