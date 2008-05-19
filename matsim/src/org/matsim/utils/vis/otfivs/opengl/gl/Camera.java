/* *********************************************************************** *
 * project: org.matsim.*
 * Camera.java
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

package org.matsim.utils.vis.otfivs.opengl.gl;

import javax.media.opengl.GL;
import javax.media.opengl.glu.GLU;

import org.matsim.utils.vis.otfivs.opengl.gl.Point3f;



/**
 * A simple representation of a camera, which allows the user to easily
 * position the viewpoint in 3D space and to aim the viewpoint at a
 * specific target.
 *
 * @author Chris Campbell
 */
public class Camera {
    
    private Point3f location;
    private Point3f target;
    private final Point3f targetOffset;
 
    public Camera() {
        location = new Point3f();
        target = new Point3f();
        targetOffset = new Point3f();
    }
    
    public void setup(GL gl, GLU glu) {
        glu.gluLookAt(location.getX(), location.getY(), location.getZ(),
                      target.getX() + targetOffset.getX(), target.getY() + targetOffset.getY(), target.getZ() + targetOffset.getZ(),
                      0.0f, 1.0f, 0.0f);
    }
    
    public Point3f getLocation() {
        return location;
    }
    
    public void setLocation(Point3f location) {
        this.location = location;
    }
    
    public Point3f getTarget() {
        return target;
    }
    
    public void setTarget(Point3f target) {
        this.target = target;
    }
}
