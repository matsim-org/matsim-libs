package org.matsim.contrib.locationchoice;

import java.util.Random;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.LocationChoiceConfigGroup.Algotype;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.ActivityOptionImpl;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimFactory;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.testcases.MatsimTestCase;

public class LocationChoiceIntegrationTest extends MatsimTestCase {

	public void testLocationChoiceJan2013() {
		final Config config = localCreateConfig();

		config.locationchoice().setAlgorithm(Algotype.bestResponse) ;
		config.locationchoice().setEpsilonScaleFactors("100.0") ;
//		config.locationchoice().setProbChoiceExponent("1.") ;
		
		config.otfVis().setEffectiveLaneWidth(1.) ;
		config.otfVis().setLinkWidth((float)1.) ;
		config.otfVis().setShowTeleportedAgents(true) ;
		config.otfVis().setDrawNonMovingItems(true) ;

		final ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(config);

		// setup network
		Network network = scenario.getNetwork();
		
		final double scale = 1000. ;

		Node node0 = network.getFactory().createNode(new IdImpl(0), new CoordImpl(-scale,0) ) ;
		network.addNode(node0) ;

		Node node1 = network.getFactory().createNode(new IdImpl(1), new CoordImpl(10,0) ) ;
		network.addNode(node1) ;

		Link link1 = network.getFactory().createLink(new IdImpl(1), node0, node1 ) ;
		network.addLink(link1) ;
		Link link1b = network.getFactory().createLink(new IdImpl("1b"), node1, node0 ) ;
		network.addLink(link1b) ;

		ActivityFacility facility1 = scenario.getActivityFacilities().createAndAddFacility(new IdImpl(1), new CoordImpl(scale,0) ) ;
		// (this should be "createAndAdd". kai, jan'13)
		facility1.getActivityOptions().put("initial-work", new ActivityOptionImpl("initial-work", facility1)) ;
		
		final int nNodes = 100 ;
		Random random = new Random(4711) ;
		for ( int ii=2 ; ii<nNodes+2 ; ii++ ) {
			double tmp = Math.PI*(ii-1)/nNodes ;
			Coord coord = new CoordImpl( scale*Math.sin(tmp),scale*Math.cos(tmp) ) ;

			Node node = network.getFactory().createNode(new IdImpl(ii), coord ) ;
			network.addNode(node) ;

			double rnd = random.nextDouble() ;
			{
				Link link = network.getFactory().createLink(new IdImpl(ii), node1, node) ;
				link.setLength(rnd*scale) ;
				link.setFreespeed(10.) ;
				link.setCapacity(1.) ;
				network.addLink(link) ;
			}
			{
				Link link = network.getFactory().createLink(new IdImpl(ii+"b"), node, node1) ;
				link.setLength(rnd*scale) ;
				link.setFreespeed(10.) ;
				link.setCapacity(1.) ;
				network.addLink(link) ;
			}
			
			ActivityFacility facility = scenario.getActivityFacilities().createAndAddFacility(new IdImpl(ii), coord ) ;
			facility.getActivityOptions().put("work", new ActivityOptionImpl("work", facility)) ;
		}
		
		Person person = localCreatePopWOnePerson(scenario, link1, facility1, 8.*60*60+5*60);

		Controler controler = new Controler(scenario);
		
		controler.setMobsimFactory(new MobsimFactory() {
			@Override
			public Mobsim createMobsim(Scenario sc, EventsManager eventsManager) {
				QSim qSim = (QSim) new QSimFactory().createMobsim(sc, eventsManager) ;
//				OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(sc.getConfig(), sc, eventsManager, qSim);
//				OTFClientLive.run(sc.getConfig(), server);
				return qSim ;
			} 
		} ) ;

		controler.run();

		assertEquals("number of plans in person.", 2, person.getPlans().size());
		Plan newPlan = person.getSelectedPlan();
		System.err.println( " newPlan: " + newPlan ) ;
		ActivityImpl newWork = (ActivityImpl) newPlan.getPlanElements().get(2);
		System.err.println( " newWork: " + newWork ) ;
		System.err.println( " facilityId: " + newWork.getFacilityId() ) ;
//		assertTrue( !newWork.getFacilityId().equals(new IdImpl(1) ) ) ; // should be different from facility number 1 !!
		assertEquals( new IdImpl(55), newWork.getFacilityId() );
	}
	public void testLocationChoice() {
		final Config config = localCreateConfig();

		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(config);

		// setup network
		NetworkImpl network = (NetworkImpl) scenario.getNetwork();
		Node node1 = network.createAndAddNode(new IdImpl(1), new CoordImpl(0, 0));
		Node node2 = network.createAndAddNode(new IdImpl(2), new CoordImpl(1000, 0));
		Link link = network.createAndAddLink(new IdImpl(1), node1, node2, 1000, 10, 3600, 1);
		ActivityFacilityImpl facility1 = scenario.getActivityFacilities().createAndAddFacility(new IdImpl(1), new CoordImpl(0, 500));
		facility1.getActivityOptions().put("initial-work", new ActivityOptionImpl("initial-work", facility1));
		ActivityFacilityImpl facility2 = scenario.getActivityFacilities().createAndAddFacility(new IdImpl(2), new CoordImpl(0, 400));
		facility2.getActivityOptions().put("work", new ActivityOptionImpl("work", facility2));
		ActivityFacilityImpl facility3 = scenario.getActivityFacilities().createAndAddFacility(new IdImpl(3), new CoordImpl(0, 300));
		facility3.getActivityOptions().put("work", new ActivityOptionImpl("work", facility3));
		
		Person person = localCreatePopWOnePerson(scenario, link, facility1, 17.*60.*60.);

		Controler controler = new Controler(scenario);

		controler.run();

		// test that everything worked as expected
		// The initial facility is *not* part of the choice set (its supported activity type is called "initial-work" for that reason)
		// so that the test can notice that there is a difference. In my earlier attempt, the random facility chosen would always be the one 
		// on which I already am, so the test was no good.
		// Secondly, I need to give it two facilities to choose from, because a choice set of size 1 is treated specially
		// (it is assumed that the one element is the one I'm already on, so nothing is done).
		// I tricked it. :-)   michaz
		assertEquals("number of plans in person.", 2, person.getPlans().size());
		Plan newPlan = person.getSelectedPlan();
		ActivityImpl newWork = (ActivityImpl) newPlan.getPlanElements().get(2);
		assertTrue(newWork.getFacilityId().equals(new IdImpl(2)) || newWork.getFacilityId().equals(new IdImpl(3)));
	}

