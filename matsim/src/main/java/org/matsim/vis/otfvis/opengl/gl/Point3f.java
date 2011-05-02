/* *********************************************************************** *
 * project: org.matsim.*
 * Point3f.java
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

package org.matsim.vis.otfvis.opengl.gl;

/**
 * Represents a very basic, immutable three-dimensional point.  (Since we
 * have limited use for it in this project, it's easier to roll our own than
 * to use the one defined in the javax.vecmath package.)
 *
 * @author Chris Campbell
 */
public class Point3f {
    
    public float x, y, z;
    
    public Point3f() {
    }
    
    public Point3f(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
    
    public float getX() {
        return x;
    }
    
    public float getY() {
        return y;
    }
    
    public float getZ() {
        return z;
    }
    
    public String toString() {
    	return "("+x+","+y+","+z+")";
    }
    
}
