package org.matsim.simwrapper;

import com.google.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.simwrapper.dashboard.StuckAgentDashboard;

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

		// TODO: probably config group is needed
		// setup defaults dashboards

		simWrapper.addDashboard(new StuckAgentDashboard());

		try {
			simWrapper.generate(Path.of(event.getServices().getControlerIO().getOutputPath()));
		} catch (IOException e) {
			log.error("Could not create SimWrapper dashboard.");
		}
	}

	/**
	 * This needs to run after all the other matsim listeners. Currently, this is the case, but unclear if that is ensured
	 * somewhere.
	 */
	@Override
	public void notifyShutdown(ShutdownEvent event) {
		simWrapper.run(Path.of(event.getServices().getControlerIO().getOutputPath()));
	}

}
