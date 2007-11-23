/* *********************************************************************** *
 * project: org.matsim.*
 * DisplayAgent.java
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

package org.matsim.utils.vis.netvis.visNet;

import org.matsim.utils.vis.netvis.DrawableAgentI;

/**
 * 
 * @author gunnar
 * 
 */
public class DisplayAgent implements DrawableAgentI {

    // -------------------- MEMBER VARIABLES --------------------

    private double posInLink_m;

    private int lane;

    // -------------------- CONSTRUCTION --------------------

    public DisplayAgent(double posInLink_m, int lane) {
        this.posInLink_m = posInLink_m;
        this.lane = lane;
    }

    // ---------- IMPLEMENTATION OF DrawableAgentI ----------

    public double getPosInLink_m() {
        return posInLink_m;
    }

    public int getLane() {
        return lane;
    }

}