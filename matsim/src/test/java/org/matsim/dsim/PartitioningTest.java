package org.matsim.dsim;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.FacilitiesConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;

class PartitioningTest {

	@RegisterExtension
	MatsimTestUtils utils = new MatsimTestUtils();

	/**
	 * Initial plans often have no routes. The network must be partitioned after PrepareForSim has routed the plans, so
	 * that the routes can be used to weight the partitions.
	 */
	@Test
	void partitionsUseRoutesOfPrepareForSim() {

		Config config = utils.loadConfig(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("equil"), "config.xml"));
		config.facilities().setFacilitiesSource(FacilitiesConfigGroup.FacilitiesSource.none);

		config.controller().setOutputDirectory(utils.getOutputDirectory());
		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setLastIteration(0);
		config.controller().setMobsim("dsim");

		int parts = 4;

		config.dsim().setThreads(parts);
		config.dsim().setPartitioning(DSimConfigGroup.Partitioning.bisect);
		config.dsim().setLinkDynamics(config.qsim().getLinkDynamics());
		config.dsim().setTrafficDynamics(config.qsim().getTrafficDynamics());
		config.dsim().setNetworkModes(new HashSet<>(config.qsim().getMainModes()));
		config.dsim().setStartTime(0);
		config.dsim().setEndTime(30 * 3600);
		config.qsim().setFlowCapFactor(1.);
		config.qsim().setStorageCapFactor(1.);

		Activities.addScoringParams(config);

		Scenario scenario = ScenarioUtils.loadScenario(config);

		// remove all routes from the initial plans
		for (Person person : scenario.getPopulation().getPersons().values()) {
			for (Plan plan : person.getPlans()) {
				TripStructureUtils.getLegs(plan).forEach(leg -> leg.setRoute(null));
			}
		}

		Network network = scenario.getNetwork();

		// partitioning without any routes, as it would be done at startup
		NetworkDecomposition.bisection(network, scenario.getPopulation(), parts);
		int[] unrouted = NetworkDecomposition.getNodePartitions(network);

		new Controler(scenario).run();
		int[] used = NetworkDecomposition.getNodePartitions(network);

		// no replanning in iteration 0, plans still have the routes of PrepareForSim
		assertThat(scenario.getPopulation().getPersons().values())
			.allMatch(p -> TripStructureUtils.getLegs(p.getSelectedPlan()).stream().map(Leg::getRoute).allMatch(r -> r != null));

		NetworkDecomposition.bisection(network, scenario.getPopulation(), parts);
		int[] routed = NetworkDecomposition.getNodePartitions(network);

		assertThat(used).isEqualTo(routed);
		assertThat(used).isNotEqualTo(unrouted);
	}
}
