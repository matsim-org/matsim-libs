/**
 * se.vti.atap.minimalframework
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
package se.vti.atap.minimalframework.defaults.replannerselection.proposed;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import se.vti.atap.minimalframework.Agent;
import se.vti.atap.minimalframework.NetworkConditions;
import se.vti.atap.minimalframework.Plan;
import se.vti.atap.minimalframework.PlanSelection;
import se.vti.atap.minimalframework.defaults.replannerselection.MSAStepSize;

/**
 * 
 * @author GunnarF
 *
 */
public class ProposedMethodWithLocalSearchPlanSelection<P extends Plan, A extends Agent<P>, T extends NetworkConditions, Q extends ApproximateNetworkConditions<P, A, Q>>
		implements PlanSelection<A, T> {

	private final int maxNumberOfFailures = 0;

	private final MSAStepSize stepSize;

	private final Random rnd;

	private final ApproximateNetworkLoading<P, A, T, Q> approximateNetworkLoading;

	private final double epsPsi;
	private final double epsT;

	private boolean approximateDistance = false;

	private boolean transformDistance = true;

	private boolean normalizeDistance = false;

	private double minimalImprovement = 1e-6;

//	private double individualMinimalGap = 0.0;

	private double initialStepSize = 1.0;
	
	public ProposedMethodWithLocalSearchPlanSelection(double stepSizeIterationExponent, Random rnd,
			ApproximateNetworkLoading<P, A, T, Q> approximateNetworkLoading, double epsPsi, double epsT) {
		this.stepSize = new MSAStepSize(stepSizeIterationExponent);
		this.rnd = rnd;
		this.approximateNetworkLoading = approximateNetworkLoading;
		this.epsPsi = epsPsi;
		this.epsT = epsT;
	}

	public ProposedMethodWithLocalSearchPlanSelection<P, A, T, Q> setApproximateDistance(boolean approximateDistance) {
		this.approximateDistance = approximateDistance;
		return this;
	}

	public ProposedMethodWithLocalSearchPlanSelection<P, A, T, Q> setTransformDistance(boolean transformDistance) {
		this.transformDistance = transformDistance;
		return this;
	}

	public ProposedMethodWithLocalSearchPlanSelection<P, A, T, Q> setNormalizeDistance(boolean normalizeDistance) {
		this.normalizeDistance = normalizeDistance;
		return this;
	}
	
	public ProposedMethodWithLocalSearchPlanSelection<P, A, T, Q> setInitialStepSize(double initialStepSize) {
		this.initialStepSize = initialStepSize;
		return this;
	}

	public ProposedMethodWithLocalSearchPlanSelection<P, A, T, Q> setMinimalImprovement(double minimalImprovement) {
		this.minimalImprovement = minimalImprovement;
		return this;
	}


//	public ProposedMethodWithLocalSearchPlanSelection<P, A, T, Q> setMinimalRelativeImprovement(
//			double minimalImprovement) {
//		this.minimalRelativeImprovement = minimalImprovement;
//		return this;
//	}

//	public ProposedMethodWithLocalSearchPlanSelection<P, A, T, Q> setIndividualMinimalGap(double individualMinimalGap) {
//		this.individualMinimalGap = individualMinimalGap;
//		return this;
//	}

//	private Double objectiveFunctionNumeratorScale = null;
//	private Double objectiveFunctionDenominatorScale = null;
//	private Double objectiveFunctionScale = null;

	private Double maxPsi = null;
	private Double maxD = null;
	private Double maxT = null;

//	private Double objFctScale = null;

	private double computeObjectiveFunctionValue(double expectedImprovement, Q currentApproximateNetworkConditions,
			Q candidateApproximatNetworkConditions, double absoluteAmbitionLevel) {
		
		double distance;
		if (this.approximateDistance) {
			distance = currentApproximateNetworkConditions.computeDistance(candidateApproximatNetworkConditions);
		} else {
			distance = currentApproximateNetworkConditions
					.computeLeaveOneOutDistance(candidateApproximatNetworkConditions);
		}
		if (this.normalizeDistance) {
			distance /= this.maxD;
		}
		double _T = (this.transformDistance ? (distance + distance * distance) : (distance));

		double numerator = (expectedImprovement - absoluteAmbitionLevel) / this.maxPsi;	
		double denominator = _T / (this.maxT + this.epsT) + this.epsT;

		return (numerator / denominator);
	}

	@Override
	public void assignSelectedPlans(Set<A> agents, T networkConditions, int iteration) {

		this.maxPsi = agents.stream().mapToDouble(a -> a.computeGap()).sum();
		if (this.maxPsi < this.epsPsi) {
			agents.stream().forEach(a -> a.setCandidatePlan(null));
			return;
		}
		double absoluteAmbitionLevel = this.initialStepSize * this.stepSize.compute(iteration) * this.maxPsi;
		// * agents.stream().mapToDouble(a -> a.computeGap()).sum();

		Q approximateNetworkConditionsWithoutAnySwitch = this.approximateNetworkLoading.compute(agents,
				Collections.emptySet(), networkConditions);
		Q approximateNetworkConditionsWithAllSwitch = this.approximateNetworkLoading.compute(Collections.emptySet(),
				agents, networkConditions);
		this.maxD = approximateNetworkConditionsWithoutAnySwitch
				.computeDistance(approximateNetworkConditionsWithAllSwitch);
		
		if (this.normalizeDistance) {
			this.maxT = (this.transformDistance ? (1.0 + 1.0 * 1.0) : 1.0);
		} else {
			this.maxT = (this.transformDistance ? (this.maxD + this.maxD * this.maxD) : this.maxD);
		}


		Set<A> agentsUsingCurrentPlan = new LinkedHashSet<>(agents);
		Set<A> agentsUsingCandidatePlan = new LinkedHashSet<>();

		Q approximateNetworkConditions = this.approximateNetworkLoading.compute(agentsUsingCurrentPlan,
				agentsUsingCandidatePlan, networkConditions);
		double expectedImprovement = agentsUsingCandidatePlan.stream().mapToDouble(a -> a.computeGap()).sum();
		double objectiveFunctionValue = this.computeObjectiveFunctionValue(expectedImprovement,
				approximateNetworkConditionsWithoutAnySwitch, approximateNetworkConditions, absoluteAmbitionLevel);

		long innerIteration = 0;
		
		List<A> agentsList = new ArrayList<>(agents);
		int failures = 0;
		boolean switched;
		do {
			innerIteration++;
			
//			System.out.println(objectiveFunctionValue);

			switched = false;
			Collections.shuffle(agentsList, this.rnd);
//			Collections.sort(allAgents, new Comparator<>() {
//				@Override
//				public int compare(A agent1, A agent2) { // sort in descending order
//					return Double.compare(agent2.computeGap(), agent1.computeGap());
//				}
//			});
			for (A agent : agentsList) {
				boolean agentWasUsingCandidatePlan = agentsUsingCandidatePlan.contains(agent);
				PlanSwitch<P, A> candidateSwitch;
				double expectedImprovementAfterSwitch;
				if (agentWasUsingCandidatePlan) {
					candidateSwitch = approximateNetworkConditions.switchToPlan(agent.getCurrentPlan(), agent);
					expectedImprovementAfterSwitch = expectedImprovement - agent.computeGap();
				} else {
					candidateSwitch = approximateNetworkConditions.switchToPlan(agent.getCandidatePlan(), agent);
					expectedImprovementAfterSwitch = expectedImprovement + agent.computeGap();
				}
				double objectiveFunctionValueAfterSwitch = this.computeObjectiveFunctionValue(
						expectedImprovementAfterSwitch, approximateNetworkConditionsWithoutAnySwitch,
						approximateNetworkConditions, absoluteAmbitionLevel);

//				if (objectiveFunctionValueAfterSwitch > objectiveFunctionValue
//						* (1.0 + 1e-0 * this.epsPsi / this.epsT)) {
				if (objectiveFunctionValueAfterSwitch - objectiveFunctionValue >= this.minimalImprovement * Math.max(1.0, objectiveFunctionValue)) {
					expectedImprovement = expectedImprovementAfterSwitch;
					objectiveFunctionValue = objectiveFunctionValueAfterSwitch;
					if (agentWasUsingCandidatePlan) {
						agentsUsingCandidatePlan.remove(agent);
						agentsUsingCurrentPlan.add(agent);
					} else {
						agentsUsingCurrentPlan.remove(agent);
						agentsUsingCandidatePlan.add(agent);
					}
					switched = true;
				} else {
					approximateNetworkConditions.undoSwitch(candidateSwitch);
				}
			}
			
			if (innerIteration % 100 == 0) {
				System.out.println(innerIteration + "\t" + objectiveFunctionValue);
			}
			
			if (!switched) {
				failures++;
			}
		} while (failures <= this.maxNumberOfFailures);

		agentsUsingCandidatePlan.stream().forEach(a -> a.setCurrentPlanToCandidatePlan());
		agents.stream().forEach(a -> a.setCandidatePlan(null));
	}
}
