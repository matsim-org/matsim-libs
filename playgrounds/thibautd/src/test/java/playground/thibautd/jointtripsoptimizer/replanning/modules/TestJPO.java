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
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.jgap.Gene;
import org.jgap.GeneticOperator;
import org.jgap.Genotype;
import org.jgap.IChromosome;
import org.jgap.impl.BooleanGene;
import org.jgap.InvalidConfigurationException;
import org.jgap.Population;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.planomat.costestimators.DepartureDelayAverageCalculator;
import org.matsim.testcases.MatsimTestUtils;

import playground.thibautd.jointtripsoptimizer.population.Clique;
import playground.thibautd.jointtripsoptimizer.population.JointLeg;
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
import playground.thibautd.jointtripsoptimizer.replanning.modules.pipeddecoder.DurationDecoder;
import playground.thibautd.jointtripsoptimizer.replanning.modules.pipeddecoder.DurationDecoderPartial;
import playground.thibautd.jointtripsoptimizer.replanning.modules.pipeddecoder.JointPlanOptimizerDecoderFactory;
import playground.thibautd.jointtripsoptimizer.replanning.modules.pipeddecoder.JointPlanOptimizerDimensionDecoder;
import playground.thibautd.jointtripsoptimizer.run.config.JointReplanningConfigGroup;
import playground.thibautd.jointtripsoptimizer.run.JointControler;
import playground.thibautd.jointtripsoptimizer.utils.JointControlerUtils;

/**
 * various tests related to the JointPlanOptimizer.
 * Those tests are grouped here not to split too much all the initialisation procedures.
 * @author thibautd
 */
public class TestJPO {
//TODO: refactor (one abstract fixtures initializing class, and individual class tests)
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
	/////////////////////////// fitness and decoder ////////////////////////////
	/**
	 * Tests synchronisation between driver and passenger.
	 */
	@Test
	public void testJointTravelTimes() {
		IChromosome sampleChrom;

		try {
			sampleChrom = ((JointPlanOptimizerJGAPChromosome) this.jgapConf.getSampleChromosome())
				.randomInitialJointPlanOptimizerJGAPChromosome();

			//set joint trips on
			for (Gene gene : sampleChrom.getGenes()) {
				if (gene instanceof BooleanGene) {
					gene.setAllele(true);
				}
			}
		} catch (InvalidConfigurationException e) {
			throw new RuntimeException(e);
		}

		JointPlanOptimizerDecoder decoder = jgapConf.getDecoder();

		JointPlan plan = decoder.decode(sampleChrom);

		boolean wasJoint = false;

		for (PlanElement element : plan.getPlanElements()) {
			if (element instanceof JointLeg) {
				JointLeg current = (JointLeg) element;
				
				for (JointLeg coLeg : current.getLinkedElements().values()) {
					wasJoint = true;
					Assert.assertEquals(
							"joint legs have different departure times",
							current.getDepartureTime(),
							coLeg.getDepartureTime(),
							MatsimTestUtils.EPSILON);

					Assert.assertEquals(
							"joint legs have different arrival times",
							current.getArrivalTime(),
							coLeg.getArrivalTime(),
							MatsimTestUtils.EPSILON);
				}
			}
		}

		if (!wasJoint) {
			throw new RuntimeException("test was run on data without joint trip");
		}
	}

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
	 * tests whether the OTF fitness always returns the same result when it is
	 * given the same chromosome.
	 */
	@Test
	public void testOTFDeterministic() {
		int nChrom = 10;
		int nTries = 10;

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

		IChromosome[] sampleChrom = new IChromosome[nChrom];
		double[] score = new double[nChrom];
		
		// first try each chromosome
		for (int i=0; i < nChrom; i++) {
			try {
				sampleChrom[i] = ((JointPlanOptimizerJGAPChromosome) this.jgapConf.getSampleChromosome())
					.randomInitialJointPlanOptimizerJGAPChromosome();
			} catch (InvalidConfigurationException e) {
				throw new RuntimeException(e);
			}

			score[i] = otf.getFitnessValue(sampleChrom[i]);

			for (int j=1; j < nTries; j++) {
				Assert.assertEquals(
						"several tries of the OTF fitness on the same chrom gave different fitnesses",
						score[i],
						otf.getFitnessValue(sampleChrom[i]),
						MatsimTestUtils.EPSILON);
			}
		}

		//than check that evaluating other chromosomes didn't change the obtained value
		for (int i=0; i < nChrom; i++) {
			Assert.assertEquals(
					"evaluating other chromosomes changed the obtained score",
					score[i],
					otf.getFitnessValue(sampleChrom[i]),
					MatsimTestUtils.EPSILON);
		}
	}

