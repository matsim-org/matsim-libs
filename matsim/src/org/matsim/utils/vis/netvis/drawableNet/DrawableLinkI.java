/* *********************************************************************** *
 * project: org.matsim.*
 * DrawableLinkI.java
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

package org.matsim.utils.vis.netvis.drawableNet;

import java.util.Collection;

import org.matsim.utils.vis.netvis.DrawableAgentI;

public interface DrawableLinkI {

    public int getDisplayValueCount();

    public double getDisplayValue(int index);

    public String getDisplayText();

    public Collection<? extends DrawableAgentI> getMovingAgents();

}
