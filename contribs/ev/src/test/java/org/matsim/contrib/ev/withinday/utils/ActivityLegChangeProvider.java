package org.matsim.contrib.ev.withinday.utils;

import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructure;
import org.matsim.contrib.ev.withinday.ChargingAlternative;
import org.matsim.contrib.ev.withinday.ChargingAlternativeProvider;
import org.matsim.contrib.ev.withinday.ChargingSlot;
import org.matsim.contrib.ev.withinday.ChargingSlotProvider;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class ActivityLegChangeProvider implements ChargingAlternativeProvider, ChargingSlotProvider {
    private boolean useFirstActivityBased;
    private ChargingInfrastructure infrastructure;

    private Activity chargingActivity = null;
    private Leg chargingLeg = null;

    ActivityLegChangeProvider(ChargingInfrastructure infrasturcutre, boolean useFirstActivityBased) {
        this.infrastructure = infrasturcutre;
        this.useFirstActivityBased = useFirstActivityBased;
    }

    private void prepare(Plan plan) {
        for (PlanElement element : plan.getPlanElements()) {
            if (element instanceof Activity activity && activity.getType().equals("work")) {
                chargingActivity = activity;
                break;
            }
        }

        for (Trip trip : TripStructureUtils.getTrips(plan)) {
            if (trip.getDestinationActivity().getType().equals("work")) {
                for (Leg leg : trip.getLegsOnly()) {
                    if (leg.getMode().equals("car")) {
                        chargingLeg = leg;
                        break;
                    }
                }
            }
        }
    }

    @Override
    public List<ChargingSlot> findSlots(Person person, Plan plan, ElectricVehicle vehicle) {
        Charger charger = infrastructure.getChargers().get(Id.create("charger1", Charger.class));
        prepare(plan);

        if (useFirstActivityBased) {
            return Collections.singletonList(new ChargingSlot(chargingActivity, chargingActivity, null, 0, charger));
        } else {
            return Collections.singletonList(new ChargingSlot(null, null, chargingLeg, 3600.0, charger));
        }
    }

    @Override
    @Nullable
    public ChargingAlternative findEnrouteAlternative(double now, Person person, Plan plan,
            ElectricVehicle vehicle,
            @Nullable ChargingSlot initialSlot) {
        Charger charger = infrastructure.getChargers().get(Id.create("charger2", Charger.class));
        prepare(plan);

        if (useFirstActivityBased) {
            // now switch to leg-based
            return new ChargingAlternative(charger, 3600.0);
        } else {
            // now switch to activity-based
            return new ChargingAlternative(charger);
        }
    }

    @Override
    @Nullable
    public ChargingAlternative findAlternative(double now, Person person, Plan plan, ElectricVehicle vehicle,
            @Nullable ChargingSlot slot, List<ChargingAlternative> trace) {
        return null;
    }

    static public Provider<ActivityLegChangeProvider> createProvider(boolean useFirstActivityBased) {
        return new Provider<>() {
            @Inject
            ChargingInfrastructure infrastructure;

            @Override
            public ActivityLegChangeProvider get() {
                return new ActivityLegChangeProvider(infrastructure, useFirstActivityBased);
            }
        };
    }
}
