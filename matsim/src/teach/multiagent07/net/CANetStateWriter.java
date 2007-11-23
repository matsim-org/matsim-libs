/* *********************************************************************** *
 * project: org.matsim.*
 * CANetStateWriter.java
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

package teach.multiagent07.net;
import java.util.Collection;

import org.matsim.basic.v01.BasicLink;
import org.matsim.interfaces.networks.basicNet.BasicLinkI;
import org.matsim.interfaces.networks.basicNet.BasicNetI;
import org.matsim.interfaces.networks.basicNet.BasicNodeI;
import org.matsim.utils.vis.netvis.DisplayNetStateWriter;
import org.matsim.utils.vis.netvis.DrawableAgentI;

public class CANetStateWriter extends DisplayNetStateWriter {

	public CANetStateWriter(BasicNetI network, String networkFileName, String filePrefix, int timeStepLength_s, int bufferSize) {
		super(network, networkFileName, filePrefix, timeStepLength_s, bufferSize);
	}
	@Override
	protected Collection<DrawableAgentI> getAgentsOnLink(BasicLinkI link) {
		return ((CANetStateWritableI)link).getDisplayAgents();
	}
	@Override
	protected String getLinkDisplLabel(BasicLinkI link) {
		return ((BasicLink)link).getId().toString();
	}
	@Override
	protected double getLinkDisplValue(BasicLinkI link) {
		return ((CANetStateWritableI)link).getDisplayValue();
	}
	public static CANetStateWriter createWriter(BasicNetI network, String netFile, String outputFile) {
		return new CANetStateWriter(network, netFile, outputFile, 60, 300);
	}

	@Override
	protected String getNodeDisplLabel(BasicNodeI node) {
		return ((CANode)node).getId().toString();
	}

	public static class AgentOnLink implements DrawableAgentI {
		public double posInLink_m;
		public double getPosInLink_m() {
			return posInLink_m;
		}
		public int getLane() {
			return 1;
		}
		AgentOnLink(double position) {
			posInLink_m = position;
		}
	}


}
