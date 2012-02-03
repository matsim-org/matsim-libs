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
package playground.thibautd.jointtrips.replanning.modules.jointplanoptimizer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

import org.jgap.FitnessFunction;
import org.jgap.Gene;
import org.jgap.GeneticOperator;
import org.jgap.Genotype;
import org.jgap.IChromosome;
import org.jgap.IInitializer;
import org.jgap.impl.DoubleGene;
import org.jgap.Population;
import org.jgap.impl.BooleanGene;
import org.jgap.RandomGenerator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.Time;
import org.matsim.planomat.costestimators.DepartureDelayAverageCalculator;
import org.matsim.testcases.MatsimTestUtils;

import playground.thibautd.jointtrips.config.JointReplanningConfigGroup;
import playground.thibautd.jointtrips.population.Clique;
import playground.thibautd.jointtrips.population.JointActingTypes;
import playground.thibautd.jointtrips.population.JointLeg;
import playground.thibautd.jointtrips.population.JointPlan;
import playground.thibautd.jointtrips.population.PopulationWithCliques;
import playground.thibautd.jointtrips.replanning.modules.jointplanoptimizer.geneticoperators.ConstraintsManager;
import playground.thibautd.jointtrips.replanning.modules.jointplanoptimizer.geneticoperators.UpperBoundsConstraintsManager;
import playground.thibautd.jointtrips.replanning.modules.jointplanoptimizer.JointPlanOptimizerDecoder;
import playground.thibautd.jointtrips.replanning.modules.jointplanoptimizer.JointPlanOptimizerJGAPConfiguration;
import playground.thibautd.jointtrips.replanning.modules.jointplanoptimizer.costestimators.JointPlanOptimizerLegTravelTimeEstimatorFactory;
import playground.thibautd.jointtrips.replanning.modules.jointplanoptimizer.fitness.JointPlanOptimizerFitnessFunction;
import playground.thibautd.jointtrips.replanning.modules.jointplanoptimizer.fitness.JointPlanOptimizerOTFFitnessFunction;
import playground.thibautd.jointtrips.replanning.modules.jointplanoptimizer.geneticoperators.JointPlanOptimizerJGAPCrossOver;
import playground.thibautd.jointtrips.replanning.modules.jointplanoptimizer.geneticoperators.JointPlanOptimizerJGAPMutation;
import playground.thibautd.jointtrips.replanning.modules.jointplanoptimizer.pipeddecoder.DurationDecoder;
import playground.thibautd.jointtrips.replanning.modules.jointplanoptimizer.pipeddecoder.DurationDecoderActivityEndsEncoding;
import playground.thibautd.jointtrips.replanning.modules.jointplanoptimizer.pipeddecoder.DurationDecoderPartial;
import playground.thibautd.jointtrips.replanning.modules.jointplanoptimizer.pipeddecoder.JointPlanOptimizerDimensionDecoder;
import playground.thibautd.jointtrips.run.JointControler;
import playground.thibautd.jointtrips.utils.JointControlerUtils;

/**
 * various tests related to the JointPlanOptimizer.
 * Those tests are grouped here not to split too much all the initialisation procedures.
 * It should be split in the future.
 *
 * <br>
 * It uses a parameterized approach to test various encodings:
 * <ul>
 * <li> 0: activity ends
 * <li> 1: activity durations
 * </ul>
 * @author thibautd
 */
@RunWith(Parameterized.class)
public class TestJPO {
//TODO: refactor (one abstract fixtures initializing class, and individual class tests)
	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	// /////////////////////////////////////////////////////////////////////////
	// Trick to test both encoding without reimplementing everything:
	// use the "parameterized" approach
	// problem: when a test fails, it is more difficult to see
	// which semantics is used
	private final SemanticsFactory semanticsFactory;

	@Parameters
	public static Collection<Object[]> getSemantics() {
		Object[][] semantics = {
			{new EndsSemanticsFactory()} ,
			{new DurationsSemanticsFactory()}};
		return Arrays.asList( semantics );
	}

	public TestJPO(
			final SemanticsFactory semanticsFactory) {
		this.semanticsFactory = semanticsFactory;
	}
	// end of the trick
	// /////////////////////////////////////////////////////////////////////////

	// sample clique
	private static final Id idSample = new IdImpl("2593674");
	private static final int N_TEST_CHROM = 1000;
	private static final double P_MUT = 0.4;
	private static final double P_CO = 1;

	private JointControler controler;
	//private JointPlanOptimizer algo;
	private JointPlanOptimizerJGAPConfiguration jgapConf;
	private JointReplanningConfigGroup configGroup;
	private JointPlanOptimizerLegTravelTimeEstimatorFactory legTTEstFactory;
	private PlansCalcRoute routingAlgo;
	private JointPlan samplePlan;
	private Clique sampleClique;

