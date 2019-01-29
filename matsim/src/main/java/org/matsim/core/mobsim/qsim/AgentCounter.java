/* *********************************************************************** *
 * project: org.matsim.*
 * AgentCounter
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package org.matsim.core.mobsim.qsim;

import java.util.concurrent.atomic.AtomicInteger;


/**
 * This class is responsible for living/lost agent counting.
 *
 *
 * @author dgrether
 *
 */
class AgentCounter implements org.matsim.core.mobsim.qsim.interfaces.AgentCounter {

    /**
     * Number of agents that have not yet reached their final activity location
     */
    private final AtomicInteger living = new AtomicInteger(0);

    /**
     * Number of agents that got stuck in a traffic jam and were removed from the simulation to solve a possible deadlock
     */
    private final AtomicInteger lost = new AtomicInteger(0);

    @Override
    public final int getLiving() {return living.get();	}

    @Override
    public final boolean isLiving() {return living.get() > 0;	}

    @Override
    public final int getLost() {return lost.get();	}

    @Override
    public final void incLost() {lost.incrementAndGet(); }

    final void incLiving() {living.incrementAndGet();}

    @Override
    public final void decLiving() {living.decrementAndGet();}

}
