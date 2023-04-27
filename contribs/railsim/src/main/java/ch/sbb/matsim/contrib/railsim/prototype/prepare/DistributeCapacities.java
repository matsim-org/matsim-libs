package ch.sbb.matsim.contrib.railsim.prototype.prepare;

import ch.sbb.matsim.contrib.railsim.prototype.RailsimUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Ihab Kaddoura
 */
public class DistributeCapacities {
	private static final Logger log = LogManager.getLogger(DistributeCapacities.class);

	private final Scenario scenario;
	private final Set<Id<Link>> stopLinkIds = new HashSet<>();

	/**
	 * @param scenario
	 */
	public DistributeCapacities(Scenario scenario) {
		this.scenario = scenario;
	}

	public void run() {

		log.info("Adjust network speed levels to the transit schedule...");

		// first store the information which link is a stop link
		for (TransitStopFacility facility : scenario.getTransitSchedule().getFacilities().values()) {
			stopLinkIds.add(facility.getLinkId());
		}

		for (Link link : scenario.getNetwork().getLinks().values()) {
			if (stopLinkIds.contains(link.getId())) {
				// bhf

				// inLinks
				int linkCapacity = RailsimUtils.getTrainCapacity(link);

				reduceCapacityOnAllInLinks(link, linkCapacity);
				reduceCapacityOnAllOutLinks(link, linkCapacity);

			}
		}

		new NetworkWriter(scenario.getNetwork()).write(scenario.getConfig().controler().getOutputDirectory() + "../modified_inputTrainNetwork_capacities.xml.gz");
	}

	/**
	 * @param link
	 * @param capacity
	 */
	private void reduceCapacityOnAllOutLinks(Link link, int capacity) {
		if (capacity == 1) {
			// can't further decrease the capacity
		} else {
			for (Link outLink : link.getToNode().getOutLinks().values()) {
				int outLinkCapacity = RailsimUtils.getTrainCapacity(outLink);

				if (outLinkCapacity > capacity || stopLinkIds.contains(outLink.getId())) {
					// stop
				} else {
					// in all other cases: reduce capacity by one.
					int updatedCapacity = capacity - 1;
					outLink.getAttributes().putAttribute(RailsimUtils.LINK_ATTRIBUTE_CAPACITY, updatedCapacity);
					reduceCapacityOnAllOutLinks(outLink, updatedCapacity);
				}
			}
		}

	}

	private void reduceCapacityOnAllInLinks(Link link, int capacity) {
		if (capacity == 1) {
			// can't further decrease the capacity
		} else {
			for (Link inLink : link.getFromNode().getInLinks().values()) {
				int inLinkCapacity = RailsimUtils.getTrainCapacity(inLink);

				if (inLinkCapacity > capacity || stopLinkIds.contains(inLink.getId())) {
					// stop
				} else {
					// in all other cases: reduce capacity by one.
					int updatedCapacity = capacity - 1;
					inLink.getAttributes().putAttribute(RailsimUtils.LINK_ATTRIBUTE_CAPACITY, updatedCapacity);
					reduceCapacityOnAllInLinks(inLink, updatedCapacity);
				}
			}
		}
	}

}
