package org.matsim.contrib.drt.optimizer.rebalancing.adaptiveRealTime;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.contrib.drt.analysis.zonal.DrtZonalSystem;
import org.matsim.contrib.drt.analysis.zonal.DrtZone;

public class InactiveZoneIdentifier implements PersonDepartureEventHandler {
	private static final Logger log = Logger.getLogger(InactiveZoneIdentifier.class);
	private final DrtZonalSystem zonalSystem;

	private final Set<DrtZone> activeZones = new HashSet<>();
	private final Set<DrtZone> inactiveZones = new HashSet<>();

	public InactiveZoneIdentifier(DrtZonalSystem zonalSystem) {
		this.zonalSystem = zonalSystem;
	}

	@Override
	public void reset(int iteration) {
		inactiveZones.clear();
		if (iteration > 0) {
			inactiveZones.addAll(zonalSystem.getZones().values());
			inactiveZones.removeAll(activeZones);
		}
	}

	public Set<DrtZone> getInactiveZone() {
		log.info("There are in total " + Integer.toString(zonalSystem.getZones().keySet().size()) + " zones");
		log.info("Of which " + Integer.toString(activeZones.size()) + " are active zones");
		log.info("Note: At iteration 0, all zones are assumed to be active");
		return inactiveZones;
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		activeZones.add(zonalSystem.getZoneForLinkId(event.getLinkId()));
	}

}
