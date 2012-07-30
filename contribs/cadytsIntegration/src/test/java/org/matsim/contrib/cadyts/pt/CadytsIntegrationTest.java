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
import java.util.List;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.cadyts.pt.utils.CalibrationStatReader;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.MatsimCountsReader;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.testcases.MatsimTestUtils;

import utilities.io.tabularfileparser.TabularFileParser;
import utilities.misc.DynamicData;
import cadyts.measurements.SingleLinkMeasurement;

public class CadytsIntegrationTest {
	
	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public final void testInitialization() {
		String inputDir = this.utils.getClassInputDirectory();

		String configFile = inputDir + "equil_config.xml";
		Config config = this.utils.loadConfig(configFile);

		config.controler().setLastIteration(0);

		final Controler controler = new Controler(config);
		controler.setCreateGraphs(false);
		controler.setWriteEventsInterval(0);
		controler.setDumpDataAtEnd(true);
		controler.setMobsimFactory(new DummyMobsimFactory());
		controler.run();

		List <PlanStrategy> strategyList = controler.getStrategyManager().getStrategies();
		CadytsPtPlanStrategy cadytsStrategy = null;
		for (PlanStrategy strategy : strategyList) {
			if (strategy.getClass() == CadytsPtPlanStrategy.class) {
				cadytsStrategy = (CadytsPtPlanStrategy) strategy;
				break;
			}
		}
		Assert.assertNotNull("CadytsPtPlanStrategy could not be found.", cadytsStrategy);
		
		//test calibration settings
		Assert.assertEquals(true, cadytsStrategy.getCalibrator().getBruteForce());
		Assert.assertEquals(false, cadytsStrategy.getCalibrator().getCenterRegression());
		Assert.assertEquals(Integer.MAX_VALUE, cadytsStrategy.getCalibrator().getFreezeIteration());
		Assert.assertEquals(8.0, cadytsStrategy.getCalibrator().getMinStddev(SingleLinkMeasurement.TYPE.FLOW_VEH_H), MatsimTestUtils.EPSILON);
		Assert.assertEquals(1, cadytsStrategy.getCalibrator().getPreparatoryIterations());
		Assert.assertEquals(0.95, cadytsStrategy.getCalibrator().getRegressionInertia(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(1.0, cadytsStrategy.getCalibrator().getVarianceScale(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(3600.0, cadytsStrategy.getCalibrator().getTimeBinSize_s(), MatsimTestUtils.EPSILON);

	}

	@Test
	public final void testCalibration() throws IOException {
		String inputDir = this.utils.getClassInputDirectory();
		String outputDir = this.utils.getOutputDirectory();

		String configFile = inputDir + "equil_config.xml";
		Config config = this.utils.loadConfig(configFile);

		final Controler controler = new Controler(config);
		controler.setCreateGraphs(false);
		controler.setWriteEventsInterval(0);
		controler.setDumpDataAtEnd(true);
		controler.run();
		
		
		//scenario data  test
		Assert.assertNotNull("config is null" , controler.getConfig());
		Assert.assertEquals("Different number of links in network.", controler.getNetwork().getLinks().size() , 23 );
		Assert.assertEquals("Different number of nodes in network.", controler.getNetwork().getNodes().size() , 15 );
		Assert.assertNotNull("Transit schedule is null.", controler.getScenario().getTransitSchedule());
		Assert.assertEquals("Num. of trLines is wrong.", controler.getScenario().getTransitSchedule().getTransitLines().size() , 1);
		Assert.assertEquals("Num of facilities in schedule is wrong.", controler.getScenario().getTransitSchedule().getFacilities().size() , 5);
		Assert.assertNotNull("Population is null.", controler.getScenario().getPopulation());
		Assert.assertEquals("Num. of persons in population is wrong.", controler.getPopulation().getPersons().size() , 4);
		Assert.assertEquals("Scale factor is wrong.", controler.getScenario().getConfig().ptCounts().getCountsScaleFactor(), 1.0, MatsimTestUtils.EPSILON);
		Assert.assertEquals("Distance filter is wrong.", controler.getScenario().getConfig().ptCounts().getDistanceFilter() , 30000.0, MatsimTestUtils.EPSILON);
		Assert.assertEquals("DistanceFilterCenterNode is wrong.", controler.getScenario().getConfig().ptCounts().getDistanceFilterCenterNode(), "7");
		//counts
		Assert.assertEquals("Occupancy count file is wrong.", controler.getScenario().getConfig().ptCounts().getOccupancyCountsFileName(), inputDir + "counts/counts_occupancy.xml");
		Counts occupCounts = new Counts();
		new MatsimCountsReader(occupCounts).readFile(controler.getScenario().getConfig().ptCounts().getOccupancyCountsFileName());
		Count count =  occupCounts.getCount(new IdImpl("stop1"));
		Assert.assertEquals("Occupancy counts description is wrong", occupCounts.getDescription(), "counts values for equil net");
		Assert.assertEquals("CsId is wrong.", count.getCsId() , "stop1");
		Assert.assertEquals("Volume of hour 4 is wrong", count.getVolume(7).getValue(), 4.0 , MatsimTestUtils.EPSILON);
		Assert.assertEquals("Max count volume is wrong.", count.getMaxVolume().getValue(), 4.0 , MatsimTestUtils.EPSILON);

		// test resulting simulation volumes
		{
			String outCounts = outputDir + "ITERS/it.10/10.simCountCompareOccupancy.txt";
			CountsReader reader = new CountsReader(outCounts);
			double[] simValues;
			double[] realValues;

			Id stopId1 = new IdImpl("stop1");
			simValues = reader.getSimulatedValues(stopId1);
			realValues= reader.getRealValues(stopId1);
			Assert.assertEquals("Volume of hour 6 is wrong", 4.0, simValues[6], MatsimTestUtils.EPSILON);
			Assert.assertEquals("Volume of hour 6 is wrong", 4.0, realValues[6], MatsimTestUtils.EPSILON);

			Id stopId2 = new IdImpl("stop2");
			simValues = reader.getSimulatedValues(stopId2);
			realValues= reader.getRealValues(stopId2);
			Assert.assertEquals("Volume of hour 6 is wrong", 2.0, simValues[6], MatsimTestUtils.EPSILON);
			Assert.assertEquals("Volume of hour 6 is wrong", 1.0, realValues[6] , MatsimTestUtils.EPSILON);

			Id stopId6 = new IdImpl("stop6");
			simValues = reader.getSimulatedValues(stopId6);
			realValues= reader.getRealValues(stopId6);
			Assert.assertEquals("Volume of hour 6 is wrong", 0.0, simValues[6], MatsimTestUtils.EPSILON);
			Assert.assertEquals("Volume of hour 6 is wrong", 2.0, realValues[6], MatsimTestUtils.EPSILON);

			Id stopId10 = new IdImpl("stop10");
			simValues = reader.getSimulatedValues(stopId10);
			realValues= reader.getRealValues(stopId10);
			Assert.assertEquals("Volume of hour 6 is wrong", 2.0, simValues[6], MatsimTestUtils.EPSILON);
			Assert.assertEquals("Volume of hour 6 is wrong", 1.0, realValues[6], MatsimTestUtils.EPSILON);
	    
		}
	}

	
	@Test 
	public final void testCalibrationTwo() throws IOException {
		String inputDir = this.utils.getClassInputDirectory();
		String outputDir = this.utils.getOutputDirectory();

		String configFile = inputDir + "equil_config2.xml";
		Config config = this.utils.loadConfig(configFile);

		final Controler controler = new Controler(config);
		controler.setCreateGraphs(false);
		controler.setWriteEventsInterval(0);
		controler.setDumpDataAtEnd(true);
		
//		controler.addControlerListener( new StartupListener() {
//			
//			@Override
//			public void notifyStartup(StartupEvent event) {
//				CadytsPtConfigGroup cadytsPtConfig = (CadytsPtConfigGroup) event.getControler().getConfig().getModule(CadytsPtConfigGroup.GROUP_NAME);
//				cadytsPtConfig.setTimeBinSize(3600) ;
//			}
//		}) ;
		
		
		controler.run();

		//scenario data  test
		Assert.assertNotNull("config is null" , controler.getConfig());
		Assert.assertEquals("Different number of links in network.", controler.getNetwork().getLinks().size() , 23 );
		Assert.assertEquals("Different number of nodes in network.", controler.getNetwork().getNodes().size() , 15 );
		Assert.assertNotNull("Transit schedule is null.", controler.getScenario().getTransitSchedule());
		Assert.assertEquals("Num. of trLines is wrong.", controler.getScenario().getTransitSchedule().getTransitLines().size() , 1);
		Assert.assertEquals("Num of facilities in schedule is wrong.", controler.getScenario().getTransitSchedule().getFacilities().size() , 5);
		Assert.assertNotNull("Population is null.", controler.getScenario().getPopulation());
		Assert.assertEquals("Num. of persons in population is wrong.", controler.getPopulation().getPersons().size() , 4);
		Assert.assertEquals("Scale factor is wrong.", controler.getScenario().getConfig().ptCounts().getCountsScaleFactor(), 1.0, MatsimTestUtils.EPSILON);
		Assert.assertEquals("Distance filter is wrong.", controler.getScenario().getConfig().ptCounts().getDistanceFilter() , 30000.0, MatsimTestUtils.EPSILON);
		Assert.assertEquals("DistanceFilterCenterNode is wrong.", controler.getScenario().getConfig().ptCounts().getDistanceFilterCenterNode(), "7");
		//counts
		Assert.assertEquals("Occupancy count file is wrong.", controler.getScenario().getConfig().ptCounts().getOccupancyCountsFileName(), inputDir + "counts/counts_occupancy.xml");
		Counts occupCounts = new Counts();
		new MatsimCountsReader(occupCounts).readFile(controler.getScenario().getConfig().ptCounts().getOccupancyCountsFileName());
		Count count =  occupCounts.getCount(new IdImpl("stop1"));
		Assert.assertEquals("Occupancy counts description is wrong", occupCounts.getDescription(), "counts values for equil net");
		Assert.assertEquals("CsId is wrong.", count.getCsId() , "stop1");
		Assert.assertEquals("Volume of hour 4 is wrong", count.getVolume(7).getValue(), 4.0 , MatsimTestUtils.EPSILON);
		Assert.assertEquals("Max count volume is wrong.", count.getMaxVolume().getValue(), 4.0 , MatsimTestUtils.EPSILON);

		// test resulting simulation volumes
			String outCounts = outputDir + "ITERS/it.10/10.simCountCompareOccupancy.txt";
			CountsReader reader = new CountsReader(outCounts);
			double[] simValues;
			double[] realValues;

			Id stopId1 = new IdImpl("stop1");
			simValues = reader.getSimulatedValues(stopId1);
			realValues= reader.getRealValues(stopId1);
			Assert.assertEquals("Volume of hour 6 is wrong", 4.0, simValues[6], MatsimTestUtils.EPSILON);
			Assert.assertEquals("Volume of hour 6 is wrong", 4.0, realValues[6], MatsimTestUtils.EPSILON);

			Id stopId2 = new IdImpl("stop2");
			simValues = reader.getSimulatedValues(stopId2);
			realValues= reader.getRealValues(stopId2);
			Assert.assertEquals("Volume of hour 6 is wrong", 2.0, simValues[6], MatsimTestUtils.EPSILON);
			Assert.assertEquals("Volume of hour 6 is wrong", 1.0, realValues[6] , MatsimTestUtils.EPSILON);

			Id stopId6 = new IdImpl("stop6");
			simValues = reader.getSimulatedValues(stopId6);
			realValues= reader.getRealValues(stopId6);
			Assert.assertEquals("Volume of hour 6 is wrong", 0.0, simValues[6], MatsimTestUtils.EPSILON);
			Assert.assertEquals("Volume of hour 6 is wrong", 2.0, realValues[6], MatsimTestUtils.EPSILON);

			Id stopId10 = new IdImpl("stop10");
			simValues = reader.getSimulatedValues(stopId10);
			realValues= reader.getRealValues(stopId10);
			Assert.assertEquals("Volume of hour 6 is wrong", 2.0, simValues[6], MatsimTestUtils.EPSILON);
			Assert.assertEquals("Volume of hour 6 is wrong", 1.0, realValues[6], MatsimTestUtils.EPSILON);
	

		// test calibration statistics
			String testCalibStatPath = outputDir + "calibration-stats.txt";
			CalibrationStatReader calibrationStatReader = new CalibrationStatReader();
			new TabularFileParser().parse(testCalibStatPath, calibrationStatReader);

			CalibrationStatReader.StatisticsData outStatData= calibrationStatReader.getCalStatMap().get(Integer.valueOf(6));
			//	old values, seem not to work
			//			Assert.assertEquals("different Count_ll", "-1.546875", outStatData.getCount_ll() );
			//			Assert.assertEquals("different Count_ll_pred_err",  "9.917082938182276E-8" , outStatData.getCount_ll_pred_err() );
			//			Assert.assertEquals("different Link_lambda_avg", "0.0013507168476099964", outStatData.getLink_lambda_avg() );
			//			Assert.assertEquals("different Link_lambda_max", "0.031434867572002166" , outStatData.getLink_lambda_max() );
			//			Assert.assertEquals("different Link_lambda_min", "0.0", outStatData.getLink_lambda_min() );
			//			Assert.assertEquals("different Link_lambda_stddev", "0.0058320747961925256" , outStatData.getLink_lambda_stddev());
			//			Assert.assertEquals("different P2p_ll", "--" , outStatData.getP2p_ll());
			//			Assert.assertEquals("different Plan_lambda_avg", "0.04322293912351989", outStatData.getPlan_lambda_avg() );
			//			Assert.assertEquals("different Plan_lambda_max", "0.04715229919344063" , outStatData.getPlan_lambda_max() );
			//			Assert.assertEquals("different Plan_lambda_min", "0.03929357905359915" , outStatData.getPlan_lambda_min() );
			//			Assert.assertEquals("different Plan_lambda_stddev", "0.004200662608832472" , outStatData.getPlan_lambda_stddev());
			//			Assert.assertEquals("different Total_ll", "-1.546875", outStatData.getTotal_ll() );
			Assert.assertEquals("different Count_ll", "-0.046875", outStatData.getCount_ll() );
			Assert.assertEquals("different Count_ll_pred_err",  "1.9637069748057535E-15" , outStatData.getCount_ll_pred_err() );
			Assert.assertEquals("different Link_lambda_avg", /*"-2.2604922388914356E-10"*/ "-2.411191721484198E-10", outStatData.getLink_lambda_avg() );
			Assert.assertEquals("different Link_lambda_max", "0.0" , outStatData.getLink_lambda_max() );
			Assert.assertEquals("different Link_lambda_min", "-7.233575164452593E-9", outStatData.getLink_lambda_min() );
			Assert.assertEquals("different Link_lambda_stddev", /*"1.261054219517188E-9"*/ "1.301180101697312E-9" , outStatData.getLink_lambda_stddev());
			Assert.assertEquals("different P2p_ll", "--" , outStatData.getP2p_ll());
			Assert.assertEquals("different Plan_lambda_avg", "-7.233575164452594E-9", outStatData.getPlan_lambda_avg() );
			Assert.assertEquals("different Plan_lambda_max", "-7.233575164452593E-9" , outStatData.getPlan_lambda_max() );
			Assert.assertEquals("different Plan_lambda_min", "-7.233575164452593E-9" , outStatData.getPlan_lambda_min() );
			Assert.assertEquals("different Plan_lambda_stddev", "0.0" , outStatData.getPlan_lambda_stddev());
			Assert.assertEquals("different Total_ll", "-0.046875", outStatData.getTotal_ll() );
		
			
		//test link offsets
		final TransitSchedule schedule = controler.getScenario().getTransitSchedule();
		String linkOffsetFile = outputDir + "ITERS/it.10/10.linkCostOffsets.xml";
		CadytsPtLinkCostOffsetsXMLFileIO offsetReader = new CadytsPtLinkCostOffsetsXMLFileIO (schedule);
		DynamicData<TransitStopFacility> stopOffsets = offsetReader.read(linkOffsetFile);
	
		TransitStopFacility stop1 = schedule.getFacilities().get(stopId1);
		TransitStopFacility stop2 = schedule.getFacilities().get(stopId2);
		TransitStopFacility stop6 = schedule.getFacilities().get(stopId6);
		TransitStopFacility stop10 = schedule.getFacilities().get(stopId10);
	
		//find first offset value different from null to compare. Useful to test with different time bin sizes
		int binIndex=-1;
		boolean isZero;
		do {
			binIndex++;
			isZero = (Math.abs(stopOffsets.getBinValue(stop2 , binIndex) - 0.0) < MatsimTestUtils.EPSILON);
		}while (isZero && binIndex<86400);
		
		Assert.assertEquals("Wrong bin index for first link offset", 6, binIndex);
		Assert.assertEquals("Wrong link offset of stop 10", -7.231566167513828E-9, stopOffsets.getBinValue(stop10 , binIndex), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Wrong link offset of stop 2", -7.231566167513828E-9, stopOffsets.getBinValue(stop2 , binIndex), MatsimTestUtils.EPSILON);
	}


	//test with time bin size = 2hrs 
	@Test 
	public final void testCalibrationLinkOffsets() throws IOException {
		String inputDir = this.utils.getClassInputDirectory();
		String outputDir = this.utils.getOutputDirectory();

		String configFile = inputDir + "equil_configLinkOffsets.xml";
		Config config = this.utils.loadConfig(configFile);
		
		final Controler controler = new Controler(config);
		controler.setCreateGraphs(false);
		controler.setWriteEventsInterval(0);
		controler.setDumpDataAtEnd(true);
		
		
//		controler.addControlerListener( new StartupListener() {
//			
//			@Override
//			public void notifyStartup(StartupEvent event) {
//				CadytsPtConfigGroup cadytsPtConfig = (CadytsPtConfigGroup) event.getControler().getConfig().getModule(CadytsPtConfigGroup.GROUP_NAME);
//				cadytsPtConfig.setTimeBinSize(3600) ;
//			}
//		}) ;
		
		controler.run();

		// test resulting simulation volumes
			String outCounts = outputDir + "ITERS/it.10/10.simCountCompareOccupancy.txt";
			CountsReader reader = new CountsReader(outCounts);
			double[] simValues;
			double[] realValues;

			Id stopId1 = new IdImpl("stop1");
			simValues = reader.getSimulatedValues(stopId1);
			realValues= reader.getRealValues(stopId1);
			Assert.assertEquals("Volume of hour 6 is wrong", 4.0, simValues[6], MatsimTestUtils.EPSILON);
			Assert.assertEquals("Volume of hour 6 is wrong", 4.0, realValues[6], MatsimTestUtils.EPSILON);

			Id stopId2 = new IdImpl("stop2");
			simValues = reader.getSimulatedValues(stopId2);
			realValues= reader.getRealValues(stopId2);
			Assert.assertEquals("Volume of hour 6 is wrong", 2.0, simValues[6], MatsimTestUtils.EPSILON);
			Assert.assertEquals("Volume of hour 6 is wrong", 1.0, realValues[6] , MatsimTestUtils.EPSILON);

			Id stopId6 = new IdImpl("stop6");
			simValues = reader.getSimulatedValues(stopId6);
			realValues= reader.getRealValues(stopId6);
			Assert.assertEquals("Volume of hour 6 is wrong", 0.0, simValues[6], MatsimTestUtils.EPSILON);
			Assert.assertEquals("Volume of hour 6 is wrong", 2.0, realValues[6], MatsimTestUtils.EPSILON);

			Id stopId10 = new IdImpl("stop10");
			simValues = reader.getSimulatedValues(stopId10);
			realValues= reader.getRealValues(stopId10);
			Assert.assertEquals("Volume of hour 6 is wrong", 2.0, simValues[6], MatsimTestUtils.EPSILON);
			Assert.assertEquals("Volume of hour 6 is wrong", 1.0, realValues[6], MatsimTestUtils.EPSILON);
		

		//test link offsets
		final TransitSchedule schedule = controler.getScenario().getTransitSchedule();
		String linkOffsetFile = outputDir + "ITERS/it.10/10.linkCostOffsets.xml";
		CadytsPtLinkCostOffsetsXMLFileIO offsetReader = new CadytsPtLinkCostOffsetsXMLFileIO (schedule);
		DynamicData<TransitStopFacility> stopOffsets = offsetReader.read(linkOffsetFile);
	
		TransitStopFacility stop1 = schedule.getFacilities().get(stopId1);
		TransitStopFacility stop2 = schedule.getFacilities().get(stopId2);
		TransitStopFacility stop6 = schedule.getFacilities().get(stopId6);
		TransitStopFacility stop10 = schedule.getFacilities().get(stopId10);
	
		//find first offset value different from null to compare. Useful to test with different time bin sizes
		int binIndex=-1;
		boolean isZero;
		do {
			binIndex++;
			isZero = (Math.abs(stopOffsets.getBinValue(stop2 , binIndex) - 0.0) < MatsimTestUtils.EPSILON);
		}while (isZero && binIndex<86400);
		
		Assert.assertEquals("Wrong Bin index for first link offset", 3, binIndex);
		Assert.assertEquals("Wrong link offset of stop 1", -2.8926267665165917E-8, stopOffsets.getBinValue(stop1 , binIndex), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Wrong link offset of stop 2", -1.446313233891264E-8, stopOffsets.getBinValue(stop2 , binIndex), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Wrong link offset of stop 10", -1.446313233891264E-8, stopOffsets.getBinValue(stop10 , binIndex), MatsimTestUtils.EPSILON);
	}

	
	
	private static class DummyMobsim implements Mobsim {
		public DummyMobsim() {
		}
		@Override
		public void run() {
		}
	}

	private static class DummyMobsimFactory implements MobsimFactory {
		@Override
		public Mobsim createMobsim(final Scenario sc, final EventsManager eventsManager) {
			return new DummyMobsim();
		}
	}

}
