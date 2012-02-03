/* *********************************************************************** *
 * project: org.matsim.*
 * JointPlanOptimizerActivityDurationEncodingSemanticsBuilder.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.thibautd.jointtrips.replanning.modules.jointplanoptimizer.configuration;

import java.util.ArrayList;
import java.util.List;

import org.jgap.Gene;
import org.jgap.InvalidConfigurationException;
import org.jgap.impl.BooleanGene;
import org.jgap.impl.DoubleGene;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.population.algorithms.PlanAnalyzeSubtours;

import playground.thibautd.jointtrips.config.JointReplanningConfigGroup;
import playground.thibautd.jointtrips.population.JointActingTypes;
import playground.thibautd.jointtrips.population.JointActivity;
import playground.thibautd.jointtrips.population.JointLeg;
import playground.thibautd.jointtrips.population.JointPlan;
import playground.thibautd.jointtrips.replanning.modules.jointplanoptimizer.JointPlanOptimizerJGAPChromosome;
import playground.thibautd.jointtrips.replanning.modules.jointplanoptimizer.JointPlanOptimizerJGAPModeGene;
import playground.thibautd.jointtrips.replanning.modules.jointplanoptimizer.costestimators.JointPlanOptimizerLegTravelTimeEstimatorFactory;
import playground.thibautd.jointtrips.replanning.modules.jointplanoptimizer.fitness.AbstractJointPlanOptimizerFitnessFunction;
import playground.thibautd.jointtrips.replanning.modules.jointplanoptimizer.fitness.JointPlanOptimizerOTFFitnessFunction;
import playground.thibautd.jointtrips.replanning.modules.jointplanoptimizer.geneticoperators.ConstraintsManager;
import playground.thibautd.jointtrips.replanning.modules.jointplanoptimizer.geneticoperators.UpperBoundsConstraintsManager;

/**
 * The {@link JointPlanOptimizerSemanticsBuilder} to use for duration-based encoding.
 *
 * @author thibautd
 */
public class JointPlanOptimizerActivityDurationEncodingSemanticsBuilder implements JointPlanOptimizerSemanticsBuilder {
	private final JointReplanningConfigGroup configGroup;
	private final ScoringFunctionFactory scoringFunctionFactory;
	private final JointPlanOptimizerLegTravelTimeEstimatorFactory legTravelTimeEstimatorFactory;
	private final PlansCalcRoute routingAlgorithm;
	private final Network network;

	private JointPlan lastExaminedPlan = null;
	private PlanStructureInfo lastComputedInfo = null;

	public JointPlanOptimizerActivityDurationEncodingSemanticsBuilder(
			final JointReplanningConfigGroup configGroup,
			final ScoringFunctionFactory scoringFunctionFactory,
			final JointPlanOptimizerLegTravelTimeEstimatorFactory legTravelTimeEstimatorFactory,
			final PlansCalcRoute routingAlgorithm,
			final Network network) {
		this.configGroup = configGroup;
		this.scoringFunctionFactory = scoringFunctionFactory;
		this.legTravelTimeEstimatorFactory = legTravelTimeEstimatorFactory;
		this.routingAlgorithm = routingAlgorithm;
		this.network = network;
	}

	@Override
	public JointPlanOptimizerJGAPChromosome createSampleChromosome(
			final JointPlan plan,
			final JointPlanOptimizerJGAPConfiguration configuration) throws InvalidConfigurationException {
		PlanStructureInfo info = countEpisodes( plan );

		Gene[] sampleGenes =
			new Gene[info.numToggleGenes + info.numEpisodes + info.numModeGenes];

		for (int i=0; i < info.numToggleGenes; i++) {
			sampleGenes[i] = new BooleanGene( configuration );
		}
		for (int i=info.numToggleGenes;
				i < info.numToggleGenes + info.numEpisodes; i++) {
			//sampleGenes[i] = new IntegerGene(this, 0, numTimeIntervals);
			sampleGenes[i] = new DoubleGene(configuration, 0, JointPlanOptimizerJGAPConfiguration.DAY_DUR);
		}
		for (int i=info.numToggleGenes + info.numEpisodes;
				i < info.numToggleGenes + info.numEpisodes + info.numModeGenes;
				i++) {
			sampleGenes[i] =
				new JointPlanOptimizerJGAPModeGene(configuration, configGroup.getAvailableModes());
		}

		return new JointPlanOptimizerJGAPChromosome(configuration, sampleGenes);
	}

