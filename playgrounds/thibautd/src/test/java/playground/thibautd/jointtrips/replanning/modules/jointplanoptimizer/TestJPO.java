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
import java.util.Collection;
import java.util.List;

import org.jgap.Gene;
import org.jgap.GeneticOperator;
import org.jgap.Genotype;
import org.jgap.IChromosome;
import org.jgap.IInitializer;
import org.jgap.InvalidConfigurationException;
import org.jgap.Population;
import org.jgap.impl.BooleanGene;
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
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.core.utils.misc.Time;
import org.matsim.planomat.costestimators.DepartureDelayAverageCalculator;
import org.matsim.testcases.MatsimTestUtils;

import playground.thibautd.jointtrips.config.JointReplanningConfigGroup;
import playground.thibautd.jointtrips.population.Clique;
import playground.thibautd.jointtrips.population.JointActingTypes;
import playground.thibautd.jointtrips.population.JointLeg;
import playground.thibautd.jointtrips.population.JointPlan;
import playground.thibautd.jointtrips.population.PopulationWithCliques;
import playground.thibautd.jointtrips.replanning.modules.jointplanoptimizer.geneticoperators.UpperBoundsConstraintsManager;
import playground.thibautd.jointtrips.replanning.modules.jointplanoptimizer.JointPlanOptimizerDecoder;
import playground.thibautd.jointtrips.replanning.modules.jointplanoptimizer.JointPlanOptimizerJGAPChromosome;
import playground.thibautd.jointtrips.replanning.modules.jointplanoptimizer.JointPlanOptimizerJGAPConfiguration;
import playground.thibautd.jointtrips.replanning.modules.jointplanoptimizer.costestimators.JointPlanOptimizerLegTravelTimeEstimatorFactory;
import playground.thibautd.jointtrips.replanning.modules.jointplanoptimizer.fitness.JointPlanOptimizerFitnessFunction;
import playground.thibautd.jointtrips.replanning.modules.jointplanoptimizer.fitness.JointPlanOptimizerOTFFitnessFunction;
import playground.thibautd.jointtrips.replanning.modules.jointplanoptimizer.geneticoperators.JointPlanOptimizerJGAPCrossOver;
import playground.thibautd.jointtrips.replanning.modules.jointplanoptimizer.geneticoperators.JointPlanOptimizerJGAPMutation;
import playground.thibautd.jointtrips.replanning.modules.jointplanoptimizer.pipeddecoder.DurationDecoder;
import playground.thibautd.jointtrips.replanning.modules.jointplanoptimizer.pipeddecoder.DurationDecoderPartial;
import playground.thibautd.jointtrips.replanning.modules.jointplanoptimizer.pipeddecoder.JointPlanOptimizerDimensionDecoder;
import playground.thibautd.jointtrips.run.JointControler;
import playground.thibautd.jointtrips.utils.JointControlerUtils;

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
	public void testFitnesses() throws Exception {
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
	public void testOTFDeterministic() throws Exception {
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
	public void testDurationDecoderClassicalEndTime() throws Exception {
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
	public void testDurationDecoderPartialEndTime() throws Exception {
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
	public void testTimeLineClassical() throws Exception {
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
	public void testTimeLinePartial() throws Exception {
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
	 * correspond to the scores obtained by decoding them, in the non-memetic setting.
	 */
	@Test
	public void testScoresConsistencyGA() throws Exception {
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


		Genotype gaPopulation = Genotype.randomInitialGenotype( jgapConf );

		gaPopulation.evolve(2);

		// notify end
		jgapConf.finish();

		for ( Object elem : gaPopulation.getPopulation().getChromosomes() ) {
			IChromosome chrom = (IChromosome) elem;
			double score = chrom.getFitnessValue();
			double decoded = scorer.getFitnessValue(chrom);

			Assert.assertEquals(
					"score returned by chromosome do not correspond to decoded score",
					decoded, score, MatsimTestUtils.EPSILON);
		}
	}

	/////////////////////////////////// operators //////////////////////////////
	/**
	 * Checks whether the offspring of the cross-overs respect the constraints.
	 */
	@Test
	public void testCOConstraints() throws Exception {
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
		testGeneticOperator(crossOver, jgapPop, offsprings);
	}

	/**
	 * Checks whether the offspring of the cross-overs respect the constraints.
	 */
	@Test
	public void testMutationConstraints() throws Exception {
		this.configGroup.setInPlaceMutation(false);
		this.configGroup.setMutationProbability(P_MUT);

		JointPlanOptimizerJGAPMutation mutation =
			new JointPlanOptimizerJGAPMutation(
					this.jgapConf,
					this.configGroup,
					this.jgapConf.getConstraintsManager());

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

		UpperBoundsConstraintsManager constr = (UpperBoundsConstraintsManager) jgapConf.getConstraintsManager();
		for (IChromosome chrom : offsprings) {
			Assert.assertTrue(
					operator.getClass().getSimpleName()+"returned offspring not"+
						" respecting the constraints.",
					constr.respectsConstraints( chrom ));
		}
	}

	private Population createSampleGeneticPopulation() throws Exception {
		IChromosome[] chromosomes = new IChromosome[N_TEST_CHROM];

		UpperBoundsConstraintsManager constr = (UpperBoundsConstraintsManager) jgapConf.getConstraintsManager();

		for (int i=0; i < chromosomes.length; i++) {
			// create "full" chromosomes so that the propability of creating
			// invalid chromosomes is high in the case the constraints are not
			// well considered in the operators.
			chromosomes[i] = createRandomChromosome();
			constr.fillChromosome( chromosomes[i] );
		}

		return new Population(this.jgapConf, chromosomes);
	}

	private IChromosome createRandomChromosome() throws Exception {
		return (IChromosome) ((IInitializer) jgapConf.getSampleChromosome()).perform( null , null , null );
	}
}

