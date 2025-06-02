/* ********************************************************************** *
 * project: org.matsim.*
 * ScoringListenerProfilingAspect.aj
 *                                                                        *
 * ********************************************************************** *
 *                                                                        *
 * copyright       : (C) 2025 by the members listed in the COPYING,       *
 *                   LICENSE and WARRANTY file.                           *
 * email           : info at matsim dot org                               *
 *                                                                        *
 * ********************************************************************** *
 *                                                                        *
 *   This program is free software; you can redistribute it and/or modify *
 *   it under the terms of the GNU General Public License as published by *
 *   the Free Software Foundation; either version 2 of the License, or    *
 *   (at your option) any later version.                                  *
 *   See also COPYING, LICENSE and WARRANTY file                          *
 *                                                                        *
 * ********************************************************************** */

package org.matsim.contrib.profiling.aop;

import jdk.jfr.Event;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.matsim.contrib.profiling.events.MatsimJfrEvent;

@Aspect
public class ScoringListenerProfilingAspect extends AbstractProfilingEventAspect{

    /**
     * Use the full canonical name (including package) in the pointcut declaration to ensure aspectj finds the targeted class.
     */
    @Pointcut("call(void org.matsim.core.controler.listener.ScoringListener.notifyScoring(..))")
    public void eventPoints() {}

    public Event createEvent(JoinPoint.StaticPart thisJoinPointStaticPart) {
        Event jfrEvent = MatsimJfrEvent.create("AOP profiling: " + thisJoinPointStaticPart.getSignature());
        System.out.println("AOP profiling: " + thisJoinPointStaticPart.getSignature());
        return jfrEvent;
    }

}
