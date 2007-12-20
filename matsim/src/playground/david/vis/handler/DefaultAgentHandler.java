/* *********************************************************************** *
 * project: org.matsim.*
 * DefaultAgentHandler.java
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

import org.matsim.mobsim.snapshots.PositionInfo;
import org.matsim.mobsim.snapshots.PositionInfo.VehicleState;

import playground.david.vis.OTFParamProviderA;
import playground.david.vis.OTFVisNet;
import playground.david.vis.interfaces.OTFAgentHandler;

public class DefaultAgentHandler extends OTFParamProviderA implements  OTFAgentHandler<PositionInfo> {

	static private OTFVisNet visnet = null;
	
	protected float x, y, color;
	protected int state;
	protected String id="not def.";
	
	public void readAgent(DataInputStream in) throws IOException {
		id = in.readUTF();
		x = in.readFloat();
		y = in.readFloat();
		state = in.readInt();
		// Convert to km/h 
		color = in.readFloat()*3.6f;
 	}

	public void writeAgent(PositionInfo pos, DataOutputStream out) throws IOException {
		out.writeUTF(pos.getAgentId().toString());
		//out.writeFloat((float)(pos.getDistanceOnLink()/pos.getLink().getLength()));
		out.writeFloat((float)((pos.getEasting()-visnet.minEasting())*OTFVisNet.zoomFactorX));
		out.writeFloat((float)((pos.getNorthing()-visnet.minNorthing())*OTFVisNet.zoomFactorY));
		out.writeInt(pos.getVehicleState()== VehicleState.Parking ? 1:0);
		out.writeFloat((float)pos.getSpeed());
	}

	@Override
	public int getIntParam(int index) throws UnsupportedOperationException {
		switch(index) {
		case 3: return state;
		};
		return 0; // throw exception here
	}

	@Override
	public float getFloatParam(int index) throws UnsupportedOperationException {
		switch(index) {
		case 0: return x;
		case 1: return y;
		case 2: return color;
		};
		return 0; // throw exception here
	}

	@Override
	public String getStringParam(int index) throws UnsupportedOperationException {
		return id;
	}

	public int getParamCount() {
		return 5;
	}
	public final String getLongName(int index) {
		switch(index) {
		case 0: return "PosX";
		case 1: return "PosY";
		case 2: return "Color";
		case 3: return "State";
		case 4: return "AgentId";
		};
		return null; // throw exception here
	}

	public void setOTFNet(OTFVisNet net) {
		visnet = net;
	}
}

