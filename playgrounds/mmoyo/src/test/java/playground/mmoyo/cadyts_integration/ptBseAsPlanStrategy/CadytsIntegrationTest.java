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
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.testcases.MatsimTestUtils;

import playground.mmoyo.analysis.counts.reader.CountsReader;
import playground.mmoyo.cadyts_integration.ptBseAsPlanStrategy.NewPtBsePlanStrategy;
import cadyts.utilities.io.tabularFileParser.TabularFileParser;

public class CadytsIntegrationTest extends MatsimTestCase {

	@Rule public MatsimTestUtils utils = new MatsimTestUtils();
	@Test
	public final void testCalibration() {

		String inputDir = this.getInputDirectory();
		String outputDir = this.getOutputDirectory();

		System.out.println(" Input Dir " + this.getInputDirectory() );
		System.out.println(" Output Dir " + this.getOutputDirectory() );

		String configFile = inputDir + "equil_config.xml";
		Config config = ConfigUtils.loadConfig(configFile);


		final Controler controler = new Controler(config);
		controler.setCreateGraphs(false);
		controler.setWriteEventsInterval(0);
		controler.setDumpDataAtEnd(false);
		controler.setOverwriteFiles(true);
		controler.run();

		//scenario data  test
		Assert.assertNotNull("config is null" , controler.getConfig());
		Assert.assertEquals("Diferent number of links in network.", controler.getNetwork().getLinks().size() , 23 );
		Assert.assertEquals("Diferent number of nodes in network.", controler.getNetwork().getNodes().size() , 15 );
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
		Counts occupCounts = controler.getScenario().getScenarioElement(org.matsim.counts.Counts.class);
		controler.getCounts();
		Count count =  occupCounts.getCount(new IdImpl("stop1"));
		Assert.assertEquals("Occupancy counts description is wrong", occupCounts.getDescription(), "counts values for equil net");
		Assert.assertEquals("CsId is wrong.", count.getCsId() , "stop1");
		Assert.assertEquals("Volume of hour 4 is wrong", count.getVolume(7).getValue(), 4.0 , MatsimTestUtils.EPSILON);
		Assert.assertEquals("Max count volume is wrong.", count.getMaxVolume().getValue(), 4.0 , MatsimTestUtils.EPSILON);

		//test that NewPtBsePlanStrategy is present as replanning strategy
		List <PlanStrategy> strategyList = controler.getStrategyManager().getStrategies(); 
		NewPtBsePlanStrategy ptBsestrategy = null;
		int i=0;
		do{
			if (strategyList.get(i).getClass().equals(NewPtBsePlanStrategy.class)){
				ptBsestrategy = (NewPtBsePlanStrategy)strategyList.get(i); 	
			}
			i++;
		}while (ptBsestrategy==null && i< strategyList.size());
		Assert.assertNotNull(ptBsestrategy);

		//test calibration settings
		String expectedCalibSettings ="[BruteForce=true][CenterRegression=false][FreezeIteration=2147483647][MinStddev=8.0][PreparatoryIterations=1][RegressionInertia=0.95][VarianceScale=1.0]";
		Assert.assertEquals("calibrator settings do not match" , ptBsestrategy.getCalibratorSettings(), expectedCalibSettings );

		//  results  
		// Test first that the calibrationStatReader works properly 
		TabularFileParser tabularFileParser = new TabularFileParser();
		String calibStatFile = inputDir + "input_calibration-stats.txt";
		CalibrationStatReader calibrationStatReader = new CalibrationStatReader();
		try {
			tabularFileParser.parse(calibStatFile, calibrationStatReader);
		} catch (IOException e) {
			e.printStackTrace();
		}
		CalibrationStatReader.StatisticsData statData6= calibrationStatReader.getCalStatMap().get(Integer.valueOf(6));
		Assert.assertEquals("diferrent Count_ll", statData6.getCount_ll() , "-1.546875" );
		Assert.assertEquals("diferrent Count_ll_pred_err", statData6.getCount_ll_pred_err() , "9.917082938182276E-8" );
		Assert.assertEquals("diferrent Link_lambda_avg", statData6.getLink_lambda_avg() , "0.0013507168476099964" );
		Assert.assertEquals("diferrent Link_lambda_max", statData6.getLink_lambda_max() , "0.031434867572002166" );
		Assert.assertEquals("diferrent Link_lambda_min", statData6.getLink_lambda_min() , "0.0" );
		Assert.assertEquals("diferrent Link_lambda_stddev", statData6.getLink_lambda_stddev() , "0.0058320747961925256" );
		Assert.assertEquals("diferrent P2p_ll", statData6.getP2p_ll() , "--" );
		Assert.assertEquals("diferrent Plan_lambda_avg", statData6.getPlan_lambda_avg() , "0.04322293912351989" );
		Assert.assertEquals("diferrent Plan_lambda_max", statData6.getPlan_lambda_max() , "0.04715229919344063" );
		Assert.assertEquals("diferrent Plan_lambda_min", statData6.getPlan_lambda_min() , "0.03929357905359915" );
		Assert.assertEquals("diferrent Plan_lambda_stddev", statData6.getPlan_lambda_stddev() , "0.004200662608832472" );
		Assert.assertEquals("diferrent Total_ll", statData6.getTotal_ll() , "-1.546875" );

		//now test calibration-stats.txt from calibration
		String testCalibStatPath = outputDir + "calibration-stats.txt";
		calibrationStatReader = new CalibrationStatReader();
		try {
			tabularFileParser.parse(testCalibStatPath, calibrationStatReader);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		CalibrationStatReader.StatisticsData outStatData= calibrationStatReader.getCalStatMap().get(Integer.valueOf(6));
		Assert.assertEquals("diferrent Count_ll", "-1.546875", outStatData.getCount_ll() );
		Assert.assertEquals("diferrent Count_ll_pred_err",  "9.917082938182276E-8" , outStatData.getCount_ll_pred_err() );
		Assert.assertEquals("diferrent Link_lambda_avg", "0.0013507168476099964", outStatData.getLink_lambda_avg() );
		Assert.assertEquals("diferrent Link_lambda_max", "0.031434867572002166" , outStatData.getLink_lambda_max() );
		Assert.assertEquals("diferrent Link_lambda_min", "0.0", outStatData.getLink_lambda_min() );
		Assert.assertEquals("diferrent Link_lambda_stddev", "0.0058320747961925256" , outStatData.getLink_lambda_stddev());
		Assert.assertEquals("diferrent P2p_ll", "--" , outStatData.getP2p_ll());
		Assert.assertEquals("diferrent Plan_lambda_avg", "0.04322293912351989", outStatData.getPlan_lambda_avg() );
		Assert.assertEquals("diferrent Plan_lambda_max", "0.04715229919344063" , outStatData.getPlan_lambda_max() );
		Assert.assertEquals("diferrent Plan_lambda_min", "0.03929357905359915" , outStatData.getPlan_lambda_min() );
		Assert.assertEquals("diferrent Plan_lambda_stddev", "0.004200662608832472" , outStatData.getPlan_lambda_stddev());
		Assert.assertEquals("diferrent Total_ll", "-1.546875", outStatData.getTotal_ll() );

		//test resulting simulation volumes
		String outCounts = outputDir + "ITERS/it.10/10.simCountCompareOccupancy.txt";
		CountsReader reader = new CountsReader(outCounts);
		double[] simValues;
		double[] realValues;
		Id stopId;

		stopId= new IdImpl("stop1");
		simValues = reader.getStopSimCounts(stopId);
		realValues= reader.getStopCounts(stopId);
		Assert.assertEquals("Volume of hour 6 is wrong", simValues[6], 4.0 , MatsimTestUtils.EPSILON);
		Assert.assertEquals("Volume of hour 6 is wrong", realValues[6], 4.0 , MatsimTestUtils.EPSILON);

		stopId= new IdImpl("stop2");
		simValues = reader.getStopSimCounts(stopId);
		realValues= reader.getStopCounts(stopId);
		Assert.assertEquals("Volume of hour 6 is wrong", simValues[6], 1.0 , MatsimTestUtils.EPSILON);
		Assert.assertEquals("Volume of hour 6 is wrong", realValues[6], 1.0 , MatsimTestUtils.EPSILON);

		stopId= new IdImpl("stop6");
		simValues = reader.getStopSimCounts(stopId);
		realValues= reader.getStopCounts(stopId);
		Assert.assertEquals("Volume of hour 6 is wrong", simValues[6], 2.0 , MatsimTestUtils.EPSILON);
		Assert.assertEquals("Volume of hour 6 is wrong", realValues[6], 2.0 , MatsimTestUtils.EPSILON);

		stopId= new IdImpl("stop10");
		simValues = reader.getStopSimCounts(stopId);
		realValues= reader.getStopCounts(stopId);
		Assert.assertEquals("Volume of hour 6 is wrong", simValues[6], 1.0 , MatsimTestUtils.EPSILON);
		Assert.assertEquals("Volume of hour 6 is wrong", realValues[6], 1.0 , MatsimTestUtils.EPSILON);

	}



}
