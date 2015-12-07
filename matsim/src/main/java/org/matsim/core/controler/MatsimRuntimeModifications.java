package org.matsim.core.controler;

import org.apache.log4j.Logger;

import java.util.concurrent.atomic.AtomicBoolean;

class MatsimRuntimeModifications {

	private final MyRunnable runnable;
	private final boolean dirtyShutdown;
	// for tests
	private volatile Throwable uncaughtException;

	private AtomicBoolean unexpectedShutdown = new AtomicBoolean(false);

	private static Logger log = Logger.getLogger(MatsimRuntimeModifications.class);

	private class InterruptAndJoin extends Thread {
		private final Thread controllerThread;
		private AtomicBoolean unexpectedShutdown;

		public InterruptAndJoin(Thread controllerThread, AtomicBoolean unexpectedShutdown) {
			this.controllerThread = controllerThread;
			this.unexpectedShutdown = unexpectedShutdown;
		}

		@Override
		public void run() {
			log.error("received unexpected shutdown request.");
			unexpectedShutdown.set(true);
			log.info("sending interrupt request to controllerThread");
			controllerThread.interrupt();

			if (!dirtyShutdown) {
				// for me, the following code just makes the shutdown hang indefinitely (on mac, tested mostly with otfvis).
				// Therefore, now providing a switch.  kai, mar'15
				try {
					// Wait until it has shut down.
					log.info("waiting for controllerThread to terminate") ;
					controllerThread.join();
					log.info("controllerThread terminated") ;
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
				// The JVM will exit when this method returns.
			}
		}
	}

	private void logMemorizeAndRequestShutdown(Thread t, Throwable e) {
		log.error("Getting uncaught Exception in Thread " + t.getName(), e);
		uncaughtException = e;
		unexpectedShutdown.set(true);
	}

	interface MyRunnable {
		void run() throws UnexpectedShutdownException;
		void shutdown(boolean unexpected);
	}


	static class UnexpectedShutdownException extends Exception {
	}

	MatsimRuntimeModifications(MyRunnable runnable, boolean dirtyShutdown) {
		this.runnable = runnable;
		this.dirtyShutdown = dirtyShutdown;
	}

	static void run(MyRunnable runnable, boolean dirtyShutdown) {
		new MatsimRuntimeModifications(runnable, dirtyShutdown).run();
	}

	void run() {
		Thread.UncaughtExceptionHandler previousDefaultUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
		final Thread controllerThread = Thread.currentThread();
		Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
			@Override
			public void uncaughtException(Thread t, Throwable e) {
				// We want to shut down when any Thread dies with an Exception.
				logMemorizeAndRequestShutdown(t, e);
				controllerThread.interrupt();
			}
		});
		Thread shutdownHook = new InterruptAndJoin(controllerThread, unexpectedShutdown);
		Runtime.getRuntime().addShutdownHook(shutdownHook);
		try {
			runnable.run();
		} catch (UnexpectedShutdownException e) {
			// Doesn't matter. Just shut down.
		} catch (Throwable e) {
			// Don't let it fall through to the UncaughtExceptionHandler. We want to first log the Exception,
			// then shut down.
			logMemorizeAndRequestShutdown(Thread.currentThread(), e);
		} finally {
			try {
				Runtime.getRuntime().removeShutdownHook(shutdownHook);
			} catch (IllegalStateException e) {
				// Doesn't matter. Only means that we are already shutting down.
				// But if we are not, this is neccessary because otherwise the
				// shutdown hook will try to interrupt and join this thread later where
				// it may already be used for something else.
				// I still think all this JVM-stuff should go somewhere outside,
				// where it is only used when e.g. run from the command line,
				// and not e.g. from tests or scripts, where multiple
				// instances of Controler can be run.
			}
			log.info("S H U T D O W N   ---   start shutdown.");
			if (unexpectedShutdown.get()) {
				log.error("ERROR --- This is an unexpected shutdown!");
			}
			if (uncaughtException != null) {
				log.error("Shutdown possibly caused by the following Exception:", uncaughtException);
			}
			try {
				runnable.shutdown(unexpectedShutdown.get());
			} catch (Exception e) {
				unexpectedShutdown.set(true);
				log.error("Exception during shutdown:", e);
				if (uncaughtException == null) {
					// If there has been a previous exception, it is likely more important
					// than this new one. This new one may just be a consequence of the
					// previous one.
					uncaughtException = e;
				}
			}
			if (unexpectedShutdown.get()) {
				log.error("ERROR --- MATSim unexpectedly terminated. Please check the output or the logfile with warnings and errors for hints.");
				log.error("ERROR --- results should not be used for further analysis.");
			}
			log.info("S H U T D O W N   ---   shutdown completed.");
			if (unexpectedShutdown.get()) {
				log.error("ERROR --- This was an unexpected shutdown! See the log file for a possible reason.");
			}
			Thread.setDefaultUncaughtExceptionHandler(previousDefaultUncaughtExceptionHandler);
			// Propagate Exception in case Controler.run is called by someone who wants to catch
			// it. It is probably not strictly correct to wrap the exception here.
			// But otherwise, this method would have to declare "throws Throwable".
			// In theory, a run method for test cases would probably need to
			// be different from the run method of the "MATSim platform" which
			// takes control of the JVM by installing hooks and exception
			// handlers.
			if (uncaughtException != null) {
				throw new RuntimeException(uncaughtException);
			}
		}
	}


}
