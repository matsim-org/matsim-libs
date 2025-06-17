package playground.vsp.changeExpBeta;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ReplanningConfigGroup;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;

public class RunTestingScenario {
	static final String modeA = "modeA";
	static final String modeB = "modeB";
	private static final String dummy = "dummy";

	enum ExpBetaChanger {ChangeExpBeta, SelectExpBeta}

	public static void main(String[] args) {
		Config config = createTestingConfig(0., args[0], ExpBetaChanger.ChangeExpBeta);
		config.controller().setLastIteration(1000);
		config.replanning().setFractionOfIterationsToDisableInnovation(0.75);
		config.scoring().setFractionOfIterationsToStartScoreMSA(0.75);
		Scenario scenario = prepareScenario(config);
		Controler controler = new Controler(scenario);
		// binding for score stochastic
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addEventHandlerBinding().toInstance(new DisturbanceGenerator(0., 0.3));
			}
		});
		// run simulation
		controler.run();
	}

	static Config createTestingConfig(double ascForModeB, String output, ExpBetaChanger expBetaChanger) {
		Config config = ConfigUtils.createConfig();
		config.controller().setOutputDirectory(output);
		config.controller().setLastIteration(500);
		config.replanning().setFractionOfIterationsToDisableInnovation(0.9);
		// specify modes
		config.changeMode().setModes(new String[]{modeA, modeB});

		// specify strategy settings
		// change mode
		ReplanningConfigGroup.StrategySettings changeMode = new ReplanningConfigGroup.StrategySettings();
		changeMode.setStrategyName("ChangeSingleTripMode");
		changeMode.setWeight(0.1);
		config.replanning().addStrategySettings(changeMode);

		// exponential beta setting
		ReplanningConfigGroup.StrategySettings changeExpBeta = new ReplanningConfigGroup.StrategySettings();
		changeExpBeta.setStrategyName(expBetaChanger.name());
		changeExpBeta.setWeight(0.9);
		config.replanning().addStrategySettings(changeExpBeta);

		// scoring params
		ScoringConfigGroup.ModeParams modeAParams = new ScoringConfigGroup.ModeParams(modeA);
		modeAParams.setConstant(0.);
		ScoringConfigGroup.ModeParams modeBParams = new ScoringConfigGroup.ModeParams(modeB);
		modeBParams.setConstant(ascForModeB);
		ScoringConfigGroup.ActivityParams dummyActParams = new ScoringConfigGroup.ActivityParams();
		dummyActParams.setTypicalDuration(3600);
		dummyActParams.setActivityType(dummy);

		config.scoring().addModeParams(modeAParams);
		config.scoring().addModeParams(modeBParams);
		config.scoring().addActivityParams(dummyActParams);

		// teleportation setting
		RoutingConfigGroup.TeleportedModeParams modeATeleportation = new RoutingConfigGroup.TeleportedModeParams();
		modeATeleportation.setMode(modeA);
		modeATeleportation.setTeleportedModeSpeed(5.);

		RoutingConfigGroup.TeleportedModeParams modeBTeleportation = new RoutingConfigGroup.TeleportedModeParams();
		modeBTeleportation.setMode(modeB);
		modeBTeleportation.setTeleportedModeSpeed(5.);

		RoutingConfigGroup.TeleportedModeParams walk = new RoutingConfigGroup.TeleportedModeParams();
		walk.setMode(TransportMode.walk);
		walk.setTeleportedModeSpeed(1.);

		config.routing().addTeleportedModeParams(modeATeleportation);
		config.routing().addTeleportedModeParams(modeBTeleportation);
		config.routing().addTeleportedModeParams(walk);

		return config;
	}

	static Scenario prepareScenario(Config config) {
		Scenario scenario = ScenarioUtils.loadScenario(config);
		// prepare network
		Network network = scenario.getNetwork();
		Node nodeA = NetworkUtils.createNode(Id.createNodeId("A"), new Coord(0, 0));
		Node nodeB = NetworkUtils.createNode(Id.createNodeId("B"), new Coord(2000, 0));
		network.addNode(nodeA);
		network.addNode(nodeB);
		Link link1 = NetworkUtils.createLink(Id.createLinkId("1"), nodeA, nodeB, network, 2000, 10, 3600, 1);
		Link link2 = NetworkUtils.createLink(Id.createLinkId("2"), nodeB, nodeA, network, 2000, 10, 3600, 1);
		network.addLink(link1);
		network.addLink(link2);

		// prepare plans
		Population plans = scenario.getPopulation();
		PopulationFactory populationFactory = plans.getFactory();
		for (int i = 0; i < 10000; i++) {
			Person person = populationFactory.createPerson(Id.createPersonId("dummy_person_" + i));
			Plan plan = populationFactory.createPlan();
			Activity fromAct = populationFactory.createActivityFromCoord(dummy, new Coord(0, 0));
			fromAct.setEndTime(3600);
			Leg leg = populationFactory.createLeg(modeA);
			Activity toAct = populationFactory.createActivityFromCoord(dummy, new Coord(2000, 0));
			plan.addActivity(fromAct);
			plan.addLeg(leg);
			plan.addActivity(toAct);
			person.addPlan(plan);
			plans.addPerson(person);
		}
		return scenario;
	}
}
