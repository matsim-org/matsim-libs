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

package playground.kai.usecases.cadyts4freightchains;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.contrib.cadyts.general.CadytsConfigGroup;
import org.matsim.contrib.cadyts.general.CadytsCostOffsetsXMLFileIO;
import org.matsim.contrib.cadyts.general.ExpBetaPlanChangerWithCadytsPlanRegistration;
import org.matsim.contrib.cadyts.utils.CalibrationStatReader;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyFactory;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.MatsimCountsReader;
import org.matsim.testcases.MatsimTestUtils;

import utilities.io.tabularfileparser.TabularFileParser;
import utilities.misc.DynamicData;

/**
 * @author nagel
 */
public class CadytsFreightChainTest {

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public final void testCalibrationAsScoring() throws IOException {
		final double beta=30. ;
		final int lastIteration = 20 ;

		String inputDir = this.utils.getClassInputDirectory();
		String outputDir = this.utils.getOutputDirectory();

		final Config config = createTestConfig(inputDir, outputDir);

		config.controler().setLastIteration(lastIteration);

		config.planCalcScore().setBrainExpBeta(beta);

		StrategySettings strategySettings = new StrategySettings(new IdImpl("1"));
		strategySettings.setModuleName("ccc");
		strategySettings.setProbability(1.0);
		config.strategy().addStrategySettings(strategySettings);

		// ===
		final Scenario scenario = ScenarioUtils.createScenario( config ) ;
		createTestNetwork( scenario.getNetwork() ) ;
		createTestPopulation( scenario.getPopulation() ) ;
		
		// ===

		final Controler controler = new Controler(scenario);
		controler.setOverwriteFiles(true);

		final CadytsFreightChainsContext cContext = new CadytsFreightChainsContext(config);
		controler.addControlerListener(cContext);

		controler.addPlanStrategyFactory("ccc", new PlanStrategyFactory() {
			@Override
			public PlanStrategy createPlanStrategy(Scenario scenario, EventsManager eventsManager) {
				return new PlanStrategyImpl(new ExpBetaPlanChangerWithCadytsPlanRegistration<Item>(
						scenario.getConfig().planCalcScore().getBrainExpBeta(), cContext));
			}
		} ) ;

		controler.setScoringFunctionFactory(new ScoringFunctionFactory() {
			@Override
			public ScoringFunction createNewScoringFunction(Plan plan) {
				SumScoringFunction sum = new SumScoringFunction() ;
				return sum ; // dummy
			}
		}) ;
		
		controler.setMobsimFactory( new DummyMobsimFactory() );

		controler.run();


		//scenario data  test
		Assert.assertNotNull("config is null" , controler.getConfig());
		Assert.assertEquals("Different number of links in network.", controler.getNetwork().getLinks().size() , 23 );
		Assert.assertEquals("Different number of nodes in network.", controler.getNetwork().getNodes().size() , 15 );

		Assert.assertNotNull("Population is null.", controler.getScenario().getPopulation());

		Assert.assertEquals("Num. of persons in population is wrong.", controler.getPopulation().getPersons().size(), 5);
		Assert.assertEquals("Scale factor is wrong.", controler.getScenario().getConfig().counts().getCountsScaleFactor(), 1.0, MatsimTestUtils.EPSILON);

		//counts
		Assert.assertEquals("Count file is wrong.", controler.getScenario().getConfig().counts().getCountsFileName(), inputDir + "counts5.xml");

		Counts occupCounts = new Counts();

		new MatsimCountsReader(occupCounts).readFile(controler.getScenario().getConfig().counts().getCountsFileName());

		Count count =  occupCounts.getCount(new IdImpl(19));
		Assert.assertEquals("Occupancy counts description is wrong", occupCounts.getDescription(), "counts values for equil net");
		Assert.assertEquals("CsId is wrong.", count.getCsId() , "link_19");
		Assert.assertEquals("Volume of hour 6 is wrong", count.getVolume(7).getValue(), 5.0 , MatsimTestUtils.EPSILON);
		Assert.assertEquals("Max count volume is wrong.", count.getMaxVolume().getValue(), 5.0 , MatsimTestUtils.EPSILON);

		// test resulting simulation volumes
		String outCounts = outputDir + "ITERS/it." + lastIteration + "/" + lastIteration + ".countscompare.txt";
//		CountsReaderCar reader = new CountsReaderCar(outCounts);
//		double[] simValues;
//		double[] realValues;
//
//		Id locId11 = new IdImpl(11);
//		simValues = reader.getSimulatedValues(locId11);
//		realValues= reader.getRealValues(locId11);
//		Assert.assertEquals("Volume of hour 6 is wrong", 0.0, simValues[6], MatsimTestUtils.EPSILON);
//		Assert.assertEquals("Volume of hour 6 is wrong", 0.0, realValues[6], MatsimTestUtils.EPSILON);
//
//		Id locId12 = new IdImpl("12");
//		simValues = reader.getSimulatedValues(locId12);
//		realValues= reader.getRealValues(locId12);
//		Assert.assertEquals("Volume of hour 6 is wrong", 0.0, simValues[6], MatsimTestUtils.EPSILON);
//		Assert.assertEquals("Volume of hour 6 is wrong", 0.0, realValues[6] , MatsimTestUtils.EPSILON);
//
//		Id locId19 = new IdImpl("19");
//		simValues = reader.getSimulatedValues(locId19);
//		realValues= reader.getRealValues(locId19);
//		Assert.assertEquals("Volume of hour 6 is wrong", 5.0, simValues[6], MatsimTestUtils.EPSILON);
//		Assert.assertEquals("Volume of hour 6 is wrong", 5.0, realValues[6], MatsimTestUtils.EPSILON);
//
//		Id locId21 = new IdImpl("21");
//		simValues = reader.getSimulatedValues(locId21);
//		realValues= reader.getRealValues(locId21);
//		Assert.assertEquals("Volume of hour 6 is wrong", 5.0, simValues[6], MatsimTestUtils.EPSILON);
//		Assert.assertEquals("Volume of hour 6 is wrong", 5.0, realValues[6], MatsimTestUtils.EPSILON);

		// test calibration statistics
		String testCalibStatPath = outputDir + "calibration-stats.txt";
		CalibrationStatReader calibrationStatReader = new CalibrationStatReader();
		new TabularFileParser().parse(testCalibStatPath, calibrationStatReader);

		CalibrationStatReader.StatisticsData outStatData= calibrationStatReader.getCalStatMap().get(lastIteration);
		// Assert.assertEquals("different Count_ll", "-0.046875", outStatData.getCount_ll() );
		// Assert.assertEquals("different Count_ll_pred_err",  "0.01836234363152515" , outStatData.getCount_ll_pred_err() );
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
		// final TransitSchedule schedule = controler.getScenario().getTransitSchedule();
		final Network network = controler.getScenario().getNetwork();
		String linkOffsetFile = outputDir + "ITERS/it." + lastIteration + "/" + lastIteration + ".linkCostOffsets.xml";

		// CadytsPtLinkCostOffsetsXMLFileIO offsetReader = new CadytsPtLinkCostOffsetsXMLFileIO (schedule);
		//			CadytsLinkCostOffsetsXMLFileIO offsetReader = new CadytsLinkCostOffsetsXMLFileIO(network);
		CadytsCostOffsetsXMLFileIO<Item> offsetReader = new CadytsCostOffsetsXMLFileIO<Item>( cContext.getLookUp() );

		DynamicData<Item> linkOffsets = offsetReader.read(linkOffsetFile);

//		Link link11 = network.getLinks().get(locId11);
//		Link link19 = network.getLinks().get(locId19);

		//find first offset value different from null to compare. Useful to test with different time bin sizes
		int binIndex=-1;
		boolean isZero;
		//		do {
		//			binIndex++;
		//			isZero = (Math.abs(linkOffsets.getBinValue(link19 , binIndex) - 0.0) < MatsimTestUtils.EPSILON);
		//		}while (isZero && binIndex<86400);

		Assert.assertEquals("Wrong bin index for first link offset", 6, binIndex);

		//		Assert.assertEquals("Wrong link offset of link 11", 0.0, linkOffsets.getBinValue(link11 , binIndex), MatsimTestUtils.EPSILON);
		//		Assert.assertEquals("Wrong link offset of link 19", 0.0014707121641471912, linkOffsets.getBinValue(link19 , binIndex), MatsimTestUtils.EPSILON);
		//}
	}


