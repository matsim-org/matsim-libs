package org.matsim.contrib.ev.strategic.reservation;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.ev.fleet.ElectricFleet;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.contrib.ev.infrastructure.ChargerSpecification;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructureSpecification;
import org.matsim.contrib.ev.reservation.ChargerReservationManager;
import org.matsim.contrib.ev.strategic.plan.ChargingPlan;
import org.matsim.contrib.ev.strategic.plan.ChargingPlanActivity;
import org.matsim.contrib.ev.strategic.plan.ChargingPlans;
import org.matsim.contrib.ev.withinday.WithinDayEvEngine;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.core.utils.timing.TimeTracker;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;

public class StrategicChargingReservationEngine implements MobsimEngine {
    static public final String PERSON_ATTRIBUTE = "sevc:reservationSlack";

    private final Population population;

    private final ChargerReservationManager manager;
    private final ChargingInfrastructureSpecification infrastructure;
    private final TimeInterpretation timeInterpretation;
    private final ElectricFleet electricFleet;
    private final EventsManager eventsManager;

    private final String chargingMode;

    private record AdvanceReservation(double reservationTime, Person person, ChargerSpecification charger,
            ElectricVehicle vehicle,
            double startTime, double endTime) {
    }

    private final PriorityQueue<AdvanceReservation> queue = new PriorityQueue<>(
            Comparator.comparing(AdvanceReservation::reservationTime));

    public StrategicChargingReservationEngine(Population population, ChargerReservationManager manager,
            ChargingInfrastructureSpecification infrastructure, TimeInterpretation timeInterpretation,
            ElectricFleet electricFleet, String chargingMode, EventsManager eventsManager) {
        this.population = population;
        this.manager = manager;
        this.electricFleet = electricFleet;
        this.chargingMode = chargingMode;
        this.infrastructure = infrastructure;
        this.timeInterpretation = timeInterpretation;
        this.eventsManager = eventsManager;
    }

    @Override
    public void doSimStep(double time) {
        while (queue.size() > 0 && queue.peek().reservationTime <= time) {
            AdvanceReservation advance = queue.poll();

            var reservation = manager.addReservation(advance.charger, advance.vehicle, advance.startTime,
                    advance.endTime);

            // TODO: What if the reservation is unsuccessful? Here we could implement some
            // fallback strategy: Choose another charger

            boolean successful = reservation != null;
            eventsManager.processEvent(new AdvanceReservationEvent(time, advance.person.getId(),
                    advance.vehicle.getId(), advance.charger.getId(), advance.startTime, advance.endTime, successful));
        }
    }

    @Override
    public void onPrepareSim() {
        for (Person person : population.getPersons().values()) {
            if (!WithinDayEvEngine.isActive(person)) {
                continue; // not relevant
            }

            Double reservationSlack = getReservationSlack(person);

            if (reservationSlack == null) {
                continue; // no slack defined
            }

            ChargingPlan plan = ChargingPlans.get(person.getSelectedPlan()).getSelectedPlan();

            if (plan == null) {
                continue; // no charging plan yet
            }

            Id<Vehicle> vehicleId = VehicleUtils.getVehicleId(person, chargingMode);
            ElectricVehicle vehicle = electricFleet.getElectricVehicles().get(vehicleId);

            List<Double> startTimes = new LinkedList<>();
            List<Double> endTimes = new LinkedList<>();

            TimeTracker timeTracker = new TimeTracker(timeInterpretation);

            for (PlanElement element : person.getSelectedPlan().getPlanElements()) {
                double startTime = timeTracker.getTime().seconds();
                timeTracker.addElement(element);
                double endTime = timeTracker.getTime().orElse(Double.POSITIVE_INFINITY);

                if (element instanceof Activity activity) {
                    if (TripStructureUtils.isStageActivityType(activity.getType())) {
                        startTimes.add(startTime);
                        endTimes.add(endTime);
                    }
                }
            }

            for (ChargingPlanActivity activity : plan.getChargingActivities()) {
                if (!activity.isReserved()) {
                    continue;
                }

                if (activity.isEnroute()) {
                    // TODO: This is relatively imprecise, but not sure we can do better in the
                    // beginning of the day.
                    double startTime = endTimes.get(activity.getFollowingActivityIndex() - 1);
                    double endTime = startTime + activity.getDuration();

                    double reservationTime = startTime - reservationSlack;
                    ChargerSpecification charger = infrastructure.getChargerSpecifications()
                            .get(activity.getChargerId());

                    queue.add(new AdvanceReservation(reservationTime, person, charger, vehicle, startTime, endTime));
                } else {
                    double startTime = startTimes.get(activity.getStartActivityIndex());
                    double endTime = endTimes.get(activity.getEndActivityIndex());

                    double reservationTime = startTime - reservationSlack;
                    ChargerSpecification charger = infrastructure.getChargerSpecifications()
                            .get(activity.getChargerId());

                    queue.add(new AdvanceReservation(reservationTime, person, charger, vehicle, startTime, endTime));
                }
            }
        }
    }

    @Override
    public void afterSim() {
    }

    @Override
    public void setInternalInterface(InternalInterface internalInterface) {
    }

    static public void setReservationSlack(Person person, double slack) {
        person.getAttributes().putAttribute(PERSON_ATTRIBUTE, slack);
    }

    static public Double getReservationSlack(Person person) {
        return (Double) person.getAttributes().getAttribute(PERSON_ATTRIBUTE);
    }
}
