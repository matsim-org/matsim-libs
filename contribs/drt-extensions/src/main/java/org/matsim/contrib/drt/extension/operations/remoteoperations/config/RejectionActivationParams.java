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
import jakarta.validation.constraints.Positive;
import org.matsim.core.config.Config;
import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * Optional configuration for the demand-driven {@code RejectionRateActivation} trigger. When this
 * parameter set is present under {@link RemoteGuidanceParams}, a {@code RejectionRateActivation} trigger is added to the
 * shared activation policy: while the recent request-rejection rate exceeds {@link #rejectionRateThreshold}, the trigger
 * targets the full activation capacity (activate everything demand pressure allows); when rejections subside
 * the responsiveness buffer / floor take over and the fleet ramps back down. Absent this set, no demand-driven trigger
 * is wired and behaviour is unchanged.
 * <p>
 * The rejection rate is measured over a trailing window of {@link #windowSize} seconds as
 * {@code rejected / (rejected + scheduled)} of the mode's requests.
 *
 * @author nkuehnel / MOIA
 */
public class RejectionActivationParams extends ReflectiveConfigGroup {

	public static final String SET_NAME = "rejectionActivation";

	@Parameter
	@Comment("Trailing window in [seconds] over which the request-rejection rate is measured. Defaults to 900.")
	@Positive
	private double windowSize = 900;

	@Parameter
	@Comment("Rejection-rate threshold in [0,1]: while the recent rejection rate (rejected / (rejected + scheduled)) "
			+ "over the trailing window exceeds this, the trigger targets the full activation capacity. Defaults to "
			+ "0.1 (10% of requests rejected).")
	private double rejectionRateThreshold = 0.1;

	public RejectionActivationParams() {
		super(SET_NAME);
	}

	public double getWindowSize() {
		return windowSize;
	}

	public void setWindowSize(double windowSize) {
		this.windowSize = windowSize;
	}

	public double getRejectionRateThreshold() {
		return rejectionRateThreshold;
	}

	public void setRejectionRateThreshold(double rejectionRateThreshold) {
		this.rejectionRateThreshold = rejectionRateThreshold;
	}

	@Override
	protected void checkConsistency(Config config) {
		super.checkConsistency(config);
		Verify.verify(windowSize > 0, "windowSize must be positive.");
		Verify.verify(rejectionRateThreshold >= 0 && rejectionRateThreshold <= 1,
				"rejectionRateThreshold must be in [0, 1].");
	}
}
