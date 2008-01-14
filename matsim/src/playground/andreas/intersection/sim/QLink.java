package playground.andreas.intersection.sim;

import org.apache.log4j.Logger;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.networks.basicNet.BasicNodeI;
import org.matsim.mobsim.QueueLink;
import org.matsim.mobsim.SimulationTimer;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;

public class QLink {
	
	final private static Logger log = Logger.getLogger(QueueLink.class);
	private static int spaceCapWarningCount = 0;
	
	private Link link;
	private double freeTravelDuration;
	/** The number of vehicles able to leave the buffer in one time step (usually 1s). */
	private double simulatedFlowCapacity;
	
	private int timeCapCeil; // optimization, cache Math.ceil(timeCap)
	private double timeCapFraction; // optimization, cache timeCap - (int)timeCap
	
	protected double storageCapacity;

	public QLink(final NetworkLayer network, final String id, final BasicNodeI from,
			final BasicNodeI to, final String length, final String freespeed, final String capacity,
			final String permlanes, final String origid, final String type) {
				
		this.link = new Link(network, id, (Node) from, (Node) to, length, freespeed, capacity,
				permlanes, origid, type);

		this.freeTravelDuration = this.link.getLength() / this.link.getFreespeed();

		/* moved capacity calculation to two methods, to be able to call it from outside
		 * e.g. for reducing cap in case of an incident             */
		initFlowCapacity();
		recalcCapacity();
	}
	
	private void initFlowCapacity() {
		/* network.capperiod is in hours, we need it per sim-tick and multiplied
		 * with flowCapFactor                */
		double flowCapFactor = Gbl.getConfig().simulation().getFlowCapFactor();

		/* multiplying capacity from file by simTickCapFactor **and**
		 * flowCapFactor:                     */
		this.simulatedFlowCapacity = this.link.getFlowCapacity() * SimulationTimer.getSimTickTime() * flowCapFactor;
	}
	
	private void recalcCapacity() {
		/* network.capperiod is in hours, we need it per sim-tick and multiplied
		 * with flowCapFactor                */
		double storageCapFactor = Gbl.getConfig().simulation().getStorageCapFactor();

		// also computing the ceiling of the capacity:
		this.timeCapCeil = (int) Math.ceil(this.simulatedFlowCapacity);

		// ... and also the fractional part of timeCap
		this.timeCapFraction = this.simulatedFlowCapacity - (int) this.simulatedFlowCapacity;

		// first guess at storageCapacity:
		this.storageCapacity = (this.link.getLength() * this.link.getLanes()) / NetworkLayer.CELL_LENGTH * storageCapFactor;

		/* storage capacity needs to be at least enough to handle the
		 * cap_per_time_step:                  */
		this.storageCapacity = Math.max(this.storageCapacity, this.timeCapCeil);

		/* If speed on link is relatively slow, then we need MORE cells than the above spaceCap to handle the flowCap. Example:
		 * Assume freeSpeedTravelTime (aka freeTravelDuration) is 2 seconds. Than I need the spaceCap TWO times the flowCap to
		 * handle the flowCap.
		 */
		if (this.storageCapacity < this.freeTravelDuration * this.simulatedFlowCapacity) {
			if ( spaceCapWarningCount <=10 ) {
				log.warn("Link " + this.link.getId() + " too small: enlarge spaceCap.  This is not fatal, but modifies the traffic flow dynamics.");
				if ( spaceCapWarningCount == 10 ) {
					log.warn("Additional warnings of this type are suppressed.");
				}
				spaceCapWarningCount++ ;
			}
			this.storageCapacity = this.freeTravelDuration * this.simulatedFlowCapacity;
		}
	}
	

	public Link getLink() {
		// TODO Auto-generated method stub
		return this.link;
	}

}
