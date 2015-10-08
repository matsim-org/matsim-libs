package playground.gregor.ctsim.simulation.physics;
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

/**
 * Created by laemmel on 07/10/15.
 */
public class CTPed {


    private CTCell currentCell;
    private double dir;
    private CTCell tentativeNextCell;

    public CTPed(CTCell cell, double dir) {
        this.currentCell = cell;
        this.dir = dir;
    }

    public double chooseNextCellAndReturnJumpRate() {

        CTCell bestNB = null;
        double maxFlowFactor = 0;
        for (CTCellFace face : this.currentCell.getFaces()) {
            double flowFactor = (1 + Math.cos(dir - face.h_i)) * currentCell.getJ(face.nb);
            if (flowFactor > maxFlowFactor) {
                maxFlowFactor = flowFactor;
                bestNB = face.nb;

            }
        }
        if (bestNB == null) {
            return Double.NaN;
        }
        this.tentativeNextCell = bestNB;
        return currentCell.getJ(this.tentativeNextCell) * maxFlowFactor;
    }


    public CTCell getNextCellAndJump() {
        this.currentCell.jumpOffPed(this);
        this.currentCell = tentativeNextCell;
        this.currentCell.jumpOnPed(this);
        this.tentativeNextCell = null;
        return this.currentCell;
    }
}
