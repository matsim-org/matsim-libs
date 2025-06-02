package org.matsim.contrib.profiling.instrument;

import org.aspectj.lang.annotation.Pointcut;
import org.matsim.contrib.profiling.aop.trace.TraceProfilingAspect;
import org.matsim.contrib.profiling.aop.trace.TraceProfilingEvent;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Instruct, whether {@link TraceProfilingAspect} shall create {@link TraceProfilingEvent JFR events}.
 */
public final class Trace {

	private Trace() {}

	// todo potentially use volatile boolean instead, when limiting visibility of setters and only one-thread will update the value
	private static final AtomicBoolean enabled = new AtomicBoolean(false);

	@Pointcut("if()")
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
