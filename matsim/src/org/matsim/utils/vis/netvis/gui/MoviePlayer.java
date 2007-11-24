/* *********************************************************************** *
 * project: org.matsim.*
 * MoviePlayer.java
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

package org.matsim.utils.vis.netvis.gui;

import java.io.IOException;
import java.util.TimerTask;

import org.matsim.utils.vis.netvis.NetVis;
import org.matsim.utils.vis.netvis.visNet.DisplayNetStateReader;

/**
 * @author gunnar
 * 
 */
class MoviePlayer extends TimerTask {

    private DisplayNetStateReader reader;

    private NetVis viz;

    public MoviePlayer(DisplayNetStateReader readers, NetVis viz) {
        this.reader = readers;
        this.viz = viz;
    }

    @Override
		public void run() {
        try {
            reader.toNextTimeStep();
        } catch (IOException e) {
            System.err.println("MoviePlayer encountered problem: " + e);
        }
        viz.paintNow();
    }

}