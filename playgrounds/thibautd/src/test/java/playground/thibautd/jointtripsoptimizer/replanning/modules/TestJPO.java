/* *********************************************************************** *
 * project: org.matsim.*
 * TestJPO.java
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
package playground.thibautd.jointtripsoptimizer.replanning.modules;

import java.util.ArrayList;
import java.util.List;

import org.jgap.GeneticOperator;
import org.jgap.IChromosome;
import org.jgap.InvalidConfigurationException;
import org.jgap.Population;
import org.jgap.Population;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.planomat.costestimators.DepartureDelayAverageCalculator;
import org.matsim.testcases.MatsimTestUtils;

import playground.thibautd.jointtripsoptimizer.population.Clique;
import playground.thibautd.jointtripsoptimizer.population.JointPlan;
import playground.thibautd.jointtripsoptimizer.population.PopulationWithCliques;
import playground.thibautd.jointtripsoptimizer.replanning.modules.costestimators.JointPlanOptimizerLegTravelTimeEstimatorFactory;
import playground.thibautd.jointtripsoptimizer.replanning.modules.fitness.JointPlanOptimizerFitnessFunction;
import playground.thibautd.jointtripsoptimizer.replanning.modules.fitness.JointPlanOptimizerOTFFitnessFunction;
import playground.thibautd.jointtripsoptimizer.replanning.modules.geneticoperators.JointPlanOptimizerJGAPCrossOver;
import playground.thibautd.jointtripsoptimizer.replanning.modules.geneticoperators.JointPlanOptimizerJGAPMutation;
import playground.thibautd.jointtripsoptimizer.replanning.modules.JointPlanOptimizer;
import playground.thibautd.jointtripsoptimizer.replanning.modules.JointPlanOptimizerJGAPChromosome;
import playground.thibautd.jointtripsoptimizer.replanning.modules.JointPlanOptimizerJGAPConfiguration;
import playground.thibautd.jointtripsoptimizer.replanning.modules.JointPlanOptimizerModule;
import playground.thibautd.jointtripsoptimizer.run.config.JointReplanningConfigGroup;
import playground.thibautd.jointtripsoptimizer.run.JointControler;
import playground.thibautd.jointtripsoptimizer.utils.JointControlerUtils;

/**
 * various tests related to the JointPlanOptimizer.
 * Those tests are grouped here not to split too much all the initialisation procedures.
 * @author thibautd
 */
public class TestJPO {

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();
	// sample clique
	private static final Id idSample = new IdImpl("2593674");
	private static final int N_TEST_CHROM = 100;
	private static final double P_MUT = 0.3;
	private static final double P_CO = 1;

	private JointControler controler;
	//private JointPlanOptimizer algo;
	private JointPlanOptimizerJGAPConfiguration jgapConf;
	private JointReplanningConfigGroup configGroup;
	private JointPlanOptimizerLegTravelTimeEstimatorFactory legTTEstFactory;
	private PlansCalcRoute routingAlgo;
	private JointPlan samplePlan;
	private Clique sampleClique;

	// /////////////////////////////////////////////////////////////////////////
	// Fixtures setting
	// /////////////////////////////////////////////////////////////////////////
	@Before
	public void initConf() {
		String inputPath = getParentDirectory(utils.getPackageInputDirectory(), 2);
		String outputPath = utils.getOutputDirectory();

		// to be sure to load properly the joint replanning config group.
		this.controler = (JointControler) JointControlerUtils.createControler(inputPath+"/config.xml");
		this.controler.getConfig().controler().setOutputDirectory(outputPath);

		// run a "trash" iteration to be sure everithing is loaded
		this.controler.getConfig().controler().setFirstIteration(0);
		this.controler.getConfig().controler().setLastIteration(0);
		this.controler.run();

		//this.algo = (JointPlanOptimizer)
		//	(new JointPlanOptimizerModule(controler)).getPlanAlgoInstance();
		this.sampleClique = ((PopulationWithCliques) controler.getPopulation())
			.getCliques().getCliques().get(idSample);
		this.samplePlan = (JointPlan) sampleClique.getSelectedPlan();
		// XXX very ugly: find a nice way to get from the "real" code
		PersonalizableTravelCost travelCost = controler.createTravelCostCalculator();
		PersonalizableTravelTime travelTime = controler.getTravelTimeCalculator();
		this.configGroup = (JointReplanningConfigGroup)
					this.controler.getConfig().getModule(JointReplanningConfigGroup.GROUP_NAME);
		this.legTTEstFactory = new JointPlanOptimizerLegTravelTimeEstimatorFactory(
					 travelTime,
					 new DepartureDelayAverageCalculator(
						controler.getNetwork(),
						controler.getConfig().travelTimeCalculator().getTraveltimeBinSize()));

		this.routingAlgo = (PlansCalcRoute) this.controler.createRoutingAlgorithm(travelCost, travelTime);

		this.jgapConf = new JointPlanOptimizerJGAPConfiguration(
				this.samplePlan,
				this.configGroup,
				this.controler.getScoringFunctionFactory(),
				this.legTTEstFactory,
				this.routingAlgo,
				this.controler.getNetwork(),
				outputPath,
				123);
	}

