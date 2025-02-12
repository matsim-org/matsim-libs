package org.matsim.profiling.aop;

import jdk.jfr.Event;
import org.matsim.profiling.JFRMatsimEvent;

public abstract aspect AbstractProfilingEventAspect {

    abstract pointcut eventPoints(Object o);

    void around(Object o): eventPoints(o) {
        Event jfrEvent = JFRMatsimEvent.create("AOP profiling: " + o.getClass().getName());

        System.out.println("AOP profiling: " + o.getClass().getSimpleName());

        jfrEvent.begin();
        proceed(o);
        jfrEvent.commit();
    }
}
