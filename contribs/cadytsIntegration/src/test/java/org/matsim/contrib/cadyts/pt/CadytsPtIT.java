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

package org.matsim.contrib.cadyts.pt;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

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
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControlerConfigGroup.MobsimType;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.scenario.ScenarioUtils;
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
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.testcases.MatsimTestUtils;

import com.google.inject.Provider;

import cadyts.measurements.SingleLinkMeasurement;
import cadyts.utilities.io.tabularFileParser.TabularFileParser;
import cadyts.utilities.misc.DynamicData;

import javax.inject.Inject;

public class CadytsPtIT {

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public final void testInitialization() {
		String inputDir = this.utils.getClassInputDirectory();

		Config config = createTestConfig(inputDir, this.utils.getOutputDirectory());
		config.controler().setLastIteration(0);
		StrategySettings stratSets = new StrategySettings(Id.create(1, StrategySettings.class));
		stratSets.setStrategyName("ccc") ;
		stratSets.setWeight(1.) ;
		config.strategy().addStrategySettings(stratSets) ;

		final Scenario scenario = ScenarioUtils.loadScenario(config) ;
		final Controler controler = new Controler(scenario);
		controler.addOverridingModule(new CadytsPtModule());
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addPlanStrategyBinding("ccc").toProvider(new javax.inject.Provider<PlanStrategy>() {
					@Inject CadytsPtContext context;
					@Override
					public PlanStrategy get() {
						return new PlanStrategyImpl(new CadytsPlanChanger<>(scenario, context));
					}
				});
			}
		});

		controler.getConfig().controler().setCreateGraphs(false);
        controler.getConfig().controler().setWriteEventsInterval(0);
		controler.getConfig().controler().setDumpDataAtEnd(true);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bindMobsim().toProvider(new Provider<Mobsim>() {
					@Override
					public Mobsim get() {
						return new DummyMobsimFactory().createMobsim(controler.getScenario(), controler.getEvents());
					}
				});
			}
		});
		controler.run();

		CadytsPtContext context = controler.getInjector().getInstance(CadytsPtContext.class);

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

	@Test
	public final void testCalibrationAsScoring() throws IOException {
		final double beta=30. ;
		final int lastIteration = 20 ;

		String inputDir = this.utils.getClassInputDirectory();
		String outputDir = this.utils.getOutputDirectory();

		final Config config = createTestConfig(inputDir, outputDir);

		config.controler().setLastIteration(lastIteration) ;

		config.planCalcScore().setBrainExpBeta(beta) ;

		StrategySettings stratSets = new StrategySettings(Id.create("1", StrategySettings.class)) ;
		//		stratSets.setModuleName("ChangeExpBeta") ;
		stratSets.setStrategyName("ccc") ;
		stratSets.setWeight(1.0) ;
		config.strategy().addStrategySettings(stratSets) ;

		// ===

		final Controler controler = new Controler(config);
        controler.getConfig().controler().setCreateGraphs(false);
		controler.getConfig().controler().setDumpDataAtEnd(true);
		controler.addOverridingModule(new CadytsPtModule());
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addPlanStrategyBinding("ccc").toProvider(new javax.inject.Provider<PlanStrategy>() {
					@Inject Scenario scenario;
					@Inject CadytsPtContext cContext;
					@Override
					public PlanStrategy get() {
						final CadytsPlanChanger<TransitStopFacility> planSelector = new CadytsPlanChanger<TransitStopFacility>(scenario, cContext);
						planSelector.setCadytsWeight(0.);
						// weight 0 is correct: this is only in order to use getCalibrator().addToDemand.
						// would certainly be cleaner (and less confusing) to write a separate method for this.  (But how?)
						// kai, may'13
						//				final PlanSelector planSelector = new ExpBetaPlanChangerWithCadytsPlanRegistration<TransitStopFacility>(beta, cContext) ;
						return new PlanStrategyImpl(planSelector);
					}
				});
			}
		});

		controler.setScoringFunctionFactory(new ScoringFunctionFactory() {
			@Inject CharyparNagelScoringParametersForPerson parameters;
			@Inject Network network;
			@Inject CadytsPtContext cContext;
			@Override
			public ScoringFunction createNewScoringFunction(Person person) {
				final CharyparNagelScoringParameters params = parameters.getScoringParameters(person);

				SumScoringFunction scoringFunctionAccumulator = new SumScoringFunction();
				scoringFunctionAccumulator.addScoringFunction(new CharyparNagelLegScoring(params, network));
				scoringFunctionAccumulator.addScoringFunction(new CharyparNagelActivityScoring(params)) ;
				scoringFunctionAccumulator.addScoringFunction(new CharyparNagelAgentStuckScoring(params));

				final CadytsScoring<TransitStopFacility> scoringFunction = new CadytsScoring<TransitStopFacility>(person.getSelectedPlan() ,config, cContext);
				scoringFunction.setWeightOfCadytsCorrection(beta*30.) ;
				scoringFunctionAccumulator.addScoringFunction(scoringFunction );

				return scoringFunctionAccumulator;
			}
		}) ;


		controler.run();


		//scenario data  test
		Assert.assertNotNull("config is null" , controler.getConfig());
        Assert.assertEquals("Different number of links in network.", controler.getScenario().getNetwork().getLinks().size() , 23 );
        Assert.assertEquals("Different number of nodes in network.", controler.getScenario().getNetwork().getNodes().size() , 15 );
		Assert.assertNotNull("Transit schedule is null.", controler.getScenario().getTransitSchedule());
		Assert.assertEquals("Num. of trLines is wrong.", 2, controler.getScenario().getTransitSchedule().getTransitLines().size() );
		Assert.assertEquals("Num of facilities in schedule is wrong.", controler.getScenario().getTransitSchedule().getFacilities().size() , 5);
		Assert.assertNotNull("Population is null.", controler.getScenario().getPopulation());
        Assert.assertEquals("Num. of persons in population is wrong.", controler.getScenario().getPopulation().getPersons().size() , 4);
		Assert.assertEquals("Scale factor is wrong.", controler.getScenario().getConfig().ptCounts().getCountsScaleFactor(), 1.0, MatsimTestUtils.EPSILON);
		//		Assert.assertEquals("Distance filter is wrong.", controler.getScenario().getConfig().ptCounts().getDistanceFilter() , 30000.0, MatsimTestUtils.EPSILON);
		//		Assert.assertEquals("DistanceFilterCenterNode is wrong.", controler.getScenario().getConfig().ptCounts().getDistanceFilterCenterNode(), "7");
		//counts
		Assert.assertEquals("Occupancy count file is wrong.", controler.getScenario().getConfig().ptCounts().getOccupancyCountsFileName(), inputDir + "counts/counts_occupancy.xml");
		Counts occupCounts = new Counts();
		new MatsimCountsReader(occupCounts).readFile(controler.getScenario().getConfig().ptCounts().getOccupancyCountsFileName());
		Count count =  occupCounts.getCount(Id.create("stop1", Link.class)); // casting id from stop to link, not nice
		Assert.assertEquals("Occupancy counts description is wrong", occupCounts.getDescription(), "counts values for equil net");
		Assert.assertEquals("CsId is wrong.", count.getCsId() , "stop1");
		Assert.assertEquals("Volume of hour 4 is wrong", count.getVolume(7).getValue(), 4.0 , MatsimTestUtils.EPSILON);
		Assert.assertEquals("Max count volume is wrong.", count.getMaxVolume().getValue(), 4.0 , MatsimTestUtils.EPSILON);

		// test resulting simulation volumes
		{
			String outCounts = outputDir + "ITERS/it." + lastIteration + "/" + lastIteration + ".simCountCompareOccupancy.txt";
			CountsReaderPt reader = new CountsReaderPt(outCounts);
			double[] simValues;
			double[] realValues;

			Id<TransitStopFacility> stopId1 = Id.create("stop1", TransitStopFacility.class);
			simValues = reader.getSimulatedValues(stopId1);
			realValues= reader.getRealValues(stopId1);
			Assert.assertEquals("Volume of hour 6 is wrong", 4.0, simValues[6], MatsimTestUtils.EPSILON);
			Assert.assertEquals("Volume of hour 6 is wrong", 4.0, realValues[6], MatsimTestUtils.EPSILON);

			Id<TransitStopFacility> stopId2 = Id.create("stop2", TransitStopFacility.class);
			simValues = reader.getSimulatedValues(stopId2);
			realValues= reader.getRealValues(stopId2);
			Assert.assertEquals("Volume of hour 6 is wrong", 0.0, simValues[6], MatsimTestUtils.EPSILON);
			Assert.assertEquals("Volume of hour 6 is wrong", 1.0, realValues[6] , MatsimTestUtils.EPSILON);

			Id<TransitStopFacility> stopId6 = Id.create("stop6", TransitStopFacility.class);
			simValues = reader.getSimulatedValues(stopId6);
			realValues= reader.getRealValues(stopId6);
			Assert.assertEquals("Volume of hour 6 is wrong", 0.0, simValues[6], MatsimTestUtils.EPSILON);
			Assert.assertEquals("Volume of hour 6 is wrong", 2.0, realValues[6], MatsimTestUtils.EPSILON);

			Id<TransitStopFacility> stopId10 = Id.create("stop10", TransitStopFacility.class);
			simValues = reader.getSimulatedValues(stopId10);
			realValues= reader.getRealValues(stopId10);
			Assert.assertEquals("Volume of hour 6 is wrong", 4.0, simValues[6], MatsimTestUtils.EPSILON);
			Assert.assertEquals("Volume of hour 6 is wrong", 5.0, realValues[6], MatsimTestUtils.EPSILON);

			// test calibration statistics
			String testCalibStatPath = outputDir + "calibration-stats.txt";
			CalibrationStatReader calibrationStatReader = new CalibrationStatReader();
			new TabularFileParser().parse(testCalibStatPath, calibrationStatReader);

			CalibrationStatReader.StatisticsData outStatData= calibrationStatReader.getCalStatMap().get(lastIteration);
			Assert.assertEquals("different Count_ll", "-0.046875", outStatData.getCount_ll() );
			Assert.assertEquals("different Count_ll_pred_err",  "0.01836234363152515" , outStatData.getCount_ll_pred_err() );
			//			Assert.assertEquals("different Link_lambda_avg", "-2.2604922388914356E-10", outStatData.getLink_lambda_avg() );
			//			Assert.assertEquals("different Link_lambda_max", "0.0" , outStatData.getLink_lambda_max() );
			//			Assert.assertEquals("different Link_lambda_min", "-7.233575164452593E-9", outStatData.getLink_lambda_min() );
			//			Assert.assertEquals("different Link_lambda_stddev", "1.261054219517188E-9", outStatData.getLink_lambda_stddev());
			//			Assert.assertEquals("different P2p_ll", "--" , outStatData.getP2p_ll());
			//			Assert.assertEquals("different Plan_lambda_avg", "-7.233575164452594E-9", outStatData.getPlan_lambda_avg() );
			//			Assert.assertEquals("different Plan_lambda_max", "-7.233575164452593E-9" , outStatData.getPlan_lambda_max() );
			//			Assert.assertEquals("different Plan_lambda_min", "-7.233575164452593E-9" , outStatData.getPlan_lambda_min() );
			//			Assert.assertEquals("different Plan_lambda_stddev", "0.0" , outStatData.getPlan_lambda_stddev());
			Assert.assertEquals("different Total_ll", "-0.046875", outStatData.getTotal_ll() );


			//test link offsets
			final TransitSchedule schedule = controler.getScenario().getTransitSchedule();
			String linkOffsetFile = outputDir + "ITERS/it." + lastIteration + "/" + lastIteration + ".linkCostOffsets.xml";
			//			CadytsPtLinkCostOffsetsXMLFileIO offsetReader = new CadytsPtLinkCostOffsetsXMLFileIO (schedule);
			CadytsCostOffsetsXMLFileIO<TransitStopFacility> offsetReader 
			= new CadytsCostOffsetsXMLFileIO<TransitStopFacility> (new TransitStopFacilityLookUp(controler.getScenario()), TransitStopFacility.class);
			DynamicData<TransitStopFacility> stopOffsets = offsetReader.read(linkOffsetFile);

			TransitStopFacility stop2 = schedule.getFacilities().get(stopId2);
			TransitStopFacility stop10 = schedule.getFacilities().get(stopId10);

			//find first offset value different from null to compare. Useful to test with different time bin sizes
			int binIndex=-1;
			boolean isZero;
			do {
				binIndex++;
				isZero = (Math.abs(stopOffsets.getBinValue(stop2 , binIndex) - 0.0) < MatsimTestUtils.EPSILON);
			}while (isZero && binIndex<86400);

			Assert.assertEquals("Wrong bin index for first link offset", 6, binIndex);
			Assert.assertEquals("Wrong link offset of stop 10", 0.03515757824042241, stopOffsets.getBinValue(stop10 , binIndex), MatsimTestUtils.EPSILON);
			Assert.assertEquals("Wrong link offset of stop 2", -0.011353248321030008, stopOffsets.getBinValue(stop2 , binIndex), MatsimTestUtils.EPSILON);
		}
	}

	@Test
	public final void testCalibration() throws IOException {
		final double beta = 30. ;
		final int lastIteration = 20 ;

		String inputDir = this.utils.getClassInputDirectory();
		String outputDir = this.utils.getOutputDirectory();

		Config config = createTestConfig(inputDir, outputDir) ;

		config.controler().setWriteEventsInterval(0) ;
		config.controler().setLastIteration(lastIteration) ;
		// seems to need 15 iterations as "warm-up"; at least the cadyts corrections are much smaller until then.

		config.planCalcScore().setBrainExpBeta(beta) ;

		StrategySettings stratSets = new StrategySettings(Id.create(1, StrategySettings.class));
		stratSets.setStrategyName("ccc") ;
		stratSets.setWeight(1.) ;
		config.strategy().addStrategySettings(stratSets) ;

		final Scenario scenario = ScenarioUtils.loadScenario(config) ;

		final Controler controler = new Controler( scenario );
        controler.getConfig().controler().setCreateGraphs(false);
		controler.getConfig().controler().setDumpDataAtEnd(true);

		controler.addOverridingModule(new CadytsPtModule());
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addPlanStrategyBinding("ccc").toProvider(new javax.inject.Provider<PlanStrategy>() {
					@Inject CadytsPtContext context;
					@Override
					public PlanStrategy get() {
						final CadytsPlanChanger<TransitStopFacility> planSelector = new CadytsPlanChanger<TransitStopFacility>(scenario, context);
						planSelector.setCadytsWeight(beta * 30.);
						return new PlanStrategyImpl(planSelector);
					}
				});
			}
		});

		controler.run();


		//scenario data  test
		Assert.assertNotNull("config is null" , controler.getConfig());
        Assert.assertEquals("Different number of links in network.", controler.getScenario().getNetwork().getLinks().size() , 23 );
        Assert.assertEquals("Different number of nodes in network.", controler.getScenario().getNetwork().getNodes().size() , 15 );
		Assert.assertNotNull("Transit schedule is null.", controler.getScenario().getTransitSchedule());
		Assert.assertEquals("Num. of trLines is wrong.", 2, controler.getScenario().getTransitSchedule().getTransitLines().size()  );
		Assert.assertEquals("Num of facilities in schedule is wrong.", controler.getScenario().getTransitSchedule().getFacilities().size() , 5);
		Assert.assertNotNull("Population is null.", controler.getScenario().getPopulation());
        Assert.assertEquals("Num. of persons in population is wrong.", controler.getScenario().getPopulation().getPersons().size() , 4);
		Assert.assertEquals("Scale factor is wrong.", controler.getScenario().getConfig().ptCounts().getCountsScaleFactor(), 1.0, MatsimTestUtils.EPSILON);
		//		Assert.assertEquals("Distance filter is wrong.", controler.getScenario().getConfig().ptCounts().getDistanceFilter() , 30000.0, MatsimTestUtils.EPSILON);
		//		Assert.assertEquals("DistanceFilterCenterNode is wrong.", controler.getScenario().getConfig().ptCounts().getDistanceFilterCenterNode(), "7");
		//counts
		Assert.assertEquals("Occupancy count file is wrong.", controler.getScenario().getConfig().ptCounts().getOccupancyCountsFileName(), inputDir + "counts/counts_occupancy.xml");
		Counts occupCounts = new Counts();
		new MatsimCountsReader(occupCounts).readFile(controler.getScenario().getConfig().ptCounts().getOccupancyCountsFileName());
		Count count =  occupCounts.getCount(Id.create("stop1", Link.class)); // casting the id from a stop to a link, not nice..
		Assert.assertEquals("Occupancy counts description is wrong", occupCounts.getDescription(), "counts values for equil net");
		Assert.assertEquals("CsId is wrong.", count.getCsId() , "stop1");
		Assert.assertEquals("Volume of hour 4 is wrong", count.getVolume(7).getValue(), 4.0 , MatsimTestUtils.EPSILON);
		Assert.assertEquals("Max count volume is wrong.", count.getMaxVolume().getValue(), 4.0 , MatsimTestUtils.EPSILON);

		// test resulting simulation volumes
		{
			String outCounts = outputDir + "ITERS/it." + lastIteration + "/" + lastIteration + ".simCountCompareOccupancy.txt";
			CountsReaderPt reader = new CountsReaderPt(outCounts);
			double[] simValues;
			double[] realValues;

			Id<TransitStopFacility> stopId1 = Id.create("stop1", TransitStopFacility.class);
			simValues = reader.getSimulatedValues(stopId1);
			realValues= reader.getRealValues(stopId1);
			Assert.assertEquals("Volume of hour 6 is wrong", 4.0, simValues[6], MatsimTestUtils.EPSILON);
			Assert.assertEquals("Volume of hour 6 is wrong", 4.0, realValues[6], MatsimTestUtils.EPSILON);

			Id<TransitStopFacility> stopId2 = Id.create("stop2", TransitStopFacility.class);
			simValues = reader.getSimulatedValues(stopId2);
			realValues= reader.getRealValues(stopId2);
			Assert.assertEquals("Volume of hour 6 is wrong", 0.0, simValues[6], MatsimTestUtils.EPSILON);
			Assert.assertEquals("Volume of hour 6 is wrong", 1.0, realValues[6] , MatsimTestUtils.EPSILON);

			Id<TransitStopFacility> stopId6 = Id.create("stop6", TransitStopFacility.class);
			simValues = reader.getSimulatedValues(stopId6);
			realValues= reader.getRealValues(stopId6);
			Assert.assertEquals("Volume of hour 6 is wrong", 0.0, simValues[6], MatsimTestUtils.EPSILON);
			Assert.assertEquals("Volume of hour 6 is wrong", 2.0, realValues[6], MatsimTestUtils.EPSILON);

			Id<TransitStopFacility> stopId10 = Id.create("stop10", TransitStopFacility.class);
			simValues = reader.getSimulatedValues(stopId10);
			realValues= reader.getRealValues(stopId10);
			Assert.assertEquals("Volume of hour 6 is wrong", 4.0, simValues[6], MatsimTestUtils.EPSILON);
			Assert.assertEquals("Volume of hour 6 is wrong", 5.0, realValues[6], MatsimTestUtils.EPSILON);

			// test calibration statistics
			String testCalibStatPath = outputDir + "calibration-stats.txt";
			CalibrationStatReader calibrationStatReader = new CalibrationStatReader();
			new TabularFileParser().parse(testCalibStatPath, calibrationStatReader);

			CalibrationStatReader.StatisticsData outStatData= calibrationStatReader.getCalStatMap().get(lastIteration);
			Assert.assertEquals("different Count_ll", "-0.046875", outStatData.getCount_ll() );
			Assert.assertEquals("different Count_ll_pred_err",  "0.008670972399424905" , outStatData.getCount_ll_pred_err() );
			Assert.assertEquals("different Link_lambda_avg", "3.642292018550638E-4", outStatData.getLink_lambda_avg() );
			Assert.assertEquals("different Link_lambda_max", "0.032081715026130615" , outStatData.getLink_lambda_max() );
			Assert.assertEquals("different Link_lambda_min", "-0.008771046107406533", outStatData.getLink_lambda_min() );
			Assert.assertEquals("different Link_lambda_stddev", "0.0041495140513996154", outStatData.getLink_lambda_stddev());
			Assert.assertEquals("different P2p_ll", "--" , outStatData.getP2p_ll());
			Assert.assertEquals("different Plan_lambda_avg", "0.011655334459362041", outStatData.getPlan_lambda_avg() );
			Assert.assertEquals("different Plan_lambda_max", "0.032081715026130615" , outStatData.getPlan_lambda_max() );
			Assert.assertEquals("different Plan_lambda_min", "-0.008771046107406533" , outStatData.getPlan_lambda_min() );
			Assert.assertEquals("different Plan_lambda_stddev", "0.02183671935220152" , outStatData.getPlan_lambda_stddev());
			Assert.assertEquals("different Total_ll", "-0.046875", outStatData.getTotal_ll() );


			//test link offsets
			final TransitSchedule schedule = controler.getScenario().getTransitSchedule();
			String linkOffsetFile = outputDir + "ITERS/it." + lastIteration + "/" + lastIteration + ".linkCostOffsets.xml";
			//			CadytsPtLinkCostOffsetsXMLFileIO offsetReader = new CadytsPtLinkCostOffsetsXMLFileIO (schedule);
			CadytsCostOffsetsXMLFileIO<TransitStopFacility> offsetReader 
			= new CadytsCostOffsetsXMLFileIO<TransitStopFacility> (new TransitStopFacilityLookUp(controler.getScenario()), TransitStopFacility.class);
			DynamicData<TransitStopFacility> stopOffsets = offsetReader.read(linkOffsetFile);

			TransitStopFacility stop2 = schedule.getFacilities().get(stopId2);
			TransitStopFacility stop10 = schedule.getFacilities().get(stopId10);

			//find first offset value different from null to compare. Useful to test with different time bin sizes
			int binIndex=-1;
			boolean isZero;
			do {
				binIndex++;
				isZero = (Math.abs(stopOffsets.getBinValue(stop2 , binIndex) - 0.0) < MatsimTestUtils.EPSILON);
			}while (isZero && binIndex<86400);

			Assert.assertEquals("Wrong bin index for first link offset", 6, binIndex);
			Assert.assertEquals("Wrong link offset of stop 10", 0.031842616249416734, stopOffsets.getBinValue(stop10 , binIndex), MatsimTestUtils.EPSILON);
			Assert.assertEquals("Wrong link offset of stop 2", -0.0079478186053482, stopOffsets.getBinValue(stop2 , binIndex), MatsimTestUtils.EPSILON);
		}
	}


	/**
	 * @author mmoyo
	 */
	@Test 
	public final void testCalibrationTwo() throws IOException {
		// yyyy I cannot fully certify that this test is doing something reasonable, since simCountComparisonOccupancy.txt and 
		// cadytsSimCountComparisonOccupancy.txt are returning different results.  kai, feb'13
		// There is a comment in CadytsContext that the "cadyts" version may be wrong for time bins different from one hour. kai, dec'13

		final double beta = 30. ;
		final int lastIteration = 20 ;

		String inputDir = this.utils.getClassInputDirectory();
		String outputDir = this.utils.getOutputDirectory();

		Config config = createTestConfig(inputDir, this.utils.getOutputDirectory());

		config.controler().setLastIteration(lastIteration) ;
		config.controler().setWritePlansInterval(1) ;
		config.controler().setWriteEventsInterval(1) ;

		config.ptCounts().setPtCountsInterval(1) ;

		StrategySettings stratSets = new StrategySettings(Id.create(1, StrategySettings.class));
		stratSets.setStrategyName("ccc") ;
		stratSets.setWeight(1.) ;
		config.strategy().addStrategySettings(stratSets) ;

		CadytsConfigGroup cConfig = (CadytsConfigGroup) config.getModule(CadytsConfigGroup.GROUP_NAME) ;
		cConfig.setTimeBinSize(7200) ;

		// ---

		final Controler controler = new Controler(config);
		controler.addOverridingModule(new CadytsPtModule());
//		controler.setOverwriteFiles(true);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addPlanStrategyBinding("ccc").toProvider(new javax.inject.Provider<PlanStrategy>() {
					@Inject Scenario scenario;
					@Inject CadytsPtContext context;
					@Override
					public PlanStrategy get() {
						final CadytsPlanChanger<TransitStopFacility> planSelector = new CadytsPlanChanger<TransitStopFacility>(scenario, context);
						planSelector.setCadytsWeight(beta * 30.);
						return new PlanStrategyImpl(planSelector);
					}
				});
			}
		});

		controler.getConfig().controler().setCreateGraphs(false);
        controler.getConfig().controler().setWriteEventsInterval(0);
		controler.getConfig().controler().setDumpDataAtEnd(true);
		controler.run();

		// ====================================

		//scenario data  test
		Assert.assertNotNull("config is null" , controler.getConfig());
        Assert.assertEquals("Different number of links in network.", controler.getScenario().getNetwork().getLinks().size() , 23 );
        Assert.assertEquals("Different number of nodes in network.", controler.getScenario().getNetwork().getNodes().size() , 15 );
		Assert.assertNotNull("Transit schedule is null.", controler.getScenario().getTransitSchedule());
		Assert.assertEquals("Num. of trLines is wrong.", 2, controler.getScenario().getTransitSchedule().getTransitLines().size() );
		Assert.assertEquals("Num of facilities in schedule is wrong.", controler.getScenario().getTransitSchedule().getFacilities().size() , 5);
		Assert.assertNotNull("Population is null.", controler.getScenario().getPopulation());
        Assert.assertEquals("Num. of persons in population is wrong.", controler.getScenario().getPopulation().getPersons().size() , 4);
		Assert.assertEquals("Scale factor is wrong.", controler.getScenario().getConfig().ptCounts().getCountsScaleFactor(), 1.0, MatsimTestUtils.EPSILON);
		//		Assert.assertEquals("Distance filter is wrong.", controler.getScenario().getConfig().ptCounts().getDistanceFilter() , 30000.0, MatsimTestUtils.EPSILON);
		//		Assert.assertEquals("DistanceFilterCenterNode is wrong.", controler.getScenario().getConfig().ptCounts().getDistanceFilterCenterNode(), "7");
		//counts
		Assert.assertEquals("Occupancy count file is wrong.", controler.getScenario().getConfig().ptCounts().getOccupancyCountsFileName(), inputDir + "counts/counts_occupancy.xml");
		Counts occupCounts = new Counts();
		new MatsimCountsReader(occupCounts).readFile(controler.getScenario().getConfig().ptCounts().getOccupancyCountsFileName());
		Count count =  occupCounts.getCount(Id.create("stop1", Link.class));
		Assert.assertEquals("Occupancy counts description is wrong", occupCounts.getDescription(), "counts values for equil net");
		Assert.assertEquals("CsId is wrong.", count.getCsId() , "stop1");

		Assert.assertEquals("Volume of hour 4 is wrong", count.getVolume(7).getValue(), 4.0 , MatsimTestUtils.EPSILON);
		// yy I don't know why it says "hour 4" but "getVolume(7)". kai, sep'14
		
		Assert.assertEquals("Max count volume is wrong.", count.getMaxVolume().getValue(), 4.0 , MatsimTestUtils.EPSILON);

		// test resulting simulation volumes
		String outCounts = outputDir + "ITERS/it." + lastIteration + "/" + lastIteration + ".simCountCompareOccupancy.txt";
		CountsReaderPt reader = new CountsReaderPt(outCounts);
		Id<TransitStopFacility> stopId1 = Id.create("stop1", TransitStopFacility.class);
		{
			double[] simValues = reader.getSimulatedValues(stopId1);
			double[] realValues= reader.getRealValues(stopId1);
			Assert.assertEquals("Volume of hour 6 (probably: 7) is wrong", 4.0, simValues[6], MatsimTestUtils.EPSILON);
			Assert.assertEquals("Volume of hour 6 (probably: 7) is wrong", 4.0, realValues[6], MatsimTestUtils.EPSILON);
		}
		Id<TransitStopFacility> stopId2 = Id.create("stop2", TransitStopFacility.class);
		{
			double[] simValues = reader.getSimulatedValues(stopId2);
			double[] realValues= reader.getRealValues(stopId2);
			Assert.assertEquals("Volume of hour 6 (probably: 7) is wrong", 0.0, simValues[6], MatsimTestUtils.EPSILON);
			Assert.assertEquals("Volume of hour 6 (probably: 7) is wrong", 1.0, realValues[6] , MatsimTestUtils.EPSILON);
		}
		Id<TransitStopFacility> stopId6 = Id.create("stop6", TransitStopFacility.class);
		{
			double[] simValues = reader.getSimulatedValues(stopId6);
			double[] realValues= reader.getRealValues(stopId6);
			Assert.assertEquals("Volume of hour 6 (probably: 7) is wrong", 0.0, simValues[6], MatsimTestUtils.EPSILON);
			Assert.assertEquals("Volume of hour 6 (probably: 7) is wrong", 2.0, realValues[6], MatsimTestUtils.EPSILON);
		}
		Id<TransitStopFacility> stopId10 = Id.create("stop10", TransitStopFacility.class);
		{
			double[] simValues = reader.getSimulatedValues(stopId10);
			double[] realValues= reader.getRealValues(stopId10);
			Assert.assertEquals("Volume of hour 6 (probably: 7) is wrong", 4.0, simValues[6], MatsimTestUtils.EPSILON);
			Assert.assertEquals("Volume of hour 6 (probably: 7) is wrong", 5.0, realValues[6], MatsimTestUtils.EPSILON);
		}
		// 2 bus lines, one using stops 1, 2, 12, the other using 1, 10, 12.  Counts say:
		// stop1: 4 (should always be satisfied)
		// stop2: 1 (should be possible to satisfy bus see below)
		// stop6: 2 (not possible to satisfy (no line going there))
		// stop10: 5 (not possible to satisfy (we have not enough plans) but see below)
		// and they say this at h=7 (which may, or may not, be the entry "6" in the counts array, but then the error messages are wrong).
		//
		// stop2 and stop10 are pulling in different directions; would need 7 plans to satisfy but have only 4.  Satisfying the higher
		// volume station better is the cadyts-conforming solution.

		// test calibration statistics
		String testCalibStatPath = outputDir + "calibration-stats.txt";
		CalibrationStatReader calibrationStatReader = new CalibrationStatReader();
		new TabularFileParser().parse(testCalibStatPath, calibrationStatReader);

		CalibrationStatReader.StatisticsData outStatData= calibrationStatReader.getCalStatMap().get(lastIteration);
		//		Assert.assertEquals("different Count_ll", "-0.01171875", outStatData.getCount_ll() );
		//		Assert.assertEquals("different Count_ll_pred_err",  "0.004590585907794875" , outStatData.getCount_ll_pred_err() );
		//			Assert.assertEquals("different Link_lambda_avg", "-1.8081427328702926E-9", outStatData.getLink_lambda_avg() );
		//			Assert.assertEquals("different Link_lambda_max", "0.0" , outStatData.getLink_lambda_max() );
		//			Assert.assertEquals("different Link_lambda_min", "-1.4465142715757458E-8", outStatData.getLink_lambda_min() );
		//			Assert.assertEquals("different Link_lambda_stddev", "4.501584893410135E-9" , outStatData.getLink_lambda_stddev());
		//			Assert.assertEquals("different P2p_ll", "--" , outStatData.getP2p_ll());
		//			Assert.assertEquals("different Plan_lambda_avg", "-2.5313998260184097E-8", outStatData.getPlan_lambda_avg() );
		//			Assert.assertEquals("different Plan_lambda_max", "-2.5313998260184097E-8" , outStatData.getPlan_lambda_max() );
		//			Assert.assertEquals("different Plan_lambda_min", "-2.5313998260184097E-8" , outStatData.getPlan_lambda_min() );
		//			Assert.assertEquals("different Plan_lambda_stddev", "NaN" , outStatData.getPlan_lambda_stddev());
		//		Assert.assertEquals("different Total_ll", "-0.01171875", outStatData.getTotal_ll() );
		//
		// I think that the above quantities change when changing between FLOW_VEH_H and COUNT_VEH.  The calibration result, though,
		// seems the same.


		//test link offsets
		final TransitSchedule schedule = controler.getScenario().getTransitSchedule();
		CadytsCostOffsetsXMLFileIO<TransitStopFacility> offsetReader = 
				new CadytsCostOffsetsXMLFileIO<TransitStopFacility> (new TransitStopFacilityLookUp(controler.getScenario()), TransitStopFacility.class);
		DynamicData<TransitStopFacility> stopOffsets = 
				offsetReader.read(outputDir + "ITERS/it." + lastIteration + "/" + lastIteration + ".linkCostOffsets.xml");

		TransitStopFacility stop1 = schedule.getFacilities().get(stopId1);
		TransitStopFacility stop2 = schedule.getFacilities().get(stopId2);
		//TransitStopFacility stop6 = schedule.getFacilities().get(stopId6);
		TransitStopFacility stop10 = schedule.getFacilities().get(stopId10);

		//find first offset value different from zero to compare. Useful to test with different time bin sizes
		int binIndex=-1;
		boolean isZero;
		do {
			binIndex++;
			isZero = (Math.abs(stopOffsets.getBinValue(stop2 , binIndex) - 0.0) < MatsimTestUtils.EPSILON);
		}while (isZero && binIndex<86400);

		Assert.assertEquals("Wrong Bin index for first link offset", 3, binIndex); // bin size = 3600; fix!   //done manuel jul.2012
		Assert.assertEquals("Wrong link offset of stop 1", 0.0, stopOffsets.getBinValue(stop1 , binIndex), MatsimTestUtils.EPSILON);
