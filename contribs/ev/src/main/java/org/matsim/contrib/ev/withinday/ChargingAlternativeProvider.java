package org.matsim.contrib.ev.withinday;

import jakarta.annotation.Nullable;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.ev.fleet.ElectricVehicle;

import java.util.List;

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

	@Nullable
	default ChargingAlternative queryPendingAlternative(Id<ChargingAlternative> id) {
		throw new UnsupportedOperationException("async query pending is not yet implemented for " + this.getClass().getName());
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

		@Override
		public @org.jspecify.annotations.Nullable ChargingAlternative queryPendingAlternative(Id<ChargingAlternative> id) {
			return null;
		}
	};
}
