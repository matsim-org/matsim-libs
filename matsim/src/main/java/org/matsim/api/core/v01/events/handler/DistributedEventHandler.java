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
     * Defines the capability of the event handler to handle events in a distributed manner.
     */
    DistributedMode value() default DistributedMode.GLOBAL;

    /**
     * If true, events for this handler are not queued but processed directly.
     */
    boolean directProcessing() default false;

	/**
	 * If true, the sim step will not block until the event handler is finished. Only at the very end end of the simulation event processing is ensured.
	 * This has major implications, because the handler must not interact with the simulation state or vice versa.
	 * Generally, it should only be enabled for handlers that react to events and are needed at the end of the simulation.
	 */
	boolean async() default false;

}