//		Assert.assertEquals("Wrong link offset of stop 2", -0.0028383120802772956, stopOffsets.getBinValue(stop2 , binIndex), MatsimTestUtils.EPSILON);
//		Assert.assertEquals("Wrong link offset of stop 10", 0.00878939456017082, stopOffsets.getBinValue(stop10 , binIndex), MatsimTestUtils.EPSILON);
		
		Assert.assertTrue("Offset at stop 2 has wrong sign.", stopOffsets.getBinValue(stop2, binIndex) < 0. ) ;
		Assert.assertTrue("Offset at stop 10 has wrong sign.", stopOffsets.getBinValue(stop10, binIndex) > 0. ) ;
	}



	private static Config createTestConfig(String inputDir, String outputDir) {
		Config config = ConfigUtils.createConfig() ;
		// ---
		config.global().setRandomSeed(4711) ;
		// ---
		config.network().setInputFile(inputDir + "network.xml") ;
		// ---
		config.plans().setInputFile(inputDir + "4plans.xml") ;
		// ---
		config.transit().setUseTransit(true) ;
		// ---
		config.controler().setFirstIteration(1) ;
		config.controler().setLastIteration(10) ;
		config.controler().setOutputDirectory(outputDir) ;
		config.controler().setWriteEventsInterval(1) ;
		config.controler().setMobsim(MobsimType.qsim.toString()) ;
		// ---

		config.qsim().setFlowCapFactor(0.02) ;
		config.qsim().setStorageCapFactor(0.06) ;
		config.qsim().setStuckTime(10.) ;
		config.qsim().setRemoveStuckVehicles(false) ; // ??
		// ---
		config.transit().setTransitScheduleFile(inputDir + "transitSchedule1bus.xml") ;
		config.transit().setVehiclesFile(inputDir + "vehicles.xml") ;
		Set<String> modes = new HashSet<String>() ;
		modes.add("pt") ;
		config.transit().setTransitModes(modes) ;
		// ---
		{
			ActivityParams params = new ActivityParams("h") ;
			config.planCalcScore().addActivityParams(params ) ;
			params.setTypicalDuration(12*60*60.) ;
		}{
			ActivityParams params = new ActivityParams("w") ;
			config.planCalcScore().addActivityParams(params ) ;
			params.setTypicalDuration(8*60*60.) ;
		}
		// ---
		ConfigGroup cadytsPtConfig = config.createModule(CadytsConfigGroup.GROUP_NAME ) ;

		cadytsPtConfig.addParam(CadytsConfigGroup.START_TIME, "04:00:00") ;
		cadytsPtConfig.addParam(CadytsConfigGroup.END_TIME, "20:00:00" ) ;
		cadytsPtConfig.addParam(CadytsConfigGroup.REGRESSION_INERTIA, "0.95") ;
		cadytsPtConfig.addParam(CadytsConfigGroup.USE_BRUTE_FORCE, "true") ;
		cadytsPtConfig.addParam(CadytsConfigGroup.MIN_FLOW_STDDEV, "8") ;
		cadytsPtConfig.addParam(CadytsConfigGroup.PREPARATORY_ITERATIONS, "1") ;
		cadytsPtConfig.addParam(CadytsConfigGroup.TIME_BIN_SIZE, "3600") ;
		cadytsPtConfig.addParam(CadytsConfigGroup.CALIBRATED_LINES, "M44,M43") ;

		CadytsConfigGroup ccc = new CadytsConfigGroup() ;
		config.addModule(ccc) ;


		// ---
		config.ptCounts().setOccupancyCountsFileName(inputDir + "counts/counts_occupancy.xml") ;
		config.ptCounts().setBoardCountsFileName(inputDir + "counts/counts_boarding.xml") ;
		config.ptCounts().setAlightCountsFileName(inputDir + "counts/counts_alighting.xml") ;
		//		config.ptCounts().setDistanceFilter(30000.) ; // why?
		//		config.ptCounts().setDistanceFilterCenterNode("7") ; // why?
		config.ptCounts().setOutputFormat("txt");
		config.ptCounts().setCountsScaleFactor(1.) ;
		// ---
		return config;
	}

	private static class DummyMobsimFactory implements MobsimFactory {
		@Override
		public Mobsim createMobsim(final Scenario sc, final EventsManager eventsManager) {
			return new Mobsim() {
				@Override public void run() { }
			} ;
		}
	}

}
