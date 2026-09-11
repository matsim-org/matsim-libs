/*
 * Copyright (C) 2026 MOIA GmbH
 *
 * You may use, distribute and modify this code under the terms
 * of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 */
package org.matsim.contrib.drt.extension.operations.remoteoperations.config;

import com.google.common.base.Verify;
import jakarta.annotation.Nullable;
import org.apache.logging.log4j.LogManager;
import org.matsim.contrib.common.util.ReflectiveConfigGroupWithConfigurableParameterSets;
import org.matsim.core.config.Config;

import java.util.Optional;

/**
 * Configuration for remote guidance operator shifts. A remote guidance operator supervises up to
 * {@link #defaultOperatorCapacity} autonomous vehicles simultaneously. Operator shifts are defined in the
 * regular shift input file (see {@link org.matsim.contrib.drt.extension.operations.shifts.config.ShiftsParams})
 * and distinguished by their shift type, which must equal {@link #operatorShiftType}.
 * <p>
 * Vehicles operate hub-based: they wait at an operation facility until a remote operator with free capacity
 * activates them (by means of a virtual driver shift), and they return to the hub once no operator capacity
 * remains for them.
 *
 * @author nkuehnel / MOIA
 */
public class RemoteGuidanceParams extends ReflectiveConfigGroupWithConfigurableParameterSets {

	public static final String SET_NAME = "remoteGuidance";

	@Parameter
	@Comment("Shift type (in the shift input file) that marks a shift as a remote guidance operator shift. "
			+ "Shifts of this type are not assigned to vehicles directly but supervise up to 'defaultOperatorCapacity' "
			+ "vehicles each. Defaults to 'remoteGuidance'.")
	private String operatorShiftType = "remoteGuidance";

	@Parameter
	@Comment("Maximum number of vehicles a single remote guidance operator can supervise simultaneously. "
			+ "Defaults to 10.")
	private int defaultOperatorCapacity = 10;

	@Parameter
	@Comment("Minimum remaining operator shift time in [seconds] for a vehicle to (still) be activated under that "
			+ "operator. Prevents activating vehicles shortly before an operator shift ends. Defaults to 1800.")
	private double minRemainingShiftTimeForActivation = 1800;

	@Parameter
	@Comment("Idle timeout in [seconds]: a supervised vehicle that has been idle in service (waiting with no committed "
			+ "future work) for longer than this is deactivated and returned to a hub (demand-slack recall). "
			+ "Defaults to 900.")
	private double idleTimeout = 900;

	@Parameter
	@Comment("Activation policy selecting how many vehicles to keep supervised. 'buffered' (default) = minimum-fleet floor "
			+ "+ a ready responsiveness buffer (+ demand-driven rejection trigger if configured), damping the low-demand "
			+ "sawtooth. 'greedy' = the baseline that activates every idle-at-hub vehicle up to capacity regardless of "
			+ "demand (for comparison runs).")
	private ActivationPolicy activationPolicy = ActivationPolicy.buffered;

	@Parameter
	@Comment("Hard floor on the number of active (supervised) vehicles: at least this many are kept active while "
			+ "activation capacity allows, regardless of demand. Sets a lower bound on the supervised fleet. "
			+ "Defaults to 0 (no floor).")
	private int minActiveFleet = 0;

	@Parameter
	@Comment("Responsiveness buffer: the number of vehicles kept idle-in-service (ready to absorb an incoming request "
			+ "without a hub activation delay) during a demand lull. The activation side pulls the active fleet up until "
			+ "this many are idle in service; the idle-timeout deactivation side spares exactly this many, so the two "
			+ "sides share one buffer target and no churn arises at the band. Defaults to 1.")
	private int readyBufferSize = 1;

	@Parameter
	@Comment("Busy-count smoothing window in [seconds] for the responsiveness buffer. The buffer targets "
			+ "'smoothedBusy + readyBufferSize' active vehicles; with a window > 0 'smoothedBusy' is the trailing-window "
			+ "MAXIMUM of the busy count (active - idle-in-service) rather than its instantaneous value. This damps the "
			+ "peak-demand activate<->recall sawtooth: the target jumps up immediately when demand rises but only decays "
			+ "after busy has stayed below its recent peak for a full window (an orderly ramp-down). Bind it to the "
			+ "idle-timeout to read as 'only shed a vehicle once demand has not needed it for a full timeout'. Defaults to "
			+ "0 (disabled -> instantaneous busy, i.e. unchanged behaviour).")
	private double busyWindowSize = 0;

