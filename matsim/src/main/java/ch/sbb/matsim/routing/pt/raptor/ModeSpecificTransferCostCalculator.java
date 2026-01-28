/* *********************************************************************** *
 * project: org.matsim.* 												   *
 *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2023 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package ch.sbb.matsim.routing.pt.raptor;

import java.util.function.Supplier;

import com.google.common.base.Preconditions;

/**
 * @author mrieser / Simunto
 * @author sebhoerl / IRT SystemX
 */
public class ModeSpecificTransferCostCalculator implements RaptorTransferCostCalculator {
	private final CostSupplier costSupplier;

	public ModeSpecificTransferCostCalculator(CostSupplier costSupplier) {
		this.costSupplier = costSupplier;
	}

	public ModeSpecificTransferCostCalculator() {
		this(new DefaultCostSupplier());
	}

	@Override
	public double calcTransferCost(SwissRailRaptorCore.PathElement currentPE, Supplier<Transfer> transfer,
			RaptorStaticConfig staticConfig, RaptorParameters raptorParams, int totalTravelTime, int transferCount,
			double existingTransferCosts, double currentTime) {
		Preconditions.checkState(raptorParams.getTransferPenaltyPerTravelTimeHour() == 0.0,
				"cannot use time-based transfer penalty with mode-specific transfer penalties");

		double transferCost = 0.0;

		// base transfer cost
		transferCost += raptorParams.getTransferPenaltyFixCostPerTransfer();

		// mode-specific offset
		transferCost += costSupplier.get( //
				transfer.get().getFromTransitRoute().getTransportMode(), //
				transfer.get().getToTransitRoute().getTransportMode(), //
				staticConfig);

		transferCost = Math.max(transferCost, raptorParams.getTransferPenaltyMinimum());
		transferCost = Math.min(transferCost, raptorParams.getTransferPenaltyMaximum());

		return transferCost;
	}

	public interface CostSupplier {
		double get(String fromMode, String toMode, RaptorStaticConfig config);
	}

	private static class DefaultCostSupplier implements CostSupplier {
		@Override
		public double get(String fromMode, String toMode, RaptorStaticConfig config) {
			return config.getModeToModeTransferPenalty(fromMode, toMode);
		}
	}
}
