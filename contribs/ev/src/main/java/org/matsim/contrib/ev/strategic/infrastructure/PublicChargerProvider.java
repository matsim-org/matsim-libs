package org.matsim.contrib.ev.strategic.infrastructure;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.ev.infrastructure.ChargerSpecification;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructureSpecification;
import org.matsim.contrib.ev.strategic.access.ChargerAccess;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.collections.QuadTrees;
import org.matsim.core.utils.geometry.CoordUtils;

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
	private final double activityBasedRadius;
	private final double legBasedRadius;

	private final ChargerAccess access;

	PublicChargerProvider(Scenario scenario, QuadTree<ChargerSpecification> index, ChargerAccess access,
			double activityBasedRadius, double legBasedRadius) {
		this.scenario = scenario;
		this.index = index;
		this.activityBasedRadius = activityBasedRadius;
		this.legBasedRadius = legBasedRadius;
		this.access = access;
	}

	@Override
	public Collection<ChargerSpecification> findChargers(Person person, Plan plan, ChargerRequest request) {
		return request.isLegBased() ? //
				findChargersLegBased(person, plan, request) : //
				findChargersActivityBased(person, plan, request);
	}

	private Collection<ChargerSpecification> findChargersActivityBased(Person person, Plan plan,
			ChargerRequest request) {
		Coord location = PopulationUtils.decideOnCoordForActivity(request.startActivity(), scenario);

		return index.getDisk(location.getX(), location.getY(), activityBasedRadius).stream()
				.filter(charger -> access.hasAccess(person, charger)).toList();
	}

	private Collection<ChargerSpecification> findChargersLegBased(Person person, Plan plan, ChargerRequest request) {
		List<Coord> locations = new LinkedList<>();

		Coord location = scenario.getNetwork().getLinks().get(request.leg().getRoute().getStartLinkId()).getCoord();
		locations.add(location);

		if (request.leg().getRoute() instanceof NetworkRoute route) {
			for (Id<Link> linkId : route.getLinkIds()) {
				Coord candidate = scenario.getNetwork().getLinks().get(linkId).getCoord();

				if (CoordUtils.calcEuclideanDistance(candidate, location) >= legBasedRadius * 2.0) {
					location = candidate;
					locations.add(location);
				}
			}
		}

		location = scenario.getNetwork().getLinks().get(request.leg().getRoute().getEndLinkId()).getCoord();
		locations.add(location);

		return locations.stream().flatMap(point -> index.getDisk(point.getX(), point.getY(), legBasedRadius).stream())
				.filter(charger -> access.hasAccess(person, charger)).distinct().toList();
	}

	static public PublicChargerProvider create(Scenario scenario, ChargingInfrastructureSpecification infrastrcuture,
			ChargerAccess access, double activityBasedRadius, double legBasedRadius) {
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
				}, 0.0), access, activityBasedRadius, legBasedRadius);
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
