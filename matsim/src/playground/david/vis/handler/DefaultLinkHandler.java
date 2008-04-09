/* *********************************************************************** *
 * project: org.matsim.*
 * DefaultLinkHandler.java
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

package playground.david.vis.handler;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.matsim.mobsim.QueueLink;

import playground.david.vis.OTFParamProviderA;
import playground.david.vis.interfaces.OTFLinkHandler;

public class DefaultLinkHandler  extends OTFParamProviderA implements  OTFLinkHandler<QueueLink> {

	float value;
	String text;

	public void readLink(DataInputStream in) throws IOException {
		this.value = in.readFloat();
		this.text = in.readUTF();
	}

    public void writeLink(QueueLink link, DataOutputStream out) throws IOException {
        /*
         * (1) write display value count
         */
        double value = link.getDisplayableSpaceCapValue();
        out.writeFloat((float) value);

        /* (3) write display text         */
        String displText = link.getLink().getId().toString();
        if (displText == null)
            out.writeUTF("");
        else
            out.writeUTF(displText);
    }

	@Override
	public float getFloatParam(int index) throws UnsupportedOperationException {
		switch(index) {
		case 1: return this.value;
		};
		return 0; // throw exception here
	}

	@Override
	public String getStringParam(int index) throws UnsupportedOperationException {
		return this.text;
	}

	public int getParamCount() {
		return 2;
	}
	public final String getLongName(int index) {
		switch(index) {
		case 0: return "Text";
		case 1: return "Value";
		};
		return null; // throw exception here
	}
}
