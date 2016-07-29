package org.matsim.contrib.pseudosimulation.distributed;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.Route;
import org.matsim.contrib.pseudosimulation.distributed.plans.PlanGenome;
import org.matsim.contrib.pseudosimulation.distributed.scoring.PlanScoreComponent;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.facilities.ActivityFacility;
import org.matsim.pt.routes.ExperimentalTransitRouteFactory;
import org.matsim.vehicles.Vehicle;

class PlanSerializable implements Serializable {
    public static boolean isUseTransit = false;

    public void setScoreComponents(ArrayList<PlanScoreComponent> scoreComponents) {
        this.scoreComponents = scoreComponents;
    }

    ArrayList<PlanScoreComponent> scoreComponents = new ArrayList<>();
    private final ArrayList<PlanElementSerializable> planElements;
    private final String personId;
    private final Double score;
    private final String type;
    double pSimScore;
    private String genome = "";
    public PlanSerializable(Plan plan) {
        planElements = new ArrayList<>();
        for (PlanElement planElement : plan.getPlanElements())
            if (planElement instanceof Activity)
                planElements.add(new ActivitySerializable((Activity) planElement));
            else
                planElements.add(new LegSerializable((Leg) planElement));
        personId = plan.getPerson().getId().toString();
        score = plan.getScore();
//        score = 0.0;
        type = plan.getType();
        if (plan instanceof PlanGenome) {
            PlanGenome planGenome = (PlanGenome) plan;
            genome = planGenome.getGenome();
            pSimScore = planGenome.getpSimScore();
            scoreComponents = planGenome.getScoreComponents();
        }
    }

    public Double getScore() {
        return score;
    }

    public Plan getPlan(Population population) {
        PlanGenome plan = new PlanGenome(population.getPersons().get(Id.createPersonId(personId)));
        plan.setGenome(genome);
        plan.setpSimScore(pSimScore);
        plan.setScore(score);
        plan.setType(type);
        plan.setAltScoreComponents(scoreComponents);
        for (PlanElementSerializable planElementSerializable : planElements)
            if (planElementSerializable instanceof ActivitySerializable)
                plan.addActivity(((ActivitySerializable) planElementSerializable).getActivity());
            else
                plan.addLeg(((LegSerializable) planElementSerializable).getLeg());
        return plan;
    }

    public Plan getPlan(Person person) {
        PlanGenome plan = new PlanGenome(person);
        plan.setGenome(genome);
        plan.setpSimScore(pSimScore);
        plan.setScore(score);
        plan.setType(type);
        plan.setAltScoreComponents(scoreComponents);
        for (PlanElementSerializable planElementSerializable : planElements)
            if (planElementSerializable instanceof ActivitySerializable)
                plan.addActivity(((ActivitySerializable) planElementSerializable).getActivity());
            else
                plan.addLeg(((LegSerializable) planElementSerializable).getLeg());
        return plan;
    }

    private interface PlanElementSerializable extends Serializable {

    }

    interface RouteSerializable extends Serializable {
        Route getRoute(String mode);
    }

    class ActivitySerializable implements PlanElementSerializable {
        private final CoordSerializable coord;
        private final double endTime;
        private final String facIdString;
        private final String linkIdString;
        private final double maximumDuration;
        private final double startTime;
        private final String type;

        public ActivitySerializable(Activity act) {
            coord = new CoordSerializable(act.getCoord());
            endTime = act.getEndTime();
            facIdString = act.getFacilityId() == null ? null : act.getFacilityId().toString();
            linkIdString = act.getLinkId() == null ? null : act.getLinkId().toString();
            maximumDuration = act.getMaximumDuration();
            startTime = act.getStartTime();
            type = act.getType();
        }

        public Activity getActivity() {
            Activity activity = PopulationUtils.createActivityFromCoordAndLinkId(type, coord.getCoord(), linkIdString == null ? null : Id.createLinkId(linkIdString));
            activity.setEndTime(endTime);
            activity.setFacilityId(facIdString == null ? null : Id.create(facIdString, ActivityFacility.class));
            activity.setMaximumDuration(maximumDuration);
            activity.setStartTime(startTime);
            return activity;
        }
    }

