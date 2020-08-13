package org.matsim.contrib.drt.optimizer.rebalancing.adaptiveRealTime;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.contrib.drt.analysis.zonal.DrtZonalSystem;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEvent;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEventHandler;

public class InactiveZoneIdentifier implements DrtRequestSubmittedEventHandler {
	private static final Logger log = Logger.getLogger(InactiveZoneIdentifier.class);
	private final DrtZonalSystem zonalSystem;

	private final Set<String> activeZones = new HashSet<>();
	private final Set<String> inactiveZones = new HashSet<>();

	public InactiveZoneIdentifier(DrtZonalSystem zonalSystem) {
		this.zonalSystem = zonalSystem;
	}

	@Override
	public void handleEvent(DrtRequestSubmittedEvent event) {
		activeZones.add(zonalSystem.getZoneForLinkId(event.getFromLinkId()));
	}

	@Override
	public void reset(int iteration) {
		inactiveZones.clear();
		if (iteration > 0) {
			inactiveZones.addAll(zonalSystem.getZones().keySet());
			inactiveZones.removeAll(activeZones);
		}
	}

	public Set<String> getInactiveZone() {
		log.info("There are in totoal" + Integer.toString(zonalSystem.getZones().keySet().size()) + " zones");
		log.info("Of which " + Integer.toString(activeZones.size()) + " are active zones");
		log.info("Note: At iteration 0, all zone are assumed to be active");
		return inactiveZones;
	}

}
