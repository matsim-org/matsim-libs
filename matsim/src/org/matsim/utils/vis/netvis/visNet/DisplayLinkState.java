/* *********************************************************************** *
 * project: org.matsim.*
 * DisplayLinkState.java
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.matsim.utils.vis.netvis.DrawableAgentI;
import org.matsim.utils.vis.netvis.drawableNet.DrawableLinkI;
import org.matsim.utils.vis.netvis.streaming.BufferedStateA;

public final class DisplayLinkState extends BufferedStateA {

	// -------------------- MEMBER VARIABLES --------------------

	private DrawableLinkI link;

	// -------------------- CONSTRUCTION --------------------

	public DisplayLinkState(DrawableLinkI link) {
		super();
		this.link = link;
	}

	// -------------------- FROM/TO STREAM --------------------

	/**
	 * Not to be called by extending classes.
	 */
	@Override
	public final void writeMyself(DataOutputStream out) throws IOException {
		/*
		 * (1) write display value count
		 */
		int valueCnt = link.getDisplayValueCount();
		out.writeInt(valueCnt);
		/*
		 * (2) write the according number of display values
		 */
		for (int i = 0; i < valueCnt; i++)
			out.writeFloat((float) link.getDisplayValue(i));
		/*
		 * (3) write display text
		 */
		String displText = link.getDisplayText();
		if (displText == null) {
			out.writeUTF("");
		} else {
			out.writeUTF(displText);
		}
		/*
		 * (4) write agents
		 */
		Collection<? extends DrawableAgentI> agents = link.getMovingAgents();
		if (agents != null) {
			/*
			 * (4.1) write agent count
			 */
			out.writeInt(agents.size());

			for (DrawableAgentI agent : agents) {
				/*
				 * (4.2.1) write agent position in link
				 */
				out.writeFloat((float) agent.getPosInLink_m());
				/*
				 * (4.2.2) write agent lane
				 */
				out.writeInt(agent.getLane());
			}
		} else
			out.writeInt(0);
	}

	/**
	 * Not to be called by extending classes.
	 */
	@Override
	public final void readMyself(DataInputStream in) throws IOException {
		DisplayLink displLink = (DisplayLink) link;

		/*
		 * (1) read display value count
		 */
		int valueCnt = in.readInt();
		displLink.setDisplValueCnt(valueCnt);
		/*
		 * (2) read the according number of display values
		 */
		for (int i = 0; i < valueCnt; i++)
			displLink.setDisplayValue(in.readFloat(), i);
		/*
		 * (3) read display text
		 */
		displLink.setDisplayLabel(in.readUTF());
		/*
		 * (4) read agents
		 */
		List<DrawableAgentI> agentsNow = new ArrayList<DrawableAgentI>();
		// displLink.getMovingAgents().clear();
		/*
		 * (4.1) read agent count
		 */
		int agentCnt = in.readInt();

		for (int i = 0; i < agentCnt; i++) {
			/*
			 * (4.2.1) read agent position in link
			 */
			double posInLink_m = in.readFloat();
			/*
			 * (4.2.2) read agent lane
			 */
			int lane = in.readInt();

			// displLink.getMovingAgents()
			// .add(new DisplayAgent(posInLink_m, lane));
			agentsNow.add(new DisplayAgent(posInLink_m, lane));
		}
		displLink.setMovingAgents(agentsNow);

	}

}
