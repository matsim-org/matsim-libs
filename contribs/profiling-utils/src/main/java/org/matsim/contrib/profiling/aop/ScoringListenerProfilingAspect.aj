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

public aspect ScoringListenerProfilingAspect extends AbstractProfilingEventAspect {

    pointcut eventPoints(Object o):
            target(o) &&
            call(void ScoringListener.notifyScoring(..));

}
