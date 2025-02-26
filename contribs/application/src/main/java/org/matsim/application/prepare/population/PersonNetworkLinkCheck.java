package org.matsim.application.prepare.population;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.algorithms.ParallelPersonAlgorithmUtils;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import org.matsim.core.population.routes.NetworkRoute;
import picocli.CommandLine;

import java.util.Objects;
import java.util.stream.Stream;

/**
 * Checks the plan of a person for non-existing link ids.
 * Invalid occurrences will be removed and routes or activities link ids reset if necesarry.
 *
 * This class can be used from CLI or as a {@link PersonAlgorithm}.
 */
@CommandLine.Command(name = "person-network-link-check", description = "Check the plan of a person for non-existing link ids.")
public final class PersonNetworkLinkCheck implements MATSimAppCommand, PersonAlgorithm {

	private static final Logger log = LogManager.getLogger(PersonNetworkLinkCheck.class);

	@CommandLine.Option(names = {"--input", "--population"}, description = "Path to population", required = true)
	private String populationPath;

	@CommandLine.Option(names = "--network", description = "Path to network", required = true)
	private String networkPath;

	@CommandLine.Option(names = "--output", description = "Path to output population", required = true)
	private String output;

	private Network network;

	@SuppressWarnings("unused")
	public PersonNetworkLinkCheck() {
	}

	private PersonNetworkLinkCheck(Network network) {
		this.network = network;
	}

	/**
	 * Create an instance of the class used directly as a {@link PersonAlgorithm}.
	 */
	public static PersonAlgorithm createPersonAlgorithm(Network network) {
		return new PersonNetworkLinkCheck(network);
	}

	public static void main(String[] args) {
		new PersonNetworkLinkCheck().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		network = NetworkUtils.readNetwork(networkPath);
		Population population = PopulationUtils.readPopulation(populationPath);
		ParallelPersonAlgorithmUtils.run(population, Runtime.getRuntime().availableProcessors(), this);

		PopulationUtils.writePopulation(population, output);

		return 0;
	}

	@Override
	public void run(Person person) {

		Objects.requireNonNull(network, "Network not set. Make sure to use PersonNetworkLinkCheck.createPersonAlgorithm(network).");

		for (Plan plan : person.getPlans()) {

			for (PlanElement el : plan.getPlanElements()) {

				if (el instanceof Activity act) {
					checkActivity(person, act);
				} else if (el instanceof Leg leg) {

					if (leg.getRoute() instanceof NetworkRoute r)
						checkNetworkRoute(leg, r);
					else if (leg.getRoute() != null)
						checkRoute(leg, leg.getRoute());

				}
			}
		}
	}

	private void checkActivity(Person person, Activity act) {
		// activity link ids are reset, if they are not contained in the network
		if (act.getLinkId() != null && !network.getLinks().containsKey(act.getLinkId())) {

			act.setLinkId(null);
			if (act.getFacilityId() == null && act.getCoord() == null) {
				log.warn("Person {} now has activity without link id and no facility id or coordinate.", person.getId());
			}
		}
	}

	private void checkRoute(Leg leg, Route route) {
		if (!network.getLinks().containsKey(route.getStartLinkId()) || !network.getLinks().containsKey(route.getEndLinkId()))
			leg.setRoute(null);
	}

	private void checkNetworkRoute(Leg leg, NetworkRoute r) {

		Stream<Id<Link>> stream = Stream.concat(Stream.of(r.getStartLinkId(), r.getEndLinkId()), r.getLinkIds().stream());

		boolean valid = stream.allMatch(l -> {

			Link link = network.getLinks().get(l);

			// Check if link is present in the network
			if (link == null)
				return false;

			// Check if the link has the needed mode
			return link.getAllowedModes().contains(leg.getMode());
		});

		if (!valid) {
			leg.setRoute(null);
		}
	}
}
