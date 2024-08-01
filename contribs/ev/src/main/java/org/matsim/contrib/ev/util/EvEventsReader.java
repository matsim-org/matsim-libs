package org.matsim.contrib.ev.util;

import org.matsim.contrib.ev.charging.*;
import org.matsim.contrib.ev.discharging.DrivingEnergyConsumptionEvent;
import org.matsim.contrib.ev.discharging.IdlingEnergyConsumptionEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.MatsimEventsReader;

/**
 * @author nkuehnel / MOIA
 */
public final class EvEventsReader {

	private EvEventsReader(){}

	public static void registerEvEventMappers(MatsimEventsReader reader) {
		reader.addCustomEventMapper(DrivingEnergyConsumptionEvent.EVENT_TYPE, DrivingEnergyConsumptionEvent::convert);
		reader.addCustomEventMapper(IdlingEnergyConsumptionEvent.EVENT_TYPE, IdlingEnergyConsumptionEvent::convert);
		reader.addCustomEventMapper(EnergyChargedEvent.EVENT_TYPE, EnergyChargedEvent::convert);
		reader.addCustomEventMapper(ChargingStartEvent.EVENT_TYPE, ChargingStartEvent::convert);
		reader.addCustomEventMapper(ChargingEndEvent.EVENT_TYPE, ChargingEndEvent::convert);
		reader.addCustomEventMapper(QueuedAtChargerEvent.EVENT_TYPE, QueuedAtChargerEvent::convert);
		reader.addCustomEventMapper(QuitQueueAtChargerEvent.EVENT_TYPE, QuitQueueAtChargerEvent::convert);
	}

	public static MatsimEventsReader createEvEventsReader(EventsManager eventsManager) {
		MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
		registerEvEventMappers(reader);
		return reader;
	}
}