	private String getParentDirectory(final String path, final int levels) {
		String[] pathArray = path.split("/");
		String output = "";

		for (int i=0; i < pathArray.length - levels; i++) {
			output += pathArray[i] + "/";
		}

		return output;
	}



	// /////////////////////////////////////////////////////////////////////////
	// test methods
	// /////////////////////////////////////////////////////////////////////////
	/**
	 * Tests whether the fitness "on the fly" and the fitness with the
	 * "full decoder" give the same result.
	 *
	 * This is very important, as the plan returned by an optimisation
	 * with an on the fly fitness is created by the full decoder.
	 */
	@Test
	public void testFitnesses() {
		JointPlanOptimizerFitnessFunction full = new JointPlanOptimizerFitnessFunction(
					this.samplePlan,
					this.configGroup,
					this.legTTEstFactory,
					this.routingAlgo,
					this.controler.getNetwork(),
					this.jgapConf.getNumJointEpisodes(),
					this.jgapConf.getNumEpisodes(),
					this.sampleClique.getMembers().size(),
					this.controler.getScoringFunctionFactory());

		JointPlanOptimizerOTFFitnessFunction otf = new JointPlanOptimizerOTFFitnessFunction(
					this.samplePlan,
					this.configGroup,
					this.legTTEstFactory,
					this.routingAlgo,
					this.controler.getNetwork(),
					this.jgapConf.getNumJointEpisodes(),
					this.jgapConf.getNumEpisodes(),
					this.sampleClique.getMembers().size(),
					this.controler.getScoringFunctionFactory());

		IChromosome sampleChrom;
		
		try {
			sampleChrom = ((JointPlanOptimizerJGAPChromosome) this.jgapConf.getSampleChromosome())
				.randomInitialJointPlanOptimizerJGAPChromosome();
		} catch (InvalidConfigurationException e) {
			throw new RuntimeException(e);
		}

		Assert.assertEquals(
				"inconsistency between the two scoring approaches.",
				full.getFitnessValue(sampleChrom),
				otf.getFitnessValue(sampleChrom),
				MatsimTestUtils.EPSILON);
	}

	/**
	 * Checks whether the offspring of the cross-overs respect the constraints.
	 */
	@Test
	public void testCOConstraints() {
		this.configGroup.setWholeCrossOverProbability(P_CO);
		this.configGroup.setSingleCrossOverProbability(P_CO);
		this.configGroup.setSimpleCrossOverProbability(P_CO);

		JointPlanOptimizerJGAPCrossOver crossOver =
			new JointPlanOptimizerJGAPCrossOver(
					this.jgapConf,
					this.configGroup,
					this.jgapConf.getNumJointEpisodes(),
					this.jgapConf.getNumEpisodes(),
					this.jgapConf.getNumModeGenes(),
					this.jgapConf.getNDurationGenesPerIndiv());

		List<IChromosome> offsprings = new ArrayList<IChromosome>();
		Population jgapPop = createSampleGeneticPopulation();
		testGeneticOperator(crossOver, jgapPop, offsprings);
	}

	/**
	 * Checks whether the offspring of the cross-overs respect the constraints.
	 */
	@Test
	public void testMutationConstraints() {
		this.configGroup.setInPlaceMutation(false);
		this.configGroup.setMutationProbability(P_MUT);

		JointPlanOptimizerJGAPMutation mutation =
			new JointPlanOptimizerJGAPMutation(
					this.jgapConf,
					this.configGroup,
					this.jgapConf.getSampleChromosome().size(),
					this.jgapConf.getNDurationGenesPerIndiv());

		List<IChromosome> offsprings = new ArrayList<IChromosome>();
		Population jgapPop = createSampleGeneticPopulation();
		testGeneticOperator(mutation, jgapPop, offsprings);
	}

	/**
	 * Tests the behaviour of the mutation depending on the "in place"
	 * parameter.
	 */
	@Test
	@Ignore
	public void testMutationInPlace() {
	}

	/**
	 * Tests whether the randomly generated chromosomes respect the constraints.
	 */
	@Test
	@Ignore
	public void testConstraintsRandomChromosome() {
	}

	// /////////////////////////////////////////////////////////////////////////
	// helpers
	// /////////////////////////////////////////////////////////////////////////
	private void testGeneticOperator(
			final GeneticOperator operator,
			final Population pop,
			final List<IChromosome> offsprings) {
		operator.operate(pop, offsprings);

		for (IChromosome chrom : offsprings) {
			Assert.assertTrue(
					operator.getClass().getSimpleName()+"returned offspring not"+
						" respecting the constraints.",
					((JointPlanOptimizerJGAPChromosome) chrom).respectsConstraints());
		}
	}

	private Population createSampleGeneticPopulation() {
		IChromosome[] chromosomes = new IChromosome[N_TEST_CHROM];

		try {
			for (int i=0; i < chromosomes.length; i++) {
				// create "full" chromosomes so that the propability of creating
				// invalid chromosomes is high in the case the constraints are not
				// well considered in the operators.
				chromosomes[i] = ((JointPlanOptimizerJGAPChromosome) this.jgapConf.getSampleChromosome())
					.randomFullChromosome();
			}

			return new Population(this.jgapConf, chromosomes);
		} catch (InvalidConfigurationException e) {
			throw new RuntimeException(e);
		}
	}
}

