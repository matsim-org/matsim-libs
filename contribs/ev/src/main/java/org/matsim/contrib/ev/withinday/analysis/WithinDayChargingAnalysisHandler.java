package org.matsim.contrib.ev.withinday.analysis;

import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.ev.EvUnits;
import org.matsim.contrib.ev.charging.ChargingEndEvent;
import org.matsim.contrib.ev.charging.ChargingEndEventHandler;
import org.matsim.contrib.ev.charging.ChargingStartEvent;
import org.matsim.contrib.ev.charging.ChargingStartEventHandler;
import org.matsim.contrib.ev.charging.EnergyChargedEvent;
import org.matsim.contrib.ev.charging.EnergyChargedEventHandler;
import org.matsim.contrib.ev.charging.QueuedAtChargerEvent;
import org.matsim.contrib.ev.charging.QueuedAtChargerEventHandler;
import org.matsim.contrib.ev.charging.QuitQueueAtChargerEvent;
import org.matsim.contrib.ev.charging.QuitQueueAtChargerEventHandler;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.contrib.ev.withinday.events.AbortChargingAttemptEvent;
import org.matsim.contrib.ev.withinday.events.AbortChargingAttemptEventHandler;
import org.matsim.contrib.ev.withinday.events.AbortChargingProcessEvent;
import org.matsim.contrib.ev.withinday.events.AbortChargingProcessEventHandler;
import org.matsim.contrib.ev.withinday.events.FinishChargingAttemptEvent;
import org.matsim.contrib.ev.withinday.events.FinishChargingAttemptEventHandler;
import org.matsim.contrib.ev.withinday.events.FinishChargingProcessEvent;
import org.matsim.contrib.ev.withinday.events.FinishChargingProcessEventHandler;
import org.matsim.contrib.ev.withinday.events.StartChargingAttemptEvent;
import org.matsim.contrib.ev.withinday.events.StartChargingAttemptEventHandler;
import org.matsim.contrib.ev.withinday.events.StartChargingProcessEvent;
import org.matsim.contrib.ev.withinday.events.StartChargingProcessEventHandler;
import org.matsim.contrib.ev.withinday.events.UpdateChargingAttemptEvent;
import org.matsim.contrib.ev.withinday.events.UpdateChargingAttemptEventHandler;
import org.matsim.vehicles.Vehicle;

import com.google.common.base.Preconditions;

