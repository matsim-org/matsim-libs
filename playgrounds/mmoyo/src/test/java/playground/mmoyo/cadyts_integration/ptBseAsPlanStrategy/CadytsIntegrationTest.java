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

package playground.mmoyo.cadyts_integration.ptBseAsPlanStrategy;

import java.io.IOException;
import java.util.List;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
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
import org.matsim.testcases.MatsimTestUtils;

import playground.mmoyo.analysis.counts.reader.CountsReader;
import playground.mmoyo.cadyts_integration.ptBseAsPlanStrategy.utils.CalibrationStatReader;
import utilities.io.tabularfileparser.TabularFileParser;
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

		// test that NewPtBsePlanStrategy is present as replanning strategy
		List <PlanStrategy> strategyList = controler.getStrategyManager().getStrategies();
		NewPtBsePlanStrategy ptBseStrategy = null;
		for (PlanStrategy strategy : strategyList) {
			if (strategy.getClass() == NewPtBsePlanStrategy.class) {
				ptBseStrategy = (NewPtBsePlanStrategy) strategy;
				break;
			}
		}
		Assert.assertNotNull("PtBsePlanStrategy could not be found.", ptBseStrategy);

		//test calibration settings
		Assert.assertEquals(true, ptBseStrategy.getCalibrator().getBruteForce());
		Assert.assertEquals(false, ptBseStrategy.getCalibrator().getCenterRegression());
		Assert.assertEquals(Integer.MAX_VALUE, ptBseStrategy.getCalibrator().getFreezeIteration());
		Assert.assertEquals(8.0, ptBseStrategy.getCalibrator().getMinStddev(SingleLinkMeasurement.TYPE.FLOW_VEH_H), MatsimTestUtils.EPSILON);
		Assert.assertEquals(1, ptBseStrategy.getCalibrator().getPreparatoryIterations());
		Assert.assertEquals(0.95, ptBseStrategy.getCalibrator().getRegressionInertia(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(1.0, ptBseStrategy.getCalibrator().getVarianceScale(), MatsimTestUtils.EPSILON);
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
			Id stopId;

			stopId = new IdImpl("stop1");
			simValues = reader.getSimulatedValues(stopId);
			realValues= reader.getRealValues(stopId);
			Assert.assertEquals("Volume of hour 6 is wrong", simValues[6], 4.0 , MatsimTestUtils.EPSILON);
			Assert.assertEquals("Volume of hour 6 is wrong", realValues[6], 4.0 , MatsimTestUtils.EPSILON);

			stopId = new IdImpl("stop2");
			simValues = reader.getSimulatedValues(stopId);
			realValues= reader.getRealValues(stopId);
			Assert.assertEquals("Volume of hour 6 is wrong", simValues[6], 1.0 , MatsimTestUtils.EPSILON);
			Assert.assertEquals("Volume of hour 6 is wrong", realValues[6], 1.0 , MatsimTestUtils.EPSILON);

			stopId = new IdImpl("stop6");
			simValues = reader.getSimulatedValues(stopId);
			realValues= reader.getRealValues(stopId);
			Assert.assertEquals("Volume of hour 6 is wrong", simValues[6], 2.0 , MatsimTestUtils.EPSILON);
			Assert.assertEquals("Volume of hour 6 is wrong", realValues[6], 2.0 , MatsimTestUtils.EPSILON);

			stopId = new IdImpl("stop10");
			simValues = reader.getSimulatedValues(stopId);
			realValues= reader.getRealValues(stopId);
			Assert.assertEquals("Volume of hour 6 is wrong", simValues[6], 1.0 , MatsimTestUtils.EPSILON);
			Assert.assertEquals("Volume of hour 6 is wrong", realValues[6], 1.0 , MatsimTestUtils.EPSILON);
		}

		// test calibration statistics
		{
		String testCalibStatPath = outputDir + "calibration-stats.txt";
		CalibrationStatReader calibrationStatReader = new CalibrationStatReader();
		new TabularFileParser().parse(testCalibStatPath, calibrationStatReader);

		CalibrationStatReader.StatisticsData outStatData= calibrationStatReader.getCalStatMap().get(Integer.valueOf(6));
		Assert.assertEquals("Different Count_ll", "-1.546875", outStatData.getCount_ll() );
		Assert.assertEquals("Different Count_ll_pred_err",  "9.917082938182276E-8" , outStatData.getCount_ll_pred_err() );
		Assert.assertEquals("Different Link_lambda_avg", "0.0013507168476099964", outStatData.getLink_lambda_avg() );
		Assert.assertEquals("Different Link_lambda_max", "0.031434867572002166" , outStatData.getLink_lambda_max() );
		Assert.assertEquals("Different Link_lambda_min", "0.0", outStatData.getLink_lambda_min() );
		Assert.assertEquals("Different Link_lambda_stddev", "0.0058320747961925256" , outStatData.getLink_lambda_stddev());
		Assert.assertEquals("Different P2p_ll", "--" , outStatData.getP2p_ll());
		Assert.assertEquals("Different Plan_lambda_avg", "0.04322293912351989", outStatData.getPlan_lambda_avg() );
		Assert.assertEquals("Different Plan_lambda_max", "0.04715229919344063" , outStatData.getPlan_lambda_max() );
		Assert.assertEquals("Different Plan_lambda_min", "0.03929357905359915" , outStatData.getPlan_lambda_min() );
		Assert.assertEquals("Different Plan_lambda_stddev", "0.004200662608832472" , outStatData.getPlan_lambda_stddev());
		Assert.assertEquals("Different Total_ll", "-1.546875", outStatData.getTotal_ll() );
		}

	}

	private static class DummyMobsim implements Mobsim {
		public DummyMobsim() {
		}
		@Override
		public void run() {
		}
	}

	private static class DummyMobsimFactory implements MobsimFactory {
		private final int count = 1;
		@Override
		public Mobsim createMobsim(final Scenario sc, final EventsManager eventsManager) {
			return new DummyMobsim();
		}
	}

}
