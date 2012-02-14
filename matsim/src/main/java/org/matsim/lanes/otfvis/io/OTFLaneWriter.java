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

import org.matsim.core.utils.misc.ByteBufferUtils;
import org.matsim.lanes.data.v20.LaneDefinitionsV2;
import org.matsim.lanes.data.v20.LanesToLinkAssignmentV2;
import org.matsim.lanes.otfvis.OTFLaneModelBuilder;
import org.matsim.vis.otfvis.data.OTFDataWriter;
import org.matsim.vis.otfvis.gui.OTFVisConfigGroup;
import org.matsim.vis.snapshotwriters.VisLink;
import org.matsim.vis.snapshotwriters.VisNetwork;


/**
 * @author dgrether
 *
 */
public class OTFLaneWriter extends OTFDataWriter<Void> {

	private final transient VisNetwork network;

	private final transient LaneDefinitionsV2 lanes;
	
	
	private OTFLaneModelBuilder laneModelBuilder = new OTFLaneModelBuilder();

	private OTFVisConfigGroup otfVisConfig;
	
	public OTFLaneWriter(VisNetwork visNetwork, LaneDefinitionsV2 laneDefinitions, OTFVisConfigGroup otfVisConfigGroup){
		this.network = visNetwork;
		this.lanes = laneDefinitions;
		this.otfVisConfig = otfVisConfigGroup;
	}
	
	@Override
	public void writeConstData(ByteBuffer out) throws IOException {
		//write the data for the links
		out.putInt(this.network.getVisLinks().size());
		for (VisLink visLink : this.network.getVisLinks().values()) {
			LanesToLinkAssignmentV2 l2l = this.lanes.getLanesToLinkAssignments().get(visLink.getLink().getId());
			OTFLinkWLanes otfLink = this.laneModelBuilder.createOTFLinkWLanes(visLink, otfVisConfig.getNodeOffset(), l2l);
			//write link data
			ByteBufferUtils.putObject(out, otfLink);
		}
	}
	
	@Override
	public void writeDynData(ByteBuffer out) throws IOException {
		//nothing to do as lanes are not dynamic
	}


}
