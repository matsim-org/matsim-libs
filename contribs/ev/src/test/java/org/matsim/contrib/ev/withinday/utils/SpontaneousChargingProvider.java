package org.matsim.contrib.ev.withinday.utils;

import java.util.List;

import javax.annotation.Nullable;

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
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.router.TripStructureUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class SpontaneousChargingProvider implements ChargingAlternativeProvider {
    @Inject
    ChargingInfrastructure infrastructure;

    @Inject
    QSim qsim;

    private boolean hasCharged = false;

    @Override
    public ChargingAlternative findEnrouteAlternative(double now, Person person, Plan plan,
            ElectricVehicle vehicle,
            @Nullable ChargingSlot initialSlot) {
        if (hasCharged) {
            return null;
        }

        MobsimAgent agent = qsim.getAgents().get(person.getId());
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
            return new ChargingAlternative(charger, 3600.0);
        }

        return null;
    }

    @Override
    @Nullable
    public ChargingAlternative findAlternative(double now, Person person, Plan plan, ElectricVehicle vehicle,
            ChargingSlot slot, List<ChargingAlternative> trace) {
        return null;
    }
}
