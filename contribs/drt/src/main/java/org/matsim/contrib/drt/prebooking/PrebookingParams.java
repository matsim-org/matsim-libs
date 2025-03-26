package org.matsim.contrib.drt.prebooking;

import org.matsim.core.config.ReflectiveConfigGroup;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class PrebookingParams extends ReflectiveConfigGroup {
	public static final String SET_NAME = "prebooking";

	public PrebookingParams() {
		super(SET_NAME);
	}

	@Parameter
	@Comment("Defines whether vehicles drive immediately to the next"
			+ " (prebooked) future task and wait for the planned stop to begin, or wait at the current"
			+ " position and depart to arrive on time at the following stop. The latter behavior (not"
			+ " the default) may lead to larger ucnertainty in highly congested scenarios.")
	private boolean scheduleWaitBeforeDrive = false; // in the future, this could also become a double value indicating
													// how many minutes before the next stop the vehicle should plan to
													// be there

	@Parameter
	@Comment("Request gets rejected if a vehicle waits longer than the indicated duration at the stop")
	@NotNull
	@Positive
	private double maximumPassengerDelay = Double.POSITIVE_INFINITY;

	public enum UnschedulingMode {
		StopBased, Routing
	}

	@Parameter
	@Comment("When unscheduling requests because they have been canceled,"
			+ " we either simply remove the requests from the planned stops"
			+ " along the vehicle's schedule or we adaptively reconfigure and reroute the vehicle's schedule.")
	@NotNull
	private UnschedulingMode unschedulingMode = UnschedulingMode.StopBased;

	@Parameter
	@Comment("Defines whether agents are set to stuck and abort when a prebooked request is rejected." +
			"If false, prebooked agents will re-attempt to ride at their initially planned departure time." +
			"Note that additional passenger events for submission, scheduling etc. will occur for re-attempts.")
	private boolean abortRejectedPrebookings = true;


	public boolean isScheduleWaitBeforeDrive() {
		return scheduleWaitBeforeDrive;
	}

	public void setScheduleWaitBeforeDrive(boolean scheduleWaitBeforeDrive) {
		this.scheduleWaitBeforeDrive = scheduleWaitBeforeDrive;
	}

	@NotNull
	@Positive
	public double getMaximumPassengerDelay() {
		return maximumPassengerDelay;
	}

	public void setMaximumPassengerDelay(@NotNull @Positive double maximumPassengerDelay) {
		this.maximumPassengerDelay = maximumPassengerDelay;
	}


	public @NotNull UnschedulingMode getUnschedulingMode() {
		return unschedulingMode;
	}

	public void setUnschedulingMode(@NotNull UnschedulingMode unschedulingMode) {
		this.unschedulingMode = unschedulingMode;
	}

	public boolean isAbortRejectedPrebookings() {
		return abortRejectedPrebookings;
	}

	public void setAbortRejectedPrebookings(boolean abortRejectedPrebookings) {
		this.abortRejectedPrebookings = abortRejectedPrebookings;
	}

}