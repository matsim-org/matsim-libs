/* *********************************************************************** *
 * project: org.matsim.*
 * CadytsIntegrationTest.java
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

package org.matsim.contrib.cadyts.car;

import cadyts.measurements.SingleLinkMeasurement;
import cadyts.utilities.io.tabularFileParser.TabularFileParser;
import cadyts.utilities.misc.DynamicData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.cadyts.general.CadytsConfigGroup;
import org.matsim.contrib.cadyts.general.CadytsCostOffsetsXMLFileIO;
import org.matsim.contrib.cadyts.general.CadytsPlanChanger;
import org.matsim.contrib.cadyts.general.CadytsScoring;
import org.matsim.contrib.cadyts.utils.CalibrationStatReader;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControllerConfigGroup.MobsimType;
import org.matsim.core.config.groups.ScoringConfigGroup.ActivityParams;
import org.matsim.core.config.groups.ReplanningConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.ControlerDefaultsModule;
import org.matsim.core.controler.ControlerI;
import org.matsim.core.controler.Injector;
import org.matsim.core.controler.NewControlerModule;
import org.matsim.core.controler.corelisteners.ControlerDefaultCoreListenersModule;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.scenario.ScenarioByConfigModule;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.ScoringParameters;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.MatsimCountsReader;
import org.matsim.testcases.MatsimTestUtils;

import jakarta.inject.Inject;
import java.io.IOException;
import java.util.Collections;

import static org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.*;

/**
 * This is a modified copy of CadytsIntegrationTest (which is used for the cadyts pt integration)
 * in order to establish an according test for the cadyts car integration.
 * At this stage all original pt code is still included here, but outcommeted, to make the adaptations
 * from pt to car well traceable in case of any errors.
 */
public class CadytsCarIT {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	final void testInitialization() {
		final String CADYTS_STRATEGY_NAME = "ccc";

		String inputDir = this.utils.getClassInputDirectory();
		String outputDir = this.utils.getOutputDirectory();

		Config config = createTestConfig(inputDir, outputDir);
		config.controller().setLastIteration(0);

		StrategySettings strategySettings = new StrategySettings(Id.create(1, StrategySettings.class));
		strategySettings.setStrategyName(CADYTS_STRATEGY_NAME) ;
		strategySettings.setWeight(1.0) ;
		config.replanning().addStrategySettings(strategySettings);
		CadytsConfigGroup cadytsCar = ConfigUtils.addOrGetModule(config, CadytsConfigGroup.GROUP_NAME, CadytsConfigGroup.class);
//		cadytsCar.addParam("startTime", "04:00:00");
		cadytsCar.setStartTime( 4*3600 );
//		cadytsCar.addParam("endTime", "20:00:00");
		cadytsCar.setEndTime( 20*3600 );
		cadytsCar.addParam("regressionInertia", "0.95");
		cadytsCar.addParam("useBruteForce", "true");
		cadytsCar.addParam("minFlowStddevVehH", "8");
		cadytsCar.addParam("preparatoryIterations", "1");
		cadytsCar.addParam("timeBinSize", "3600");


		com.google.inject.Injector injector = Injector.createInjector(config, new AbstractModule() {
			@Override
			public void install() {
				install(new NewControlerModule());
				install(new ControlerDefaultCoreListenersModule());
				install(AbstractModule.override(Collections.singleton(new ControlerDefaultsModule()), new AbstractModule() {
					@Override
					public void install() {
						bindMobsim().to(DummyMobsim.class);
					}
				}));
				install(new ScenarioByConfigModule());
				install(new CadytsCarModule());
				install(new AbstractModule() {
					@Override
					public void install() {
						addPlanStrategyBinding(CADYTS_STRATEGY_NAME).toProvider(new jakarta.inject.Provider<PlanStrategy>() {
							@Inject Scenario scenario;
							@Inject CadytsContext cadytsContext;
							@Override
							public PlanStrategy get() {
								return new PlanStrategyImpl(new CadytsPlanChanger(scenario, cadytsContext));
							}
						});
					}
				});
			}
		});
		ControlerI controler = injector.getInstance(ControlerI.class);
		controler.run();