	/**
	 * tests whether the score of the optimum given by the Simplex
	 * fitness optimizer corresponds to the fitness of the modified chromosome.
	 */
	@Test
	public void testSimplexFitness() {
		int nTries = 10;

		this.configGroup.setIsMemetic("false");
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

		this.configGroup.setIsMemetic("true");
		JointPlanOptimizerOTFFitnessFunction otfMemetic = new JointPlanOptimizerOTFFitnessFunction(
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
		
		for (int i = 0; i < nTries; i++) {
			try {
				sampleChrom = ((JointPlanOptimizerJGAPChromosome) this.jgapConf.getSampleChromosome())
					.randomInitialJointPlanOptimizerJGAPChromosome();
			} catch (InvalidConfigurationException e) {
				throw new RuntimeException(e);
			}

			IChromosome beforeOptClone = (IChromosome) sampleChrom.clone();
			double beforeOptNormalScore = otf.getFitnessValue(sampleChrom);
			double optimisedScore = otfMemetic.getFitnessValue(sampleChrom);
			double afterOptNormalScore = otf.getFitnessValue(sampleChrom);

			Assert.assertFalse(
					"chromosome left unmodified after optimisation",
					beforeOptClone.equals(sampleChrom));

			Assert.assertFalse(
					"score of modified chromosome is the same as the score of unmodified chromosome",
					Math.abs(beforeOptNormalScore - afterOptNormalScore) < MatsimTestUtils.EPSILON);

			Assert.assertTrue(
					"optimised score is not better than unoptimised one",
					optimisedScore > beforeOptNormalScore);

			Assert.assertEquals(
					"score returned by the optimiser not consistent with the chromosome real score",
					optimisedScore,
					afterOptNormalScore,
					MatsimTestUtils.EPSILON);
		}
	}

	/**
	 * Tests the consistency of the partial decoder. More precisely,
	 * the test fails if the partial decoder launched on a whole clique
	 * does not produce the same output at the standard decoder.
	 */
	@Test
	@Ignore
	public void testPartialDecoder() {
		int nTrys = 3;

		DurationDecoderPartial partial = new DurationDecoderPartial(
					this.samplePlan,
					new ArrayList<Id>(this.sampleClique.getMembers().keySet()),
					this.configGroup,
					this.legTTEstFactory,
					this.routingAlgo,
					this.controler.getNetwork(),
					this.jgapConf.getNumJointEpisodes(),
					this.jgapConf.getNumEpisodes(),
					this.sampleClique.getMembers().size());

		DurationDecoder classical = new DurationDecoder(
					this.samplePlan,
					this.configGroup,
					this.legTTEstFactory,
					this.routingAlgo,
					this.controler.getNetwork(),
					this.jgapConf.getNumJointEpisodes(),
					this.jgapConf.getNumEpisodes(),
					this.sampleClique.getMembers().size());

		IChromosome sampleChrom;

		for (int tryCount=0; tryCount < nTrys; tryCount++) {
			try {
				sampleChrom = ((JointPlanOptimizerJGAPChromosome) this.jgapConf.getSampleChromosome())
					.randomInitialJointPlanOptimizerJGAPChromosome();
			} catch (InvalidConfigurationException e) {
				throw new RuntimeException(e);
			}

			List<PlanElement> planPartial = partial.decode(sampleChrom, /*new JointPlan*/(this.samplePlan)).getPlanElements();
			List<PlanElement> planClassical = classical.decode(sampleChrom, /*new JointPlan*/(this.samplePlan)).getPlanElements();
			double endTimeClassical;
			double endTimePartial;

			Assert.assertEquals(
					"plans decoded with classical and partial decoder do not have the same length",
					planClassical.size(),
					planPartial.size());

			for (int i=0; i < planClassical.size(); i++) {
				if (planClassical.get(i) instanceof Activity) {
					endTimeClassical = ((Activity) planClassical.get(i)).getEndTime();
					endTimePartial = ((Activity) planPartial.get(i)).getEndTime();

					Assert.assertEquals(
							"activities were found with different ending times in"+
							" classical and partial decoder.",
							endTimeClassical,
							endTimePartial,
							MatsimTestUtils.EPSILON);
				}
			}
		}
	}

	/**
	 * Tests whether a plan decoded with the duration decoder ends at midnight.
	 */
	@Test
	public void testDurationDecoderClassicalEndTime() {
		DurationDecoder classical = new DurationDecoder(
					this.samplePlan,
					this.configGroup,
					this.legTTEstFactory,
					this.routingAlgo,
					this.controler.getNetwork(),
					this.jgapConf.getNumJointEpisodes(),
					this.jgapConf.getNumEpisodes(),
					this.sampleClique.getMembers().size());

		testDurationDecoderEndTime(classical);
	}

	@Test
	public void testDurationDecoderPartialEndTime() {
		DurationDecoderPartial partial = new DurationDecoderPartial(
					this.samplePlan,
					new ArrayList<Id>(this.sampleClique.getMembers().keySet()),
					this.configGroup,
					this.legTTEstFactory,
					this.routingAlgo,
					this.controler.getNetwork(),
					this.jgapConf.getNumJointEpisodes(),
					this.jgapConf.getNumEpisodes(),
					this.sampleClique.getMembers().size());

		testDurationDecoderEndTime(partial);
	}

	private void testDurationDecoderEndTime(final JointPlanOptimizerDimensionDecoder decoder) {
		int nTrys = 3;
		double expectedEndTime = 3600d*24;

		IChromosome sampleChrom;

		for (int tryCount=0; tryCount < nTrys; tryCount++) {
			try {
				sampleChrom = ((JointPlanOptimizerJGAPChromosome) this.jgapConf.getSampleChromosome())
					.randomInitialJointPlanOptimizerJGAPChromosome();
			} catch (InvalidConfigurationException e) {
				throw new RuntimeException(e);
			}

			Collection<Plan> individualPlans = decoder.decode(sampleChrom, this.samplePlan).getIndividualPlans().values();

			for (Plan indivPlan : individualPlans) {
				Assert.assertEquals(
						"plan of incorrect duration created by "+decoder.getClass().getSimpleName(),
						expectedEndTime,
						((PlanImpl) indivPlan).getLastActivity().getEndTime(),
						MatsimTestUtils.EPSILON);
			}
		}
	}

	@Test
	public void testTimeLineClassical() {
		DurationDecoder classical = new DurationDecoder(
					this.samplePlan,
					this.configGroup,
					this.legTTEstFactory,
					this.routingAlgo,
					this.controler.getNetwork(),
					this.jgapConf.getNumJointEpisodes(),
					this.jgapConf.getNumEpisodes(),
					this.sampleClique.getMembers().size());

		testTimeLine(classical);
	}

	@Test
	public void testTimeLinePartial() {
		DurationDecoderPartial partial = new DurationDecoderPartial(
					this.samplePlan,
					new ArrayList<Id>(this.sampleClique.getMembers().keySet()),
					this.configGroup,
					this.legTTEstFactory,
					this.routingAlgo,
					this.controler.getNetwork(),
					this.jgapConf.getNumJointEpisodes(),
					this.jgapConf.getNumEpisodes(),
					this.sampleClique.getMembers().size());

		testTimeLine(partial);
	}

	private void testTimeLine(final JointPlanOptimizerDimensionDecoder decoder) {
		int nTrys = 3;

		IChromosome sampleChrom;
		double oldNow=0;
		double now =0;

		for (int tryCount=0; tryCount < nTrys; tryCount++) {
			try {
				sampleChrom = ((JointPlanOptimizerJGAPChromosome) this.jgapConf.getSampleChromosome())
					.randomInitialJointPlanOptimizerJGAPChromosome();
			} catch (InvalidConfigurationException e) {
				throw new RuntimeException(e);
			}

			Collection<Plan> indivPlans = decoder.decode(sampleChrom, (this.samplePlan)).getIndividualPlans().values();

			for (Plan indivPlan : indivPlans) {
				oldNow = 0d;
				for (PlanElement pe : indivPlan.getPlanElements()) {
					if (pe instanceof Activity) {
						now = ((Activity) pe).getEndTime();
						
						Assert.assertTrue(
								"inconsistency in the timeline in "+decoder.getClass().getSimpleName()
								+": time goes from "+oldNow+" to "+now+".",
								now >= oldNow);
						oldNow = now;
					}
				}
			}
		}
	}

	/**
	 * Tests whether the scores obtained by the getter on the chromosomes
	 * correspond to the scores obtained by decoding them, in the non-memetic setting.
	 */
	@Test
	public void testScoresConsistencyGA() {
		JointPlanOptimizerPopulationFactory populationFactory =
			new JointPlanOptimizerPopulationFactory(jgapConf);

		JointPlanOptimizerOTFFitnessFunction scorer = new JointPlanOptimizerOTFFitnessFunction(
					this.samplePlan,
					this.configGroup,
					this.legTTEstFactory,
					this.routingAlgo,
					this.controler.getNetwork(),
					this.jgapConf.getNumJointEpisodes(),
					this.jgapConf.getNumEpisodes(),
					this.sampleClique.getMembers().size(),
					this.controler.getScoringFunctionFactory());


		Genotype gaPopulation = populationFactory.createRandomInitialGenotype();

		gaPopulation.evolve(2);

		// notify end
		jgapConf.finish();

		for ( IChromosome chrom : gaPopulation.getChromosomes() ) {
			double score = chrom.getFitnessValue();
			double decoded = scorer.getFitnessValue(chrom);

			Assert.assertEquals(
					"score returned by chromosome do not correspond to decoded score",
					decoded, score, MatsimTestUtils.EPSILON);
		}
	}

	/**
	 * Tests whether the scores obtained by the getter on the chromosomes
	 * correspond to the scores obtained by decoding them, in the memetic setting.
	 */
	@Test
	public void testScoresConsistencyMA() {
		this.configGroup.setIsMemetic("true");

		JointPlanOptimizerJGAPConfiguration jgapConfMeme = new JointPlanOptimizerJGAPConfiguration(
				this.samplePlan,
				this.configGroup,
				this.controler.getScoringFunctionFactory(),
				this.legTTEstFactory,
				this.routingAlgo,
				this.controler.getNetwork(),
				utils.getOutputDirectory(),
				123);

		JointPlanOptimizerPopulationFactory populationFactory =
			new JointPlanOptimizerPopulationFactory(jgapConfMeme);

		this.configGroup.setIsMemetic("false");
		JointPlanOptimizerOTFFitnessFunction scorer = new JointPlanOptimizerOTFFitnessFunction(
					this.samplePlan,
					this.configGroup,
					this.legTTEstFactory,
					this.routingAlgo,
					this.controler.getNetwork(),
					this.jgapConf.getNumJointEpisodes(),
					this.jgapConf.getNumEpisodes(),
					this.sampleClique.getMembers().size(),
					this.controler.getScoringFunctionFactory());

		Genotype gaPopulation = populationFactory.createRandomInitialGenotype();

		gaPopulation.evolve(2);

		// notify end
		jgapConfMeme.finish();

		int count = 0;
		int countLess = 0;
		int countMore = 0;
		for ( IChromosome chrom : gaPopulation.getChromosomes() ) {
			double score = chrom.getFitnessValue();
			double decoded = scorer.getFitnessValue(chrom);
			double diff = score - decoded;

			count++;

			// rather than failing on the first inconsistent result,
			// collect some statistics
			if (diff > MatsimTestUtils.EPSILON) {
				countMore++;
			}
			else if (diff < -MatsimTestUtils.EPSILON) {
				countLess++;
			}
		}

		Assert.assertEquals(
				"score returned by some chromosome do not correspond to decoded score: "+
				"on "+count+" chromosomes, "+countMore+" returned a score greater than the decoded one, "+
				countLess+" returned a score lower",
				0, countMore + countLess);
	}

	/////////////////////////////////// operators //////////////////////////////
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

