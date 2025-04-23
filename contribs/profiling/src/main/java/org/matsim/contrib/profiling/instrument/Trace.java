package org.matsim.contrib.profiling.instrument;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Instruct, whether {@link org.matsim.contrib.profiling.aop.TraceProfilingAspect} shall create {@link org.matsim.contrib.profiling.aop.TraceProfilingEvent JFR events}.
 */
public final class Trace {

	private Trace() {}

	// todo potentially use volatile boolean instead, when limiting visibility of setters and only one-thread will update the value
	private static final AtomicBoolean enabled = new AtomicBoolean(false);

	public static boolean isEnabled() {
		return enabled.get();
	}

	public static void enable() {
		enabled.set(true);
	}

	public static void disable() {
		enabled.set(false);
	}

}