	@Override
	public ConstraintsManager createConstraintsManager(
			final JointPlan plan,
			final JointPlanOptimizerJGAPConfiguration configuration) {
		List<Integer> nGenes = new ArrayList<Integer>();
		List<Double> maxDurations = new ArrayList<Double>();

		for (int nGenesTotal : countEpisodes( plan ).nDurationGenes) {
			// the first activity of an individual plan can start at any time
			// before midnight
			//nGenes.add( 1 );
			//maxDurations.add( DAY_DUR );

			// the sum of other activity durations must be below the day duration
			// (the last one must end before the end of the first one)
			// nGenes.add( nGenesTotal - 1 );
			nGenes.add( nGenesTotal );
			maxDurations.add( JointPlanOptimizerJGAPConfiguration.DAY_DUR );
		}

		return new UpperBoundsConstraintsManager( nGenes , maxDurations );
	}

	@Override
	public AbstractJointPlanOptimizerFitnessFunction createFitnessFunction(
			final JointPlan plan,
			final JointPlanOptimizerJGAPConfiguration configuration) {
		PlanStructureInfo info = countEpisodes( plan );

		return new JointPlanOptimizerOTFFitnessFunction(
				plan,
				configGroup,
				legTravelTimeEstimatorFactory,
				routingAlgorithm,
				network,
				info.numToggleGenes,
				info.numEpisodes,
				info.nMembers,
				scoringFunctionFactory);
	}

	// /////////////////////////////////////////////////////////////////////////
	// helpers
	// /////////////////////////////////////////////////////////////////////////

	private PlanStructureInfo countEpisodes(
			final JointPlan plan) {
		if (plan == lastExaminedPlan) {
			return lastComputedInfo;
		}

		PlanAnalyzeSubtours analyseSubtours = new PlanAnalyzeSubtours();
		analyseSubtours.setTripStructureAnalysisLayer(configGroup.getTripStructureAnalysisLayer());
		Id[] ids = new Id[1];
		ids = plan.getClique().getMembers().keySet().toArray(ids);
		//List<JointLeg> alreadyExamined = new ArrayList<JointLeg>();
		Plan currentPlan;
		int currentNDurationGenes;
		boolean sharedRideExamined = false;

		lastExaminedPlan = plan;
		lastComputedInfo = new PlanStructureInfo();

		for (Id id : ids) {
			currentPlan = plan.getIndividualPlan(id);
			currentNDurationGenes = 0;
			//TODO: use indices (and suppress the booleans)
			for (PlanElement pe : currentPlan.getPlanElements()) {
				// count activities for which duration is optimized
				if ((pe instanceof JointActivity)&&
						(!((JointActivity) pe).getType().equals(JointActingTypes.PICK_UP))&&
						(!((JointActivity) pe).getType().equals(JointActingTypes.DROP_OFF)) ) {
					currentNDurationGenes++;

					if (sharedRideExamined) {
						//reset the marker
						sharedRideExamined = false;
					}
				}
				else if (( configGroup.getOptimizeToggle() )&&
						(pe instanceof JointLeg)&&
						(((JointLeg) pe).getJoint())&&
						//(!alreadyExamined.contains(pe))
						(((JointLeg) pe).getMode().equals(JointActingTypes.PASSENGER))&&
						(!sharedRideExamined)
						) {
					// we are on the first shared ride of a passenger ride
					lastComputedInfo.numToggleGenes++;
					sharedRideExamined = true;

					//alreadyExamined.addAll(
					//		((JointLeg) pe).getLinkedElements().values());
				}
			}
			//do not count last activity
			currentNDurationGenes--;
			lastComputedInfo.numEpisodes += currentNDurationGenes;
			lastComputedInfo.nDurationGenes.add(currentNDurationGenes);

			//finally, count subtours
			analyseSubtours.run(currentPlan);
			lastComputedInfo.numModeGenes += analyseSubtours.getNumSubtours();

			lastComputedInfo.nMembers++;
		 }

		return lastComputedInfo;
	 }

}

class PlanStructureInfo {
	public int numToggleGenes = 0;
	public int numEpisodes = 0;
	public int numModeGenes = 0;
	public final List<Integer> nDurationGenes = new ArrayList<Integer>();
	public int nMembers = 0;
}
