/**
 * se.vti.atap
 * 
 * Copyright (C) 2025 by Gunnar Flötteröd (VTI, LiU).
 * 
 * VTI = Swedish National Road and Transport Institute
 * LiU = Linköping University, Sweden
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation, either 
 * version 3 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>. See also COPYING and WARRANTY file.
 */
package se.vti.atap.matsim;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultStrategy;
import org.matsim.core.utils.misc.StringUtils;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class ATAPConfigGroup extends ReflectiveConfigGroup {

	public static final String GROUP_NAME = "atap";

	public ATAPConfigGroup() {
		super(GROUP_NAME);
	}

	// -------------------- SETTERS / GETTERS --------------------
	
	private Set<String> cheapStrategies = new LinkedHashSet<String>(
			Arrays.asList(DefaultStrategy.TimeAllocationMutator));

	@StringGetter("cheapStrategies")
	public String getCheapStrategies() {
		return listToString(new ArrayList<>(this.cheapStrategies));
	}

	@StringSetter("cheapStrategies")
	public void setCheapStrategies(final String cheapStrategies) {
		this.cheapStrategies = new LinkedHashSet<>(stringToList(cheapStrategies));
	}

	public void addCheapStrategy(final String cheapStrategy) {
		this.cheapStrategies.add(cheapStrategy);
	}

	public Set<String> getCheapStrategySet() {
		return this.cheapStrategies;
	}

	private boolean checkDistance = false;

	@StringGetter("checkDistance")
	public boolean getCheckDistance() {
		return this.checkDistance;
	}

	@StringSetter("checkDistance")
	public void setCheckDistance(boolean checkDistance) {
		this.checkDistance = checkDistance;
	}
	
	private int checkEmulatedAgentsCnt = 0;

	@StringGetter("checkEmulatedAgentsCnt")
	public int getCheckEmulatedAgentsCnt() {
		return this.checkEmulatedAgentsCnt;
	}

	@StringSetter("checkEmulatedAgentsCnt")
	public void setCheckEmulatedAgentsCnt(final int checkEmulatedAgentsCnt) {
		this.checkEmulatedAgentsCnt = checkEmulatedAgentsCnt;
	}
	
	private Set<String> expensiveStrategies = new LinkedHashSet<String>(Arrays.asList(DefaultStrategy.ReRoute,
			DefaultStrategy.TimeAllocationMutator_ReRoute, DefaultStrategy.ChangeSingleTripMode,
			DefaultStrategy.SubtourModeChoice, DefaultStrategy.ChangeTripMode));

	@StringGetter("expensiveStrategies")
	public String getExpensiveStrategies() {
		return listToString(new ArrayList<>(this.expensiveStrategies));
	}

	@StringSetter("expensiveStrategies")
	public void setExpensiveStrategies(final String expensiveStrategies) {
		this.expensiveStrategies = new LinkedHashSet<>(stringToList(expensiveStrategies));
	}

	public Set<String> getExpensiveStrategySet() {
		return this.expensiveStrategies;
	}

	public void addExpensiveStrategy(final String expensiveStrategy) {
		this.expensiveStrategies.add(expensiveStrategy);
	}

	private double initialStepSizeFactor = 1.0;

	@StringGetter("initialStepSizeFactor")
	public double getInitialStepSizeFactor() {
		return this.initialStepSizeFactor;
	}

	@StringSetter("initialStepSizeFactor")
	public void setInitialStepSizeFactor(double initialStepSizeFactor) {
		this.initialStepSizeFactor = initialStepSizeFactor;
	}

	private double kernelHalftime_s = 300.0;

	@StringGetter("kernelHalftime_s")
	public double getKernelHalftime_s() {
		return this.kernelHalftime_s;
	}

	@StringSetter("kernelHalftime_s")
	public void setKernelHalftime_s(double kernelHalftime_s) {
		this.kernelHalftime_s = kernelHalftime_s;
	}

	private double kernelThreshold = 0.01;

	@StringGetter("kernelThreshold")
	public double getKernelThreshold() {
		return this.kernelThreshold;
	}

	@StringSetter("kernelThreshold")
	public void setKernelThreshold(double kernelThreshold) {
		this.kernelThreshold = kernelThreshold;
	}

	private double linkShareInDistance = 1.0;

	@StringGetter("linkShareInDistance")
	public double getLinkShareInDistance() {
		return this.linkShareInDistance;
	}

	@StringSetter("linkShareInDistance")
	public void setLinkShareInDistance(double linkShareInDistance) {
		this.linkShareInDistance = linkShareInDistance;
	}

	private int maxMemory = 1;

	@StringGetter("maxMemory")
	public int getMaxMemory() {
		return this.maxMemory;
	}

	@StringSetter("maxMemory")
	public void setMaxMemory(final int maxMemory) {
		this.maxMemory = maxMemory;
	}

	private boolean normalizeDistance = false;

	@StringGetter("normalizeDistance")
	public boolean getNormalizeDistance() {
		return this.normalizeDistance;
	}

	@StringSetter("normalizeDistance")
	public void setNormalizeDistance(boolean normalizeDistance) {
		this.normalizeDistance = normalizeDistance;
	}

	private boolean reduceLogging = false;
	
	@StringGetter("reduceLogging")
	public boolean getReduceLogging() {
		return this.reduceLogging;
	}

	@StringSetter("reduceLogging")
	public void setReduceLogging(final boolean reduceLogging) {
		this.reduceLogging = reduceLogging;
	}
	
	private double replanningRateIterationExponent = -0.5;

	@StringGetter("replanningRateIterationExponent")
	public double getReplanningRateIterationExponent() {
		return this.replanningRateIterationExponent;
	}
	
	public static enum ReplannerIdentifierType {
		UNIFORM, SORTING, ATAP_APPROXIMATE_DISTANCE, ATAP_EXACT_DISTANCE, DO_NOTHING
	}

	private ReplannerIdentifierType replannerIdentifier = ReplannerIdentifierType.ATAP_APPROXIMATE_DISTANCE;

	@StringGetter("replannerIdentifier")
	public ReplannerIdentifierType getReplannerIdentifier() {
		return this.replannerIdentifier;
	}

	@StringSetter("replannerIdentifier")
	public void setReplannerIdentifier(final ReplannerIdentifierType replannerIdentifier) {
		this.replannerIdentifier = replannerIdentifier;
	}

	@StringSetter("replanningRateIterationExponent")
	public void setReplanningRateIterationExponent(double replanningRateIterationExponent) {
		this.replanningRateIterationExponent = replanningRateIterationExponent;
	}

	private boolean shuffleBeforeReplannerSelection = true;

	@StringGetter("shuffleBeforeReplannerSelection")
	public boolean getShuffleBeforeReplannerSelection() {
		return this.shuffleBeforeReplannerSelection;
	}

	@StringSetter("shuffleBeforeReplannerSelection")
	public void setShuffleBeforeReplannerSelection(boolean shuffleBeforeReplannerSelection) {
		this.shuffleBeforeReplannerSelection = shuffleBeforeReplannerSelection;
	}

	private boolean useLinearDistance = true;

	@StringGetter("useLinearDistance")
	public boolean getUseLinearDistance() {
		return this.useLinearDistance;
	}

	@StringSetter("useLinearDistance")
	public void setUseLinearDistance(boolean useLinearDistance) {
		this.useLinearDistance = useLinearDistance;
	}

	private boolean useQuadraticDistance = true;

	@StringGetter("useQuadraticDistance")
	public boolean getUseQuadraticDistance() {
		return this.useQuadraticDistance;
	}

	@StringSetter("useQuadraticDistance")
	public void setUseQuadraticDistance(boolean useQuadraticDistance) {
		this.useQuadraticDistance = useQuadraticDistance;
	}

	private boolean useExponentialDistance = false;

	@StringGetter("useExponentialDistance")
	public boolean getUseExponentialDistance() {
		return this.useExponentialDistance;
	}

	@StringSetter("useExponentialDistance")
	public void setUseExponentialDistance(boolean useExponentialDistance) {
		this.useExponentialDistance = useExponentialDistance;
	}

	private boolean useLogarithmicDistance = false;

	@StringGetter("useLogarithmicDistance")
	public boolean getUseLogarithmicDistance() {
		return this.useLogarithmicDistance;
	}

	@StringSetter("useLogarithmicDistance")
	public void setUseLogarithmicDistance(boolean useLogarithmicDistance) {
		this.useLogarithmicDistance = useLogarithmicDistance;
	}

	private boolean useFilteredTravelTimesForEmulation = false;

	@StringGetter("useFilteredTravelTimesForEmulation")
	public boolean getUseFilteredTravelTimesForEmulation() {
		return this.useFilteredTravelTimesForEmulation;
	}

	@StringSetter("useFilteredTravelTimesForEmulation")
	public void setUseFilteredTravelTimesForEmulation(boolean useFilteredTravelTimesForEmulation) {
		this.useFilteredTravelTimesForEmulation = useFilteredTravelTimesForEmulation;
	}

	private boolean useFilteredTravelTimesForReplanning = false;

	@StringGetter("useFilteredTravelTimesForReplanning")
	public boolean getUseFilteredTravelTimesForReplanning() {
		return this.useFilteredTravelTimesForReplanning;
	}

	@StringSetter("useFilteredTravelTimesForReplanning")
	public void setUseFilteredTravelTimesForReplanning(boolean useFilteredTravelTimesForReplanning) {
		this.useFilteredTravelTimesForReplanning = useFilteredTravelTimesForReplanning;
	}

	// -------------------- CONVENIENCE FUNCTIONS --------------------

	public interface DistanceTransformation {
		public double transform(double _D, Double _Dmax);
	}

	public DistanceTransformation newDistanceTransformation() {
		return new DistanceTransformation() {
			@Override
			public double transform(double _D, Double _Dmax) {
				if (getNormalizeDistance()) {
					_D = _D / _Dmax;
				}
				return (getUseLinearDistance() ? _D : 0.0) + (getUseQuadraticDistance() ? _D * _D : 0.0)
						+ (getUseExponentialDistance() ? (Math.exp(_D) - 1.0) : 0.0)
						+ (getUseLogarithmicDistance() ? Math.log(1.0 + _D) : 0.0);
			}
		};
	}

	public Function<Integer, Double> newIterationToTargetReplanningRate() {
		return new Function<>() {
			@Override
			public Double apply(Integer iteration) {
				return initialStepSizeFactor * Math.pow(1.0 + iteration, replanningRateIterationExponent);
			}
		};
	}

	// -------------------- INTERNALS --------------------

	private static String listToString(final List<String> list) {
		final StringBuilder builder = new StringBuilder();
		if (list.size() > 0) {
			builder.append(list.get(0));
		}
		for (int i = 1; i < list.size(); i++) {
			builder.append(',');
			builder.append(list.get(i));
		}
		return builder.toString();
	}

	private static List<String> stringToList(final String string) {
		final ArrayList<String> result = new ArrayList<>();
		for (String part : StringUtils.explode(string, ',')) {
			result.add(part.trim().intern());
		}
		result.trimToSize();
		return result;
	}
}
