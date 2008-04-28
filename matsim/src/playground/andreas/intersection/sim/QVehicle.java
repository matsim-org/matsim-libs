package playground.andreas.intersection.sim;

import org.matsim.mobsim.QueueLink;
import org.matsim.mobsim.Vehicle;
import org.matsim.network.Link;

@SuppressWarnings("serial")
public class QVehicle extends Vehicle {

	 protected void reachActivity(final double now, QueueLink currentQueueLink) {
		 super.reachActivity(now, currentQueueLink);
	 }
	 
	 protected Link chooseNextLink() {
		 return super.chooseNextLink();
	 }

	 protected void leaveActivity(final double now) {
		 super.leaveActivity(now);
	 }
	 
}
