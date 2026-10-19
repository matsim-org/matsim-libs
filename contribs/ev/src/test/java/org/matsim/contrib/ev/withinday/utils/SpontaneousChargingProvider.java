package org.matsim.contrib.ev.withinday.utils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import jakarta.annotation.Nullable;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructure;
import org.matsim.contrib.ev.withinday.ChargingAlternative;
import org.matsim.contrib.ev.withinday.ChargingAlternativeProvider;
import org.matsim.contrib.ev.withinday.ChargingSlot;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.router.TripStructureUtils;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

@Singleton
public class SpontaneousChargingProvider implements ChargingAlternativeProvider {
	@Inject
	ChargingInfrastructure infrastructure;

	@Inject
	Netsim netsim;

	private boolean hasCharged = false;

	@Override
	public ChargingAlternative findEnrouteAlternative(double now, Person person, Plan plan,
	                                                  ElectricVehicle vehicle,
	                                                  @Nullable ChargingSlot initialSlot) {
		if (hasCharged) {
			return null;
		}

		MobsimAgent agent = netsim.getAgents().get(person.getId());
		int currentIndex = WithinDayAgentUtils.getCurrentPlanElementIndex(agent);

		Activity nextActivity = null;
		for (int index = currentIndex; index < plan.getPlanElements().size(); index++) {
			PlanElement element = plan.getPlanElements().get(index);

			if (element instanceof Activity activity) {
				if (!TripStructureUtils.isStageActivityType(activity.getType())) {
					nextActivity = activity;
					break;
				}
			}
		}

		if (nextActivity != null && nextActivity.getType().equals("work")) {
			Charger charger = infrastructure.getChargers().values().iterator().next();
			hasCharged = true;
			return new ChargingAlternative(charger.getId(), 3600.0);
		}

		return null;
	}

	@Override
	public void findEnrouteAlternativeAsync(double now, PlanAgent agent, ElectricVehicle vehicle,
	                                        @Nullable ChargingSlot slot, Consumer<Optional<ChargingAlternative>> callback) {
		// we cannot use hasCharged, as we might have multiple providers across partitions
		// Since this is a test provider anyway, we use this simple condition below.
		if (now > 40000) {
			callback.accept(Optional.empty());
			return;
		}

		var currentPlanElement = agent.getCurrentPlanElement();
		var nextActivity = agent.getCurrentPlan().getPlanElements().stream()
			.dropWhile(e -> !e.equals(currentPlanElement))
			.filter(e -> e instanceof Activity)
			.map(e -> (Activity) e)
			.filter(a -> !TripStructureUtils.isStageActivityType(a.getType()))
			.findFirst();

		if (nextActivity.isPresent() && nextActivity.get().getType().equals("work")) {
			var c = infrastructure.getChargers().values().iterator().next();
			var alternative = new ChargingAlternative(c.getId(), 3600.0);
			callback.accept(Optional.of(alternative));
		} else {
			callback.accept(Optional.empty());
		}
	}

	@Override
	@Nullable
	public ChargingAlternative findAlternative(double now, Person person, Plan plan, ElectricVehicle vehicle,
	                                           ChargingSlot slot, List<ChargingAlternative> trace) {
		return null;
	}
}