	/** 
	 * setup population with one person
	 * @param workActEndTime TODO
	 */
	private static Person localCreatePopWOnePerson(ScenarioImpl scenario, Link link, ActivityFacility facility1, double workActEndTime) {

		Population population = scenario.getPopulation();

		Person person = population.getFactory().createPerson(new IdImpl(1));
		population.addPerson(person);

		PlanImpl plan = (PlanImpl) population.getFactory().createPlan() ;
		person.addPlan(plan) ;
		
		{
			Activity act = population.getFactory().createActivityFromCoord("home", new CoordImpl(0,0)) ;
			//		act.setLinkId(link.getId());
			act.setEndTime(8.0 * 3600);
			plan.addActivity(act) ;
		}
		plan.addLeg(population.getFactory().createLeg(TransportMode.car)) ;
		{
			Activity act = population.getFactory().createActivityFromCoord("work", scenario.getActivityFacilities().getLocation(facility1.getId()).getCoord() ) ;
			act.setEndTime(workActEndTime);
			((ActivityImpl)act).setFacilityId(facility1.getId());
			plan.addActivity(act) ;
		}
		plan.createAndAddLeg(TransportMode.car);
		{
//		act = plan.createAndAddActivity("home", new CoordImpl(0, 0));
//		act.setLinkId(link.getId());
			Activity act = population.getFactory().createActivityFromCoord("home", new CoordImpl(0,0)) ;
			plan.addActivity(act) ;
		}

		return person;
	}

	private Config localCreateConfig() {
		// setup config
		final Config config = loadConfig(null);
		
		config.global().setNumberOfThreads(0);
		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(1);
		config.controler().setMobsim("qsim");

		config.addQSimConfigGroup(new QSimConfigGroup()) ;
		config.getQSimConfigGroup().setSnapshotStyle(QSimConfigGroup.SNAPSHOT_AS_QUEUE) ;

		config.locationchoice().setAlgorithm(Algotype.random);
		config.locationchoice().setFlexibleTypes("work");

		ActivityParams home = new ActivityParams("home");
		home.setTypicalDuration(12*60*60);
		config.planCalcScore().addActivityParams(home);
		ActivityParams work = new ActivityParams("work");
		work.setTypicalDuration(12*60*60);
		config.planCalcScore().addActivityParams(work);
		
		final StrategySettings strategySettings = new StrategySettings(new IdImpl("1"));
		strategySettings.setModuleName("LocationChoice");
		strategySettings.setProbability(1.0);
		config.strategy().addStrategySettings(strategySettings);

		return config;
	}

}
