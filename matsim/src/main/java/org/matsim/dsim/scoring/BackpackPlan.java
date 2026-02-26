package org.matsim.dsim.scoring;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Message;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.PopulationUtils;
import org.matsim.vehicles.Vehicle;

import java.util.Map;

/**
 * A backpack plan is collecting information about what a person has experienced during the simulation. Over the course
 * of the simulation it produces an experienced plan, which can be used for scoring.
 */
class BackpackPlan {

	private final Plan experiencedPlan;
	private final Map<String, ExperiencedRouteBuilderProvider> providers;

	private ExperiencedActivityBuilder currentActivity;
	private ExperiencedLegBuilder currentLeg;

	BackpackPlan(Msg msg, Map<String, ExperiencedRouteBuilderProvider> providers) {
		if (msg.actState() != null) {
			currentActivity = new ExperiencedActivityBuilder(msg.actState());
		}
		if (msg.legState() != null) {
			var mode = msg.legState().getMode();
			var provider = providers.get(mode);
			var routeBuilder = provider.get(msg.legState().routeBuilderData());
			var legData = msg.legState().data();
			currentLeg = new ExperiencedLegBuilder(legData, routeBuilder);
		}
		experiencedPlan = msg.experiencedPlan();
		this.providers = providers;
	}

	BackpackPlan(Map<String, ExperiencedRouteBuilderProvider> providers) {
		this.providers = providers;
		this.experiencedPlan = PopulationUtils.createPlan();
	}

	public void handleEvent(Event e) {

		switch (e) {
			case PersonDepartureEvent pde -> handlePersonDeparture(pde);
			case PersonArrivalEvent pae -> handlePersonArrival(pae);
			case ActivityStartEvent ase -> handleActivityStart(ase);
			case ActivityEndEvent aee -> handleActivityEnd(aee);
			case PersonStuckEvent _ -> handleStuck();
			default -> dispatchToBuilders(e);
		}
	}

	private void handlePersonDeparture(PersonDepartureEvent e) {
		if (currentLeg != null)
			throw new IllegalStateException("Agent already performs a leg.");

		var provider = providers.get(e.getLegMode());

		if (provider == null) {
			throw new IllegalStateException("No route builder registered for mode " + e.getLegMode() +
				" an implementation of ExperiencedRouteBuilder interface must be bound for each mode in the simulation.");
		}

		var routeBuilder = provider.get();
		currentLeg = new ExperiencedLegBuilder(routeBuilder);
		currentLeg.handleEvent(e);
	}

	private void handlePersonArrival(PersonArrivalEvent e) {
		if (currentLeg == null) {
			throw new IllegalStateException("Agent arrives but leg was not started.");
		}

		currentLeg.handleEvent(e);
		var leg = currentLeg.finishLeg();
		experiencedPlan.addLeg(leg);
		currentLeg = null;
	}

	void handleActivityStart(ActivityStartEvent e) {
		if (currentActivity != null) throw new IllegalStateException("Agent already performs an activity.");
		currentActivity = new ExperiencedActivityBuilder();
		currentActivity.handleEvent(e);
	}

	void handleActivityEnd(ActivityEndEvent e) {
		if (currentActivity == null) {
			currentActivity = new ExperiencedActivityBuilder();
		}
		currentActivity.handleEvent(e);
		var act = currentActivity.finishActivity();
		experiencedPlan.addActivity(act);
		currentActivity = null;
	}

	void handleStuck() {
		// discard what is currently going on, so that we don't put
		// unfinished plan elements into the experienced plan.
		this.currentLeg = null;
		this.currentActivity = null;
	}

	void dispatchToBuilders(Event e) {
		if (currentLeg != null)
			currentLeg.handleEvent(e);
		if (currentActivity != null)
			currentActivity.handleEvent(e);
	}

	void finishLegAndActivity() {
		if (currentLeg != null) {
			var leg = currentLeg.finishLeg();
			experiencedPlan.addLeg(leg);
			currentLeg = null;
		}
		if (currentActivity != null) {
			var act = currentActivity.finishActivity();
			experiencedPlan.addActivity(act);
			currentActivity = null;
		}
	}

	Plan finishPlan() {
		finishLegAndActivity();
		return experiencedPlan;
	}

	Id<Vehicle> getCurrentVehicle() {
		return currentLeg.getCurrentVehicleId();
	}

	boolean isInVehicle() {
		return currentLeg != null && currentLeg.getCurrentVehicleId() != null;
	}

	Msg toMessage() {
		var actMsg = currentActivity == null ? null : currentActivity.toMessage();
		var legMsg = currentLeg == null ? null : currentLeg.toMessage();
		return new Msg(experiencedPlan, actMsg, legMsg);
	}

	record Msg(Plan experiencedPlan, ExperiencedActivityBuilder.Msg actState,
			   ExperiencedLegBuilder.Msg legState) implements Message {
	}
}
