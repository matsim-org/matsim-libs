/* *********************************************************************** *
 * project: org.matsim.*
 * TestUpperBoundsConstraintsManager.java
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
package playground.thibautd.jointtrips.replanning.modules.jointplanoptimizer.geneticoperators;

import org.jgap.Gene;
import org.jgap.IChromosome;
import org.jgap.impl.DoubleGene;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.planomat.costestimators.DepartureDelayAverageCalculator;
import org.matsim.testcases.MatsimTestUtils;

import playground.thibautd.jointtrips.config.JointReplanningConfigGroup;
import playground.thibautd.jointtrips.population.Clique;
import playground.thibautd.jointtrips.population.JointPlan;
import playground.thibautd.jointtrips.population.PopulationWithCliques;
import playground.thibautd.jointtrips.replanning.modules.jointplanoptimizer.costestimators.JointPlanOptimizerLegTravelTimeEstimatorFactory;
import playground.thibautd.jointtrips.replanning.modules.jointplanoptimizer.JointPlanOptimizerActivityDurationEncodingSemanticsBuilder;
import playground.thibautd.jointtrips.replanning.modules.jointplanoptimizer.JointPlanOptimizerJGAPConfiguration;
import playground.thibautd.jointtrips.run.JointControler;
import playground.thibautd.jointtrips.utils.JointControlerUtils;

/**
 * test class for the UpperBoundsConstraintsManager.
 * @author thibautd
 */
public class TestUpperBoundsConstraintsManager {
	private static final Id idSample = new IdImpl("2593674");

	private JointControler controler;
	//private JointPlanOptimizer algo;
	private JointPlanOptimizerJGAPConfiguration jgapConf;
	private JointReplanningConfigGroup configGroup;
	private JointPlanOptimizerLegTravelTimeEstimatorFactory legTTEstFactory;
	private PlansCalcRoute routingAlgo;
	private JointPlan samplePlan;
	private Clique sampleClique;

	private UpperBoundsConstraintsManager testee;

	@Rule
	public final MatsimTestUtils utils = new MatsimTestUtils();

	// /////////////////////////////////////////////////////////////////////////
	// Fixtures setting
	// /////////////////////////////////////////////////////////////////////////
	@Before
	public void initConf() {
		String inputPath = getParentDirectory(utils.getPackageInputDirectory(), 4);
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
				new JointPlanOptimizerActivityDurationEncodingSemanticsBuilder(
					this.configGroup,
					this.controler.getScoringFunctionFactory(),
					this.legTTEstFactory,
					this.routingAlgo,
					this.controler.getNetwork()),
				outputPath,
				123);

		testee = (UpperBoundsConstraintsManager) jgapConf.getConstraintsManager();
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
	// actual testing
	// /////////////////////////////////////////////////////////////////////////
	@Test
	public void testFillChromosome() {
		for (int i=0; i <= 10 ; i++ ) {
			IChromosome sampleChrom = (IChromosome)
				jgapConf.getSampleChromosome().clone();

			testee.fillChromosome( sampleChrom );

			testValidChromosome( sampleChrom );

			Assert.assertEquals(
					"filling chromosome still lets free space!",
					0d,
					testee.getRemainingFreeSpace( sampleChrom ),
					MatsimTestUtils.EPSILON);
		}
	}

	@Test
	public void testRandomizeChromosome() {
		for (int i=0; i <= 10 ; i++ ) {
			IChromosome sampleChrom = (IChromosome)
				jgapConf.getSampleChromosome().clone();

			testee.randomiseChromosome( sampleChrom );

			testValidChromosome( sampleChrom );

			Assert.assertTrue(
					"randomize chromosome has no free space! free space: "+testee.getRemainingFreeSpace( sampleChrom ),
					testee.getRemainingFreeSpace( sampleChrom ) > MatsimTestUtils.EPSILON);
		}
	}

	@Test
	public void testCheckConstraints() {
		IChromosome sampleChrom = (IChromosome)
			jgapConf.getSampleChromosome().clone();

		testee.fillChromosome( sampleChrom );

		for (Gene gene : sampleChrom.getGenes()) {
			if (gene instanceof DoubleGene) {
				gene.setAllele( ((DoubleGene) gene).getUpperBound() );
			}
		}

		Assert.assertFalse(
				"totally full chromosome respects constraints!",
				testee.respectsConstraints( sampleChrom ));
	}

	// /////////////////////////////////////////////////////////////////////////
	// private particular testing methods, to use in various testcases
	// /////////////////////////////////////////////////////////////////////////
	private void testValidChromosome(final IChromosome chromosome) {
		Assert.assertTrue(
				"chromosome does not respects constraints!",
				testee.respectsConstraints( chromosome ));
	}
}

