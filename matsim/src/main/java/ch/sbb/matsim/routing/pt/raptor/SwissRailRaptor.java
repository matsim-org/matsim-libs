/*
 * Copyright (C) Schweizerische Bundesbahnen SBB, 2017.
 */

package ch.sbb.matsim.routing.pt.raptor;

import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;
import ch.sbb.matsim.routing.pt.raptor.RaptorRoute.RoutePart;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.Facility;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Provides public transport route search capabilities using an implementation of the
 * RAPTOR algorithm underneath.
 *
 * @author mrieser / SBB
 */
public class SwissRailRaptor implements TransitRouter {

    private static final Logger log = Logger.getLogger(SwissRailRaptor.class);

    private final SwissRailRaptorData data;
    private final SwissRailRaptorCore raptor;
    private final RaptorParametersForPerson parametersForPerson;
    private final RaptorRouteSelector defaultRouteSelector;
    private final RaptorStopFinder stopFinder;
    private final String subpopulationAttribute;

    private boolean treeWarningShown = false;

    public SwissRailRaptor(final SwissRailRaptorData data, RaptorParametersForPerson parametersForPerson,
                           RaptorRouteSelector routeSelector, RaptorStopFinder stopFinder) {
        this(data, parametersForPerson, routeSelector, stopFinder, null );
        log.info("SwissRailRaptor was initialized without support for subpopulations or intermodal access/egress legs.");
    }

    public SwissRailRaptor( final SwissRailRaptorData data, RaptorParametersForPerson parametersForPerson,
				    RaptorRouteSelector routeSelector,
				    RaptorStopFinder stopFinder,
				    String subpopulationAttribute ) {
        this.data = data;
        this.raptor = new SwissRailRaptorCore(data);
        this.parametersForPerson = parametersForPerson;
        this.defaultRouteSelector = routeSelector;
        this.stopFinder = stopFinder;
        this.subpopulationAttribute = subpopulationAttribute;
    }

    @Override
    public List<Leg> calcRoute(Facility fromFacility, Facility toFacility, double departureTime, Person person) {
        RaptorParameters parameters = this.parametersForPerson.getRaptorParameters(person);
        if (parameters.getConfig().isUseRangeQuery()) {
            return this.performRangeQuery(fromFacility, toFacility, departureTime, person, parameters);
        }
        List<InitialStop> accessStops = findAccessStops(fromFacility, person, departureTime, parameters);
        List<InitialStop> egressStops = findEgressStops(toFacility, person, departureTime, parameters);

        RaptorRoute foundRoute = this.raptor.calcLeastCostRoute(departureTime, fromFacility, toFacility, accessStops, egressStops, parameters);
        RaptorRoute directWalk = createDirectWalk(fromFacility, toFacility, departureTime, person, parameters);

        /*
		 * The pt trip is compared with a direct walk from trip origin to trip destination. This is useful for backwards
		 * compatibility, but leads to many trips with only a single "transit_walk" leg which are then considered pt
		 * trips by the main mode identifier even though they do not contain any pt leg and should rather be considered
		 * "walk" trips.
		 * 
		 * That problem can be avoided by setting a very high direct walk factor in TransitRouterConfigGroup. However
		 * this should be combined with enabling mode choice for pt and walk trips such that slow pt trips can be
		 * replaced by (faster) walk trips by mode choice. Otherwise agents can be stuck with very slow pt trips.
		 * 
		 * Comparison is only made between a pt trip and a direct walk trip, other modes (e.g. intermodal access/egress
		 * modes) are not considered. If they had been considered here, the router would effectively be a mode choice
		 * module although it is supposed not to change mode choice but rather to simply return a route for a given
		 * mode. Furthermore there is the problem that the generalized cost calculated by the router can be different
		 * from the cost the agent will be exposed to in scoring, because the mode performed differently in mobsim than
		 * anticipated by the router (e.g. the drt travel time turns out to be higher than expected, but the router will
		 * always chose a direct drt trip over the pt trip, because the router might consistently underestimate drt
		 * travel time). So it seems a bad idea to compare other modes than walk here. Walk is usually teleported at a
		 * fixed speed, so it is usually completely deterministic whereas other modes are not.
		 * 
		 * Overall enabling mode choice and setting a very high direct walk factor (e.g. Double.POSITIVE_INFINITY which 
		 * effectively excludes all direct walks) seems cleaner and better.
		 * 
		 * vsp-gleich sep'19 (after talking with KN)
		 * 
		 * 
		 * foundRoute.parts.size() == 0 can happen if SwissRasilRaptorCore.createRaptorRoute() finds a trip made up of,
		 * only 2 parts which consists only of an access and an egress leg without any pt leg inbetween.
		 */
        if (foundRoute == null || foundRoute.parts.size() == 0 || hasNoPtLeg(foundRoute.parts) || 
        		directWalk.getTotalCosts() * parameters.getDirectWalkFactor() < foundRoute.getTotalCosts()) {
        	if (person == null) {
            	log.debug("No route found for person null: trip from x=" + fromFacility.getCoord().getX() + ",y=" + fromFacility.getCoord().getY() + " departure at " + departureTime + " to x=" + toFacility.getCoord().getX() + ",y=" + toFacility.getCoord().getY());
        	} else {
            	log.debug("No route found for person " + person.getId() + ": trip from x=" + fromFacility.getCoord().getX() + ",y=" + fromFacility.getCoord().getY() + " departure at " + departureTime + " to x=" + toFacility.getCoord().getX() + ",y=" + toFacility.getCoord().getY());
        	}
            foundRoute = directWalk;
        }
        
        List<Leg> legs = RaptorUtils.convertRouteToLegs(foundRoute);
        return legs;
    }
    
