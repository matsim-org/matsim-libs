/*
 * Copyright (C) Schweizerische Bundesbahnen SBB, 2018.
 */

package ch.sbb.matsim.routing.pt.raptor;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.DefaultRoutingRequest;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.RoutingRequest;
import org.matsim.facilities.Facility;
import org.matsim.pt.routes.TransitPassengerRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * This replicates the functionality of {@link org.matsim.core.router.TransitRouterWrapper},
 * but in an adapted form suitable for the SwissRailRaptor.
 *
 * @author mrieser / SBB
 */
public class SwissRailRaptorRoutingModule implements RoutingModule {

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
    public List<? extends PlanElement> calcRoute(RoutingRequest request) {
        List<? extends PlanElement> legs = this.raptor.calcRoute(request);
        return legs != null ?
                fillWithActivities(legs) :
                walkRouter.calcRoute(request);
    }

    private List<? extends PlanElement> fillWithActivities(List<? extends PlanElement> segments) {
        List<PlanElement> planElements = new ArrayList<>(segments.size() * 2);
        PlanElement prevLeg = null;
        for (PlanElement pe : segments) {
            if (prevLeg != null) {
            	// only add pt interaction activities between two legs
				// otherwise we maintain interaction activities from
				// access and egress trips
				if (prevLeg instanceof Leg && pe instanceof Leg) {
					Coord coord = findCoordinate((Leg)prevLeg, (Leg)pe);
                	Id<Link> linkId = ((Leg)pe).getRoute().getStartLinkId();
                	Activity act = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(coord, linkId, TransportMode.pt);
                	planElements.add(act);
				}
            }
            planElements.add(pe);
            prevLeg = pe;
        }
        return planElements;
    }

    private Coord findCoordinate(Leg prevLeg, Leg nextLeg) {
        if (prevLeg.getRoute() instanceof TransitPassengerRoute) {
            Id<TransitStopFacility> stopId = ((TransitPassengerRoute) prevLeg.getRoute()).getEgressStopId();
            return this.transitSchedule.getFacilities().get(stopId).getCoord();
        }
        if (nextLeg.getRoute() instanceof TransitPassengerRoute) {
            Id<TransitStopFacility> stopId = ((TransitPassengerRoute) nextLeg.getRoute()).getAccessStopId();
            return this.transitSchedule.getFacilities().get(stopId).getCoord();
        }
        // fallback: prevLeg and nextLeg are not pt routes, so we have to guess the coordinate based on the link id
        Id<Link> linkId = prevLeg.getRoute().getEndLinkId();
        Link link = this.network.getLinks().get(linkId);
        return link.getToNode().getCoord();
    }

}
