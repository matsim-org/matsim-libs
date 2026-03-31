package org.matsim.core.mobsim.qsim.qnetsimengine.parking;

import com.google.inject.Inject;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.utils.io.IOUtils;

import java.io.IOException;

public class DumpParkingDataAtEnd implements ShutdownListener {
	@Inject
	OutputDirectoryHierarchy outputDirectoryHierarchy;

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		try {
			IOUtils.copyFile(outputDirectoryHierarchy.getIterationFilename(event.getIteration(), ParkingUtils.PARKING_INITIAL_FILE),
				outputDirectoryHierarchy.getOutputFilename(ParkingUtils.PARKING_INITIAL_FILE));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
