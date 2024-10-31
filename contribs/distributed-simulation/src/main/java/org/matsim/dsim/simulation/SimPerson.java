package org.matsim.dsim.simulation;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.dsim.messages.PersonMsg;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Person which is used in the DiSim. The person keeps track of which plan element it currently
 * operates on. If the current plan element is a leg, the person also keeps track at which route
 * element the person currently is.
 * <p>
 * Internally, this person stores the protobuf representation of a plan. Since protobuf objects are
 * immutable, I decided to create this wrapper person around the plan. The plan therefore can't change
 * at the moment. We have to decide later, whether we want to have the flexibility of changing plans
 * frequently
 */
@Log4j2
public class SimPerson {

    public enum State {ACTIVITY, LEG}

    @Getter
    private final Id<Person> id;

    private int currentPlanElement;
    private int currentRouteElement;

    private final List<Activity> activities = new ArrayList<>();
    private final List<Leg> legs = new ArrayList<>();

    public SimPerson(Person person) {
        // convert plan to protobuf plan
        var fromPlan = person.getSelectedPlan();

        for (PlanElement element : fromPlan.getPlanElements()) {
            if (element instanceof Leg leg) {
                legs.add(leg);
            } else if (element instanceof Activity activity) {
               activities.add(activity);
            }
        }
        this.id = person.getId();
    }

    public SimPerson(PersonMsg fromMessage) {
        for (PlanElement element : fromMessage.getPlan()) {
            if (element instanceof Leg leg) {
                legs.add(leg);
            } else if (element instanceof Activity activity) {
                activities.add(activity);
            }
        }
        this.currentRouteElement = fromMessage.getCurrentRouteElement();
        this.currentPlanElement = fromMessage.getCurrentPlanElement();
        this.id = fromMessage.getId();
    }

    public PersonMsg toMessage() {
        return PersonMsg.builder()
                .setPlan(Stream.concat(activities.stream(), legs.stream()).toList())
                .setId(id)
                .setCurrentPlanElement(currentPlanElement)
                .setCurrentRouteElement(currentRouteElement)
                .build();
    }

    public void advancePlan() {
        currentPlanElement++;
        currentRouteElement = 0;
    }

    public enum Advance {One, Last}

    public enum RouteAccess {Curent, Next, Last}

    public enum ActivityAccess {Curent, Next, Prev}

    public void advanceRoute(Advance advance) {
        currentRouteElement = switch (advance) {
            case One -> currentRouteElement + 1;
            case Last -> getCurrentRouteSize() - 1;
        };
    }

    public State getCurrentState() {
        return currentPlanElement % 2 == 0 ? State.ACTIVITY : State.LEG;
    }

    public Activity getCurrentActivity() {
        return getActivity(ActivityAccess.Curent);
    }

    public Activity getActivity(ActivityAccess accessType) {
        int index = getActivityIndex(accessType);
        return activities.get(index);
    }

    private int getActivityIndex(ActivityAccess accessType) {
        int planElement = switch (accessType) {
            case Curent -> this.currentPlanElement;
            case Next -> (currentPlanElement & 1) == 0 ? currentPlanElement + 2 : currentPlanElement + 1;
            case Prev -> (currentPlanElement & 1) == 0 ? currentPlanElement - 2 : currentPlanElement - 1;
        };
        if ((planElement & 1) == 1 && ActivityAccess.Curent == accessType) {
            throw new RuntimeException("Current plan element is not an activity.");
        }
        return planElement / 2;
    }

    public Leg getCurrentLeg() {
        if ((currentPlanElement & 1) == 0) throw new RuntimeException("Current plan element is not a leg.");
        int leg_index = (currentPlanElement - 1) / 2;
        return legs.get(leg_index);
    }


    public boolean hasCurrentLeg() {
        int leg_index = (currentPlanElement - 1) / 2;
        return legs.size() > leg_index;
    }

    public Id<Link> getCurrentRouteElement() {
        return getRouteElement(RouteAccess.Curent);
    }

    public Id<Link> getRouteElement(RouteAccess accessType) {
        int index = getRouteIndex(accessType);
        Route route = getCurrentLeg().getRoute();

        if (index == 0)
            return route.getStartLinkId();

        boolean sameLocation = route.getStartLinkId().equals(route.getEndLinkId());

        // if the route is a single link, we can't move further
        if (index >= 1 && sameLocation)
            return null;

        if (route instanceof NetworkRoute nr) {
            List<Id<Link>> linkIds = nr.getLinkIds();

            if (index == linkIds.size() + 1)
                return nr.getEndLinkId();

            // indicate no further elements with 'null'. We need to check bounds here because we will cause an
            // IndexOutOfBoundsException otherwise
            if (index > linkIds.size() + 1)
                return null;

            return linkIds.get(index - 1);
        }

        if (index == 1)
            return route.getEndLinkId();

        return null;
    }

    private int getRouteIndex(RouteAccess accessType) {
        return switch (accessType) {
            case Curent -> currentRouteElement;
            case Next -> currentRouteElement + 1;
            case Last -> getCurrentRouteSize() - 1;
        };
    }

    private int getCurrentRouteSize() {
        Leg leg = getCurrentLeg();

        // Start and end are counted within the route
        if (leg.getRoute() instanceof NetworkRoute nr) {
            return nr.getLinkIds().size() + 2;
        } else {
            if (leg.getRoute().getStartLinkId().equals(leg.getRoute().getEndLinkId())) {
                return 1;
            }

            return 2;
        }

    }
}
