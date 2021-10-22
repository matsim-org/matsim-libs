package org.matsim.contrib.drt.extension.eshifts.charging;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.function.Function;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.ev.EvConfigGroup;
import org.matsim.contrib.ev.charging.ChargingEndEvent;
import org.matsim.contrib.ev.charging.ChargingListener;
import org.matsim.contrib.ev.charging.ChargingStartEvent;
import org.matsim.contrib.ev.charging.ChargingStrategy;
import org.matsim.contrib.ev.charging.ChargingWithAssignmentLogic;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.contrib.ev.infrastructure.ChargerSpecification;
import org.matsim.contrib.drt.extension.shifts.schedule.OperationalStop;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * @author nkuehnel / MOIA
 */
public class ChargingWithBreaksAndAssignmentLogic implements ChargingWithAssignmentLogic {

    private Fleet fleet;

    private final ChargerSpecification charger;
    private final ChargingStrategy chargingStrategy;
    private final EventsManager eventsManager;

    private final Map<Id<ElectricVehicle>, ElectricVehicle> assignedVehicles = new LinkedHashMap<>();

    private final Map<Id<ElectricVehicle>, ElectricVehicle> pluggedVehicles = new LinkedHashMap<>();
    private final Queue<ElectricVehicle> queuedVehicles = new LinkedList<>();
    private final Map<Id<ElectricVehicle>, ChargingListener> listeners = new LinkedHashMap<>();

    private EvConfigGroup evConfig;

    public ChargingWithBreaksAndAssignmentLogic(ChargerSpecification charger, ChargingStrategy chargingStrategy,
                                                EventsManager eventsManager, EvConfigGroup evConfig) {
        this.chargingStrategy = Objects.requireNonNull(chargingStrategy);
        this.charger = Objects.requireNonNull(charger);
        this.eventsManager = Objects.requireNonNull(eventsManager);
        this.evConfig = evConfig;
    }

    public void resetFleet(Fleet fleet) {
        this.fleet = fleet;
    }

    @Override
    public void assignVehicle(ElectricVehicle ev) {
        if (assignedVehicles.put(ev.getId(), ev) != null) {
            throw new IllegalArgumentException("Vehicle is already assigned: " + ev.getId());
        }
    }

    @Override
    public void unassignVehicle(ElectricVehicle ev) {
        if (assignedVehicles.remove(ev.getId()) == null) {
            throw new IllegalArgumentException("Vehicle was not assigned: " + ev.getId());
        }
    }

    private final Collection<ElectricVehicle> unmodifiableAssignedVehicles = Collections.unmodifiableCollection(
            assignedVehicles.values());

    @Override
    public Collection<ElectricVehicle> getAssignedVehicles() {
        return unmodifiableAssignedVehicles;
    }

    public static class FactoryProvider implements Provider<Factory> {
        @Inject
        private EventsManager eventsManager;
        @Inject
        private Config config;

        private final Function<ChargerSpecification, ChargingStrategy> chargingStrategyCreator;

        public FactoryProvider(Function<ChargerSpecification, ChargingStrategy> chargingStrategyCreator) {
            this.chargingStrategyCreator = chargingStrategyCreator;
        }

        @Override
        public Factory get() {
            return charger -> new ChargingWithBreaksAndAssignmentLogic(charger,
                    chargingStrategyCreator.apply(charger), eventsManager, ConfigUtils.addOrGetModule(config, EvConfigGroup.class));
        }
    }

