package org.matsim.api.core.v01.events.handler;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for event handlers that are optimized for distributed or parallel processing.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedEventHandler {

	/**
	 * Defines how this event handler is called. To be backwards compatible, the default is GLOBAL, which is how event handlers are
	 * called when QSim is used as mobsim. GLOBAL event handlers possibly limit the scalability of a DSim run. Modes other than
	 * GLOBAL require event handlers to sync the collected data. See {@link DistributedMode} for more details.
	 */
	DistributedMode value() default DistributedMode.GLOBAL;

	/**
	 * Defines how the event handler is called. See {@link ProcessingMode} for more details.
	 */
	ProcessingMode processing() default ProcessingMode.TASK;

	/**
	 * Defines when the execution of the simulation is paused to await the execution of the event handler. See {@link BlockingMode} for more details.
	 */
	BlockingMode blocking() default BlockingMode.SIM_STEP;

}