		CadytsContext context = injector.getInstance(CadytsContext.class);

		//test calibration settings
		Assertions.assertEquals(true, context.getCalibrator().getBruteForce());
		Assertions.assertEquals(false, context.getCalibrator().getCenterRegression());
		Assertions.assertEquals(Integer.MAX_VALUE, context.getCalibrator().getFreezeIteration());
		Assertions.assertEquals(8.0, context.getCalibrator().getMinStddev(SingleLinkMeasurement.TYPE.FLOW_VEH_H), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(1, context.getCalibrator().getPreparatoryIterations());
		Assertions.assertEquals(0.95, context.getCalibrator().getRegressionInertia(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(1.0, context.getCalibrator().getVarianceScale(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(3600.0, context.getCalibrator().getTimeBinSize_s(), MatsimTestUtils.EPSILON);
	}


	//--------------------------------------------------------------
	@Test
	final void testCalibrationAsScoring() throws IOException {

		final double beta=30. ;
		final int lastIteration = 20 ;

		String inputDir = this.utils.getClassInputDirectory();
		String outputDir = this.utils.getOutputDirectory();

		final Config config = createTestConfig(inputDir, outputDir);

		config.controller().setLastIteration(lastIteration);

		config.scoring().setBrainExpBeta(beta);

		config.replanning().addStrategySettings( new StrategySettings().setStrategyName( DefaultSelector.ChangeExpBeta ).setWeight( 1.0 ) );

		// ===

		com.google.inject.Injector injector = Injector.createInjector(config, new AbstractModule() {
			@Override
			public void install() {
				install(new NewControlerModule());
				install(new ControlerDefaultCoreListenersModule());
				install(AbstractModule.override(Collections.singleton(new ControlerDefaultsModule()), new AbstractModule() {
					@Override
					public void install() {
						bindScoringFunctionFactory().toInstance(new ScoringFunctionFactory() {
							@Inject private ScoringParametersForPerson parameters;
							@Inject private Network network;
							@Inject CadytsContext cadytsContext;
							@Override
							public ScoringFunction createNewScoringFunction(Person person) {
								final ScoringParameters params = parameters.getScoringParameters(person);

								SumScoringFunction scoringFunctionAccumulator = new SumScoringFunction();
								scoringFunctionAccumulator.addScoringFunction(new CharyparNagelLegScoring(params, network, config.transit().getTransitModes()));
								scoringFunctionAccumulator.addScoringFunction(new CharyparNagelActivityScoring(params)) ;
								scoringFunctionAccumulator.addScoringFunction(new CharyparNagelAgentStuckScoring(params));

								final CadytsScoring<Link> scoringFunction = new CadytsScoring<>(person.getSelectedPlan(), config, cadytsContext);
								final double cadytsScoringWeight = beta*30.;
								scoringFunction.setWeightOfCadytsCorrection(cadytsScoringWeight) ;
								scoringFunctionAccumulator.addScoringFunction(scoringFunction );

								return scoringFunctionAccumulator;
							}
						});
					}
				}));
				install(new ScenarioByConfigModule());
				install(new CadytsCarModule());
			}
		});
		ControlerI controler = injector.getInstance(ControlerI.class);
		controler.run();

		//scenario data  test
		Scenario scenario = injector.getInstance(Scenario.class);
		Assertions.assertEquals(scenario.getNetwork().getLinks().size() , 23, "Different number of links in network." );
		Assertions.assertEquals(scenario.getNetwork().getNodes().size() , 15, "Different number of nodes in network." );

		Assertions.assertNotNull(scenario.getPopulation(), "Population is null.");

		Assertions.assertEquals(scenario.getPopulation().getPersons().size(), 5, "Num. of persons in population is wrong.");
		Assertions.assertEquals(scenario.getConfig().counts().getCountsScaleFactor(), 1.0, MatsimTestUtils.EPSILON, "Scale factor is wrong.");

		//counts
		Assertions.assertEquals(scenario.getConfig().counts().getCountsFileName(), inputDir + "counts5.xml", "Count file is wrong.");


		Counts<Link> occupCounts = new Counts<>();
		new MatsimCountsReader(occupCounts).readFile(scenario.getConfig().counts().getCountsFileName());

		Count<Link> count =  occupCounts.getCount(Id.create(19, Link.class));
		Assertions.assertEquals(occupCounts.getDescription(), "counts values for equil net", "Occupancy counts description is wrong");
		Assertions.assertEquals(count.getCsLabel() , "link_19", "CsId is wrong.");
		Assertions.assertEquals(count.getVolume(7).getValue(), 5.0 , MatsimTestUtils.EPSILON, "Volume of hour 6 is wrong");
		Assertions.assertEquals(count.getMaxVolume().getValue(), 5.0 , MatsimTestUtils.EPSILON, "Max count volume is wrong.");

		String outCounts = outputDir + "ITERS/it." + lastIteration + "/" + lastIteration + ".countscompare.txt";
		AdHocCountsReaderCar reader = new AdHocCountsReaderCar(outCounts);

		Id<Link> locId11 = Id.create( 11, Link.class );
		{
			double[] simValues = reader.getSimulatedValues( locId11 );
			double[] realValues = reader.getRealValues( locId11 );
			Assertions.assertEquals( 0.0, simValues[6], MatsimTestUtils.EPSILON, "Volume of hour 6 is wrong" );
			Assertions.assertEquals( 0.0, realValues[6], MatsimTestUtils.EPSILON, "Volume of hour 6 is wrong" );
		}
		{
			Id<Link> locId12 = Id.create( "12", Link.class );
			double[] simValues = reader.getSimulatedValues( locId12 );
			double[] realValues = reader.getRealValues( locId12 );
			Assertions.assertEquals( 0.0, simValues[6], MatsimTestUtils.EPSILON, "Volume of hour 6 is wrong" );
			Assertions.assertEquals( 0.0, realValues[6], MatsimTestUtils.EPSILON, "Volume of hour 6 is wrong" );
		}
		Id<Link> locId19 = Id.create( "19", Link.class );
		{
			double[] simValues = reader.getSimulatedValues( locId19 );
			double[] realValues = reader.getRealValues( locId19 );
			Assertions.assertEquals( 5.0, simValues[6], MatsimTestUtils.EPSILON, "Volume of hour 6 is wrong" );
			Assertions.assertEquals( 5.0, realValues[6], MatsimTestUtils.EPSILON, "Volume of hour 6 is wrong" );
		}
		{
			Id<Link> locId21 = Id.create( "21", Link.class );
			double[] simValues = reader.getSimulatedValues( locId21 );
			double[] realValues = reader.getRealValues( locId21 );
			Assertions.assertEquals( 5.0, simValues[6], MatsimTestUtils.EPSILON, "Volume of hour 6 is wrong" );
			Assertions.assertEquals( 5.0, realValues[6], MatsimTestUtils.EPSILON, "Volume of hour 6 is wrong" );
		}
		// test calibration statistics
		String testCalibStatPath = outputDir + "calibration-stats.txt";
		CalibrationStatReader calibrationStatReader = new CalibrationStatReader();
		new TabularFileParser().parse(testCalibStatPath, calibrationStatReader);

		CalibrationStatReader.StatisticsData outStatData= calibrationStatReader.getCalStatMap().get(lastIteration);
		// Assert.assertEquals("different Count_ll", "-0.046875", outStatData.getCount_ll() );
		// Assert.assertEquals("different Count_ll_pred_err",  "0.01836234363152515" , outStatData.getCount_ll_pred_err() );
//			Assert.assertEquals("different Link_lambda_avg", "-2.2604922388914356E-10", outStatData.getLink_lambda_avg() );
		Assertions.assertEquals("3.2261421242498865E-5", outStatData.getLink_lambda_avg(), "different Link_lambda_avg" );
//			Assert.assertEquals("different Link_lambda_max", "0.0" , outStatData.getLink_lambda_max() );
//			Assert.assertEquals("different Link_lambda_min", "-7.233575164452593E-9", outStatData.getLink_lambda_min() );
//			Assert.assertEquals("different Link_lambda_stddev", "1.261054219517188E-9", outStatData.getLink_lambda_stddev());
//			Assert.assertEquals("different P2p_ll", "--" , outStatData.getP2p_ll());
//			Assert.assertEquals("different Plan_lambda_avg", "-7.233575164452594E-9", outStatData.getPlan_lambda_avg() );
//			Assert.assertEquals("different Plan_lambda_max", "-7.233575164452593E-9" , outStatData.getPlan_lambda_max() );
//			Assert.assertEquals("different Plan_lambda_min", "-7.233575164452593E-9" , outStatData.getPlan_lambda_min() );
//			Assert.assertEquals("different Plan_lambda_stddev", "0.0" , outStatData.getPlan_lambda_stddev());
		// Assert.assertEquals("different Total_ll", "-0.046875", outStatData.getTotal_ll() );
		Assertions.assertEquals("0.0", outStatData.getTotal_ll(), "different Total_ll" );

		//test link offsets
		final Network network = scenario.getNetwork();
		String linkOffsetFile = outputDir + "ITERS/it." + lastIteration + "/" + lastIteration + ".linkCostOffsets.xml";

		CadytsCostOffsetsXMLFileIO<Link> offsetReader = new CadytsCostOffsetsXMLFileIO<Link>(new LinkLookUp(network), Link.class);

		DynamicData<Link> linkOffsets = offsetReader.read(linkOffsetFile);

		Link link11 = network.getLinks().get(locId11);
		Link link19 = network.getLinks().get(locId19);

		//find first offset value different from null to compare. Useful to test with different time bin sizes
		int binIndex=-1;
		boolean isZero;
		do {
			binIndex++;
			isZero = (Math.abs(linkOffsets.getBinValue(link19 , binIndex) - 0.0) < MatsimTestUtils.EPSILON);
		}while (isZero && binIndex<86400);

		Assertions.assertEquals(6, binIndex, "Wrong bin index for first link offset");

		Assertions.assertEquals(0.0, linkOffsets.getBinValue(link11 , binIndex), MatsimTestUtils.EPSILON, "Wrong link offset of link 11");
		Assertions.assertEquals(0.0014707121641471912, linkOffsets.getBinValue(link19 , binIndex), MatsimTestUtils.EPSILON, "Wrong link offset of link 19");
	}


	//--------------------------------------------------------------



	private static Config createTestConfig(String inputDir, String outputDir) {
		Config config = ConfigUtils.createConfig() ;
		config.global().setRandomSeed(4711) ;
		config.network().setInputFile(inputDir + "network.xml") ;
		config.plans().setInputFile(inputDir + "plans5.xml") ;
		config.controller().setFirstIteration(1) ;
		config.controller().setLastIteration(10) ;
		config.controller().setOutputDirectory(outputDir) ;
		config.controller().setWriteEventsInterval(1) ;
		config.controller().setMobsim(MobsimType.qsim.toString()) ;
		config.qsim().setFlowCapFactor(1.) ;
		config.qsim().setStorageCapFactor(1.) ;
		config.qsim().setStuckTime(10.) ;
		config.qsim().setRemoveStuckVehicles(false) ;
		{
			ActivityParams params = new ActivityParams("h") ;
			config.scoring().addActivityParams(params ) ;
			params.setTypicalDuration(12*60*60.) ;
		}{
			ActivityParams params = new ActivityParams("w") ;
			config.scoring().addActivityParams(params ) ;
			params.setTypicalDuration(8*60*60.) ;
		}
		config.counts().setInputFile(inputDir + "counts5.xml");
		return config;
	}


	private static class DummyMobsim implements Mobsim {
		public DummyMobsim() {
		}
		@Override
		public void run() {
		}
	}

}
