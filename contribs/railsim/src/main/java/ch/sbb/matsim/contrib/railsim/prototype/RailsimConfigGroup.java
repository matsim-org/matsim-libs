package ch.sbb.matsim.contrib.railsim.prototype;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.config.ReflectiveConfigGroup;

import java.util.Arrays;
import java.util.Map;


/**
 * Provides the parameters for railsim.
 *
 * @author Ihab Kaddoura
 */
public final class RailsimConfigGroup extends ReflectiveConfigGroup {
	public static final String GROUP_NAME = "railsim";
	private static final String REACTION_TIME = "reactionTime";
	private static final String GRAVITY = "gravity";
	private static final String DECELERATION = "deceleration";
	private static final String ACCELERATION = "acceleration";
	private static final String GRADE = "grade";
	private static final String ADJUST_NETWORK_TO_SCHEDULE = "adjustNetworkToSchedule";
	private static final String ADJUST_NETWORK_TO_SCHEDULE_VELOCITY_ABORT = "abortIfVehicleMaxVelocityIsViolated";
	private static final String SPLIT_LINKS = "splitLinks";
	private static final String SPLIT_LINKS_LENGTH = "splitLinksLength";
	private static final String LINK_FREESPEED_APPROACH = "linkFreespeedApproach";
	private static final String TRAIN_ACCELERATION_APPROACH = "trainAccelerationApproach";

	public enum TrainSpeedApproach {constantValue, fromLinkAttributesForEachVehicleType, fromLinkAttributesForEachLine, fromLinkAttributesForEachLineAndRoute}

	public enum TrainAccelerationApproach {without, euclideanDistanceBetweenStops, speedOnPreviousLink}

	public RailsimConfigGroup() {
		super(GROUP_NAME);
	}

	private static final Logger log = LogManager.getLogger(RailsimConfigGroup.class);

	private double reactionTime = 1.5; // seconds
	private double gravity = 9.81; // meters per second squared
	private double decelerationGlobalDefault = 0.5; // meters per second squared
	private double accelerationGlobalDefault = 0.5; // meters per second squared
	private double gradeGlobalDefault = 0.0; // percent

	private boolean adjustNetworkToSchedule = false;
	private boolean abortIfVehicleMaxVelocityIsViolated = true;

	private boolean splitLinks = false;
	private double splitLinksLength = 100.;
	private TrainSpeedApproach trainSpeedApproach = TrainSpeedApproach.constantValue;
	private TrainAccelerationApproach trainAccelerationApproach = TrainAccelerationApproach.without;

	@Override
	public Map<String, String> getComments() {
		Map<String, String> comments = super.getComments();
		comments.put(REACTION_TIME, "Reaction time in seconds which is used to compute the reserved train path.");
		comments.put(GRAVITY, "Gravity in meters per second^2 which is used to compute the reserved train path.");
		comments.put(DECELERATION, "Global deceleration in meters per second^2 which is used if there is no value provided in the vehicle attributes (" + RailsimUtils.VEHICLE_ATTRIBUTE_MAX_DECELERATION + ");" + " used to compute the reserved train path and the train velocity per link.");
		comments.put(ACCELERATION, "Global acceleration in meters per second^2 which is used if there is no value provided in the vehicle attributes (" + RailsimUtils.VEHICLE_ATTRIBUTE_MAX_ACCELERATION + ");" + " used to compute the train velocity per link.");
		comments.put(GRADE, "Global grade in percentage which is used if there is no value provided in the link attributes (" + RailsimUtils.LINK_ATTRIBUTE_GRADE + ");" + " used to compute the reserved train path.");
		comments.put(ADJUST_NETWORK_TO_SCHEDULE, "Set to 'true' to adjust the network to the travel times provided in the transit schedule. " + "There is no guarantee that all delays are eliminiated.");
		comments.put(ADJUST_NETWORK_TO_SCHEDULE_VELOCITY_ABORT, "Set to 'true' to throw a runtime exception if the schedule requires a speed which exceeds the vehicle maximum velocity. " + "Set to 'false' to continue anyway and only print a warning. For 'false', there is no guarantee that all delays are eliminiated.");
		comments.put(SPLIT_LINKS, "Set to 'true' to split links into smaller link segments.");
		comments.put(SPLIT_LINKS_LENGTH, "The (approximate) maximum length of link segments. Links with a higher distance will be split into smaller links.");
		comments.put(LINK_FREESPEED_APPROACH, "The freespeed calculation approach: " + Arrays.toString(TrainSpeedApproach.values()) + ". " + TrainSpeedApproach.constantValue + " is the matsim default approach: a constant freespeed value for all vehicles. " + TrainSpeedApproach.fromLinkAttributesForEachVehicleType + " uses vehicle type-specific freespeed values provided in the link attributes. " + TrainSpeedApproach.fromLinkAttributesForEachLine + " uses transit line-specific freespeed values provided in the link attributes. " + TrainSpeedApproach.fromLinkAttributesForEachLineAndRoute + " uses transit line- and route-specific freespeed values provided in the link attributes. ");
		comments.put(TRAIN_ACCELERATION_APPROACH, "The acceleration calculation approach: " + Arrays.toString(TrainAccelerationApproach.values()) + ". " + TrainAccelerationApproach.without + " is the matsim default (The acceleration and deceleration is infinite). " + TrainAccelerationApproach.euclideanDistanceBetweenStops + " accounts for the euclidean distance between the train and the previous stop (acceleration) and the train and the next stop (decelearion). " + TrainAccelerationApproach.speedOnPreviousLink + " EXPERIMENTAL: accounts for the speed on the previous link and computes the acceleration based on the distance of the current link.");
		return comments;
	}

