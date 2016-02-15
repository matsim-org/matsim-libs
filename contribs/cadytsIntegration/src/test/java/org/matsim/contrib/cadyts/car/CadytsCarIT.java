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

import java.io.IOException;
import java.util.Collections;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
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
import org.matsim.core.config.groups.ControlerConfigGroup.MobsimType;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.*;
import org.matsim.core.controler.corelisteners.ControlerDefaultCoreListenersModule;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.scenario.ScenarioByConfigModule;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringParametersForPerson;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.MatsimCountsReader;
import org.matsim.testcases.MatsimTestUtils;

import cadyts.measurements.SingleLinkMeasurement;
import cadyts.utilities.io.tabularFileParser.TabularFileParser;
import cadyts.utilities.misc.DynamicData;

import javax.inject.Inject;

/**
 * This is a modified copy of CadytsIntegrationTest (which is used for the cadyts pt integration)
 * in order to establish an according test for the cadyts car integration.
 * At this stage all original pt code is still included here, but outcommeted, to make the adaptations
 * from pt to car well traceable in case of any errors.
 */
public class CadytsCarIT {
	
	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public final void testInitialization() {
		final String CADYTS_STRATEGY_NAME = "ccc";

		String inputDir = this.utils.getClassInputDirectory();
		String outputDir = this.utils.getOutputDirectory();
		
		Config config = createTestConfig(inputDir, outputDir);
		config.controler().setLastIteration(0);
		
		StrategySettings strategySettings = new StrategySettings(Id.create(1, StrategySettings.class));
		strategySettings.setStrategyName(CADYTS_STRATEGY_NAME) ;
		strategySettings.setWeight(1.0) ;
		config.strategy().addStrategySettings(strategySettings);
		CadytsConfigGroup cadytsCar = ConfigUtils.addOrGetModule(config, CadytsConfigGroup.GROUP_NAME, CadytsConfigGroup.class);
		cadytsCar.addParam("startTime", "04:00:00");
		cadytsCar.addParam("endTime", "20:00:00");
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
						addPlanStrategyBinding(CADYTS_STRATEGY_NAME).toProvider(new javax.inject.Provider<PlanStrategy>() {
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
		Assert.assertEquals(true, context.getCalibrator().getBruteForce());
		Assert.assertEquals(false, context.getCalibrator().getCenterRegression());
		Assert.assertEquals(Integer.MAX_VALUE, context.getCalibrator().getFreezeIteration());
		Assert.assertEquals(8.0, context.getCalibrator().getMinStddev(SingleLinkMeasurement.TYPE.FLOW_VEH_H), MatsimTestUtils.EPSILON);
		Assert.assertEquals(1, context.getCalibrator().getPreparatoryIterations());
		Assert.assertEquals(0.95, context.getCalibrator().getRegressionInertia(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(1.0, context.getCalibrator().getVarianceScale(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(3600.0, context.getCalibrator().getTimeBinSize_s(), MatsimTestUtils.EPSILON);
	}
	
	
	//--------------------------------------------------------------
	@Test
	public final void testCalibrationAsScoring() throws IOException {

		final double beta=30. ;
		final int lastIteration = 20 ;
		
		String inputDir = this.utils.getClassInputDirectory();
		String outputDir = this.utils.getOutputDirectory();

		final Config config = createTestConfig(inputDir, outputDir);
		
		config.controler().setLastIteration(lastIteration);
		
		config.planCalcScore().setBrainExpBeta(beta);
		
		StrategySettings strategySettings = new StrategySettings( ConfigUtils.createAvailableStrategyId(config) ) ;
		strategySettings.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta.toString());
		strategySettings.setWeight(1.0);
		config.strategy().addStrategySettings(strategySettings);

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
							@Inject private CharyparNagelScoringParametersForPerson parameters;
							@Inject private Network network;
							@Inject CadytsContext cadytsContext;
							@Override
							public ScoringFunction createNewScoringFunction(Person person) {
								final CharyparNagelScoringParameters params = parameters.getScoringParameters(person);

								SumScoringFunction scoringFunctionAccumulator = new SumScoringFunction();
								scoringFunctionAccumulator.addScoringFunction(new CharyparNagelLegScoring(params, network));
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
		Assert.assertEquals("Different number of links in network.", scenario.getNetwork().getLinks().size() , 23 );
        Assert.assertEquals("Different number of nodes in network.", scenario.getNetwork().getNodes().size() , 15 );
		
		Assert.assertNotNull("Population is null.", scenario.getPopulation());

        Assert.assertEquals("Num. of persons in population is wrong.", scenario.getPopulation().getPersons().size(), 5);
		Assert.assertEquals("Scale factor is wrong.", scenario.getConfig().counts().getCountsScaleFactor(), 1.0, MatsimTestUtils.EPSILON);
		
		//counts
		Assert.assertEquals("Count file is wrong.", scenario.getConfig().counts().getCountsFileName(), inputDir + "counts5.xml");
				
		
		Counts occupCounts = new Counts();
		new MatsimCountsReader(occupCounts).readFile(scenario.getConfig().counts().getCountsFileName());
		
		Count count =  occupCounts.getCount(Id.create(19, Link.class));
		Assert.assertEquals("Occupancy counts description is wrong", occupCounts.getDescription(), "counts values for equil net");
		Assert.assertEquals("CsId is wrong.", count.getCsId() , "link_19");
		Assert.assertEquals("Volume of hour 6 is wrong", count.getVolume(7).getValue(), 5.0 , MatsimTestUtils.EPSILON);
		Assert.assertEquals("Max count volume is wrong.", count.getMaxVolume().getValue(), 5.0 , MatsimTestUtils.EPSILON);

		String outCounts = outputDir + "ITERS/it." + lastIteration + "/" + lastIteration + ".countscompare.txt";
		CountsReaderCar reader = new CountsReaderCar(outCounts);
		double[] simValues;
		double[] realValues;

		Id<Link> locId11 = Id.create(11, Link.class);
		simValues = reader.getSimulatedValues(locId11);
		realValues= reader.getRealValues(locId11);
		Assert.assertEquals("Volume of hour 6 is wrong", 0.0, simValues[6], MatsimTestUtils.EPSILON);
		Assert.assertEquals("Volume of hour 6 is wrong", 0.0, realValues[6], MatsimTestUtils.EPSILON);

		Id<Link> locId12 = Id.create("12", Link.class);
		simValues = reader.getSimulatedValues(locId12);
		realValues= reader.getRealValues(locId12);
		Assert.assertEquals("Volume of hour 6 is wrong", 0.0, simValues[6], MatsimTestUtils.EPSILON);
		Assert.assertEquals("Volume of hour 6 is wrong", 0.0, realValues[6] , MatsimTestUtils.EPSILON);

		Id<Link> locId19 = Id.create("19", Link.class);
		simValues = reader.getSimulatedValues(locId19);
		realValues= reader.getRealValues(locId19);
		Assert.assertEquals("Volume of hour 6 is wrong", 5.0, simValues[6], MatsimTestUtils.EPSILON);
		Assert.assertEquals("Volume of hour 6 is wrong", 5.0, realValues[6], MatsimTestUtils.EPSILON);

		// Id stopId10 = new IdImpl("stop10");
		Id<Link> locId21 = Id.create("21", Link.class);
		simValues = reader.getSimulatedValues(locId21);
		realValues= reader.getRealValues(locId21);
		Assert.assertEquals("Volume of hour 6 is wrong", 5.0, simValues[6], MatsimTestUtils.EPSILON);
		Assert.assertEquals("Volume of hour 6 is wrong", 5.0, realValues[6], MatsimTestUtils.EPSILON);

		// test calibration statistics
		String testCalibStatPath = outputDir + "calibration-stats.txt";
		CalibrationStatReader calibrationStatReader = new CalibrationStatReader();
		new TabularFileParser().parse(testCalibStatPath, calibrationStatReader);

		CalibrationStatReader.StatisticsData outStatData= calibrationStatReader.getCalStatMap().get(lastIteration);
		// Assert.assertEquals("different Count_ll", "-0.046875", outStatData.getCount_ll() );
		// Assert.assertEquals("different Count_ll_pred_err",  "0.01836234363152515" , outStatData.getCount_ll_pred_err() );
//			Assert.assertEquals("different Link_lambda_avg", "-2.2604922388914356E-10", outStatData.getLink_lambda_avg() );
		Assert.assertEquals("different Link_lambda_avg", "3.2261421242498865E-5", outStatData.getLink_lambda_avg() );
//			Assert.assertEquals("different Link_lambda_max", "0.0" , outStatData.getLink_lambda_max() );
//			Assert.assertEquals("different Link_lambda_min", "-7.233575164452593E-9", outStatData.getLink_lambda_min() );
//			Assert.assertEquals("different Link_lambda_stddev", "1.261054219517188E-9", outStatData.getLink_lambda_stddev());
//			Assert.assertEquals("different P2p_ll", "--" , outStatData.getP2p_ll());
//			Assert.assertEquals("different Plan_lambda_avg", "-7.233575164452594E-9", outStatData.getPlan_lambda_avg() );
//			Assert.assertEquals("different Plan_lambda_max", "-7.233575164452593E-9" , outStatData.getPlan_lambda_max() );
//			Assert.assertEquals("different Plan_lambda_min", "-7.233575164452593E-9" , outStatData.getPlan_lambda_min() );
//			Assert.assertEquals("different Plan_lambda_stddev", "0.0" , outStatData.getPlan_lambda_stddev());
		// Assert.assertEquals("different Total_ll", "-0.046875", outStatData.getTotal_ll() );
		Assert.assertEquals("different Total_ll", "0.0", outStatData.getTotal_ll() );

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

		Assert.assertEquals("Wrong bin index for first link offset", 6, binIndex);

		Assert.assertEquals("Wrong link offset of link 11", 0.0, linkOffsets.getBinValue(link11 , binIndex), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Wrong link offset of link 19", 0.0014707121641471912, linkOffsets.getBinValue(link19 , binIndex), MatsimTestUtils.EPSILON);
	}
	
	
	//--------------------------------------------------------------



	private static Config createTestConfig(String inputDir, String outputDir) {
		Config config = ConfigUtils.createConfig() ;
		config.global().setRandomSeed(4711) ;
		config.network().setInputFile(inputDir + "network.xml") ;
		config.plans().setInputFile(inputDir + "plans5.xml") ;
		config.controler().setFirstIteration(1) ;
		config.controler().setLastIteration(10) ;
		config.controler().setOutputDirectory(outputDir) ;
		config.controler().setWriteEventsInterval(1) ;
		config.controler().setMobsim(MobsimType.qsim.toString()) ;
		config.qsim().setFlowCapFactor(1.) ;
		config.qsim().setStorageCapFactor(1.) ;
		config.qsim().setStuckTime(10.) ;
		config.qsim().setRemoveStuckVehicles(false) ;
		{
			ActivityParams params = new ActivityParams("h") ;
			config.planCalcScore().addActivityParams(params ) ;
			params.setTypicalDuration(12*60*60.) ;
		}{
			ActivityParams params = new ActivityParams("w") ;
			config.planCalcScore().addActivityParams(params ) ;
			params.setTypicalDuration(8*60*60.) ;
		}
		config.counts().setCountsFileName(inputDir + "counts5.xml");
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
