package org.matsim.contrib.ev.strategic.replanning.innovator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.matsim.contrib.ev.charging.BatteryCharging;
import org.matsim.contrib.ev.infrastructure.ChargerSpecification;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructureSpecification;
import org.matsim.contrib.ev.strategic.plan.ChargingPlan;
import org.matsim.contrib.ev.strategic.plan.ChargingPlanActivity;

public class ChargingPlanCalculator {
    private final ChargingInfrastructureSpecification chargingInfrastructureSpecification;
    private final BatteryCharging batteryCharging;

    private final double simulationEndTime;

    private final List<Double> consumedEnergy; // at the end of the activity
    private final List<Double> startTimes;
    private final List<Double> endTimes;

    private final double maximumEnergy;

    public ChargingPlanCalculator(ChargingInfrastructureSpecification chargingInfrastructureSpecification,
            BatteryCharging batteryCharging, double simulationEndTime, double maximumEnergy,
            List<Double> consumedEnergy, List<Double> startTimes, List<Double> endTimes) {
        this.consumedEnergy = consumedEnergy;
        this.startTimes = startTimes;
        this.endTimes = endTimes;
        this.simulationEndTime = simulationEndTime;
        this.maximumEnergy = maximumEnergy;
        this.batteryCharging = batteryCharging;
        this.chargingInfrastructureSpecification = chargingInfrastructureSpecification;
    }

    /**
     * Calculates how much energy can be theoretically (without taking into account
     * battery capacity) can been charged during each activity. Leg-charging is
     * added to the following activity.
     */
    public List<Double> calculateChargeableEnergy(ChargingPlan chargingPlan) {
        // amount that can be charged at the end of every activity
        List<Double> chargeableEnergy = new ArrayList<>(Collections.nCopies(consumedEnergy.size(), 0.0));

        for (ChargingPlanActivity chargingActivity : chargingPlan.getChargingActivities()) {
            ChargerSpecification charger = chargingInfrastructureSpecification.getChargerSpecifications()
                    .get(chargingActivity.getChargerId());

            if (!chargingActivity.isEnroute()) { // activity-based charging
                int startActivityIndex = chargingActivity.getStartActivityIndex();
                int endActivityIndex = chargingActivity.getEndActivityIndex();

                double startTime = startTimes.get(startActivityIndex);
                double endTime = endTimes.get(endActivityIndex);

                // limit to evaluation period
                startTime = Math.min(startTime, simulationEndTime);
                endTime = Math.min(endTime, simulationEndTime);

                double chargingDuration = endTime - startTime;

                // what can be charged in that duration
                double energy = batteryCharging.calcEnergyCharged(charger, chargingDuration);

                chargeableEnergy.set(endActivityIndex, chargeableEnergy.get(endActivityIndex) + energy);
            } else {
                int followingActivityIndex = chargingActivity.getFollowingActivityIndex();
                double chargingDuration = chargingActivity.getDuration();

                if (endTimes.get(followingActivityIndex - 1) >= simulationEndTime) {
                    chargingDuration = 0.0;
                }

                // what can be charged in that duration
                double energy = batteryCharging.calcEnergyCharged(charger, chargingDuration);

                // We assume the worst case where the agent only charges at the end of the trip,
                // i.e. right before the following activity. This means that the charged energy
                // has an effect right after the following activity.

                chargeableEnergy.set(followingActivityIndex, chargeableEnergy.get(followingActivityIndex) + energy);
            }
        }

        return chargeableEnergy;
    }

    /**
     * Given the initial energy and the chargeable energy, we
     * calculate the energy state at the beginning and at the end of each activity.
     */
    public List<Double> calculateEnergyState(double initialEnergy,
            List<Double> chargeableEnergy) {
        // energy level at the end of activity
        List<Double> state = new ArrayList<>(consumedEnergy.size() * 2 + 1);

        double energy = initialEnergy;
        state.add(initialEnergy);

        for (int k = 0; k < consumedEnergy.size(); k++) {
            energy += chargeableEnergy.get(k);
            energy = Math.min(energy, maximumEnergy);
            state.add(energy); // after the activity (potential charging here)

            energy -= consumedEnergy.get(k);
            energy = Math.max(energy, 0.0); // cannot go negative
            state.add(energy); // after the activity and potential charging (= after driving)
        }

        return state;
    }

    public boolean isFeasible(double initialEnergy, List<Double> chargeableEnergy, double minimumEnergy,
            double minimumEndEnergy) {
        List<Double> energyState = calculateEnergyState(initialEnergy, chargeableEnergy);

        // end soc feasibility
        if (energyState.getLast() < minimumEndEnergy) {
            return false;
        }

        // zero soc feasibility
        for (double value : energyState) {
            if (value == 0.0) {
                return false;
            }
        }

        // minimum soc feasibility
        for (double value : energyState) {
            if (value < minimumEnergy) {
                return false;
            }
        }

        return true;
    }

    public Optional<Double> calculateMinimumFeasibleInitialEnergy(List<Double> chargeableEnergy, double minimumEnergy,
            double minimumEndEnergy) {
        double initialEnergy = 0.0;

        boolean isDone = false;
        boolean isFeasible = false;

        double lowestEnergy = Double.NaN;
        double endEnergy = Double.NaN;

        double previousMissingEnergy = Double.NaN;

        while (!isDone) {
            List<Double> state = calculateEnergyState(initialEnergy, chargeableEnergy);

            lowestEnergy = state.stream().mapToDouble(d -> d).min().getAsDouble();
            endEnergy = state.getLast();

            // we want to exceed the minimum energy at every point along the plan
            double missingEnergy = Math.max(0.0, minimumEnergy - lowestEnergy);

            double missingEndEnergy = Math.max(0.0, minimumEndEnergy - endEnergy);
            missingEnergy = Math.max(missingEnergy, missingEndEnergy);

            if (initialEnergy + missingEnergy > maximumEnergy) {
                // cannot update since otherwise we need to increase battery capacity
                isDone = true;
            } else if (missingEnergy == previousMissingEnergy) {
                // soc drops below zero along the way, but independent of initial soc
                isDone = true;
            } else if (missingEnergy > 0.0) {
                // updating initial energy and try next round
                initialEnergy += missingEnergy;
            } else {
                // we found a feasible configuration
                isDone = true;
                isFeasible = true;
            }

            previousMissingEnergy = missingEnergy;
        }

        return isFeasible ? Optional.of(initialEnergy) : Optional.empty();
    }
}
