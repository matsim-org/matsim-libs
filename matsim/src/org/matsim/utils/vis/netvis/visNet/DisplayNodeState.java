/* *********************************************************************** *
 * project: org.matsim.*
 * DisplayNodeState.java
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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.matsim.utils.vis.netvis.drawableNet.DrawableNodeI;
import org.matsim.utils.vis.netvis.streaming.BufferedStateA;

public final class DisplayNodeState extends BufferedStateA {

    // -------------------- MEMBER VARIABLES --------------------

    private DrawableNodeI node;

    // -------------------- CONSTRUCTION --------------------

    public DisplayNodeState(DrawableNodeI node) {
        super();
        this.node = node;
    }

    // -------------------- FROM/TO STREAM --------------------

    public final void writeMyself(DataOutputStream out) throws IOException {
        /*
         * (1) write display value
         */
        out.writeFloat((float) node.getDisplayValue());
        /*
         * (3) write display text
         */
        String displLabel = node.getDisplayText();
        if (displLabel == null)
            out.writeUTF("");
        else
            out.writeUTF(displLabel);
    }

    public final void readMyself(DataInputStream in) throws IOException {
        DisplayNode displNode = (DisplayNode) node;
        /*
         * (2) read display value
         */
        displNode.setDisplayValue(in.readFloat());
        /*
         * (3) read display text
         */
        displNode.setDisplayText(in.readUTF());
    }

}
