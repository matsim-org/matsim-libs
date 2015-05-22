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

package playground.southafrica.freight.cadyts4freightchains;

import cadyts.utilities.io.tabularFileParser.TabularFileParser;
import cadyts.utilities.misc.DynamicData;
import com.google.inject.Provider;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.cadyts.general.CadytsConfigGroup;
import org.matsim.contrib.cadyts.general.CadytsCostOffsetsXMLFileIO;
import org.matsim.contrib.cadyts.general.CadytsScoring;
import org.matsim.contrib.cadyts.general.PlanSelectionByCadyts;
import org.matsim.contrib.cadyts.utils.CalibrationStatReader;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author nagel
 */
public class CadytsFreightChainTest {
	Logger log = Logger.getLogger( CadytsFreightChainTest.class ) ;

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	@Ignore
	public final void testCalibrationAsScoring() throws IOException {
		final double beta=1. ;
		final int lastIteration = 100 ;

		String outputDir = this.utils.getOutputDirectory();
		IOUtils.createDirectory(outputDir) ;

		final Config config = createTestConfig(outputDir);

		config.controler().setLastIteration(lastIteration);

		config.planCalcScore().setBrainExpBeta(beta);

		StrategySettings strategySettings = new StrategySettings(Id.create("1", StrategySettings.class));
		strategySettings.setStrategyName("ccc");
		strategySettings.setWeight(1.0);
		config.strategy().addStrategySettings(strategySettings);

		// ===
		final Scenario scenario = ScenarioUtils.createScenario( config ) ;
		createTestNetwork( scenario.getNetwork() ) ;
		createTestPopulation( scenario.getPopulation() ) ;
		
		// ===

		final Controler controler = new Controler(scenario);
		controler.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );

		List<Integer> nChainsOfLength = new ArrayList<Integer>() ;
		for ( int ii=0 ; ii<=6 ; ii++ ) {
			nChainsOfLength.add(0) ;
		}
		int nChainsOfLength4 = 5 ;
		nChainsOfLength.set(4, nChainsOfLength4 ) ; 
		nChainsOfLength.set(5, scenario.getPopulation().getPersons().size() - nChainsOfLength4 ) ; // meaning all the other chains should be length 5
		
		final CadytsFreightChainsContext cContext = new CadytsFreightChainsContext(config, nChainsOfLength );
		controler.addControlerListener(cContext);

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addPlanStrategyBinding("ccc").toProvider(new javax.inject.Provider<PlanStrategy>() {
					@Override
					public PlanStrategy get() {
//				return new PlanStrategyImpl(new ExpBetaPlanSelectorWithCadytsPlanRegistration<Item>(
//						sc.getConfig().planCalcScore().getBrainExpBeta(), cContext));
//				return new PlanStrategyImpl(new ExpBetaPlanChangerWithCadytsPlanRegistration<Item>(
//						sc.getConfig().planCalcScore().getBrainExpBeta(), cContext));
						return new PlanStrategyImpl(new PlanSelectionByCadyts<Item>(
								scenario.getConfig().planCalcScore().getBrainExpBeta(), cContext));
					}
				});
			}
		});

		controler.setScoringFunctionFactory(new ScoringFunctionFactory() {
			@Override
			public ScoringFunction createNewScoringFunction(Person person) {
				SumScoringFunction sum = new SumScoringFunction() ;
				final CadytsScoring<Item> cadytsScoring = new CadytsScoring<Item>(person.getSelectedPlan(), config, cContext );
				cadytsScoring.setWeightOfCadytsCorrection(100.);
				sum.addScoringFunction( cadytsScoring ); 
				return sum ; 
			}
		}) ;

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

		Assert.assertNotNull("config is null" , controler.getConfig());

		Assert.assertNotNull("Population is null.", controler.getScenario().getPopulation());
		
		for ( Person driver : controler.getScenario().getPopulation().getPersons().values() ) {
			log.warn( " person with id: " + driver.getId() ) ;
			for ( Plan plan : driver.getPlans() ) {
				log.warn( " score: " + plan.getScore() + "; plan: " + plan ) ;
			}
		}


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
//		Assert.assertEquals("different Total_ll", "0.0", outStatData.getTotal_ll() );

		//test link offsets
		// final TransitSchedule schedule = controler.getScenario().getTransitSchedule();
		final Network network = controler.getScenario().getNetwork();
		String linkOffsetFile = outputDir + "ITERS/it." + lastIteration + "/" + lastIteration + ".linkCostOffsets.xml";

		// CadytsPtLinkCostOffsetsXMLFileIO offsetReader = new CadytsPtLinkCostOffsetsXMLFileIO (schedule);
		//			CadytsLinkCostOffsetsXMLFileIO offsetReader = new CadytsLinkCostOffsetsXMLFileIO(network);
		CadytsCostOffsetsXMLFileIO<Item> offsetReader = new CadytsCostOffsetsXMLFileIO<Item>( cContext.getLookUp(), Item.class );

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

//		Assert.assertEquals("Wrong bin index for first link offset", 6, binIndex);

		//		Assert.assertEquals("Wrong link offset of link 11", 0.0, linkOffsets.getBinValue(link11 , binIndex), MatsimTestUtils.EPSILON);
		//		Assert.assertEquals("Wrong link offset of link 19", 0.0014707121641471912, linkOffsets.getBinValue(link19 , binIndex), MatsimTestUtils.EPSILON);
		//}
	}


	//--------------------------------------------------------------



	private static Config createTestConfig(String outputDir) {
		Config config = ConfigUtils.createConfig() ;
		
		config.controler().setOutputDirectory(outputDir);
		
//		ActivityParams params = new ActivityParams("minor") ;
//		config.planCalcScore().addActivityParams(params);
		
		CadytsConfigGroup cadytsConfig = new CadytsConfigGroup() ;
		cadytsConfig.setTimeBinSize(3600) ;
		cadytsConfig.setStartTime(0);
		cadytsConfig.setEndTime(3600);

//		cadytsConfig.setUseBruteForce(true);
		// makes a difference with PlanSelectionByCadyts.  Does not make a difference with the other ways to do this.  kai, dec'13

		cadytsConfig.setMinFlowStddev_vehPerHour(1.);
		config.addModule(cadytsConfig);
		
		return config;
	}
	
	private static void createTestNetwork( Network net ) {
		NetworkFactory nf = net.getFactory() ;

		Node node1 = nf.createNode( Id.create(1, Node.class), new CoordImpl(0.,0.)) ;
		net.addNode(node1) ;
		Node node2 = nf.createNode( Id.create(2, Node.class), new CoordImpl(10.,0.)) ;
		net.addNode(node2);
		
		Link link1 = nf.createLink( Id.create("1-2", Link.class) , node1, node2 ) ;
		net.addLink(link1); 
		Link link2 = nf.createLink( Id.create("2-1", Link.class) , node2, node1 ) ;
		net.addLink(link2) ;
	}

	private static void createTestPopulation( Population pop ) {
		PopulationFactory pf = pop.getFactory();
		for ( int ii=0 ; ii<=20 ; ii++ ) {
			Person person = pf.createPerson( Id.create(ii, Person.class) ) ;
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
