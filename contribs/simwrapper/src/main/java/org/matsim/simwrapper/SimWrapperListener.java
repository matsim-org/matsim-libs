package org.matsim.simwrapper;

import com.google.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Listener to execute {@link SimWrapper} when simulation starts or ends.
 */
public class SimWrapperListener implements StartupListener, ShutdownListener {

	private static final Logger log = LogManager.getLogger(SimWrapper.class);

	@Inject
	private SimWrapper simWrapper;

	@Override
	public void notifyStartup(StartupEvent event) {

		try {
			simWrapper.generate(Path.of(event.getServices().getControlerIO().getOutputPath()));
		} catch (IOException e) {
			log.error("Could not create SimWrapper dashboard.");
		}


	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {

		// TODO: dump data at end

		// TODO: simwwrapper run

	}

}
