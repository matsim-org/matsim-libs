package org.matsim.contrib.ev.withinday;

import jakarta.annotation.Nullable;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.core.mobsim.framework.PlanAgent;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * This interface provides alternative charging configurations online during the
 * day, for instance, if the initial planned charger is occupied. In most cases,
 * this interface is used to provide an alternative charger to the agent.
 *
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public interface ChargingAlternativeProvider {
	@Nullable
	ChargingAlternative findAlternative(double now, Person person, Plan plan, ElectricVehicle vehicle,
	                                    ChargingSlot slot, List<ChargingAlternative> trace);

	@Nullable
	ChargingAlternative findEnrouteAlternative(double now, Person person, Plan plan, ElectricVehicle vehicle,
	                                           @Nullable ChargingSlot slot);

	default void findEnrouteAlternativeAsync(double now, PlanAgent agent, ElectricVehicle vehicle,
	                                         @Nullable ChargingSlot slot, Consumer<Optional<ChargingAlternative>> callback) {
		var plan = agent.getCurrentPlan();
		var person = plan.getPerson();
		var result = findEnrouteAlternative(now, person, plan, vehicle, slot);
		callback.accept(Optional.ofNullable(result));
	}

	default void findAlternativeAsync(double now, PlanAgent agent, ElectricVehicle vehicle, ChargingSlot slot, List<ChargingAlternative> trace,
	                                  Consumer<Optional<ChargingAlternative>> callback) {
		var plan = agent.getCurrentPlan();
		var person = plan.getPerson();
		var result = findAlternative(now, person, plan, vehicle, slot, trace);
		callback.accept(Optional.ofNullable(result));
	}

	static public final ChargingAlternativeProvider NOOP = new ChargingAlternativeProvider() {
		@Override
		@Nullable
		public ChargingAlternative findAlternative(double now, Person person, Plan plan, ElectricVehicle vehicle,
		                                           ChargingSlot slot, List<ChargingAlternative> trace) {
			return null;
		}

		@Override
		@Nullable
		public ChargingAlternative findEnrouteAlternative(double now, Person person, Plan plan, ElectricVehicle vehicle,
		                                                  @Nullable ChargingSlot slot) {
			return null;
		}
	};
}
