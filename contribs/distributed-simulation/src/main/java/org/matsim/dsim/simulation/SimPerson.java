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
@Deprecated
public class SimPerson {

	public enum State {ACTIVITY, LEG}

	@Getter
	private final Id<Person> id;
	private final List<PlanElement> planElements;

	private int currentPlanElement;
	private int currentRouteElement;

	public SimPerson(Person person) {
		// make a defensive copy, as person might be used elsewhere
		this.planElements = new ArrayList<>(person.getSelectedPlan().getPlanElements());
		this.id = person.getId();
	}

	public SimPerson(PersonMsg fromMessage) {
		// use the element from the message directly. We expect that we are the only ones using
		// the plan elements from the message
		this.planElements = fromMessage.getPlan();
		this.currentRouteElement = fromMessage.getCurrentRouteElement();
		this.currentPlanElement = fromMessage.getCurrentPlanElement();
		this.id = fromMessage.getId();
	}

	public PersonMsg toMessage() {
		return PersonMsg.builder()
			.setPlan(planElements)
			.setId(id)
			.setCurrentPlanElement(currentPlanElement)
			.setCurrentRouteElement(currentRouteElement)
			.build();
	}

	public void advancePlan() {
		currentPlanElement++;
		currentRouteElement = 0;

		assert currentPlanElement < planElements.size() : "There are no more plan elements for person: " + this.id;
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
		return (Activity) planElements.get(index);
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
		return planElement;
	}

	public Leg getCurrentLeg() {
		if ((currentPlanElement & 1) == 0) throw new RuntimeException("Current plan element is not a leg.");
		return (Leg) planElements.get(currentPlanElement);
	}

	public boolean hasCurrentLeg() {
		return (currentPlanElement & 1) == 1 && currentPlanElement < planElements.size();
	}

	public Id<Link> getCurrentRouteElement() {
		return getRouteElement(RouteAccess.Curent);
	}

	public Id<Link> getRouteElement(RouteAccess accessType) {

		var route = getCurrentLeg().getRoute();
		var routeIndex = getRouteIndex(accessType);
		var routeSize = getCurrentRouteSize();

		// this is slightly complicated because network routes don't store their start and end link in the link array.
		// return the start link regardless if index is zero
		if (routeIndex == 0) {
			return route.getStartLinkId();
		}

		// if the end link was requested, return it. If the caller wants to know the next link, and it would be the last link,
		// and it is the same as the start link, return null, because the route has no next link.
		if (routeIndex == routeSize - 1) {
			return accessType == RouteAccess.Next && route.getStartLinkId().equals(route.getEndLinkId()) ? null : route.getEndLinkId();
			// if we are larger than the route size return 'null' becuase there are no more elements in the route
		} else if (routeIndex >= routeSize) {
			return null;
		} else {
			// otherwise, if it is a network route, get one of the in between elements
			if (route instanceof NetworkRoute nr) {
				return nr.getLinkIds().get(routeIndex - 1);
			} else {
				throw new RuntimeException("Get Route Element not implemented for Routes of class: " + route.getClass());
			}
		}
	}

	private int getRouteIndex(RouteAccess accessType) {
		return switch (accessType) {
			case Curent -> currentRouteElement;
			case Next -> currentRouteElement + 1;
			case Last -> getCurrentRouteSize() - 1;
		};
	}

	private int getCurrentRouteSize() {

		if (getCurrentLeg().getRoute() instanceof NetworkRoute nr) {
			return nr.getLinkIds().size() + 2;
		}

		// the default would be a teleported route which has at least a start and an end link.
		return 2;
	}
}