    @Override
    public void chargeVehicles(double chargePeriod, double now) {
        Iterator<ElectricVehicle> evIter = pluggedVehicles.values().iterator();
        while (evIter.hasNext()) {
            ElectricVehicle ev = evIter.next();
            // with fast charging, we charge around 4% of SOC per minute,
            // so when updating SOC every 10 seconds, SOC increases by less then 1%
            ev.getBattery().changeSoc(ev.getChargingPower().calcChargingPower(charger) * chargePeriod);

            final DvrpVehicle dvrpVehicle = fleet.getVehicles().get(Id.create(ev.getId(), DvrpVehicle.class));
            final Task currentTask = dvrpVehicle.getSchedule().getCurrentTask();
            if (currentTask instanceof OperationalStop) {
                if (now >= currentTask.getEndTime() + evConfig.getChargeTimeStep() ||
						chargingStrategy.isChargingCompleted(ev)) {
                    evIter.remove();
					eventsManager.processEvent(
							new ChargingEndEvent(now, charger.getId(), ev.getId(), ev.getBattery().getSoc()));
                    listeners.remove(ev.getId()).notifyChargingEnded(ev, now);
                }
            } else {
                if (chargingStrategy.isChargingCompleted(ev)) {
                    evIter.remove();
					eventsManager.processEvent(
							new ChargingEndEvent(now, charger.getId(), ev.getId(), ev.getBattery().getSoc()));
                    listeners.remove(ev.getId()).notifyChargingEnded(ev, now);
                }
            }
        }

        int queuedToPluggedCount = Math.min(queuedVehicles.size(), charger.getPlugCount() - pluggedVehicles.size());
        for (int i = 0; i < queuedToPluggedCount; i++) {
            plugVehicle(queuedVehicles.poll(), now);
        }

    }

    @Override
    public void addVehicle(ElectricVehicle ev, double now) {
        addVehicle(ev, new ChargingListener() {
        }, now);
    }

    @Override
    public void addVehicle(ElectricVehicle ev, ChargingListener chargingListener, double now) {
        listeners.put(ev.getId(), chargingListener);
        if (pluggedVehicles.size() < charger.getPlugCount()) {
            plugVehicle(ev, now);
        } else {
            queueVehicle(ev, now);
        }
    }

    @Override
    public void removeVehicle(ElectricVehicle ev, double now) {
        if (pluggedVehicles.remove(ev.getId()) != null) {// successfully removed
			eventsManager.processEvent(
					new ChargingEndEvent(now, charger.getId(), ev.getId(), ev.getBattery().getSoc()));
            listeners.remove(ev.getId()).notifyChargingEnded(ev, now);

            if (!queuedVehicles.isEmpty()) {
                plugVehicle(queuedVehicles.poll(), now);
            }
        } else if (queuedVehicles.remove(ev)) {//
        } else {// neither plugged nor queued
            throw new IllegalArgumentException(
                    "Vehicle: " + ev.getId() + " is neither queued nor plugged at charger: " + charger.getId());
        }
    }

    private void queueVehicle(ElectricVehicle ev, double now) {
        queuedVehicles.add(ev);
        listeners.get(ev.getId()).notifyVehicleQueued(ev, now);
    }

    private void plugVehicle(ElectricVehicle ev, double now) {
        if (pluggedVehicles.put(ev.getId(), ev) != null) {
            throw new IllegalArgumentException();
        }
		eventsManager.processEvent(new ChargingStartEvent(now, charger.getId(), ev.getId(), charger.getChargerType(),
				ev.getBattery().getSoc()));
        listeners.get(ev.getId()).notifyChargingStarted(ev, now);
    }

    private final Collection<ElectricVehicle> unmodifiablePluggedVehicles = Collections.unmodifiableCollection(
            pluggedVehicles.values());

    @Override
    public Collection<ElectricVehicle> getPluggedVehicles() {
        return unmodifiablePluggedVehicles;
    }

    private final Collection<ElectricVehicle> unmodifiableQueuedVehicles = Collections.unmodifiableCollection(
            queuedVehicles);

    @Override
    public Collection<ElectricVehicle> getQueuedVehicles() {
        return unmodifiableQueuedVehicles;
    }

    @Override
    public ChargingStrategy getChargingStrategy() {
        return chargingStrategy;
    }
}