    class LegSerializable implements PlanElementSerializable {
        private final double departureTime;
        private final String mode;
        private final double travelTime;
        private RouteSerializable route;

        public LegSerializable(Leg leg) {
            departureTime = leg.getDepartureTime();
            mode = leg.getMode();
            travelTime = leg.getTravelTime();
            if (mode.equals(TransportMode.pt))
                System.out.print("");
            route = new GenericRouteSerializable(leg.getRoute());


        }

        public Leg getLeg() {
            Leg leg = PopulationUtils.createLeg(mode);
            leg.setDepartureTime(departureTime);
            leg.setTravelTime(travelTime);
            leg.setRoute(route == null ? null : route.getRoute(mode));
            return leg;
        }
    }

    class CoordSerializable implements Serializable {
        private final double x;
        private final double y;

        public CoordSerializable(Coord coord) {
            x = coord.getX();
            y = coord.getY();
        }

        public Coord getCoord() {
            return new Coord(x, y);

        }
    }

    class LinkNetworkRouteSerializable implements RouteSerializable {

        private final double distance;
        private final String endLinkIdString;
        private final String startLinkIdString;
        private final double travelCost;
        private final double travelTime;
        private final String vehicleIdString;
        private final List<String> linkIdStrings;

        public LinkNetworkRouteSerializable(NetworkRoute route) {
            distance = route.getDistance();
            endLinkIdString = route.getEndLinkId().toString();
            startLinkIdString = route.getStartLinkId().toString();
            travelCost = route.getTravelCost();
            travelTime = route.getTravelTime();
            vehicleIdString = route.getVehicleId() == null ? null : route.getVehicleId().toString();
            List<Id<Link>> linkIds = route.getLinkIds();
            linkIdStrings = new ArrayList<>();
            for (Id<Link> linkid : linkIds)
                linkIdStrings.add(linkid.toString());
        }

        @Override
        public Route getRoute(String mode) {
            Id<Link> startLinkId = Id.createLinkId(startLinkIdString);
            Id<Link> endLinkId = Id.createLinkId(endLinkIdString);
            NetworkRoute route = new LinkNetworkRouteImpl(startLinkId, endLinkId);
            route.setDistance(distance);
            List<Id<Link>> linkIds = new ArrayList<>();
            for (String linkId : linkIdStrings)
                linkIds.add(Id.createLinkId(linkId));
            route.setLinkIds(startLinkId, linkIds, endLinkId);
            route.setTravelCost(travelCost);
            route.setTravelTime(travelTime);
            route.setVehicleId(vehicleIdString == null ? null : Id.create(vehicleIdString, Vehicle.class));
            return route;
        }

    }

    class GenericRouteSerializable implements RouteSerializable {

        private final double distance;
        private final String endLinkIdString;
        private final String routeDescription;
        private final String startLinkIdString;
        private final double travelTime;

        public GenericRouteSerializable(Route route) {
            distance = route.getDistance();
            endLinkIdString = route.getEndLinkId().toString();
            routeDescription = route.getRouteDescription();
            startLinkIdString = route.getStartLinkId().toString();
            travelTime = route.getTravelTime();
        }

        @Override
        public Route getRoute(String mode) {
            Route route;
            Id<Link> startLinkId = Id.createLinkId(startLinkIdString);
            Id<Link> endLinkId = Id.createLinkId(endLinkIdString);
            if (mode.equals(TransportMode.pt) && isUseTransit) {
                route = new ExperimentalTransitRouteFactory().createRoute(startLinkId, endLinkId);
            } else {
                route = new GenericRouteImpl(startLinkId, endLinkId);
            }
            route.setDistance(distance);
            route.setTravelTime(travelTime);
            route.setStartLinkId(startLinkId);
            route.setEndLinkId(endLinkId);
            route.setRouteDescription(routeDescription);
            return route;
        }

    }

}