	private boolean hasNoPtLeg(List<RoutePart> parts) {
		for (RoutePart part : parts) {
			// if the route part has a TransitLine, it must be a real pt leg
			if (part.line != null) {
				return false;
			}
		}
		return true;
	}

	private List<Leg> performRangeQuery(Facility fromFacility, Facility toFacility, double desiredDepartureTime, Person person, RaptorParameters parameters) {
        SwissRailRaptorConfigGroup srrConfig = parameters.getConfig();

//        Object attr = this.personAttributes.getAttribute(person.getId().toString(), this.subpopulationAttribute);
	    Object attr = person.getAttributes().getAttribute( this.subpopulationAttribute ) ;
        String subpopulation = attr == null ? null : attr.toString();
        SwissRailRaptorConfigGroup.RangeQuerySettingsParameterSet rangeSettings = srrConfig.getRangeQuerySettings(subpopulation);

        double earliestDepartureTime = desiredDepartureTime - rangeSettings.getMaxEarlierDeparture();
        double latestDepartureTime = desiredDepartureTime + rangeSettings.getMaxLaterDeparture();

        if (this.defaultRouteSelector instanceof ConfigurableRaptorRouteSelector) {
            ConfigurableRaptorRouteSelector selector = (ConfigurableRaptorRouteSelector) this.defaultRouteSelector;

            SwissRailRaptorConfigGroup.RouteSelectorParameterSet params = srrConfig.getRouteSelector(subpopulation);

            selector.setBetaTransfer(params.getBetaTransfers());
            selector.setBetaTravelTime(params.getBetaTravelTime());
            selector.setBetaDepartureTime(params.getBetaDepartureTime());
        }

        return this.calcRoute(fromFacility, toFacility, earliestDepartureTime, desiredDepartureTime, latestDepartureTime, person, this.defaultRouteSelector);
    }

    public List<Leg> calcRoute(Facility fromFacility, Facility toFacility, double earliestDepartureTime, double desiredDepartureTime, double latestDepartureTime, Person person) {
        return calcRoute(fromFacility, toFacility, earliestDepartureTime, desiredDepartureTime, latestDepartureTime, person, this.defaultRouteSelector);
    }

    public List<Leg> calcRoute(Facility fromFacility, Facility toFacility, double earliestDepartureTime, double desiredDepartureTime, double latestDepartureTime, Person person, RaptorRouteSelector selector) {
        RaptorParameters parameters = this.parametersForPerson.getRaptorParameters(person);
        List<InitialStop> accessStops = findAccessStops(fromFacility, person, desiredDepartureTime, parameters);
        List<InitialStop> egressStops = findEgressStops(toFacility, person, desiredDepartureTime, parameters);

        List<RaptorRoute> foundRoutes = this.raptor.calcRoutes(earliestDepartureTime, desiredDepartureTime, latestDepartureTime, fromFacility, toFacility, accessStops, egressStops, parameters);
        RaptorRoute foundRoute = selector.selectOne(foundRoutes, desiredDepartureTime);
        RaptorRoute directWalk = createDirectWalk(fromFacility, toFacility, desiredDepartureTime, person, parameters);

        if (foundRoute == null || directWalk.getTotalCosts() < foundRoute.getTotalCosts()) {
            foundRoute = directWalk;
        }
        List<Leg> legs = RaptorUtils.convertRouteToLegs(foundRoute);
        // TODO adapt the activity end time of the activity right before this trip
        /* Sadly, it's not that easy to find the previous activity, as we only have from- and to-facility
         * and the departure time. One would have to search through the person's selectedPlan to find
         * a matching activity, but what if an agent travels twice a day between from- and to-activity
         * and it only sets the activity duration, but not the end-time?
         * One could try to come up with some heuristic, but that would be very error-prone and
         * not satisfying. The clean solution would be to implement our own PlanRouter which
         * uses our own TripRouter which would take care of adapting the departure time,
         * but sadly PlanRouter is hardcoded in several places (e.g. PrepareForSimImpl), so it
         * cannot easily be replaced. So I fear I currently don't see a simple solution for that.
         * mrieser / march 2018.
         */
        return legs;
    }

