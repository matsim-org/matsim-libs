package ch.sbb.matsim.contrib.railsim.prototype.supply;

import ch.sbb.matsim.contrib.railsim.prototype.RailsimUtils;

import java.util.Map;

/**
 * Infrastructure repository
 * <p>
 * Implement a repository to provide for capacities, speed limits and coordinates of depots, stops and links.
 *
 * @author Merlin Unterfinger
 */
public interface InfrastructureRepository {
	StopInfo getStop(String stopId, double x, double y);

	DepotInfo getDepot(StopInfo stopInfo);

	SectionPartInfo getSectionPart(StopInfo fromStop, StopInfo toStop);

	static void addRailsimAttributes(StopInfo stopInfo, int capacity, double speedLimit, double grade) {
		InfrastructureRepository.addRailsimLinkAttributes(stopInfo.getLinkAttributes(), capacity, speedLimit, grade);
	}

	static void addRailsimAttributes(DepotInfo depotInfo, int capacity, int inOutCapacity, double speedLimit, double grade) {
		// add in link attributes
		addRailsimLinkAttributes(depotInfo.getInLinkAttributes(), inOutCapacity, speedLimit, grade);
		// add depot link attributes
		addRailsimLinkAttributes(depotInfo.getDepotLinkAttributes(), capacity, speedLimit, grade);
		// add out link attributes
		addRailsimLinkAttributes(depotInfo.getOutLinkAttributes(), inOutCapacity, speedLimit, grade);
	}

	static void addRailsimAttributes(SectionSegmentInfo sectionSegmentInfo, int capacity, double speedLimit, double grade) {
		InfrastructureRepository.addRailsimLinkAttributes(sectionSegmentInfo.getLinkAttributes(), capacity, speedLimit, grade);
	}

	private static void addRailsimLinkAttributes(Map<String, Object> attributes, int capacity, double speedLimit, double grade) {
		attributes.put(RailsimUtils.LINK_ATTRIBUTE_CAPACITY, capacity);
		attributes.put(RailsimUtils.LINK_ATTRIBUTE_MAX_SPEED, speedLimit);
		attributes.put(RailsimUtils.LINK_ATTRIBUTE_GRADE, grade);
	}

	static void addRailsimLinkAttributes(Map<String, Object> attributes, String vehicleType, double speedLimit) {
		attributes.put(String.format("%s_%s", RailsimUtils.LINK_ATTRIBUTE_MAX_SPEED, vehicleType), speedLimit);
	}
}
