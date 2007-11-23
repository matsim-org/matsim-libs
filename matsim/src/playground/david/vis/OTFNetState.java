/* *********************************************************************** *
 * project: org.matsim.*
 * OTFNetState.java
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

package playground.david.vis;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.matsim.mobsim.QueueLink;
import org.matsim.mobsim.QueueNode;
import org.matsim.mobsim.snapshots.PositionInfo;
import org.matsim.utils.vis.netvis.visNet.DisplayAgent;

public class OTFNetState {
	
	private final OTFVisNet indexNet;
	public OTFNetState(OTFVisNet net) {
		this.indexNet = net;
	}

	

	public final void readLink(OTFVisNet.Link displLink, DataInputStream in) throws IOException {
		int valueCnt = in.readInt();
		displLink.setDisplayValue(in.readFloat());
		displLink.setDisplayLabel(in.readUTF());
		List agentsNow = new ArrayList();
		int agentCnt = in.readInt();

		for (int i = 0; i < agentCnt; i++) {
			String id = in.readUTF();
			double posInLink_m = in.readFloat();
			int lane = in.readInt();

			agentsNow.add(new DisplayAgent(posInLink_m, lane));
		}
		//displLink.setMovingAgents(agentsNow);

	}

	  /**
     * Not to be called by extending classes.
     */
	Collection<PositionInfo> positions = new ArrayList<PositionInfo>();
    public void writeLink(QueueLink link, DataOutputStream out) throws IOException {
        /*
         * (1) write display value count
         */
        out.writeInt(1);
        double value = link.getDisplayableSpaceCapValue();
        out.writeFloat((float) value);

        /* (3) write display text         */
        String displText = link.getId().toString();
        if (displText == null)
            out.writeUTF("");
        else
            out.writeUTF(displText);
        /*
         * (4) write agents
         */
        positions.clear();
		link.getVehiclePositions(positions);
		out.writeInt(positions.size());
		
		for (PositionInfo pos : positions) {
			out.writeUTF(pos.getAgentId().toString());
			out.writeFloat((float)pos.getDistanceOnLink());
			out.writeInt(1);
		}

    }
	
    public final void readNode(OTFVisNet.Node displNode, DataInputStream in) throws IOException {
		in.readInt();
		displNode.setDisplayValue(in.readFloat());
		displNode.setDisplayText(in.readUTF());
	}

	
    public void writeNode(QueueNode node, DataOutputStream out) throws IOException {
		out.writeInt(0);
        out.writeFloat((float)0.);
        out.writeUTF("");
    }

    public void readMyself(DataInputStream in) throws IOException {
		
		for (OTFVisNet.Node node : indexNet.getNodes())
			if (node != null) {
				readNode(node, in);
			}else {
		        int length = in.readInt();
		        in.skipBytes(length);
			}

		for (OTFVisNet.Link link : indexNet.getLinks())
			if (link != null) {
				readLink(link, in);
			}else {
		        int length = in.readInt();
		        in.skipBytes(length);

			}

		return;
	}

    public void writeMyself(DataOutputStream out) throws IOException {
		for (OTFVisNet.Node node : indexNet.getNodes())
			if (node != null) {
				writeNode(node.getSrc(), out);
			}else {
				out.writeInt(0);
			}

		for (OTFVisNet.Link link : indexNet.getLinks())
			if (link != null) {
				writeLink(link.getSrc(), out);
			}else {
				out.writeInt(0);
			}

		return;
	}
}