	private int numToggleGenes;
	private int numDurationGenes;

	// /////////////////////////////////////////////////////////////////////////
	// Fixtures setting
	// /////////////////////////////////////////////////////////////////////////
	@Before
	public void initConf() throws Exception {
		String inputPath = getParentDirectory(utils.getPackageInputDirectory(), 3);
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
				this.semanticsFactory.createSemantics(
					this.configGroup,
					this.controler.getScoringFunctionFactory(),
					this.legTTEstFactory,
					this.routingAlgo,
					this.controler.getNetwork()),
				new JointPlanOptimizerRTSProcessBuilder( configGroup ),
				this.configGroup,
				outputPath,
				123);

		numToggleGenes = 0;
		numDurationGenes = 0;
		for (Gene gene : this.jgapConf.getSampleChromosome().getGenes()) {
			if (gene instanceof DoubleGene) {
				numDurationGenes++;
			}
			else if (gene instanceof BooleanGene) {
				numToggleGenes++;
			}
		}

		jgapConf.setPopulationSize( N_TEST_CHROM );
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
	public void testJointTravelTimes() throws Exception {
		IChromosome sampleChrom;

		sampleChrom = createRandomChromosome();

		//set joint trips on
		for (Gene gene : sampleChrom.getGenes()) {
			if (gene instanceof BooleanGene) {
				gene.setAllele(true);
			}
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
	// FIXME: linked to one encoding, but ran for each
	public void testFitnesses() throws Exception {
		JointPlanOptimizerFitnessFunction full = new JointPlanOptimizerFitnessFunction(
					this.samplePlan,
					this.configGroup,
					this.legTTEstFactory,
					this.routingAlgo,
					this.controler.getNetwork(),
					numToggleGenes,
					numDurationGenes,
					this.sampleClique.getMembers().size(),
					this.controler.getScoringFunctionFactory());

		JointPlanOptimizerOTFFitnessFunction otf = new JointPlanOptimizerOTFFitnessFunction(
					this.samplePlan,
					this.configGroup,
					this.legTTEstFactory,
					this.routingAlgo,
					this.controler.getNetwork(),
					numToggleGenes,
					numDurationGenes,
					this.sampleClique.getMembers().size(),
					this.controler.getScoringFunctionFactory());

		IChromosome sampleChrom = createRandomChromosome();
		
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
	// FIXME: linked to one encoding, but ran for each
	public void testOTFDeterministic() throws Exception {
		int nChrom = 10;
		int nTries = 10;

		JointPlanOptimizerOTFFitnessFunction otf = new JointPlanOptimizerOTFFitnessFunction(
					this.samplePlan,
					this.configGroup,
					this.legTTEstFactory,
					this.routingAlgo,
					this.controler.getNetwork(),
					numToggleGenes,
					numDurationGenes,
					this.sampleClique.getMembers().size(),
					this.controler.getScoringFunctionFactory());

		IChromosome[] sampleChrom = new IChromosome[nChrom];
		double[] score = new double[nChrom];
		
		// first try each chromosome
		for (int i=0; i < nChrom; i++) {
			sampleChrom[i] = createRandomChromosome();

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
	 * Tests the consistency of the partial decoder. More precisely,
	 * the test fails if the partial decoder launched on a whole clique
	 * does not produce the same output at the standard decoder.
	 */
	@Test
	@Ignore
	public void testPartialDecoder() throws Exception {
		int nTrys = 10;

		DurationDecoderPartial partial = new DurationDecoderPartial(
					this.samplePlan,
					new ArrayList<Id>(this.sampleClique.getMembers().keySet()),
					this.configGroup,
					this.legTTEstFactory,
					this.routingAlgo,
					this.controler.getNetwork(),
					numToggleGenes,
					numDurationGenes,
					this.sampleClique.getMembers().size());

		DurationDecoder classical = new DurationDecoder(
					this.samplePlan,
					this.configGroup,
					this.legTTEstFactory,
					this.routingAlgo,
					this.controler.getNetwork(),
					numToggleGenes,
					numDurationGenes,
					this.sampleClique.getMembers().size());

		IChromosome sampleChrom;

		for (int tryCount=0; tryCount < nTrys; tryCount++) {
			sampleChrom = createRandomChromosome();

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
	public void testDurationDecoderEndTime() throws Exception {
		JointPlanOptimizerDimensionDecoder classical = semanticsFactory.createDurationDecoder(
					this.samplePlan,
					this.configGroup,
					this.legTTEstFactory,
					this.routingAlgo,
					this.controler.getNetwork(),
					numToggleGenes,
					numDurationGenes);

		testDurationDecoderEndTime(classical);
	}

	//@Test
	//public void testDurationDecoderPartialEndTime() throws Exception {
	//	DurationDecoderPartial partial = new DurationDecoderPartial(
	//				this.samplePlan,
	//				new ArrayList<Id>(this.sampleClique.getMembers().keySet()),
	//				this.configGroup,
	//				this.legTTEstFactory,
	//				this.routingAlgo,
	//				this.controler.getNetwork(),
	//				numToggleGenes,
	//				numDurationGenes,
	//				this.sampleClique.getMembers().size());

	//	testDurationDecoderEndTime(partial);
	//}

	private void testDurationDecoderEndTime(final JointPlanOptimizerDimensionDecoder decoder) throws Exception {
		int nTrys = 100;

		//double expectedEndTime = 3600d*24;

		IChromosome sampleChrom;

		boolean plansActuallyTested = false;
		for (int tryCount=0; tryCount < nTrys; tryCount++) {
			sampleChrom = createRandomChromosome();

			Collection<Plan> individualPlans = decoder.decode(sampleChrom, this.samplePlan).getIndividualPlans().values();

			for (Plan indivPlan : individualPlans) {
				List<PlanElement> pes = indivPlan.getPlanElements();
				double firstActEnd = ((Activity) pes.get( 0 )).getEndTime();
				int anteLastActIndex = pes.size() - 3;
				Activity anteLastAct = (Activity) pes.get( anteLastActIndex );
				while (anteLastAct.getType().equals( JointActingTypes.DROP_OFF ) || anteLastAct.getType().equals( JointActingTypes.PICK_UP )) {
					// we are in fact in a trip
					anteLastActIndex -= 2;
					anteLastAct = (Activity) pes.get( anteLastActIndex );
				}
				double lastEpisodeStart = anteLastAct.getEndTime();

				if ( anteLastAct.getEndTime() - anteLastAct.getStartTime() > DurationDecoder.MIN_DURATION ) {
					// due to travel time, decoded plans finishing too late are possible.
					// such plans would have a last activity duration equal to the mininal
					// allowed one
					plansActuallyTested = true;
					Assert.assertTrue(
							"plan of incorrect duration created by "+decoder.getClass().getSimpleName()
							+": first act ends at "+Time.writeTime( firstActEnd )+
							" and last act starts at "+Time.writeTime( lastEpisodeStart )
							+" ("+Time.writeTime( lastEpisodeStart - 24 * 3600 )+" the next day)",
							firstActEnd + 24 * 3600 > lastEpisodeStart);
				}
			}
		}

		if (!plansActuallyTested) {
			throw new RuntimeException( "no plan was actually tested!" );
		}
	}

	@Test
	public void testRandomInitialChromosome() throws Exception {
		int nTrys = 10;
		IChromosome sampleChrom;

		for (int tryCount=0; tryCount < nTrys; tryCount++) {
			sampleChrom = createRandomChromosome();

			for (Gene gene : sampleChrom.getGenes()) {
				Assert.assertNotNull(
						"got a null allele in random initial chromosome for gene "+gene.getClass(),
						gene.getAllele());
			}

			Assert.assertTrue(
					"radom initial chromosome does not respects constraints!",
					jgapConf.getConstraintsManager().respectsConstraints( sampleChrom ));
		}
	}

	@Test
	public void testTimeLine() throws Exception {
		JointPlanOptimizerDimensionDecoder durationDecoder = semanticsFactory.createDurationDecoder(
					this.samplePlan,
					this.configGroup,
					this.legTTEstFactory,
					this.routingAlgo,
					this.controler.getNetwork(),
					numToggleGenes,
					numDurationGenes);

		testTimeLine( durationDecoder );
	}

	//@Test
	//public void testTimeLinePartial() throws Exception {
	//	DurationDecoderPartial partial = new DurationDecoderPartial(
	//				this.samplePlan,
	//				new ArrayList<Id>(this.sampleClique.getMembers().keySet()),
	//				this.configGroup,
	//				this.legTTEstFactory,
	//				this.routingAlgo,
	//				this.controler.getNetwork(),
	//				numToggleGenes,
	//				numDurationGenes,
	//				this.sampleClique.getMembers().size());

	//	testTimeLine(partial);
	//}

	private void testTimeLine(final JointPlanOptimizerDimensionDecoder decoder) throws Exception {
		int nTrys = 10;

		IChromosome sampleChrom;
		double oldNow=0;
		double now =0;

		for (int tryCount=0; tryCount < nTrys; tryCount++) {
			sampleChrom = createRandomChromosome();

			Collection<Plan> indivPlans = decoder.decode(sampleChrom, (this.samplePlan)).getIndividualPlans().values();

			for (Plan indivPlan : indivPlans) {
				boolean hadUndefined = false;
				oldNow = 0d;
				for (PlanElement pe : indivPlan.getPlanElements()) {
					Assert.assertFalse(
							"Got an activity with undefined end time which was not the last element",
							hadUndefined);

					if (pe instanceof Activity) {
						now = ((Activity) pe).getEndTime();

						if (now == Time.UNDEFINED_TIME) {
							hadUndefined = true;
							continue;
						}
						
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
	 * correspond to the scores obtained by decoding them and than scoring
	 * them with the MATSim scoring function.
	 */
	@Test
	public void testScoresConsistency() throws Exception {
		JointPlanOptimizerDecoder decoder = jgapConf.getDecoder();

		Genotype gaPopulation = Genotype.randomInitialGenotype( jgapConf );

		gaPopulation.evolve(2);

		// notify end
		jgapConf.finish();

		ScoringFunctionFactory scoringFunctionFactory = controler.getScoringFunctionFactory();
		double bestScore = Double.NEGATIVE_INFINITY;

		for ( Object elem : gaPopulation.getPopulation().getChromosomes() ) {
			IChromosome chrom = (IChromosome) elem;
			double score = chrom.getFitnessValue();
			JointPlan plan = decoder.decode( chrom );
			bestScore = score > bestScore ? score : bestScore;

			//TODO: score with MATSim scoring function
			for (Plan individualPlan : plan.getIndividualPlans().values()) {
				ScoringFunction scoringFunction =
					scoringFunctionFactory.createNewScoringFunction( individualPlan );

				scoringFunction.reset();
				for (PlanElement pe : individualPlan.getPlanElements()) {
					if (pe instanceof Activity) {
						scoringFunction.handleActivity( (Activity) pe );
					}
					else if (pe instanceof Leg) {
						scoringFunction.handleLeg( (Leg) pe );
					}
				}
				scoringFunction.finish();
				individualPlan.setScore( scoringFunction.getScore() );
			}

			Assert.assertEquals(
					"score returned by chromosome do not correspond to decoded score",
					plan.getScore(), score, MatsimTestUtils.EPSILON);
		}

		Assert.assertEquals(
				"score of the best chromosome returned by genotype does not corresponds to the best score found",
				bestScore,
				gaPopulation.getFittestChromosome().getFitnessValue(),
				MatsimTestUtils.EPSILON);

	}

	/////////////////////////////////// operators //////////////////////////////
	/**
	 * Checks whether the offspring of the cross-overs respect the constraints.
	 */
	@Test
	public void testWholeCOConstraints() throws Exception {
		this.configGroup.setWholeCrossOverProbability(P_CO);
		this.configGroup.setSingleCrossOverProbability(0);
		this.configGroup.setSimpleCrossOverProbability(0);

		JointPlanOptimizerJGAPCrossOver crossOver =
			new JointPlanOptimizerJGAPCrossOver(
					this.jgapConf,
					this.configGroup,
					this.jgapConf.getConstraintsManager());

		List<IChromosome> offsprings = new ArrayList<IChromosome>();
		Population jgapPop = createSampleGeneticPopulation();
		testGeneticOperatorConstraints(crossOver, jgapPop, offsprings);
	}

	/**
	 * Checks whether the offspring of the cross-overs respect the constraints.
	 */
	@Test
	public void testSimpleCOConstraints() throws Exception {
		this.configGroup.setWholeCrossOverProbability(0);
		this.configGroup.setSingleCrossOverProbability(0);
		this.configGroup.setSimpleCrossOverProbability(P_CO);

		JointPlanOptimizerJGAPCrossOver crossOver =
			new JointPlanOptimizerJGAPCrossOver(
					this.jgapConf,
					this.configGroup,
					this.jgapConf.getConstraintsManager());

		List<IChromosome> offsprings = new ArrayList<IChromosome>();
		Population jgapPop = createSampleGeneticPopulation();
		testGeneticOperatorConstraints(crossOver, jgapPop, offsprings);
	}

	/**
	 * Checks whether the offspring of the cross-overs respect the constraints.
	 */
	@Test
	public void testSingleCOConstraints() throws Exception {
		this.configGroup.setWholeCrossOverProbability(0);
		this.configGroup.setSingleCrossOverProbability(P_CO);
		this.configGroup.setSimpleCrossOverProbability(0);

		JointPlanOptimizerJGAPCrossOver crossOver =
			new JointPlanOptimizerJGAPCrossOver(
					this.jgapConf,
					this.configGroup,
					this.jgapConf.getConstraintsManager());

		List<IChromosome> offsprings = new ArrayList<IChromosome>();
		Population jgapPop = createSampleGeneticPopulation();
		testGeneticOperatorConstraints(crossOver, jgapPop, offsprings);
	}

	@Test
	public void testSideEffectsCO() throws Exception {
		this.configGroup.setWholeCrossOverProbability(P_CO);
		this.configGroup.setSingleCrossOverProbability(P_CO);
		this.configGroup.setSimpleCrossOverProbability(P_CO);

		JointPlanOptimizerJGAPCrossOver crossOver =
			new JointPlanOptimizerJGAPCrossOver(
					this.jgapConf,
					this.configGroup,
					this.jgapConf.getConstraintsManager());

		List<IChromosome> offsprings = new ArrayList<IChromosome>();
		Population jgapPop = createSampleGeneticPopulation();
		testGeneticOperatorSideEffects(crossOver, jgapPop, offsprings);	
	}

	/**
	 * Checks whether the offspring of the cross-overs respect the constraints.
	 */
	@Test
	public void testUniformMutationConstraints() throws Exception {
		this.configGroup.setInPlaceMutation(false);
		this.configGroup.setMutationProbability(P_MUT);
		this.configGroup.setNonUniformMutationProbability( 0 );

		JointPlanOptimizerJGAPMutation mutation =
			new JointPlanOptimizerJGAPMutation(
					this.jgapConf,
					this.configGroup,
					this.jgapConf.getConstraintsManager());

		List<IChromosome> offsprings = new ArrayList<IChromosome>();
		Population jgapPop = createSampleGeneticPopulation();
		testGeneticOperatorConstraints(mutation, jgapPop, offsprings);
	}

	/**
	 * Checks whether the offspring of the cross-overs respect the constraints.
	 */
	@Test
	public void testNonUniformMutationConstraints() throws Exception {
		this.configGroup.setInPlaceMutation(false);
		this.configGroup.setMutationProbability(P_MUT);
		this.configGroup.setNonUniformMutationProbability( 1 );

		JointPlanOptimizerJGAPMutation mutation =
			new JointPlanOptimizerJGAPMutation(
					this.jgapConf,
					this.configGroup,
					this.jgapConf.getConstraintsManager());

		List<IChromosome> offsprings = new ArrayList<IChromosome>();
		Population jgapPop = createSampleGeneticPopulation();
		testGeneticOperatorConstraints(mutation, jgapPop, offsprings);
	}

	@Test
	public void testMutationSideEffects() throws Exception {
		this.configGroup.setInPlaceMutation(false);
		this.configGroup.setMutationProbability(P_MUT);

		JointPlanOptimizerJGAPMutation mutation =
			new JointPlanOptimizerJGAPMutation(
					this.jgapConf,
					this.configGroup,
					this.jgapConf.getConstraintsManager());

		List<IChromosome> offsprings = new ArrayList<IChromosome>();
		Population jgapPop = createSampleGeneticPopulation();
		testGeneticOperatorSideEffects(mutation, jgapPop, offsprings);	
	}

	@Test
	public void testMutationInPlaceConstraints() throws Exception {
		this.configGroup.setInPlaceMutation(true);
		this.configGroup.setMutationProbability(P_MUT);

		JointPlanOptimizerJGAPMutation mutation =
			new JointPlanOptimizerJGAPMutation(
					this.jgapConf,
					this.configGroup,
					this.jgapConf.getConstraintsManager());

		List<IChromosome> offsprings = new ArrayList<IChromosome>();
		Population jgapPop = createSampleGeneticPopulation();

		// the mutation "in place" acts on the "candidate solutions":
		// generate some.
		for (Object chrom : jgapPop.getChromosomes()) {
			offsprings.add( (IChromosome) ((IChromosome) chrom).clone() );
		}

		testGeneticOperatorConstraints(mutation, jgapPop, offsprings);	
	}

	@Test
	public void testMutationInPlaceSideEffects() throws Exception {
		this.configGroup.setInPlaceMutation(true);
		this.configGroup.setMutationProbability(P_MUT);

		JointPlanOptimizerJGAPMutation mutation =
			new JointPlanOptimizerJGAPMutation(
					this.jgapConf,
					this.configGroup,
					this.jgapConf.getConstraintsManager());

		List<IChromosome> offsprings = new ArrayList<IChromosome>();
		Population jgapPop = createSampleGeneticPopulation();

		// the mutation "in place" acts on the "candidate solutions":
		// generate some. Moreover, mimic the behaviour during the iterations
		// (the first chromosomes are the chromosomes of the previous gen)
		for (Object chrom : jgapPop.getChromosomes()) {
			offsprings.add( (IChromosome) chrom );
		}
		for (Object chrom : jgapPop.getChromosomes()) {
			offsprings.add( (IChromosome) ((IChromosome) chrom).clone() );
		}

		testGeneticOperatorSideEffects(mutation, jgapPop, offsprings);	
	}

	/////////////////////////////// constraint manager /////////////////////////
	@Test
	public void testSimpleCoef() throws Exception {
		int nTrys = 1000;
		ConstraintsManager manager = jgapConf.getConstraintsManager();

		Population jgapPop = createSampleGeneticPopulation();

		RandomGenerator rand = jgapConf.getRandomGenerator();
		int popSize = jgapPop.size();
		for (int count = 0; count < nTrys; count++) {
			IChromosome chrom1 = jgapPop.getChromosome( rand.nextInt(popSize) );
			IChromosome chrom2 = jgapPop.getChromosome( rand.nextInt(popSize) );

			double coef =
				manager.getSimpleCrossOverCoef(
						chrom1,
						chrom2,
						rand.nextInt( chrom1.size() ) );
			checkCoef( coef );
		}
	}

	@Test
	public void testSingleCoefLowerBound() throws Exception {
		int nTrys = 1000;
		ConstraintsManager manager = jgapConf.getConstraintsManager();

		Population jgapPop = createSampleGeneticPopulation();

		RandomGenerator rand = jgapConf.getRandomGenerator();
		int popSize = jgapPop.size();
		for (int count = 0; count < nTrys; count++) {
			IChromosome chrom1 = jgapPop.getChromosome( rand.nextInt(popSize) );
			IChromosome chrom2 = jgapPop.getChromosome( rand.nextInt(popSize) );

			int crossingPoint = -1;
			
			do {
				crossingPoint = rand.nextInt( chrom1.size() );
			} while ( !(chrom1.getGene( crossingPoint ) instanceof DoubleGene) );

			Tuple<Double, Double> coef =
				manager.getSingleCrossOverCoefInterval(
						chrom1,
						chrom2,
						crossingPoint );
			checkCoef( coef.getFirst() );
		}
	}

	@Test
	public void testSingleCoefUpperBound() throws Exception {
		int nTrys = 1000;
		ConstraintsManager manager = jgapConf.getConstraintsManager();

		Population jgapPop = createSampleGeneticPopulation();

		RandomGenerator rand = jgapConf.getRandomGenerator();
		int popSize = jgapPop.size();
		for (int count = 0; count < nTrys; count++) {
			IChromosome chrom1 = jgapPop.getChromosome( rand.nextInt(popSize) );
			IChromosome chrom2 = jgapPop.getChromosome( rand.nextInt(popSize) );

			int crossingPoint = -1;
			
			do {
				crossingPoint = rand.nextInt( chrom1.size() );
			} while ( !(chrom1.getGene( crossingPoint ) instanceof DoubleGene) );

			Tuple<Double, Double> coef =
				manager.getSingleCrossOverCoefInterval(
						chrom1,
						chrom2,
						crossingPoint );
			checkCoef( coef.getFirst() + coef.getSecond() );
		}
	}

	/**
	 * performs basic test of a CO coef (ie that it lies in [0, 1]
	 */
	private static void checkCoef(final double coef) {
		Assert.assertTrue(
				"got negative coef "+coef,
				coef > -MatsimTestUtils.EPSILON);

		Assert.assertTrue(
				"got too large coef "+coef,
				coef < 1 + MatsimTestUtils.EPSILON);
	}

	/////////////////////////////// chromosome /////////////////////////////////
	/**
	 * Tests whether the randomly generated chromosomes respect the constraints.
	 */
	@Test
	public void testConstraintsRandomChromosome() throws Exception {
		int nTrys = 1000;
		jgapConf.setPopulationSize( nTrys );
		Genotype genotype = Genotype.randomInitialGenotype( jgapConf );
		Population population = genotype.getPopulation();

		ConstraintsManager constr = jgapConf.getConstraintsManager();

		int countTrys = 0;
		List<Object> examined = new ArrayList<Object>();
		for (Object chromosome : population.getChromosomes()) {
			Assert.assertTrue(
					"random initial genotype does not respects constraints!",
					constr.respectsConstraints( (IChromosome) chromosome ));

			Assert.assertFalse(
					"random initial genotype produced identical chromosomes!",
					examined.contains( chromosome ));

			examined.add( chromosome );
			countTrys++;
		}

		Assert.assertEquals(
				"unexpected number of chromosomes examined",
				nTrys,
				countTrys);
	}

	@Test
	public void testRandomnessRandomInitialGenotype() throws Exception {
		Genotype genotype = Genotype.randomInitialGenotype( jgapConf );

		// approach: compare each gene of the first chromosome with the
		// corresponding genes in the other chromosomes. As soon as a gene
		// with a different value is found, set the gene in the first chromosome
		// to null. At the end, all genes must be null.
		Gene[] firstGenes = genotype.getPopulation().getChromosome( 0 ).getGenes();

		for (Object chromosome : genotype.getPopulation().getChromosomes()) {
			Gene[] currentGenes = ((IChromosome) chromosome).getGenes();

			for (int i = 0; i < firstGenes.length; i++) {
				if ( firstGenes[i] != null && !currentGenes[i].equals( firstGenes[i] ) ) {
					firstGenes[i] = null;
				}
			}
		}

		for (Gene gene : firstGenes) {
			Assert.assertNull(
					"found a mono-value gene of type "+( gene == null ? "null" : gene.getClass() ),
					gene);
		}
	}

	@Test
	public void testFitnessChromosome() throws Exception {
		JointPlanOptimizerJGAPChromosome chrom = (JointPlanOptimizerJGAPChromosome)
			((JointPlanOptimizerJGAPChromosome) jgapConf.getSampleChromosome()).createRandomChromosome();

		Assert.assertEquals(
				"newly created chromosome has unexpected fitness!",
				JointPlanOptimizerFitnessFunction.NO_FITNESS_VALUE,
				chrom.getFitnessValueDirectly(),
				MatsimTestUtils.EPSILON);

		Assert.assertFalse(
				"getFitnessValue gives undefined fitness!",
				JointPlanOptimizerFitnessFunction.NO_FITNESS_VALUE == chrom.getFitnessValue());

		double testFit = 123;
		chrom.setFitnessValue( testFit );

		Assert.assertEquals(
				"fitness badly set!",
				testFit,
				chrom.getFitnessValue(),
				MatsimTestUtils.EPSILON);
	}

	@Test
	public void testCloneChromosomeGlobal() throws Exception {
		int nTrys = 100;

		for (int i = 0; i < nTrys; i++) {
			IChromosome chrom = createRandomChromosome();
			int clonedHashCode = chrom.hashCode();
			IChromosome clone = (IChromosome) chrom.clone();
			int cloneHashCode = clone.hashCode();

			Assert.assertNotSame(
					"cloning chromosome just copies the reference!",
					chrom,
					clone);

			Assert.assertEquals(
					"clone and cloned are not equal!",
					chrom,
					clone);

			Assert.assertEquals(
						"clone and cloned have different hashCodes!",
						clonedHashCode,
						cloneHashCode);

			for (Gene gene : clone.getGenes()) {
				gene.setToRandomValue( jgapConf.getRandomGenerator() );
			}

			Assert.assertFalse(
					"clone and cloned are equal after mutation!",
					chrom.equals(clone));

			Assert.assertEquals(
						"cloned hashCode changed with mutation of clone!",
						clonedHashCode,
						chrom.hashCode() );

			Assert.assertFalse(
					"clone's hashcode did not change after mutation!",
					cloneHashCode == clone.hashCode() );

		}
	}

	@Test
	public void testCloneChromosomeGeneLevel() throws Exception {
		int nTrys = 1;

		for (int trycount = 0; trycount < nTrys; trycount++) {
			IChromosome chrom = createRandomChromosome();
			IChromosome clone = (IChromosome) chrom.clone();

			RandomGenerator rand = jgapConf.getRandomGenerator();
			for (int i = 0; i < chrom.size(); i++) {
				int chromHashCode = chrom.getGene(i).hashCode();

				Gene cloneGene = clone.getGene(i);

				do {
					cloneGene.setToRandomValue( rand );
				} while (cloneGene.hashCode() == chromHashCode);

				Assert.assertEquals(
						"modifying a gene in clone modified corresponding gene in cloned!",
						chromHashCode,
						chrom.getGene(i).hashCode());
			}
		}	
	}

	// /////////////////////////////////////////////////////////////////////////
	// helpers
	// /////////////////////////////////////////////////////////////////////////
	private void testGeneticOperatorConstraints(
			final GeneticOperator genOperator,
			final Population pop,
			final List<IChromosome> offsprings) {
		ConstraintsManager constr = jgapConf.getConstraintsManager();

		for (Object chrom : pop.getChromosomes()) {
			// just to be sure (executing the operators on a population not respecting
			// the costriants will lead to offspring not respecting the constraints.
			Assert.assertTrue(
					"population does not respects the constraints!",
					constr.respectsConstraints( (IChromosome) chrom ));
		}

		genOperator.operate(pop, offsprings);

		for (IChromosome chrom : offsprings) {
			Assert.assertTrue(
					genOperator.getClass().getSimpleName()+" returned offspring not"+
						" respecting the constraints.",
					constr.respectsConstraints( chrom ));
		}
	}

	private void testGeneticOperatorSideEffects(
			final GeneticOperator genOperator,
			final Population pop,
			final List<IChromosome> offsprings) {
		Population previousPop = (Population) pop.clone();

		genOperator.operate(pop, offsprings);

		Assert.assertEquals(
				"applying the operator "+genOperator.getClass()+" modified the previous generation!",
				previousPop,
				pop);
	}

	private Population createSampleGeneticPopulation() throws Exception {
		IChromosome[] chromosomes = new IChromosome[jgapConf.getPopulationSize()];

		ConstraintsManager constr = jgapConf.getConstraintsManager();

		for (int i=0; i < chromosomes.length; i++) {
			// create "full" chromosomes so that the propability of creating
			// invalid chromosomes is high in the case the constraints are not
			// well considered in the operators.
			chromosomes[i] = createRandomChromosome();

			if (constr instanceof UpperBoundsConstraintsManager) {
				((UpperBoundsConstraintsManager) constr).fillChromosome( chromosomes[i] );
			}
		}

		return new Population(this.jgapConf, chromosomes);
	}

	private IChromosome createRandomChromosome() throws Exception {
		return (IChromosome) ((IInitializer) jgapConf.getSampleChromosome()).perform( null , null , null );
	}
}

/**
 * Provides factories for the elements to test for a particular encoding
 */
interface SemanticsFactory {
	public JointPlanOptimizerSemanticsBuilder createSemantics(
				JointReplanningConfigGroup configGroup,
				ScoringFunctionFactory scoringFunctionFactory,
				JointPlanOptimizerLegTravelTimeEstimatorFactory legTTEstFactory,
				PlansCalcRoute routingAlgo,
				Network network);

	public JointPlanOptimizerDimensionDecoder createDurationDecoder(
			JointPlan plan,
			JointReplanningConfigGroup configGroup,
			JointPlanOptimizerLegTravelTimeEstimatorFactory legTTEstFactory,
			PlansCalcRoute routingAlgo,
			Network network,
			int numToggleGenes,
			int numDurationGenes);
}

class EndsSemanticsFactory implements SemanticsFactory {
	@Override
	public JointPlanOptimizerSemanticsBuilder createSemantics(
			final JointReplanningConfigGroup configGroup,
			final ScoringFunctionFactory scoringFunctionFactory,
			final JointPlanOptimizerLegTravelTimeEstimatorFactory legTTEstFactory,
			final PlansCalcRoute routingAlgo,
			final Network network) {
		return new JointPlanOptimizerActivityEndsEncodingSemanticsBuilder(
				configGroup,
				scoringFunctionFactory,
				legTTEstFactory,
				routingAlgo,
				network);
	}

	@Override
	public JointPlanOptimizerDimensionDecoder createDurationDecoder(
			final JointPlan plan,
			final JointReplanningConfigGroup configGroup,
			final JointPlanOptimizerLegTravelTimeEstimatorFactory legTTEstFactory,
			final PlansCalcRoute routingAlgo,
			final Network network,
			final int numToggleGenes,
			final int numDurationGenes) {
		return new DurationDecoderActivityEndsEncoding(
					plan,
					configGroup,
					legTTEstFactory,
					routingAlgo,
					network,
					numToggleGenes,
					new TreeSet<Id>(plan.getIndividualPlans().keySet()));
	}
}

class DurationsSemanticsFactory implements SemanticsFactory {
	@Override
	public JointPlanOptimizerSemanticsBuilder createSemantics(
			final JointReplanningConfigGroup configGroup,
			final ScoringFunctionFactory scoringFunctionFactory,
			final JointPlanOptimizerLegTravelTimeEstimatorFactory legTTEstFactory,
			final PlansCalcRoute routingAlgo,
			final Network network) {
		return new JointPlanOptimizerActivityDurationEncodingSemanticsBuilder(
				configGroup,
				scoringFunctionFactory,
				legTTEstFactory,
				routingAlgo,
				network);
	}

	@Override
	public JointPlanOptimizerDimensionDecoder createDurationDecoder(
			final JointPlan plan,
			final JointReplanningConfigGroup configGroup,
			final JointPlanOptimizerLegTravelTimeEstimatorFactory legTTEstFactory,
			final PlansCalcRoute routingAlgo,
			final Network network,
			final int numToggleGenes,
			final int numDurationGenes) {
		return new DurationDecoder(
					plan,
					configGroup,
					legTTEstFactory,
					routingAlgo,
					network,
					numToggleGenes,
					numDurationGenes,
					plan.getClique().getMembers().size());
	}
}
