/*
 * Copyright (C) Schweizerische Bundesbahnen SBB, 2018.
 */

package ch.sbb.matsim.routing.pt.raptor;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.facilities.Facility;
import org.matsim.pt.PtConstants;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.util.ArrayList;
import java.util.List;

/**
 * This replicates the functionality of {@link org.matsim.core.router.TransitRouterWrapper},
 * but in an adapted form suitable for the SwissRailRaptor.
 *
 * @author mrieser / SBB
 */
public class SwissRailRaptorRoutingModule implements RoutingModule {

    private static final StageActivityTypes STAGE_ACT_TYPES = new StageActivityTypesImpl(PtConstants.TRANSIT_ACTIVITY_TYPE);

    private final SwissRailRaptor raptor;
    private final RoutingModule walkRouter;
    private final TransitSchedule transitSchedule;
    private final Network network;

    public SwissRailRaptorRoutingModule(final SwissRailRaptor raptor,
                                        final TransitSchedule transitSchedule,
                                        Network network, final RoutingModule walkRouter) {
        if (raptor == null) {
            throw new NullPointerException("The router object is null, but is required later.");
        }
        this.raptor = raptor;
        this.transitSchedule = transitSchedule;
        this.network = network;
        if (walkRouter == null) {
            throw new NullPointerException("The walkRouter object is null, but is required later.");
        }
        this.walkRouter = walkRouter;
    }

    @Override
    public List<? extends PlanElement> calcRoute(Facility fromFacility, Facility toFacility, double departureTime, Person person) {
        List<Leg> legs = this.raptor.calcRoute(fromFacility, toFacility, departureTime, person);
        return legs != null ?
                fillWithActivities(legs) :
                walkRouter.calcRoute(fromFacility, toFacility, departureTime, person);
    }

    private List<? extends PlanElement> fillWithActivities(List<Leg> legs) {
        List<PlanElement> planElements = new ArrayList<>(legs.size() * 2);
        Leg prevLeg = null;
        for (Leg leg : legs) {
            if (prevLeg != null) {
                Coord coord = findCoordinate(prevLeg, leg);
                Id<Link> linkId = leg.getRoute().getStartLinkId();
                Activity act = PopulationUtils.createActivityFromCoordAndLinkId(PtConstants.TRANSIT_ACTIVITY_TYPE, coord, linkId);
                act.setMaximumDuration(0.0);
                planElements.add(act);
            }
            planElements.add(leg);
            prevLeg = leg;
        }
        return planElements;
    }

    private Coord findCoordinate(Leg prevLeg, Leg nextLeg) {
        if (prevLeg.getRoute() instanceof ExperimentalTransitRoute) {
            Id<TransitStopFacility> stopId = ((ExperimentalTransitRoute) prevLeg.getRoute()).getEgressStopId();
            return this.transitSchedule.getFacilities().get(stopId).getCoord();
        }
        if (nextLeg.getRoute() instanceof ExperimentalTransitRoute) {
            Id<TransitStopFacility> stopId = ((ExperimentalTransitRoute) nextLeg.getRoute()).getAccessStopId();
            return this.transitSchedule.getFacilities().get(stopId).getCoord();
        }
        // fallback: prevLeg and nextLeg are not pt routes, so we have to guess the coordinate based on the link id
        Id<Link> linkId = prevLeg.getRoute().getEndLinkId();
        Link link = this.network.getLinks().get(linkId);
        return link.getToNode().getCoord();
    }

    @Override
    public StageActivityTypes getStageActivityTypes() {
        return STAGE_ACT_TYPES;
    }
}
