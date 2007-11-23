/* *********************************************************************** *
 * project: org.matsim.*
 * SimStateReaderI.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.utils.vis.netvis.streaming;

import java.io.IOException;

public interface SimStateReaderI {

    public int getCurrentTime_s();

    public int startTime_s();

    public int bufferSize();

    public int timeStepLength_s();

    public int endTime_s();

    /**
     * Has to be called before anything is read. Conducts initialization actions
     * that are not possible during construction, since subclasses might not yet
     * have been fully initialized.
     */
    public void open() throws IOException;

    /**
     * Loads the very first buffer block and sets the network to the very first
     * element in this block.
     */
    public void toStart() throws IOException;

    /**
     * Moves the buffer pointer one step back in time (if possible) and applies
     * the according state to the network.
     */
    public void toPrevTimeStep() throws IOException;

    /**
     * Moves the buffer pointer one step forwards in time (if possible) and
     * applies the according state to the network.
     */
    public void toNextTimeStep() throws IOException;

    /**
     * Loads the network state according to <code>newTime_s</code> together
     * with the according buffer segment. If the passed time step is out of
     * bound or does not lie within the temporal grid, it is accordingly
     * corrected. Use <code>getCurrentTime_s</code> to check the new time
     * step.
     * 
     * @param newTime_s
     *            time of the state to be loaded
     */
    public void toTimeStep(int newTime_s) throws IOException;

    /**
     * Loads the last state together with the according buffer segment.
     */
    public void toEnd() throws IOException;

    /**
     * Does nothing.
     */
    public void close();

}