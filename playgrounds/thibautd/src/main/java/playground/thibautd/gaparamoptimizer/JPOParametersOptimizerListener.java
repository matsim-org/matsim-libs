/* *********************************************************************** *
 * project: org.matsim.*
 * JPOParametersOptimizerListener.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.thibautd.gaparamoptimizer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;

import org.jgap.Configuration;
import org.jgap.Genotype;
import org.jgap.IChromosome;
import org.jgap.InvalidConfigurationException;
import org.jgap.Population;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.controler.events.ReplanningEvent;
import org.matsim.core.controler.listener.ReplanningListener;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.planomat.costestimators.DepartureDelayAverageCalculator;

import playground.thibautd.jointtripsoptimizer.population.Clique;
import playground.thibautd.jointtripsoptimizer.population.JointPlan;
import playground.thibautd.jointtripsoptimizer.population.PopulationOfCliques;
import playground.thibautd.jointtripsoptimizer.population.PopulationWithCliques;
import playground.thibautd.jointtripsoptimizer.replanning.modules.costestimators.JointPlanOptimizerLegTravelTimeEstimatorFactory;
import playground.thibautd.jointtripsoptimizer.run.config.JointReplanningConfigGroup;
import playground.thibautd.jointtripsoptimizer.run.JointControler;

/**
 * Core of the meta-GA.
 *
 * When the replanning event is fired, this class takes random cliques from the
 * population, and uses them as test instances for scoring the parameters encoded
 * in the meta-GA's population.
 *
 * @author thibautd
 */
public class JPOParametersOptimizerListener implements ReplanningListener {
	private static final Logger log =
		Logger.getLogger(JPOParametersOptimizerListener.class);

	private String outputFileName = "optimizedConf.xml";
	private final static int N_GEN = 30;
	private final static int N_PLANS = 5;

	//private final static int MIN_MEMBERS = 3;
	//private final static int MAX_MEMBERS = 20;
	//private final static int MAX_PLANS_PER_SIZE = 1;

	@Override
	public void notifyReplanning(final ReplanningEvent event) {
		JointControler controler = (JointControler) event.getControler();
		PopulationWithCliques population = (PopulationWithCliques) controler.getPopulation();
		PopulationOfCliques cliques = population.getCliques();
		Network network = controler.getNetwork();
		this.outputFileName =
			controler.getControlerIO().getOutputPath() +"/"+ outputFileName;

		optimizeParameters(
				getSamplePlans(cliques),
				controler.getScoringFunctionFactory(),
				new JointPlanOptimizerLegTravelTimeEstimatorFactory(
					controler.getTravelTimeCalculator(),
					new DepartureDelayAverageCalculator(
						network,
						controler.getConfig().travelTimeCalculator().getTraveltimeBinSize())),
				(PlansCalcRoute) controler.createRoutingAlgorithm(),
				network,
				controler.getControlerIO().getIterationPath(event.getIteration()));
	}

	private List<JointPlan> getSamplePlans(final PopulationOfCliques popOfCliques) {
		List<Clique> cliques = new ArrayList<Clique>(popOfCliques.getCliques().values());
		Collections.shuffle(cliques);
		List<JointPlan> output = new ArrayList<JointPlan>(N_PLANS);
		JointPlan currentPlan;

		//log.debug("drawing "+N_PLANS+" test instances.");
		//log.debug("minimum members of clique: "+MIN_MEMBERS);
		//log.debug("maximum members of clique: "+MAX_MEMBERS);

		//int count = 0;
		//int[] counts = new int[MAX_MEMBERS - MIN_MEMBERS + 1];

		//for (int j = 0; j < counts.length; j++) {
		//	counts[j] = 0;
		//}

		//while (count < N_PLANS) {
		//	currentPlan = (JointPlan) cliques.get(i).getSelectedPlan();
		//	size = currentPlan.getClique().getMembers().size();;
		//	i++;

		//	// only consider joint plans satisfying the conditions
		//	if (size >= MIN_MEMBERS &&
		//			size <= MAX_MEMBERS &&
		//			counts[size - MIN_MEMBERS] < MAX_PLANS_PER_SIZE) {
		//		log.debug("adding test instance with "+size+" members.");
		//		output.add(currentPlan);
		//		count++;
		//		counts[size - MIN_MEMBERS]++;
		//	}
		//}

		int[] sizes = {1,3,4,6,20,57};

		for (int size : sizes) {
			for (int i=0; i < cliques.size(); i++) {
				currentPlan = (JointPlan) cliques.get(i).getSelectedPlan();
				int currentSize = currentPlan.getClique().getMembers().size();;
				i++;

				if (currentSize == size) {
					log.debug("adding test instance with "+size+" members.");
					output.add(currentPlan);
					break;
				}
			}
		}

		return output;
	}

	private void optimizeParameters(
			final List<JointPlan> plans,
			final ScoringFunctionFactory scoringFunctionFactory,
			final JointPlanOptimizerLegTravelTimeEstimatorFactory legTravelTimeEstimatorFactory,
			final PlansCalcRoute routingAlgorithm,
			final Network network,
			final String iterationOutputPath
			) {
		log.debug("optimizing parameters...");
		log.debug("nGenerations: "+N_GEN);
		log.debug("nPlans: "+N_PLANS);
		Configuration jgapConfig = new JgapParameterOptimizerConfig(
				plans,
				scoringFunctionFactory,
				legTravelTimeEstimatorFactory,
				routingAlgorithm,
				network,
				iterationOutputPath);

		Genotype population;
		try {
			population = Genotype.randomInitialGenotype(jgapConfig);
		} catch (InvalidConfigurationException e) {
			throw new RuntimeException(e);
		}

		log.debug("always compute fitness = "+jgapConfig.isAlwaysCalculateFitness());
		for (int i=0; i < N_GEN; i++) {
			population.evolve();
			outputParameters(population.getFittestChromosome(), ""+i);
		}

		log.debug("optimizing parameters... DONE");
		outputParameters(population.getFittestChromosome(), "final");
	}

	private void outputParameters(final IChromosome fittest, final String iter) {
		log.info("outputting parameter set: "+fittest.toString());

		JointReplanningConfigGroup optConf =
			ParameterOptimizerFitness.fromChromosomeToConfig(fittest);

		Config conf = new Config();
		conf.addModule("optimizedJointReplanning", optConf);
		(new ConfigWriter(conf)).write(iter+"."+outputFileName);
		log.debug("results written in "+iter+"."+outputFileName);
	}
}

