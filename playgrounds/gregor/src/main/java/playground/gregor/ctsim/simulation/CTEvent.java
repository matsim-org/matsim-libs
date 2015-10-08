package playground.gregor.ctsim.simulation;
/* *********************************************************************** *
 * project: org.matsim.*
 *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

import playground.gregor.ctsim.simulation.physics.CTCell;

/**
 * Created by laemmel on 07/10/15.
 */
public class CTEvent implements Comparable<CTEvent> {

    private final double execTime;
    private final CTCell cell;

    private boolean valid = true;


    public CTEvent(CTCell cell, double execTime) {
        this.cell = cell;
        this.execTime = execTime;
    }

    public void invalidate() {
        this.valid = false;
    }

    public double getExecTime() {
        return this.execTime;
    }

    public void execute() {
        invalidate();
        cell.jumpAndUpdateNeighbors(execTime);
    }

    @Override
    public int compareTo(CTEvent o) {
        double diff = this.getExecTime()
                - o.getExecTime();

        if (diff < 0) {
            return -1;
        }
        else {
            if (diff > 0) {
                return 1;
            }
        }

        return 0;
    }


    public boolean isInvalid() {
        return !valid;
    }
}