	// ########################################################################################################

	@Override
	protected void checkConsistency(Config config) {

		log.info("Checking consistency in train config group...");
		// TODO: check parameter consistency.
	}

	@StringGetter(REACTION_TIME)
	public double getReactionTime() {
		return reactionTime;
	}

	@StringSetter(REACTION_TIME)
	public void setReactionTime(double reactionTime) {
		this.reactionTime = reactionTime;
	}

	@StringGetter(GRAVITY)
	public double getGravity() {
		return gravity;
	}

	@StringSetter(GRAVITY)
	public void setGravity(double gravity) {
		this.gravity = gravity;
	}

	@StringGetter(DECELERATION)
	public double getDecelerationGlobalDefault() {
		return decelerationGlobalDefault;
	}

	@StringSetter(DECELERATION)
	public void setDecelerationGlobalDefault(double deceleration) {
		this.decelerationGlobalDefault = deceleration;
	}

	@StringGetter(GRADE)
	public double getGradeGlobalDefault() {
		return gradeGlobalDefault;
	}

	@StringSetter(GRADE)
	public void setGradeGlobalDefault(double grade) {
		this.gradeGlobalDefault = grade;
	}

	@StringGetter(ADJUST_NETWORK_TO_SCHEDULE)
	public boolean isAdjustNetworkToSchedule() {
		return adjustNetworkToSchedule;
	}

	@StringSetter(ADJUST_NETWORK_TO_SCHEDULE)
	public void setAdjustNetworkToSchedule(boolean adjustNetworkToSchedule) {
		this.adjustNetworkToSchedule = adjustNetworkToSchedule;
	}

	@StringGetter(SPLIT_LINKS)
	public boolean isSplitLinks() {
		return splitLinks;
	}

	@StringSetter(SPLIT_LINKS)
	public void setSplitLinks(boolean splitLinks) {
		this.splitLinks = splitLinks;
	}

	@StringGetter(SPLIT_LINKS_LENGTH)
	public double getSplitLinksLength() {
		return splitLinksLength;
	}

	@StringSetter(SPLIT_LINKS_LENGTH)
	public void setSplitLinksLength(double splitLinksLength) {
		this.splitLinksLength = splitLinksLength;
	}

	@StringGetter(ACCELERATION)
	public double getAccelerationGlobalDefault() {
		return accelerationGlobalDefault;
	}

	@StringSetter(ACCELERATION)
	public void setAccelerationGlobalDefault(double accelerationGlobalDefault) {
		this.accelerationGlobalDefault = accelerationGlobalDefault;
	}

	@StringGetter(LINK_FREESPEED_APPROACH)
	public TrainSpeedApproach getTrainSpeedApproach() {
		return trainSpeedApproach;
	}

	@StringSetter(LINK_FREESPEED_APPROACH)
	public void setTrainSpeedApproach(TrainSpeedApproach trainSpeedApproach) {
		this.trainSpeedApproach = trainSpeedApproach;
	}

	@StringGetter(ADJUST_NETWORK_TO_SCHEDULE_VELOCITY_ABORT)
	public boolean isAbortIfVehicleMaxVelocityIsViolated() {
		return abortIfVehicleMaxVelocityIsViolated;
	}

	@StringSetter(ADJUST_NETWORK_TO_SCHEDULE_VELOCITY_ABORT)
	public void setAbortIfVehicleMaxVelocityIsViolated(boolean abortIfVehicleMaxVelocityIsViolated) {
		this.abortIfVehicleMaxVelocityIsViolated = abortIfVehicleMaxVelocityIsViolated;
	}

	@StringGetter(TRAIN_ACCELERATION_APPROACH)
	public TrainAccelerationApproach getTrainAccelerationApproach() {
		return trainAccelerationApproach;
	}

	@StringSetter(TRAIN_ACCELERATION_APPROACH)
	public void setTrainAccelerationApproach(TrainAccelerationApproach trainAccelerationApproach) {
		this.trainAccelerationApproach = trainAccelerationApproach;
	}


}
