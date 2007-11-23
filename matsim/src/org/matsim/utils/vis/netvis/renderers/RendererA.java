/* *********************************************************************** *
 * project: org.matsim.*
 * RendererA.java
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

package org.matsim.utils.vis.netvis.renderers;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;

import org.matsim.utils.vis.netvis.VisConfig;
import org.matsim.utils.vis.netvis.gui.NetJComponent;

public abstract class RendererA {

    private VisConfig visConfig;

    private NetJComponent component;

    private RendererA prev;

    protected RendererA(VisConfig visConfig) {
        this.visConfig = visConfig;
        this.component = null;
        this.prev = null;
    }

    public void append(RendererA rendererBelow) {
        if (this.prev == null)
            this.prev = rendererBelow;
        else
            this.prev.append(rendererBelow);
    }

     public void setTargetComponent(NetJComponent component) {
        if (this.prev != null)
            this.prev.setTargetComponent(component);

        this.component = component;
    }

    public void render(Graphics2D display, AffineTransform boxTransform) {
        if (prev != null)
            prev.render(display, boxTransform);

        if (component != null)
            myRendering(display, boxTransform);
    }

    protected NetJComponent getNetJComponent() {
        return component;
    }

    protected VisConfig getVisConfig() {
        return visConfig;
    }

    protected abstract void myRendering(Graphics2D display, AffineTransform boxTransform);

}
