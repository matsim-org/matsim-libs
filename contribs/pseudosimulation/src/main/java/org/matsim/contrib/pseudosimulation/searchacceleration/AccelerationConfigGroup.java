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
package org.matsim.contrib.pseudosimulation.searchacceleration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.distribution.BinomialDistribution;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.random.Well19937c;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleCapacity;
import org.matsim.vehicles.Vehicles;

import floetteroed.utilities.TimeDiscretization;
import floetteroed.utilities.Units;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class AccelerationConfigGroup extends ReflectiveConfigGroup {

	// ==================== MATSim-SPECIFICS ====================

	// -------------------- CONSTANTS --------------------

	public static final String GROUP_NAME = "acceleration";

	// -------------------- CONSTRUCTION AND INITIALIZATION --------------------

	public AccelerationConfigGroup() {
		super(GROUP_NAME);
	}

	// TODO No way to access the network directly?
	public void configure(final Scenario scenario, final int pSimIterations) {
		this.network = scenario.getNetwork();
		this.transitVehicles = scenario.getTransitVehicles();
		this.populationSize = scenario.getPopulation().getPersons().size();
		this.pSimIterations = pSimIterations;
	}

	// -------------------- mode --------------------

	public static enum ModeType {
		off, accelerate, mah2007, mah2009
	};

	private ModeType modeTypeField = ModeType.accelerate;

	@StringGetter("mode")
	public ModeType getModeTypeField() {
		return this.modeTypeField;
	}

	@StringSetter("mode")
	public void setModeTypeField(final ModeType modeTypeField) {
		this.modeTypeField = modeTypeField;
	}

	// -------------------- startTime_s --------------------

	private int startTime_s = 0;

	@StringGetter("startTime_s")
	public int getStartTime_s() {
		return this.startTime_s;
	}

	@StringSetter("startTime_s")
	public void setStartTime_s(int startTime_s) {
		this.startTime_s = startTime_s;
	}

	// -------------------- binSize_s --------------------

	private int binSize_s = 3600;

	@StringGetter("binSize_s")
	public int getBinSize_s() {
		return this.binSize_s;
	}

	@StringSetter("binSize_s")
	public void setBinSize_s(int binSize_s) {
		this.binSize_s = binSize_s;
	}

	// -------------------- binCnt_s --------------------

	private int binCnt = 24;

	@StringGetter("binCnt")
	public int getBinCnt() {
		return this.binCnt;
	}

	@StringSetter("binCnt")
	public void setBinCnt(int binCnt) {
		this.binCnt = binCnt;
	}

	// -------------------- meanReplanningRate --------------------

	private double initialMeanReplanningRate = 0.2;

	@StringGetter("initialMeanReplanningRate")
	public double getInitialMeanReplanningRate() {
		return this.initialMeanReplanningRate;
	}

	@StringSetter("initialMeanReplanningRate")
	public void setInitialMeanReplanningRate(double initialMeanReplanningRate) {
		this.initialMeanReplanningRate = initialMeanReplanningRate;
	}

	// // -------------------- regularizationWeight --------------------
	//
	// private double initialRegularizationWeight = Double.NaN;
	//
	// @StringGetter("initialRegularizationWeight")
	// public double getInitialRegularizationWeight() {
	// return this.initialRegularizationWeight;
	// }
	//
	// @StringSetter("initialRegularizationWeight")
	// public void setInitialRegularizationWeight(double
	// initialRegularizationWeight) {
	// this.initialRegularizationWeight = initialRegularizationWeight;
	// }

	// -------------------- replanningRateIterationExponent --------------------

	private double replanningRateIterationExponent = 0.0;

	@StringGetter("replanningRateIterationExponent")
	public double getReplanningRateIterationExponent() {
		return this.replanningRateIterationExponent;
	}

	@StringSetter("replanningRateIterationExponent")
	public void setReplanningRateIterationExponent(double replanningRateIterationExponent) {
		this.replanningRateIterationExponent = replanningRateIterationExponent;
	}

	// // -------------------- replanningRateIterationExponent --------------------
	//
	// private double regularizationIterationExponent = Double.NaN;
	//
	// @StringGetter("regularizationIterationExponent")
	// public double getRegularizationIterationExponent() {
	// return this.regularizationIterationExponent;
	// }
	//
	// @StringSetter("regularizationIterationExponent")
	// public void setRegularizationIterationExponent(double
	// regularizationIterationExponent) {
	// this.regularizationIterationExponent = regularizationIterationExponent;
	// }

	// // -------------------- weighting --------------------
	//
	// public static enum RegularizationType {
	// absolute, relative
	// };
	//
	// private RegularizationType regularizationTypeField = null;
	//
	// @StringGetter("regularizationType")
	// public RegularizationType getRegularizationType() {
	// return this.regularizationTypeField;
	// }
	//
	// @StringSetter("regularizationType")
	// public void setRegularizationType(final RegularizationType
	// regularizationTypeField) {
	// this.regularizationTypeField = regularizationTypeField;
	// }

	// -------------------- weighting --------------------

	public static enum LinkWeighting {
		uniform, oneOverCapacity
	};

	private LinkWeighting weightingField = LinkWeighting.oneOverCapacity;

	@StringGetter("linkWeighting")
	public LinkWeighting getWeighting() {
		return this.weightingField;
	}

	@StringSetter("linkWeighting")
	public void setWeighting(final LinkWeighting weightingField) {
		this.weightingField = weightingField;
	}

	// // -------------------- baselineReplanningRate --------------------
	//
	// private double baselineReplanningRate = Double.NaN;
	//
	// @StringGetter("baselineReplanningRate")
	// public double getBaselineReplanningRate() {
	// return this.baselineReplanningRate;
	// }
	//
	// @StringSetter("baselineReplanningRate")
	// public void setBaselineReplanningRate(final double baselineReplanningRate) {
	// this.baselineReplanningRate = baselineReplanningRate;
	// }

	// // -------------------- randomizeIfNoImprovement --------------------
	//
	// private boolean randomizeIfNoImprovement = false;
	//
	// @StringGetter("randomizeIfNoImprovement")
	// public boolean getRandomizeIfNoImprovement() {
	// return this.randomizeIfNoImprovement;
	// }
	//
	// @StringSetter("randomizeIfNoImprovement")
	// public void setRandomizeIfNoImprovement(final boolean
	// randomizeIfNoImprovement) {
	// this.randomizeIfNoImprovement = randomizeIfNoImprovement;
	// }

	// -------------------- replanningEfficiencyThreshold --------------------
	//
	// private double replanningEfficiencyThreshold;
	//
	// @StringGetter("replanningEfficiencyThreshold")
	// public double getReplanningEfficiencyThreshold() {
	// return this.replanningEfficiencyThreshold;
	// }
	//
	// @StringSetter("replanningEfficiencyThreshold")
	// public void setReplanningEfficiencyThreshold(final double
	// replanningEfficiencyThreshold) {
	// this.replanningEfficiencyThreshold = replanningEfficiencyThreshold;
	// }

	// --------------------replanningEfficiencyThreshold--------------------

	private int averageIterations = 1; // TODO revisit

	@StringGetter("averageIterations")
	public int getAverageIterations() {
		return this.averageIterations;
	}

	@StringSetter("averageIterations")
	public void setAverageIterations(final int averageIterations) {
		this.averageIterations = averageIterations;
	}

	// -------------------- deltaRecipe --------------------
	//
	// public static enum DeltaRecipeType {
	// linearInPercentile, linearInDelta
	// };
	//
	// private DeltaRecipeType deltaRecipeField = null;
	//
	// @StringGetter("deltaRecipe")
	// public DeltaRecipeType getDeltaRecipeField() {
	// return this.deltaRecipeField;
	// }
	//
	// @StringSetter("deltaRecipe")
	// public void setDeltaRecipeFiled(final DeltaRecipeType deltaRecipeField) {
	// this.deltaRecipeField = deltaRecipeField;

	// -------------------- binomialNumberOfReplanners --------------------

	private boolean binomialNumberOfReplanners = false;

	@StringGetter("binomialNumberOfReplanners")
	public boolean getBinomialNumberOfReplanners() {
		return this.binomialNumberOfReplanners;
	}

	@StringSetter("binomialNumberOfReplanners")
	public void setBinomialNumberOfReplanners(final boolean binomialNumberOfReplanners) {
		this.binomialNumberOfReplanners = binomialNumberOfReplanners;
	}

	// ==================== SUPPLEMENTARY FUNCTIONALITY ====================

	// -------------------- STATIC UTILITIES --------------------

	public static Map<Id<?>, Double> newUniformLinkWeights(final Network network) {
		final Map<Id<?>, Double> weights = new LinkedHashMap<>();
		for (Link link : network.getLinks().values()) {
			weights.put(link.getId(), 1.0);
		}
		return weights;
	}

	public static Map<Id<?>, Double> newOneOverCapacityLinkWeights(final Network network) {
		final Map<Id<?>, Double> weights = new LinkedHashMap<>();
		for (Link link : network.getLinks().values()) {
			final double cap_veh_h = link.getFlowCapacityPerSec() * Units.VEH_H_PER_VEH_S;
			if (cap_veh_h <= 1e-6) {
				throw new RuntimeException("link " + link.getId() + " has capacity of " + cap_veh_h + " veh/h");
			}
			weights.put(link.getId(), 1.0 / cap_veh_h);
		}
		return weights;
	}

	// -------------------- MEMBERS --------------------

	private int pSimIterations;

	private Network network = null; // needs to be explicitly set

	private Vehicles transitVehicles = null; // needs to be explicitly set

	private Integer populationSize = null; // needs to be explicitly set

	private TimeDiscretization myTimeDiscretization = null; // lazy initialization

	private Map<Id<?>, Double> linkWeights = null; // lazy initialization

	// keeping track of this to avoid a re-randomization
	private List<Double> meanReplanningRates = new ArrayList<>();

	// -------------------- IMPLEMENTATION --------------------

	public TimeDiscretization getTimeDiscretization() {
		if (this.myTimeDiscretization == null) {
			this.myTimeDiscretization = new TimeDiscretization(this.getStartTime_s(), this.getBinSize_s(),
					this.getBinCnt());
		}
		return this.myTimeDiscretization;
	}

	public double getMeanReplanningRate(int iteration) {
		final int outerIteration = iteration / this.pSimIterations;
		while (outerIteration >= this.meanReplanningRates.size()) {
			double rate = this.getInitialMeanReplanningRate()
					* Math.pow(1.0 + iteration / this.pSimIterations, this.getReplanningRateIterationExponent());
			if (this.getBinomialNumberOfReplanners()) {
				final RandomGenerator rng = new Well19937c(MatsimRandom.getRandom().nextLong());
				final BinomialDistribution distr = new BinomialDistribution(rng, this.populationSize, rate);
				rate = ((double) distr.sample()) / this.populationSize;
			}
			this.meanReplanningRates.add(rate);
		}
		return this.meanReplanningRates.get(outerIteration);
	}

	// public double getAdaptiveRegularizationWeight(final double
	// currentReplanningEfficiency,
	// final double criticalDelta) {
	// final double boundedFactor = Math.max(0,
	// Math.min(10.0, this.getReplanningEfficiencyThreshold() /
	// currentReplanningEfficiency));
	// return boundedFactor * criticalDelta;
	// }

	// public double getRegularizationWeight(int iteration, Double deltaN2) {
	// double result = Math.pow(1.0 + iteration / this.pSimIterations,
	// this.getRegularizationIterationExponent())
	// * this.getInitialRegularizationWeight();
	// if (this.getRegularizationType() == RegularizationType.absolute) {
	// // Nothing to do, only here to check for unknown regularization types.
	// } else if (this.getRegularizationType() == RegularizationType.relative) {
	// result *= deltaN2;
	// } else {
	// throw new RuntimeException("Unknown regularizationType: " +
	// this.getRegularizationType());
	// }
	// return result;
	// }

	public Map<Id<?>, Double> getLinkWeightView() {
		if (this.linkWeights == null) {
			if (this.weightingField == LinkWeighting.uniform) {
				this.linkWeights = newUniformLinkWeights(this.network);
			} else if (this.weightingField == LinkWeighting.oneOverCapacity) {
				this.linkWeights = newOneOverCapacityLinkWeights(this.network);
			} else {
				throw new RuntimeException("unhandled link weighting \"" + this.weightingField + "\"");
			}
			this.linkWeights = Collections.unmodifiableMap(this.linkWeights);
		}
		return this.linkWeights;
	}

	// public double getLinkWeight(Object linkId, int bin) {
	// if (this.linkWeights == null) {
	// if (this.weightingField == LinkWeighting.uniform) {
	// this.linkWeights = newUniformLinkWeights(network);
	// } else if (this.weightingField == LinkWeighting.oneOverCapacity) {
	// this.linkWeights = newOneOverCapacityLinkWeights(network);
	// } else {
	// throw new RuntimeException("unhandled link weighting \"" +
	// this.weightingField + "\"");
	// }
	// }
	// if (!(linkId instanceof Id<?>)) {
	// throw new RuntimeException("linkId is of type " +
	// linkId.getClass().getSimpleName());
	// }
	//
	// return this.linkWeights.get(linkId);
	//
	// }

}
