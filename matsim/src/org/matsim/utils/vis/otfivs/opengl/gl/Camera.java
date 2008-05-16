/**
 * Copyright (c) 2007, Sun Microsystems, Inc.
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 *   * Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above
 *     copyright notice, this list of conditions and the following 
 *     disclaimer in the documentation and/or other materials provided 
 *     with the distribution.
 *   * Neither the name of the BezierAnim3D project nor the names of its
 *     contributors may be used to endorse or promote products derived 
 *     from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

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
