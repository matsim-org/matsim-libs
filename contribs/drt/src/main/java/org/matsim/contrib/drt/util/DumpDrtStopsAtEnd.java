package org.matsim.contrib.drt.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.contrib.drt.routing.DrtStopNetwork;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;

/**
 * @author nkuehnel / MOIA
 */
public final class DumpDrtStopsAtEnd implements ShutdownListener {

	private static final Logger log = LogManager.getLogger(DumpDrtStopsAtEnd.class);

	private final DrtStopNetwork drtStopNetwork;

	private final OutputDirectoryHierarchy outputDirectoryHierarchy;

	public DumpDrtStopsAtEnd(DrtStopNetwork drtStopNetwork, OutputDirectoryHierarchy outputDirectoryHierarchy) {
		this.drtStopNetwork = drtStopNetwork;
		this.outputDirectoryHierarchy = outputDirectoryHierarchy;
	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		if (event.isUnexpected()) {
			return;
		}
		dumpDrtStops();
	}

	private void dumpDrtStops() {
		try {
			if (this.drtStopNetwork != null) {
				TransitSchedule transitSchedule = DrtStopNetwork.toTransitSchedule(this.drtStopNetwork);
				new TransitScheduleWriter(transitSchedule).writeFile(this.outputDirectoryHierarchy.getOutputFilename("output_drt_Stops.xml.gz"));
			}
		} catch (Exception ee) {
			log.error("Exception writing drt stops.", ee);
		}
	}
}