	@Parameter
	@Comment("Proactive recall lead time in [seconds]: vehicles are recalled to a hub already when supervision capacity "
			+ "will drop within this look-ahead window (e.g. an operator shift ending soon), so they arrive at the hub "
			+ "in time instead of being caught on the road when capacity actually falls. Defaults to 900.")
	private double recallLeadTime = 900;

	// optional: stochastic incident handling. If absent, no incidents are generated.
	@Nullable
	private IncidentParams incidentParams;

	// optional: demand-driven activation. If absent, no rejection-rate trigger is wired.
	@Nullable
	private RejectionActivationParams rejectionActivationParams;

	public RemoteGuidanceParams() {
		super(SET_NAME);
		// incidents (optional)
		addDefinition(IncidentParams.SET_NAME, IncidentParams::new, () -> incidentParams,
				params -> incidentParams = (IncidentParams) params);
		// demand-driven activation (optional)
		addDefinition(RejectionActivationParams.SET_NAME, RejectionActivationParams::new,
				() -> rejectionActivationParams,
				params -> rejectionActivationParams = (RejectionActivationParams) params);
	}

	public Optional<IncidentParams> getIncidentParams() {
		return Optional.ofNullable(incidentParams);
	}

	public Optional<RejectionActivationParams> getRejectionActivationParams() {
		return Optional.ofNullable(rejectionActivationParams);
	}

	public String getOperatorShiftType() {
		return operatorShiftType;
	}

	public void setOperatorShiftType(String operatorShiftType) {
		this.operatorShiftType = operatorShiftType;
	}

	public int getDefaultOperatorCapacity() {
		return defaultOperatorCapacity;
	}

	public void setDefaultOperatorCapacity(int defaultOperatorCapacity) {
		this.defaultOperatorCapacity = defaultOperatorCapacity;
	}

	public double getMinRemainingShiftTimeForActivation() {
		return minRemainingShiftTimeForActivation;
	}

	public void setMinRemainingShiftTimeForActivation(double minRemainingShiftTimeForActivation) {
		this.minRemainingShiftTimeForActivation = minRemainingShiftTimeForActivation;
	}

	public double getIdleTimeout() {
		return idleTimeout;
	}

	public void setIdleTimeout(double idleTimeout) {
		this.idleTimeout = idleTimeout;
	}

	public double getRecallLeadTime() {
		return recallLeadTime;
	}

	public void setRecallLeadTime(double recallLeadTime) {
		this.recallLeadTime = recallLeadTime;
	}

	public ActivationPolicy getActivationPolicy() {
		return activationPolicy;
	}

	public void setActivationPolicy(ActivationPolicy activationPolicy) {
		this.activationPolicy = activationPolicy;
	}

	public int getMinActiveFleet() {
		return minActiveFleet;
	}

	public void setMinActiveFleet(int minActiveFleet) {
		this.minActiveFleet = minActiveFleet;
	}

	public int getReadyBufferSize() {
		return readyBufferSize;
	}

	public void setReadyBufferSize(int readyBufferSize) {
		this.readyBufferSize = readyBufferSize;
	}

	public double getBusyWindowSize() {
		return busyWindowSize;
	}

	public void setBusyWindowSize(double busyWindowSize) {
		this.busyWindowSize = busyWindowSize;
	}

	@Override
	protected void checkConsistency(Config config) {
		super.checkConsistency(config);
		Verify.verify(defaultOperatorCapacity > 0, "defaultOperatorCapacity must be a positive integer.");
		Verify.verify(minRemainingShiftTimeForActivation >= 0, "minRemainingShiftTimeForActivation must not be negative.");
		Verify.verify(minActiveFleet >= 0, "minActiveFleet must not be negative.");
		Verify.verify(readyBufferSize >= 0, "readyBufferSize must not be negative.");
		Verify.verify(busyWindowSize >= 0, "busyWindowSize must not be negative.");
		if (activationPolicy == ActivationPolicy.greedy && rejectionActivationParams != null) {
			LogManager.getLogger(RemoteGuidanceParams.class).warn("A 'rejectionActivation' config is present but "
					+ "activationPolicy is 'greedy'; the demand-driven rejection trigger is not wired under the greedy "
					+ "policy (greedy already drives the fleet to capacity). The rejection config will have no effect.");
		}
	}
}