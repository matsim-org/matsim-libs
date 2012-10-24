package org.matsim.contrib.locationchoice;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.ActivityOptionImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.router.TripRouterFactoryImpl;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.testcases.MatsimTestCase;

public class LocationChoiceIntegrationTest extends MatsimTestCase {

	public void testLocationChoice() {
		// setup config
		final Config config = loadConfig(null);
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(config);
		final StrategySettings strategySettings = new StrategySettings(new IdImpl("1"));
		strategySettings.setModuleName("LocationChoice");
		strategySettings.setProbability(1.0);
		config.global().setNumberOfThreads(0);
		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(1);
		config.strategy().addStrategySettings(strategySettings);
		config.locationchoice().setAlgorithm("random");
		config.locationchoice().setFlexibleTypes("work");
		ActivityParams home = new ActivityParams("home");
		home.setTypicalDuration(12*60*60);
		config.planCalcScore().addActivityParams(home);
		ActivityParams work = new ActivityParams("work");
		work.setTypicalDuration(12*60*60);
		config.planCalcScore().addActivityParams(work);

		// setup network
		NetworkImpl network = (NetworkImpl) scenario.getNetwork();
		Node node1 = network.createAndAddNode(new IdImpl(1), new CoordImpl(0, 0));
		Node node2 = network.createAndAddNode(new IdImpl(2), new CoordImpl(1000, 0));
		Link link = network.createAndAddLink(new IdImpl(1), node1, node2, 1000, 10, 3600, 1);
		ActivityFacilityImpl facility1 = scenario.getActivityFacilities().createFacility(new IdImpl(1), new CoordImpl(0, 500));
		facility1.getActivityOptions().put("initial-work", new ActivityOptionImpl("initial-work", facility1));
		ActivityFacilityImpl facility2 = scenario.getActivityFacilities().createFacility(new IdImpl(2), new CoordImpl(0, 400));
		facility2.getActivityOptions().put("work", new ActivityOptionImpl("work", facility2));
		ActivityFacilityImpl facility3 = scenario.getActivityFacilities().createFacility(new IdImpl(3), new CoordImpl(0, 300));
		facility3.getActivityOptions().put("work", new ActivityOptionImpl("work", facility3));
		
		
		// setup population with one person
		Population population = scenario.getPopulation();
		PersonImpl person = new PersonImpl(new IdImpl(1));
		population.addPerson(person);
		PlanImpl plan = person.createAndAddPlan(true);
		ActivityImpl act = plan.createAndAddActivity("home", new CoordImpl(0, 0));
		act.setLinkId(link.getId());
		act.setEndTime(8.0 * 3600);
		plan.createAndAddLeg(TransportMode.car);
		act = plan.createAndAddActivity("work", new CoordImpl(0, 500));
		act.setEndTime(17*60*60);
		((ActivityImpl) act).setFacilityId(facility1.getId());
		plan.createAndAddLeg(TransportMode.car);
		act = plan.createAndAddActivity("home", new CoordImpl(0, 0));
		act.setLinkId(link.getId());

		// setup strategy manager and load from config
		Controler controler = new Controler(scenario);
		controler.setLeastCostPathCalculatorFactory(new DijkstraFactory());
		controler.setTripRouterFactory( new TripRouterFactoryImpl( controler ) );
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

}
