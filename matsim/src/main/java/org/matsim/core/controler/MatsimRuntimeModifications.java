
/* *********************************************************************** *
 * project: org.matsim.*
 * MatsimRuntimeModifications.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

 package org.matsim.core.controler;

import jakarta.annotation.Nullable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.atomic.AtomicBoolean;

class MatsimRuntimeModifications {

	private final MyRunnable runnable;
	// for tests
	private volatile Throwable uncaughtException;

	private AtomicBoolean unexpectedShutdown = new AtomicBoolean(false);

	private static final  Logger log = LogManager.getLogger(MatsimRuntimeModifications.class);

	interface MyRunnable {
		void run() throws UnexpectedShutdownException;
		void shutdown(boolean unexpected, @Nullable Throwable exception);
	}


	static class UnexpectedShutdownException extends Exception {
	}

	MatsimRuntimeModifications(MyRunnable runnable) {
		this.runnable = runnable;
	}

	static void run(MyRunnable runnable) {
		new MatsimRuntimeModifications(runnable).run();
	}

	void run() {
		Thread.UncaughtExceptionHandler previousDefaultUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
		final Thread controllerThread = Thread.currentThread();
		Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
			@Override
			public void uncaughtException(Thread t, Throwable e) {
				// We want to shut down when any Thread dies with an Exception.
				log.error("Getting uncaught Exception in Thread " + t.getName(), e);
				uncaughtException = e;
				unexpectedShutdown.set(true);
				controllerThread.interrupt();
			}
		});
		try {
			runnable.run();
		} catch (UnexpectedShutdownException e) {
			// Doesn't matter. Just shut down.
		} catch (Throwable e) {
			// Don't let it fall through to the UncaughtExceptionHandler. We want to first log the Exception,
			// then shut down.
			log.error("Getting uncaught Exception in Thread " + Thread.currentThread().getName(), e);
			uncaughtException = e;
			unexpectedShutdown.set(true);
		} finally {
			log.info("S H U T D O W N   ---   start shutdown.");
			if (unexpectedShutdown.get()) {
				log.error("ERROR --- This is an unexpected shutdown!");
			}
			if (uncaughtException != null) {
				log.error("Shutdown possibly caused by the following Exception:", uncaughtException);
			}
			try {
				runnable.shutdown(unexpectedShutdown.get(), uncaughtException == null ? null: uncaughtException);
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
		}
		if (uncaughtException != null) {
			if (uncaughtException instanceof RuntimeException) {
				throw ((RuntimeException) uncaughtException);
			} else {
				throw new RuntimeException(uncaughtException);
			}
		}
	}


}
