/*
 * Copyright 2018 Gunnar Flötteröd
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * contact: gunnar.flotterod@gmail.com
 *
 */
package org.matsim.contrib.pseudosimulation.searchacceleration.logging;

import java.util.List;

import org.matsim.contrib.pseudosimulation.searchacceleration.ReplannerIdentifier;
import org.matsim.contrib.pseudosimulation.searchacceleration.SearchAccelerator;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class LogDataWrapper {

	private final SearchAccelerator accelerator;

	private final ReplannerIdentifier identifier;

	private final Integer driversInPseudoSim;

	public LogDataWrapper(final SearchAccelerator accelerator, final ReplannerIdentifier identifier,
			final Integer driversInPseudoSim) {
		this.accelerator = accelerator;
		this.identifier = identifier;
		this.driversInPseudoSim = driversInPseudoSim;
	}

//	public double getDeltaForUniformReplanning() {
//		return accelerator.getDeltaForUniformReplanning();
//	}

//	public double getDeltaForUniformReplanningExact() {
//		return accelerator.getDeltaForUniformReplanningExact();
//	}

//	public double getAverageDeltaForUniformReplanning() {
//		return accelerator.getAverageDeltaForUniformReplanning();
//	}

	public Integer getDriversInPhysicalSim() {
		return this.accelerator.getDriversInPhysicalSim();
	}

	public Integer getDriversInPseudoSim() {
		return this.driversInPseudoSim;
	}

	public Double getEffectiveReplanninRate() {
		return this.accelerator.getEffectiveReplanningRate();
	}

	public Double getFinalObjectiveFunctionValue() {
		return this.identifier.getFinalObjectiveFunctionValue();
	}

	public Double getMeanReplanningRate() {
		return this.identifier.getMeanReplanningRate();
	}

	public Double getRegularizationWeight() {
		return this.accelerator.getRegularizationWeight();
	}

//	public Double getReplanningEfficiency() {
//		return this.accelerator.getReplanningEfficiency();
//	}

//	public Double getAverageReplanningEfficiency() {
//		return this.accelerator.getAverageReplanningEfficiency();
//	}

	public Double getShareNeverReplanned() {
		return this.accelerator.getShareNeverReplanned();
	}

	public Double getShareOfScoreImprovingReplanners() {
		return this.identifier.getShareOfScoreImprovingReplanners();
	}

	public Double getTTSum_h() {
		return this.accelerator.getPhysicalTravelTimeSum_h();
	}

	public Double getUniformReplanningObjectiveFunctionValue() {
		return this.identifier.getUniformReplanningObjectiveFunctionValue();
	}

	public Double getSumOfWeightedCountDifferences2() {
		return this.identifier.getSumOfWeightedCountDifferences2();
	}

	public Double getUniformGreedyScoreChange() {
		return this.identifier.getUniformGreedyScoreChange();
	}

	public Double getRealizedGreedyScoreChange() {
		return this.identifier.getRealizedGreedyScoreChange();
	}

	public Double getUniformReplannerShare() {
		return this.identifier.getUniformReplannerShare();
	}

	public List<Double> getReplaningSignalAKF() {
		return this.identifier.getReplanningSignalAKF();
	}

	public Double getPercentile() {
		return this.accelerator.getPercentile();
	}

	public Double getLastExpectedUtilityChangeSumAccelerated() {
		return this.accelerator.getLastExpectedUtilityChangeSumAccelerated();
	}

	public Double getLastExpectedUtilityChangeSumUniform() {
		return this.accelerator.getLastExpectedUtilityChangeSumUniform();
	}

	public Double getLastRealizedUtilityChangeSum() {
		return this.accelerator.getLastRealizedUtilityChangeSum();
	}

	public Double getTargetPercentile() {
		return this.accelerator.getTargetPercentile();
	}
	
	public Double getAverageUtility() {
		return this.accelerator.getLastAverageUtility();
	}

}
