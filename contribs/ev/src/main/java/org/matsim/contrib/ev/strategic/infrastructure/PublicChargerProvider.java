package org.matsim.contrib.ev.strategic.infrastructure;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.ev.infrastructure.ChargerSpecification;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructureSpecification;
import org.matsim.contrib.ev.strategic.access.ChargerAccess;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.collections.QuadTrees;

/**
 * This charger provider keeps a spatial index of chargers that are tagged as
 * "public". When searching for a charger, it returns candidates within the
 * configured search radius and based on the subscriptions of the
 * charger/persons.
 * 
 * A charger can be tagged as public by setting its sevc:public charger
 * attribute to true.
 * 
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class PublicChargerProvider implements ChargerProvider {
	static public final String PUBLIC_CHARGER_ATTRIBUTE = "sevc:public";

	private final Scenario scenario;
	private final QuadTree<ChargerSpecification> index;
	private final double radius;

	private final ChargerAccess access;

	PublicChargerProvider(Scenario scenario, QuadTree<ChargerSpecification> index, ChargerAccess access,
			double radius) {
		this.scenario = scenario;
		this.index = index;
		this.radius = radius;
		this.access = access;
	}

	@Override
	public Collection<ChargerSpecification> findChargers(Person person, Plan plan, ChargerRequest request) {
		Coord location = request.isLegBased()
				? scenario.getNetwork().getLinks().get(request.leg().getRoute().getEndLinkId()).getCoord()
				: PopulationUtils.decideOnCoordForActivity(request.startActivity(), scenario);

		return index.getDisk(location.getX(), location.getY(), radius).stream()
				.filter(charger -> access.hasAccess(person, charger)).toList();
	}

	static public PublicChargerProvider create(Scenario scenario, ChargingInfrastructureSpecification infrastrcuture,
			ChargerAccess access, double radius) {
		Network network = scenario.getNetwork();
		List<ChargerSpecification> chargers = new LinkedList<>();

		for (ChargerSpecification charger : infrastrcuture.getChargerSpecifications().values()) {
			Boolean isPublic = (Boolean) charger.getAttributes().getAttribute(PUBLIC_CHARGER_ATTRIBUTE);

			if (isPublic != null && isPublic) {
				chargers.add(charger);
			}
		}

		return new PublicChargerProvider(scenario,
				chargers.isEmpty() ? new QuadTree<>(0.0, 0.0, 0.0, 0.0) : QuadTrees.createQuadTree(chargers, c -> {
					Link link = network.getLinks().get(c.getLinkId());
					return link.getCoord();
				}, 0.0), access, radius);
	}

	/**
	 * Return whether a charger is public.
	 */
	static public boolean isPublicCharger(ChargerSpecification charger) {
		Boolean isPublic = (Boolean) charger.getAttributes().getAttribute(PUBLIC_CHARGER_ATTRIBUTE);
		return isPublic != null && isPublic;
	}

	/**
	 * Sets a charge public or not.
	 */
	static public void setPublic(ChargerSpecification charger, boolean isPublic) {
		charger.getAttributes().putAttribute(PUBLIC_CHARGER_ATTRIBUTE, isPublic);
	}
}
