package org.matsim.mosaic;

import org.eclipse.mosaic.rti.api.FederateExecutor;
import org.eclipse.mosaic.rti.api.InternalFederateException;
import org.eclipse.mosaic.rti.config.CLocalHost;
import org.matsim.application.MATSimApplication;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Executes MATSim in a separate thread. It does not implement {@link org.eclipse.mosaic.rti.api.FederateExecutor}, as it was barely applicable in this context.
 */
final class MATSimFederateExecutor implements FederateExecutor {

	private static final Logger log = LoggerFactory.getLogger(MATSimFederateExecutor.class);

	private final CMATSimMosaic matsimConfig;

	/**
	 * Controler for the MATSim scenario to run.
	 */
	private Controler controler;

	/**
	 * Main thread for the matsim controller.
	 */
	private Runner runner;

	private File workingDirectory;

	/**
	 * If an exceptions occurs it is stored here.
	 */
	private final AtomicReference<Throwable> exc = new AtomicReference<>();

	public MATSimFederateExecutor(CMATSimMosaic matsimConfig) {
		this.matsimConfig = matsimConfig;
	}

	/**
	 * Starts the scenario.
	 */
	public void startLocalFederate(MATSimAmbassador ambassador) {

		Class<? extends MATSimApplication> scenario = null;
		try {
			scenario = (Class<? extends MATSimApplication>) ClassLoader.getSystemClassLoader().loadClass(matsimConfig.scenario);
		} catch (ClassNotFoundException e) {
			throw new IllegalArgumentException("Could not scenario class", e);
		}

		// TODO: respect the --config parameter
		controler = MATSimApplication.prepare(scenario, "run", matsimConfig.additionalSumoParameters);

		controler.getConfig().controler().setLastIteration(0);
		controler.getConfig().controler().setOutputDirectory(workingDirectory.toString());
		controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

		controler.addOverridingModule(new MosaicModule(ambassador, controler.getConfig()));

		runner = new Runner();
		runner.start();
	}

	/**
	 * Check and throw exception if an error occurred.
	 *
	 * @throws InternalFederateException if an exception occurred within matsim.
	 */
	public void checkError() throws InternalFederateException {
		Throwable t;
		if ((t = exc.get()) != null)
			throw new InternalFederateException("Internal MATSim exception", new Exception(t));
	}

	/**
	 * Return the controler of the MATSim scenario
	 */
	public Controler getControler() {
		return controler;
	}

	/**
	 * Does not actually start the ambassador, because this will be done later in {@link #startLocalFederate(MATSimAmbassador)}.
	 */
	@Override
	public Process startLocalFederate(File file) throws FederateStarterException {
		workingDirectory = file;
		return null;
	}

	@Override
	public void stopLocalFederate() throws FederateStarterException {

		try {
			if (runner.isAlive()) {
				runner.join(10_000);
				log.warn("MATSim not stopped yet, terminating thread...");
			}
		} catch (InterruptedException e) {
			runner.interrupt();

			try {
				runner.join(10_000);
			} catch (InterruptedException ex) {
				throw new FederateStarterException(e);
			}

		}
	}

	@Override
	public int startRemoteFederate(CLocalHost cLocalHost, PrintStream printStream, InputStream inputStream) throws FederateStarterException {
		throw new UnsupportedOperationException("Remote MATSim not supported.");
	}

	@Override
	public void stopRemoteFederate(PrintStream printStream) throws FederateStarterException {
		throw new UnsupportedOperationException("Remote MATSim not supported.");
	}

	private final class Runner extends Thread {

		@Override
		public void run() {
			try {
				controler.run();
			} catch (Throwable t) {
				exc.set(t);
			}
		}
	}

}
