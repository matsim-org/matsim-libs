/* *********************************************************************** *
 * project: org.matsim.*
 * OTFLaneWriter2
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package org.matsim.lanes.otfvis.io;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.matsim.core.config.groups.OTFVisConfigGroup;
import org.matsim.core.utils.misc.ByteBufferUtils;
import org.matsim.lanes.data.v20.LaneDefinitions20;
import org.matsim.lanes.data.v20.LanesToLinkAssignment20;
import org.matsim.lanes.vis.VisLaneModelBuilder;
import org.matsim.lanes.vis.VisLinkWLanes;
import org.matsim.vis.otfvis.data.OTFDataWriter;
import org.matsim.vis.otfvis.data.OTFServerQuadTree;
import org.matsim.vis.snapshotwriters.VisLink;
import org.matsim.vis.snapshotwriters.VisNetwork;


/**
 * @author dgrether
 *
 */
public class OTFLaneWriter extends OTFDataWriter<Void> {

	private final transient VisNetwork network;

	private final transient LaneDefinitions20 lanes;
	
	private transient VisLaneModelBuilder laneModelBuilder = new VisLaneModelBuilder();

	private OTFVisConfigGroup otfVisConfig;
	
	public OTFLaneWriter(VisNetwork visNetwork, LaneDefinitions20 laneDefinitions, OTFVisConfigGroup otfVisConfigGroup){
		this.network = visNetwork;
		this.lanes = laneDefinitions;
		this.otfVisConfig = otfVisConfigGroup;
	}
	
	@Override
	public void writeConstData(ByteBuffer out) throws IOException {
		//write the data for the links
		out.putInt(this.network.getVisLinks().size());
		for (VisLink visLink : this.network.getVisLinks().values()) {
			LanesToLinkAssignment20 l2l = null;
			if (this.lanes != null){
				l2l = this.lanes.getLanesToLinkAssignments().get(visLink.getLink().getId());
			}
			VisLinkWLanes otfLink = this.laneModelBuilder.createOTFLinkWLanes(OTFServerQuadTree.getOTFTransformation(), visLink, otfVisConfig.getNodeOffset(), l2l);
			//write link data
			ByteBufferUtils.putObject(out, otfLink);
		}
	}
	
	@Override
	public void writeDynData(ByteBuffer out) throws IOException {
		//nothing to do as lanes are not dynamic
	}


}
