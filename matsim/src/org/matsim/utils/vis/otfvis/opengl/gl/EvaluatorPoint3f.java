/* *********************************************************************** *
 * project: org.matsim.*
 * EvaluatorPoint3f.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.utils.vis.otfvis.opengl.gl;

import org.jdesktop.animation.timing.interpolation.Evaluator;

public class EvaluatorPoint3f extends Evaluator<Point3f> {
    public Point3f evaluate(Point3f v0, Point3f v1, float fraction) {
        float x = v0.getX() + ((v1.getX() - v0.getX()) * fraction + .5f);
        float y = v0.getY() + ((v1.getY() - v0.getY()) * fraction + .5f);
        float z = v0.getZ() + ((v1.getZ() - v0.getZ()) * fraction + .5f);
        return new Point3f(x, y, z);
    }
}
