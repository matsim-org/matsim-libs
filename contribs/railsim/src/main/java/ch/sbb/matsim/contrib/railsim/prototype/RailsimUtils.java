package ch.sbb.matsim.contrib.railsim.prototype;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

/**
 * @author Ihab Kaddoura
 */
public final class RailsimUtils {

	// link
	public static final String LINK_ATTRIBUTE_GRADE = "grade";
	public static final String LINK_ATTRIBUTE_OPPOSITE_DIRECTION = "trainOppositeDirectionLink";
	public static final String LINK_ATTRIBUTE_CAPACITY = "trainCapacity";
	public static final String LINK_ATTRIBUTE_MAX_SPEED = "maxSpeed";
	public static final String LINK_ATTRIBUTE_MINIMUM_TIME = "minimumTime";
	// vehicle
	public static final String VEHICLE_ATTRIBUTE_MAX_DECELERATION = "maxDeceleration";
	public static final String VEHICLE_ATTRIBUTE_MAX_ACCELERATION = "maxAcceleration";

	private RailsimUtils() {
	}

	/**
	 * @param link
	 * @return the train capacity for this link, if no link attribute is provided the default is 1.
	 */
	public static int getTrainCapacity(Link link) {
		int trainCapacity = 1;
		if (link.getAttributes().getAttribute(LINK_ATTRIBUTE_CAPACITY) != null) {
			trainCapacity = (Integer) link.getAttributes().getAttribute(LINK_ATTRIBUTE_CAPACITY);
		}
		return trainCapacity;
	}

	/**
	 * @param link
	 * @return the minimum time for the switch at the end of the link (toNode); if no link attribute is provided the default is 0.
	 */
	public static double getMinimumTrainHeadwayTime(Link link) {
		double minimumTime = 0.;
		if (link.getAttributes().getAttribute(LINK_ATTRIBUTE_MINIMUM_TIME) != null) {
			minimumTime = (Double) link.getAttributes().getAttribute(LINK_ATTRIBUTE_MINIMUM_TIME);
		}
		return minimumTime;
	}

	/**
	 * @return the default deceleration time or the vehicle-specific value
	 */
	public static double getTrainDeceleration(Vehicle vehicle, RailsimConfigGroup railsimConfigGroup) {
		double deceleration = railsimConfigGroup.getDecelerationGlobalDefault();
		if (vehicle.getAttributes().getAttribute(VEHICLE_ATTRIBUTE_MAX_DECELERATION) != null) {
			deceleration = (Double) vehicle.getAttributes().getAttribute(VEHICLE_ATTRIBUTE_MAX_DECELERATION);
		}
		return deceleration;
	}

	/**
	 * @return the default acceleration time or the vehicle-specific value
	 */
	public static double getTrainAcceleration(Vehicle vehicle, RailsimConfigGroup railsimConfigGroup) {
		double acceleration = railsimConfigGroup.getAccelerationGlobalDefault();
		if (vehicle.getAttributes().getAttribute(VEHICLE_ATTRIBUTE_MAX_ACCELERATION) != null) {
			acceleration = (Double) vehicle.getAttributes().getAttribute(VEHICLE_ATTRIBUTE_MAX_ACCELERATION);
		}
		return acceleration;
	}

	/**
	 * @return the default acceleration time or the vehicle-specific value
	 */
	public static double getGrade(Link link, RailsimConfigGroup railsimConfigGroup) {
		double grade = railsimConfigGroup.getGradeGlobalDefault();
		if (link.getAttributes().getAttribute(LINK_ATTRIBUTE_GRADE) != null) {
			grade = (Double) link.getAttributes().getAttribute(LINK_ATTRIBUTE_GRADE);
		}
		return grade;
	}

	/**
	 * @param type
	 * @param link
	 * @return the vehicle-specific freespeed or 0 if there is no vehicle-specific freespeed provided in the link attributes
	 */
	public static double getLinkFreespeedForVehicleType(Id<VehicleType> type, Link link) {
		Object attribute = link.getAttributes().getAttribute(type.toString());
		if (attribute == null) {
			return 0.;
		} else {
			return (double) attribute;
		}
	}

	/**
	 * @param line
	 * @param link
	 * @return the line-specific freespeed or 0 if there is no line-specific freespeed provided in the link attributes
	 */
	public static double getLinkFreespeedForTransitLine(Id<TransitLine> line, Link link) {
		Object attribute = link.getAttributes().getAttribute(line.toString());
		if (attribute == null) {
			return 0.;
		} else {
			return (double) attribute;
		}
	}

	/**
	 * @param line
	 * @param route
	 * @param link
	 * @return the line- and route-specific freespeed or 0 if there is no line- and route-specific freespeed provided in the link attributes
	 */
	public static double getLinkFreespeedForTransitLineAndTransitRoute(Id<TransitLine> line, Id<TransitRoute> route, Link link) {
		Object attribute = link.getAttributes().getAttribute(line.toString() + "+++" + route.toString());
		if (attribute == null) {
			return 0.;
		} else {
			return (double) attribute;
		}
	}

	public static Id<Link> getOppositeDirectionLink(Link link, Network network) {
		if (link.getAttributes().getAttribute(LINK_ATTRIBUTE_OPPOSITE_DIRECTION) == null) {
			return null;
		} else {
			String oppositeLink = (String) link.getAttributes().getAttribute(LINK_ATTRIBUTE_OPPOSITE_DIRECTION);
			return Id.createLinkId(oppositeLink);
		}
	}

}
