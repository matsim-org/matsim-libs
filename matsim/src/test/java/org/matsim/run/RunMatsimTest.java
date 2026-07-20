package org.matsim.run;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RunMatsimTest {

	private static final String E_BIKE = "eBike";

	@RegisterExtension
	private final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void runFailsIfRoutingModeHasNoScoringParameters() {
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("equil"), "network.xml").toString());
		config.controller().setOutputDirectory(utils.getOutputDirectory());
		config.controller().setLastIteration(0);
		config.controller().setCreateGraphs(false);
		config.controller().setWriteEventsInterval(0);
		config.routing().setAccessEgressType(RoutingConfigGroup.AccessEgressType.none);
		config.routing().clearTeleportedModeParams();
		config.routing().addTeleportedModeParams(
			new RoutingConfigGroup.TeleportedModeParams(TransportMode.walk).setTeleportedModeSpeed(3. / 3.6));
		config.routing().addTeleportedModeParams(
			new RoutingConfigGroup.TeleportedModeParams(E_BIKE).setTeleportedModeSpeed(25. / 3.6));
		config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams("home").setTypicalDuration(12 * 3600));
		config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams("work").setTypicalDuration(8 * 3600));

		Scenario scenario = ScenarioUtils.loadScenario(config);
		Population population = scenario.getPopulation();
		Person person = population.getFactory().createPerson(Id.createPersonId("person"));
		population.addPerson(person);
		Plan plan = PersonUtils.createAndAddPlan(person, true);

		Activity home = PopulationUtils.createAndAddActivityFromLinkId(plan, "home", Id.create("1", Link.class));
		home.setEndTime(6 * 3600);
		PopulationUtils.createAndAddLeg(plan, E_BIKE);
		Activity work = PopulationUtils.createAndAddActivityFromLinkId(plan, "work", Id.create("20", Link.class));
		work.setEndTime(17 * 3600);
		PopulationUtils.createAndAddLeg(plan, E_BIKE);
		PopulationUtils.createAndAddActivityFromLinkId(plan, "home", Id.create("1", Link.class));

		Controler controler = new Controler(scenario);

		assertThatThrownBy(controler::run)
			.rootCause()
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("No scoring parameters are defined")
			.hasMessageContaining("'" + E_BIKE + "'")
			.hasMessageContaining("modeParams parameter set")
			.hasMessageContaining("scoring configuration");
	}

	@Test
	void runFailsIfNetworkModeHasNoScoringParameters() {
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("equil"), "network.xml").toString());
		config.controller().setOutputDirectory(utils.getOutputDirectory());
		config.controller().setLastIteration(0);
		config.controller().setCreateGraphs(false);
		config.controller().setWriteEventsInterval(0);
		config.routing().setAccessEgressType(RoutingConfigGroup.AccessEgressType.none);
		config.routing().setNetworkModes(Set.of(E_BIKE));
		config.qsim().setMainModes(Set.of(E_BIKE));
		config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams("home").setTypicalDuration(12 * 3600));
		config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams("work").setTypicalDuration(8 * 3600));

		Scenario scenario = ScenarioUtils.loadScenario(config);
		for (Link link : scenario.getNetwork().getLinks().values()) {
			Set<String> allowedModes = new HashSet<>(link.getAllowedModes());
			allowedModes.add(E_BIKE);
			link.setAllowedModes(allowedModes);
		}

		Population population = scenario.getPopulation();
		Person person = population.getFactory().createPerson(Id.createPersonId("person"));
		population.addPerson(person);
		Plan plan = PersonUtils.createAndAddPlan(person, true);

		Activity home = PopulationUtils.createAndAddActivityFromLinkId(plan, "home", Id.create("1", Link.class));
		home.setEndTime(6 * 3600);
		PopulationUtils.createAndAddLeg(plan, E_BIKE);
		Activity work = PopulationUtils.createAndAddActivityFromLinkId(plan, "work", Id.create("20", Link.class));
		work.setEndTime(17 * 3600);
		PopulationUtils.createAndAddLeg(plan, E_BIKE);
		PopulationUtils.createAndAddActivityFromLinkId(plan, "home", Id.create("1", Link.class));

		Controler controler = new Controler(scenario);
		assertThatThrownBy(controler::run)
			.rootCause()
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("No scoring parameters are defined")
			.hasMessageContaining("'" + E_BIKE + "'")
			.hasMessageContaining("modeParams parameter set")
			.hasMessageContaining("scoring configuration");
	}
}
