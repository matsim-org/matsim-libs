package org.matsim.contrib.carsharing.manager.routers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.contrib.carsharing.router.CarsharingRoute;
import org.matsim.contrib.carsharing.vehicles.CSVehicle;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * @author balac
 */
public class RouteCarsharingTripImpl implements RouteCarsharingTrip {

	@Inject
	private Scenario scenario;
	@Inject
	private LeastCostPathCalculatorFactory pathCalculatorFactory;

	@Inject
	@Named("ff")
	private TravelTime travelTimes;
	@Inject
	private Map<String, TravelDisutilityFactory> travelDisutilityFactories;

	@Inject
	@Named("carnetwork")
	private Network networkFF;

	private final ArrayList<String> carsharingLegs = new ArrayList<>(Arrays.asList("oneway", "twoway", "freefloating"));

	private final String[] carsharingVehicleLegs = { "oneway_vehicle", "twoway_vehicle", "freefloating_vehicle" };
	private final String[] accessCSLegs = { "access_walk_ow", "access_walk_tw", "access_walk_ff" };

	private final String[] egressCSLegs = { "egress_walk_ow", "egress_walk_tw", "egress_walk_ff" };

	private final String[] csInteraction = { "ow_interaction", "tw_interaction", "ff_interaction" };

	@Override
	public List<PlanElement> routeCarsharingTrip(Plan plan, Leg legToBeRouted, double time, CSVehicle vehicle,
			Link vehicleLinkLocation, Link parkingLocation, boolean keepTheCarForLaterUse, boolean hasVehicle) {
		PopulationFactory pf = scenario.getPopulation().getFactory();

		TravelDisutility travelDisutility = travelDisutilityFactories.get(TransportMode.car)
				.createTravelDisutility(travelTimes);
		LeastCostPathCalculator pathCalculator = pathCalculatorFactory.createPathCalculator(networkFF, travelDisutility,
				travelTimes);

		String mainMode = legToBeRouted.getMode();
		int index = carsharingLegs.indexOf(mainMode);
		final List<PlanElement> trip = new ArrayList<PlanElement>();

		Person person = plan.getPerson();
		CarsharingRoute route = (CarsharingRoute) legToBeRouted.getRoute();
		final Link currentLink = networkFF.getLinks().get(route.getStartLinkId());
		final Link destinationLink = networkFF.getLinks().get(route.getEndLinkId());

		if (hasVehicle) {
			// === car leg

			trip.add(RouterUtils.createCarLeg(pf, pathCalculator, person,
					this.networkFF.getLinks().get(currentLink.getId()),
					this.networkFF.getLinks().get(parkingLocation.getId()), carsharingVehicleLegs[index],
					vehicle.getVehicleId(), time));

			if (!keepTheCarForLaterUse) {

				Activity activityE = scenario.getPopulation().getFactory()
						.createActivityFromLinkId(csInteraction[index], parkingLocation.getId());
				activityE.setMaximumDuration(0);

				trip.add(activityE);

				trip.add(RouterUtils.createWalkLeg(pf, parkingLocation, destinationLink, egressCSLegs[index], time));
			}

		} else {

			String ffVehId = vehicle.getVehicleId();
			trip.add(RouterUtils.createWalkLeg(scenario.getPopulation().getFactory(), currentLink, vehicleLinkLocation,
					accessCSLegs[index], time));

			Activity activityS = scenario.getPopulation().getFactory().createActivityFromLinkId(csInteraction[index],
					vehicleLinkLocation.getId());
			activityS.setMaximumDuration(0);

			trip.add(activityS);
			// === car leg: ===

			if (!keepTheCarForLaterUse) {
				trip.add(RouterUtils.createCarLeg(pf, pathCalculator, person,
						this.networkFF.getLinks().get(vehicleLinkLocation.getId()),
						this.networkFF.getLinks().get(parkingLocation.getId()), carsharingVehicleLegs[index], ffVehId,
						time));
				Activity activityE = scenario.getPopulation().getFactory()
						.createActivityFromLinkId(csInteraction[index], parkingLocation.getId());
				activityE.setMaximumDuration(0);

				trip.add(activityE);

				trip.add(RouterUtils.createWalkLeg(pf, parkingLocation, destinationLink, egressCSLegs[index], time));
			} else {
				trip.add(RouterUtils.createCarLeg(pf, pathCalculator, person,
						this.networkFF.getLinks().get(vehicleLinkLocation.getId()),
						this.networkFF.getLinks().get(destinationLink.getId()), carsharingVehicleLegs[index], ffVehId,
						time));

			}

		}
		return trip;
	}

}
