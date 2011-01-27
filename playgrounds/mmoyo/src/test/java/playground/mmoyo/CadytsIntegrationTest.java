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

package playground.mmoyo;

import java.io.IOException;
import java.util.List;

import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.core.basic.v01.IdImpl;

//import org.junit.Ignore;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.Assert;

import playground.mmoyo.cadyts_integration.ptBseAsPlanStrategy.NewPtBsePlanStrategy;
import cadyts.interfaces.matsim.MATSimUtilityModificationCalibrator;
import cadyts.utilities.io.tabularFileParser.TabularFileParser;

public class CadytsIntegrationTest extends MatsimTestCase {
	
	private static final MATSimUtilityModificationCalibrator<TransitStopFacility> String = null;
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();
	@Test
	@Ignore("not yet fully implemented")
	public final void testCalibration() {
		System.out.println("this.getInputDirectory() "      + this.getInputDirectory() );
		System.out.println("this.getClassInputDirectory() " + this.getClassInputDirectory());
		System.out.println("this.getOutputDirectory() " + this.getOutputDirectory());
		
		//String inputDir = this.getInputDirectory();
		String inputDir = "../playgrounds/mmoyo/test/input/playground/mmoyo/EquilCalibration/";
		
		String configFile = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/calibration/100plans_bestValues_config.xml";
		Config config = null;
		try {
			config = ConfigUtils.loadConfig(configFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		final Controler controler = new Controler(config);
		controler.setOverwriteFiles(true);
		controler.run();

		//scenario data  test
		Assert.assertNotNull("config is null" , controler.getConfig());
		Assert.assertEquals("Diferent number of links in network.", controler.getNetwork().getLinks().size() , 37591 );
		Assert.assertEquals("Diferent number of nodes in network.", controler.getNetwork().getNodes().size() , 17055 );
		Assert.assertNotNull("Transit schedule is null.", controler.getScenario().getTransitSchedule());
		Assert.assertEquals("Num. of trLines is wrong.", controler.getScenario().getTransitSchedule().getTransitLines().size() , 329);
		Assert.assertEquals("Num of facilities in schedule is wrong.", controler.getScenario().getTransitSchedule().getFacilities().size() , 8587);		
		Assert.assertNotNull("Population is null.", controler.getScenario().getPopulation());
		Assert.assertEquals("Num. of persons in population is wrong.", controler.getPopulation().getPersons().size() , 2);
		Assert.assertEquals("Scale factor is wrong.", controler.getScenario().getConfig().ptCounts().getCountsScaleFactor(), 10.0, MatsimTestUtils.EPSILON);
		Assert.assertEquals("Distance filter is wrong.", controler.getScenario().getConfig().ptCounts().getDistanceFilter() , 30000.0, MatsimTestUtils.EPSILON);
		Assert.assertEquals("DistanceFilterCenterNode is wrong.", controler.getScenario().getConfig().ptCounts().getDistanceFilterCenterNode(), "801030.0");
		//counts
		Assert.assertEquals("Occupancy count file is wrong.", controler.getScenario().getConfig().ptCounts().getOccupancyCountsFileName(), "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/lines344_M44/counts/chen/counts_occupancy_M44.xml");
		Counts occupCounts = controler.getScenario().getScenarioElement(org.matsim.counts.Counts.class);
		controler.getCounts();
		Count count =  occupCounts.getCount(new IdImpl("792200.5"));
		Assert.assertEquals("Occupancy counts description is wrong", occupCounts.getDescription(), "counts values from BVG 09.2009");
		Assert.assertEquals("CsId is wrong.", count.getCsId() , "R S+U Hermannstr. - Hermannstr./Mariendorfer Weg");
		Assert.assertEquals("Volume of hour 1 is wrong", count.getVolume(1).getValue(), 40.0 , MatsimTestUtils.EPSILON);
		Assert.assertEquals("Max count volume is wrong.", count.getMaxVolume().getValue(), 559.0 , MatsimTestUtils.EPSILON);
		
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
		String calibStatFile = inputDir + "input_calibration-stats.txt";
		CalibrationStatReader calibrationStatReader = new CalibrationStatReader();
		try {
			new TabularFileParser().parse(calibStatFile, calibrationStatReader);
		} catch (IOException e) {
			e.printStackTrace();
		}
		CalibrationStatReader.StatisticsData statData6= calibrationStatReader.getCalStatMap().get(Integer.valueOf(6));
		Assert.assertEquals("diferrent Count_ll", statData6.getCount_ll() , "-8520.428031642501" );
		Assert.assertEquals("diferrent Count_ll_pred_err", statData6.getCount_ll_pred_err() , "3023.1044688142874" );
		Assert.assertEquals("diferrent Link_lambda_avg", statData6.getLink_lambda_avg() , "-0.05958198651884198" );
		Assert.assertEquals("diferrent Link_lambda_max", statData6.getLink_lambda_max() , "18.83853210238568" );
		Assert.assertEquals("diferrent Link_lambda_min", statData6.getLink_lambda_min() , "-59.09955284636061" );
		Assert.assertEquals("diferrent Link_lambda_stddev", statData6.getLink_lambda_stddev() , "1.1566935710525457" );
		Assert.assertEquals("diferrent P2p_ll", statData6.getP2p_ll() , "--" );
		Assert.assertEquals("diferrent Plan_lambda_avg", statData6.getPlan_lambda_avg() , "-2.291334470647554" );
		Assert.assertEquals("diferrent Plan_lambda_max", statData6.getPlan_lambda_max() , "15.0" );
		Assert.assertEquals("diferrent Plan_lambda_min", statData6.getPlan_lambda_min() , "-15.0" );
		Assert.assertEquals("diferrent Plan_lambda_stddev", statData6.getPlan_lambda_stddev() , "11.356042894952115" );
		Assert.assertEquals("diferrent Total_ll", statData6.getTotal_ll() , "-8520.428031642501" );
		
		//test calibration-stats.txt from calibration
		String testCalibStatPath = config.controler().getOutputDirectory() + "calibration-stats.txt";
		calibrationStatReader = new CalibrationStatReader();
		try {  
			new TabularFileParser().parse(testCalibStatPath, calibrationStatReader);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		//test resulting simulation volumes
		//TODO first check that calibration is deterministic!!

		//creacion del error graph
	}

		
	
}