    public List<RaptorRoute> calcRoutes(Facility fromFacility, Facility toFacility, double earliestDepartureTime, double desiredDepartureTime, double latestDepartureTime, Person person) {
        RaptorParameters parameters = this.parametersForPerson.getRaptorParameters(person);
        List<InitialStop> accessStops = findAccessStops(fromFacility, person, desiredDepartureTime, parameters);
        List<InitialStop> egressStops = findEgressStops(toFacility, person, desiredDepartureTime, parameters);

        List<RaptorRoute> foundRoutes = this.raptor.calcRoutes(earliestDepartureTime, desiredDepartureTime, latestDepartureTime, fromFacility, toFacility, accessStops, egressStops, parameters);
        RaptorRoute directWalk = createDirectWalk(fromFacility, toFacility, desiredDepartureTime, person, parameters);

        if (foundRoutes == null) {
            foundRoutes = new ArrayList<>(1);
        }
        if (foundRoutes.isEmpty() || directWalk.getTotalCosts() < foundRoutes.get(0).getTotalCosts()) {
            foundRoutes.add(directWalk); // add direct walk if it seems plausible
        }
        return foundRoutes;
    }

    public Map<Id<TransitStopFacility>, SwissRailRaptorCore.TravelInfo> calcTree(TransitStopFacility fromStop, double departureTime, RaptorParameters parameters) {
        return this.calcTree(Collections.singletonList(fromStop), departureTime, parameters);
    }

    public Map<Id<TransitStopFacility>, SwissRailRaptorCore.TravelInfo> calcTree(Collection<TransitStopFacility> fromStops, double departureTime, RaptorParameters parameters) {
        if (this.data.config.getOptimization() != RaptorStaticConfig.RaptorOptimization.OneToAllRouting && !this.treeWarningShown) {
            log.warn("SwissRailRaptorData was not initialized with full support for tree calculations and may result in unexpected results. Use `RaptorStaticConfig.setOptimization(RaptorOptimization.OneToAllRouting)` to fix this issue.");
            this.treeWarningShown = true;
        }
        List<InitialStop> accessStops = new ArrayList<>();
        for (TransitStopFacility stop : fromStops) {
            accessStops.add(new InitialStop(stop, 0, 0, 0, null));
        }
        return this.calcLeastCostTree(accessStops, departureTime, parameters);
    }

    public Map<Id<TransitStopFacility>, SwissRailRaptorCore.TravelInfo> calcTree(Facility fromFacility, double departureTime, Person person) {
        RaptorParameters parameters = this.parametersForPerson.getRaptorParameters(person);
        List<InitialStop> accessStops = findAccessStops(fromFacility, person, departureTime, parameters);
        return this.calcLeastCostTree(accessStops, departureTime, parameters);
    }

    private Map<Id<TransitStopFacility>, SwissRailRaptorCore.TravelInfo> calcLeastCostTree(Collection<InitialStop> accessStops, double departureTime, RaptorParameters parameters) {
        return this.raptor.calcLeastCostTree(departureTime, accessStops, parameters);
    }

    public SwissRailRaptorData getUnderlyingData() {
        return this.data;
    }

    private List<InitialStop> findAccessStops(Facility facility, Person person, double departureTime, RaptorParameters parameters) {
        return this.stopFinder.findStops(facility, person, departureTime, parameters, this.data, RaptorStopFinder.Direction.ACCESS);
    }

    private List<InitialStop> findEgressStops(Facility facility, Person person, double departureTime, RaptorParameters parameters) {
        return this.stopFinder.findStops(facility, person, departureTime, parameters, this.data, RaptorStopFinder.Direction.EGRESS);
    }

    private RaptorRoute createDirectWalk(Facility fromFacility, Facility toFacility, double departureTime, Person person, RaptorParameters parameters) {
        double beelineDistance = CoordUtils.calcEuclideanDistance(fromFacility.getCoord(), toFacility.getCoord());
        double walkTime = beelineDistance / parameters.getBeelineWalkSpeed();
        double walkCost_per_s = -parameters.getMarginalUtilityOfTravelTime_utl_s(TransportMode.transit_walk);
        double walkCost = walkTime * walkCost_per_s;
        double beelineDistanceFactor = this.data.config.getBeelineWalkDistanceFactor();

        RaptorRoute route = new RaptorRoute(fromFacility, toFacility, walkCost);
        route.addNonPt(null, null, departureTime, walkTime, beelineDistance * beelineDistanceFactor, TransportMode.transit_walk);
        return route;
    }

}
