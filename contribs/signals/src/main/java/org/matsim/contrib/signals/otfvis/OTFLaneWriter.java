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
package org.matsim.contrib.signals.otfvis;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import org.matsim.core.config.Config;
import org.matsim.core.utils.misc.ByteBufferUtils;
import org.matsim.lanes.ModelLane;
import org.matsim.lanes.Lanes;
import org.matsim.lanes.LanesToLinkAssignment;
import org.matsim.lanes.LanesUtils;
import org.matsim.lanes.VisLaneModelBuilder;
import org.matsim.lanes.VisLinkWLanes;
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

	private final transient Lanes lanes;
	
	private transient VisLaneModelBuilder laneModelBuilder = new VisLaneModelBuilder();

	private Config config;
	
	public OTFLaneWriter(VisNetwork visNetwork, Lanes laneDefinitions, Config config){
		this.network = visNetwork;
		this.lanes = laneDefinitions;
		this.config = config;
	}
	
	@Override
	public void writeConstData(ByteBuffer out) throws IOException {
		//write the data for the links
		out.putInt(this.network.getVisLinks().size());
		for (VisLink visLink : this.network.getVisLinks().values()) {
			LanesToLinkAssignment l2l = null;
			if (this.lanes != null){
				l2l = this.lanes.getLanesToLinkAssignments().get(visLink.getLink().getId());
			}
			List<ModelLane> la = null;
			if (l2l != null) {
				la = LanesUtils.createLanes(visLink.getLink(), l2l);
			}
			VisLinkWLanes otfLink = this.laneModelBuilder.createVisLinkLanes(OTFServerQuadTree.getOTFTransformation(), visLink, config.qsim().getNodeOffset(), la);
			//write link data
			ByteBufferUtils.putObject(out, otfLink);
		}
	}
	
	@Override
	public void writeDynData(ByteBuffer out) throws IOException {
		//nothing to do as lanes are not dynamic
	}


}
