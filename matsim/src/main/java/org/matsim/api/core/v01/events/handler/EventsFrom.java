package org.matsim.api.core.v01.events.handler;


import org.matsim.api.core.v01.events.EventSource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface EventsFrom {

    /**
     * The source of the events. Handler only listen to events from this source.
     */
    EventSource value();

}