	//--------------------------------------------------------------



	private static Config createTestConfig(String inputDir, String outputDir) {
		Config config = ConfigUtils.createConfig() ;
		
//		ActivityParams params = new ActivityParams("minor") ;
//		config.planCalcScore().addActivityParams(params);
		
		CadytsConfigGroup cadytsConfig = new CadytsConfigGroup() ;
		cadytsConfig.setTimeBinSize(24*3600) ;
		config.addModule(cadytsConfig);
		
		return config;
	}
	
	private static void createTestNetwork( Network net ) {
		NetworkFactory nf = net.getFactory() ;

		Node node1 = nf.createNode( new IdImpl(1), new CoordImpl(0.,0.)) ;
		net.addNode(node1) ;
		Node node2 = nf.createNode( new IdImpl(2), new CoordImpl(10.,0.)) ;
		net.addNode(node2);
		
		Link link1 = nf.createLink( new IdImpl("1-2") , node1, node2 ) ;
		net.addLink(link1); 
		Link link2 = nf.createLink( new IdImpl( "2-1") , node2, node1 ) ;
		net.addLink(link2) ;
	}

	private static void createTestPopulation( Population pop ) {
		PopulationFactory pf = pop.getFactory();
		for ( int ii=0 ; ii<=20 ; ii++ ) {
			Person person = pf.createPerson( new IdImpl(ii) ) ;
			for ( int pp=1 ; pp<=5 ; pp++ ) {
				int last = pp ;
				Plan plan = pf.createPlan() ;
				for ( int aa=0 ; aa<=last ; aa++ ) {
					Coord coord = new CoordImpl(0.,0.) ;
					Activity activity = pf.createActivityFromCoord("minor", coord) ;
					activity.setEndTime( 8.*3600. ) ;
					plan.addActivity( activity ) ;
					Leg leg = pf.createLeg( TransportMode.walk ) ;
					plan.addLeg( leg ) ;
				}
				person.addPlan( plan ) ;
			}
			pop.addPerson(person);
		}
	}


	private static class DummyMobsimFactory implements MobsimFactory {
		@Override
		public Mobsim createMobsim(final Scenario sc, final EventsManager eventsManager) {
			return new Mobsim() {
				@Override
				public void run() {
					// (dummy)
				}} ;
		}
	}

}
