package org.matsim.contrib.ev.strategic;

import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.ev.charging.BatteryCharging;
import org.matsim.contrib.ev.discharging.DriveEnergyConsumption;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.contrib.ev.infrastructure.ChargerSpecification;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructure;
import org.matsim.contrib.ev.strategic.infrastructure.ChargerProvider;
import org.matsim.contrib.ev.strategic.infrastructure.ChargerProvider.ChargerRequest;
import org.matsim.contrib.ev.withinday.ChargingAlternative;
import org.matsim.contrib.ev.withinday.ChargingAlternativeProvider;
import org.matsim.contrib.ev.withinday.ChargingSlot;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

import com.google.common.base.Preconditions;

/**
 * This class is a special case that is implemented optionally in the
 * alternative charger search startegy in strategic electric vehicle charging
 * (SEVC). In particular, it treats the case in which an agent, for every leg
 * using the car, checks whether he might want to charge along the way. We apply
 * a sophisticated logic in which energy consumption along the trip is
 * calculated and the agent tries to charge once it gets clear that a
 * configurable "critical SoC" will be reached. To find a charger, the
 * ChargingProvider logic is used.
 * 
 * The critical SoC for an agent must be defined as an agent attribute.
 * 
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class CriticalAlternativeProvider implements ChargingAlternativeProvider {
    static public final String CRITICAL_SOC_PERSON_ATTRIBUTE = "sevc:criticalSoc";

    private final QSim qsim;
    private final Network network;
    private final TravelTime travelTime;
    private final ChargerProvider chargerProvider;
    private final ChargingInfrastructure infrastructure;
    private final double minimumDuration;

    public CriticalAlternativeProvider(QSim qsim, Network network, TravelTime travelTime,
            ChargerProvider chargerProvider, ChargingInfrastructure infrastructure,
            StrategicChargingConfigGroup config) {
        this.qsim = qsim;
        this.network = network;
        this.travelTime = travelTime;
        this.chargerProvider = chargerProvider;
        this.infrastructure = infrastructure;
        this.minimumDuration = config.minimumEnrouteChargingDuration;
    }

    @Override
    @Nullable
    public ChargingAlternative findAlternative(double now, Person person, Plan plan, ElectricVehicle vehicle,
            ChargingSlot slot, List<ChargingAlternative> trace) {
        throw new UnsupportedOperationException();
    }

    @Override
    @Nullable
    public ChargingAlternative findEnrouteAlternative(double now, Person person, Plan plan,
            ElectricVehicle electricVehicle,
            @Nullable ChargingSlot slot) {
        Preconditions.checkArgument(slot == null);

        Double criticalSoc = getCriticalSoc(person);
        if (criticalSoc != null) {
            // we must be on a leg when finding an enroute alternative
            Leg leg = (Leg) WithinDayAgentUtils.getCurrentPlanElement(qsim.getAgents().get(person.getId()));

            double initialCharge = electricVehicle.getBattery().getCharge();
            double targetCharge = criticalSoc * electricVehicle.getBattery().getCapacity();

            // a priori no additional charge needed right now if zero
            double deltaCharge = Math.max(0.0, targetCharge - initialCharge);

            if (deltaCharge == 0.0) {
                // Can we obtain the vehicle parameter from somewhere here? For time
                // calculation?
                double consumption = calculateConsumption(now, person, leg, electricVehicle, null);
                double finalCharge = initialCharge - consumption;

                // maybe not enough at the end
                deltaCharge = Math.max(0.0, targetCharge - finalCharge);
            }

            if (deltaCharge > 0.0) {
                // we need to charge, otherwise we don't get to the next activity
                BatteryCharging calculator = (BatteryCharging) electricVehicle.getChargingPower();

                Collection<ChargerSpecification> chargers = chargerProvider.findChargers(person, plan,
                        new ChargerRequest(leg));

                if (chargers.size() > 0) {
                    final double fixedDeltaCharge = deltaCharge;

                    ChargerSpecification selected = chargers.stream().sorted((a, b) -> {
                        double durationA = calculator.calcChargingTime(a, fixedDeltaCharge);
                        double durationB = calculator.calcChargingTime(b, fixedDeltaCharge);
                        return Double.compare(durationA, durationB);
                    }).findFirst().get();

                    double duration = calculator.calcChargingTime(selected, fixedDeltaCharge);
                    duration = Math.max(minimumDuration, duration);

                    return new ChargingAlternative(infrastructure.getChargers().get(selected.getId()), duration);
                }
            }
        }

        return null;
    }

    private double calculateConsumption(double now, Person person, Leg leg, ElectricVehicle electricVehicle,
            Vehicle vehicle) {
        double consumption = 0.0;

        DriveEnergyConsumption calculator = electricVehicle.getDriveEnergyConsumption();
        for (Id<Link> linkId : ((NetworkRoute) leg.getRoute()).getLinkIds()) {
            Link link = network.getLinks().get(linkId);
            double linkTravelTime = travelTime.getLinkTravelTime(link, now, person, vehicle);

            consumption += calculator.calcEnergyConsumption(link, linkTravelTime, now);
            now += linkTravelTime;
        }

        return consumption;
    }

    /**
     * Sets the critical SoC for an agent.
     */
    static public void setCriticalSoC(Person person, double criticalSoc) {
        person.getAttributes().putAttribute(CRITICAL_SOC_PERSON_ATTRIBUTE, criticalSoc);
    }

    /**
     * Retrieves the critical SoC from an agent.
     */
    static public Double getCriticalSoc(Person person) {
        return (Double) person.getAttributes().getAttribute(CRITICAL_SOC_PERSON_ATTRIBUTE);
    }
}