/**
 * Tracks detailed information on the electric vehilce charging processes and
 * attempts.
 * 
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class WithinDayChargingAnalysisHandler implements //
		StartChargingProcessEventHandler,
		AbortChargingProcessEventHandler, FinishChargingProcessEventHandler, //
		StartChargingAttemptEventHandler, UpdateChargingAttemptEventHandler, FinishChargingAttemptEventHandler,
		AbortChargingAttemptEventHandler, //
		ChargingStartEventHandler,
		ChargingEndEventHandler, QueuedAtChargerEventHandler, QuitQueueAtChargerEventHandler,
		EnergyChargedEventHandler {
	private final List<ChargingProcessItem> chargingItems = new LinkedList<>();
	private final List<ChargingAttemptItem> chargingAttemptItems = new LinkedList<>();
	private boolean finished = false;

	private IdMap<Vehicle, ChargingProcessTracker> active = new IdMap<>(Vehicle.class);

	@Override
	public void reset(int iteration) {
		chargingItems.clear();
		chargingAttemptItems.clear();
		finished = false;
		active.clear();
	}

	@Override
	public void handleEvent(StartChargingProcessEvent event) {
		Preconditions.checkState(!active.containsKey(event.getVehicleId()));
		active.put(event.getVehicleId(), new ChargingProcessTracker(event));
	}

	@Override
	public void handleEvent(AbortChargingProcessEvent event) {
		ChargingProcessTracker process = active.remove(event.getVehicleId());
		process.abort = event;
		registerProcess(process);
	}

	@Override
	public void handleEvent(FinishChargingProcessEvent event) {
		ChargingProcessTracker process = active.remove(event.getVehicleId());
		process.finish = event;
		registerProcess(process);
	}

	@Override
	public void handleEvent(StartChargingAttemptEvent event) {
		active.get(event.getVehicleId()).attempts.add(new ChargingAttemptTracker(event));
	}

	@Override
	public void handleEvent(UpdateChargingAttemptEvent event) {
		ChargingAttemptTracker attempt = active.get(event.getVehicleId()).attempts.getLast();
		attempt.update = event;
	}

	@Override
	public void handleEvent(AbortChargingAttemptEvent event) {
		ChargingAttemptTracker attempt = active.get(event.getVehicleId()).attempts.getLast();
		attempt.abort = event;
	}

	@Override
	public void handleEvent(FinishChargingAttemptEvent event) {
		ChargingAttemptTracker attempt = active.get(event.getVehicleId()).attempts.getLast();
		attempt.finish = event;
	}

	private class ChargingProcessTracker {
		ChargingProcessTracker(StartChargingProcessEvent start) {
			this.start = start;
		}

		final StartChargingProcessEvent start;
		AbortChargingProcessEvent abort;
		FinishChargingProcessEvent finish;

		LinkedList<ChargingAttemptTracker> attempts = new LinkedList<>();
	}

	private class ChargingAttemptTracker {
		ChargingAttemptTracker(StartChargingAttemptEvent start) {
			this.start = start;
		}

		final StartChargingAttemptEvent start;
		UpdateChargingAttemptEvent update;
		AbortChargingAttemptEvent abort;
		FinishChargingAttemptEvent finish;

		QueuedAtChargerEvent queued;
		QuitQueueAtChargerEvent quit;

		ChargingStartEvent plug;
		ChargingEndEvent unplug;

		EnergyChargedEvent lastEnergyEvent;
	}

	@Override
	public void handleEvent(QueuedAtChargerEvent event) {
		var item = active.get(event.getVehicleId());

		if (item != null) {
			item.attempts.getLast().queued = event;
		}
	}

	@Override
	public void handleEvent(QuitQueueAtChargerEvent event) {
		var item = active.get(event.getVehicleId());

		if (item != null) {
			item.attempts.getLast().quit = event;
		}
	}

	@Override
	public void handleEvent(ChargingStartEvent event) {
		var item = active.get(event.getVehicleId());

		if (item != null) {
			item.attempts.getLast().plug = event;
		}
	}

	@Override
	public void handleEvent(EnergyChargedEvent event) {
		var item = active.get(event.getVehicleId());

		if (item != null) {
			item.attempts.getLast().lastEnergyEvent = event;
		}
	}

	@Override
	public void handleEvent(ChargingEndEvent event) {
		var item = active.get(event.getVehicleId());

		if (item != null) {
			item.attempts.getLast().unplug = event;
		}
	}

	static public record ChargingProcessItem( //
			Id<Person> personId, Id<Vehicle> vehicleId, //
			int processIndex, int attempts, boolean successful, //
			double startTime, double endTime) {
	}

	static public record ChargingAttemptItem( //
			Id<Person> personId, Id<Vehicle> vehicleId, //
			int processIndex, int attemptIndex, boolean successful, //
			double startTime, double updateTime, double endTime, //
			double queueingStartTime, double queueingEndTime, boolean queued, //
			double chargingStartTime, double chargingEndTime, boolean charged, //
			Id<Charger> chargerId, Id<Charger> initialChargerId, boolean enroute, //
			boolean spontaneous, double energy_kWh) {
	}

	private void registerProcess(ChargingProcessTracker tracker) {
		Preconditions.checkState(!finished);

		double endTime = Double.NaN;
		if (tracker.finish != null) {
			endTime = tracker.finish.getTime();
		} else if (tracker.abort != null) {
			endTime = tracker.abort.getTime();
		} // otherwise was still ongoing

		ChargingProcessItem chargingItem = new ChargingProcessItem(tracker.start.getPersonId(),
				tracker.start.getVehicleId(), tracker.start.getProcessIndex(), tracker.attempts.size(),
				tracker.attempts.getLast().start != null, tracker.start.getTime(), endTime);

		synchronized (chargingItems) {
			chargingItems.add(chargingItem);
		}

		for (ChargingAttemptTracker attemptTracker : tracker.attempts) {
			double attemptStartTime = chargingItem.startTime;
			if (attemptTracker.start != null) {
				attemptStartTime = attemptTracker.start.getTime();
			}

			boolean isEnroute = attemptTracker.start.isEnroute();
			boolean isSponaneous = attemptTracker.start.isSpontaneous();

			double attemptUpdateTime = Double.NaN;
			if (attemptTracker.update != null) {
				attemptUpdateTime = attemptTracker.update.getTime();
			}

			double attemptEndTime = Double.NaN;
			if (attemptTracker.finish != null) {
				attemptEndTime = attemptTracker.finish.getTime();
			} else if (attemptTracker.abort != null) {
				attemptEndTime = attemptTracker.abort.getTime();
			}

			double queueingStartTime = Double.NaN;
			if (attemptTracker.queued != null) {
				queueingStartTime = attemptTracker.queued.getTime();
			}

			double queueingEndTime = Double.NaN;
			if (attemptTracker.quit != null) {
				queueingEndTime = attemptTracker.quit.getTime();
			} else if (attemptTracker.queued != null && attemptTracker.plug != null) {
				queueingEndTime = attemptTracker.plug.getTime();
			}

			double chargingStartTime = Double.NaN;
			if (attemptTracker.plug != null) {
				chargingStartTime = attemptTracker.plug.getTime();
			}

			double chargingEndTime = Double.NaN;
			if (attemptTracker.unplug != null && attemptTracker.lastEnergyEvent != null) {
				// this is not when we unplug, but the last time energy is added
				chargingEndTime = attemptTracker.lastEnergyEvent.getTime();
			}

			Id<Charger> chargerId = attemptTracker.start.getChargerId();
			Id<Charger> initialChargerId = attemptTracker.start.getChargerId();
			if (attemptTracker.update != null) {
				chargerId = attemptTracker.update.getChargerId();
			}

			double energy_kWh = 0.0;
			if (attemptTracker.plug != null && attemptTracker.lastEnergyEvent != null) {
				energy_kWh = EvUnits
						.J_to_kWh(attemptTracker.lastEnergyEvent.getEndCharge() - attemptTracker.plug.getCharge());
			}

			ChargingAttemptItem attemptItem = new ChargingAttemptItem(tracker.start.getPersonId(),
					tracker.start.getVehicleId(), tracker.start.getProcessIndex(),
					attemptTracker.start.getAttemptIndex(), attemptTracker.plug != null,
					attemptStartTime, attemptUpdateTime, attemptEndTime, queueingStartTime, queueingEndTime,
					attemptTracker.queued != null, chargingStartTime, chargingEndTime,
					attemptTracker.plug != null, chargerId, initialChargerId, isEnroute, isSponaneous, energy_kWh);

			synchronized (chargingAttemptItems) {
				chargingAttemptItems.add(attemptItem);
			}
		}
	}

	public void processRemainingEvents() {
		for (var process : active.values()) {
			registerProcess(process);
		}

		finished = true;
	}

	public List<ChargingProcessItem> getChargingProcessItems() {
		if (!finished) {
			processRemainingEvents();
		}

		return chargingItems;
	}

	public List<ChargingAttemptItem> getChargingAttemptItems() {
		if (!finished) {
			processRemainingEvents();
		}

		return chargingAttemptItems;
	}
}
