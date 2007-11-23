/* *********************************************************************** *
 * project: org.matsim.*
 * DefaultNodeHandler.java
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

import org.matsim.mobsim.QueueNode;

import playground.david.vis.OTFParamProviderA;
import playground.david.vis.interfaces.OTFNodeHandler;

public class DefaultNodeHandler extends OTFParamProviderA implements  OTFNodeHandler<QueueNode> {

	float value;
	String text;
	
	public void readNode(DataInputStream in) throws IOException {
		value = in.readFloat();
		text = in.readUTF();
	}

    public void writeNode(QueueNode node, DataOutputStream out) throws IOException {
        out.writeFloat((float)0.);
        out.writeUTF("");
    }
	
	public float getFloatParam(int index) throws UnsupportedOperationException {
		switch(index) {
		case 1: return value;
		};
		return 0; // throw exception here
	}

	public String getStringParam(int index) throws UnsupportedOperationException {
		return text;
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
