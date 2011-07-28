/* *********************************************************************** *
 * project: org.matsim.*
 * OTFLaneReader2
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.ByteBufferUtils;
import org.matsim.lanes.otfvis.drawer.OTFLaneSignalDrawer;
import org.matsim.vis.otfvis.caching.SceneGraph;
import org.matsim.vis.otfvis.data.OTFDataReceiver;
import org.matsim.vis.otfvis.interfaces.OTFDataReader;


/**
 * @author dgrether
 *
 */
public class OTFLaneReader extends OTFDataReader {
	
	protected OTFLaneSignalDrawer drawer;

	public OTFLaneReader(){
	}
	
	@Override
	public void readConstData(ByteBuffer in) throws IOException {
		int noLinks = in.getInt();
		Map<OTFLinkWLanes, List<String>> outLinks = new HashMap<OTFLinkWLanes, List<String>>();
		for (int i = 0; i < noLinks; i++){
			Tuple<OTFLinkWLanes, List<String>> ol = this.readVisLinkData(in);
			outLinks.put(ol.getFirst(), ol.getSecond());
		}
		int noLanes = in.getInt();
		for (int i = 0; i < noLanes; i++){
			this.readLaneData(in);
		}
		this.connectVisLinksWithoutLanes(outLinks);
	}
	
	private void connectVisLinksWithoutLanes(Map<OTFLinkWLanes, List<String>> outLinks) {
		for (Map.Entry<OTFLinkWLanes, List<String>> e : outLinks.entrySet()) {
			OTFLinkWLanes link = e.getKey();
			if (link.getLaneData() == null || link.getLaneData().isEmpty()) {
				for (String outLinkId : e.getValue()) {
					OTFLinkWLanes outlink = this.drawer.getLanesLinkData().get(outLinkId);
					link.addToLink(outlink);
				}
			}
		}
	}

	private Tuple<OTFLinkWLanes,List<String>> readVisLinkData(ByteBuffer in){
		//read link data
		String linkId = ByteBufferUtils.getString(in);
		OTFLinkWLanes lanesLinkData = new OTFLinkWLanes(linkId);
		this.drawer.addLaneLinkData(lanesLinkData);
		
		lanesLinkData.setLinkStart(in.getDouble(), in.getDouble());
		lanesLinkData.setLinkEnd(in.getDouble(), in.getDouble());
		lanesLinkData.setNormalizedLinkVector(in.getDouble(), in.getDouble());
		lanesLinkData.setLinkOrthogonalVector(in.getDouble(), in.getDouble());
		lanesLinkData.setNumberOfLanes(in.getDouble());
		int noOutLinks = in.getInt();
		List<String> outLinks = new ArrayList<String>(noOutLinks);
		for (int i = 0; i < noOutLinks; i++){
			String outLinkId = ByteBufferUtils.getString(in);
			outLinks.add(outLinkId);
		}
		Tuple<OTFLinkWLanes, List<String>> outLinkIds = new Tuple<OTFLinkWLanes, List<String>>(lanesLinkData, outLinks);
		return outLinkIds;
	}
	
	private void readLaneData(ByteBuffer in){
		String linkId = ByteBufferUtils.getString(in);
		OTFLinkWLanes laneLinkData = null;
		laneLinkData = this.drawer.getLanesLinkData().get(linkId);
		int noLanes = in.getInt();
		for (int i = 0; i < noLanes; i++){
			String laneId = ByteBufferUtils.getString(in);
			OTFLane data = new OTFLane(laneId);
			laneLinkData.addLaneData(data);
			data.setStartPosition(in.getDouble());
			data.setEndPosition(in.getDouble());
			data.setAlignment(in.getInt());
			data.setNumberOfLanes(in.getDouble());
		}
		laneLinkData.setMaximalAlignment(in.getInt());
		//read and process the connections
		if (OTFLaneWriter.DRAW_LINK_TO_LINK_LINES){
			for (int i = 0; i < noLanes; i++){
				String laneId = ByteBufferUtils.getString(in);
				OTFLane data = laneLinkData.getLaneData().get(laneId);
				this.readLaneToLinkConnections(in, data);
				this.readLaneToLaneConnections(in, data, laneLinkData);
			}
		}
	}
	
	private void readLaneToLaneConnections(ByteBuffer in, OTFLane data, OTFLinkWLanes lanesLinkData){
		int numberOfToLanes = in.getInt();
		for (int j = 0; j < numberOfToLanes; j++){
			String toLaneId = ByteBufferUtils.getString(in);
			OTFLane toLane = lanesLinkData.getLaneData().get(toLaneId);
			data.addToLane(toLane);
		}
	}
	
	private void readLaneToLinkConnections(ByteBuffer in, OTFLane data){
		int numberOfToLinks = in.getInt();
		for (int j = 0; j < numberOfToLinks; j++){
			String toLinkId = ByteBufferUtils.getString(in);
			OTFLinkWLanes toLink = this.drawer.getLanesLinkData().get(toLinkId);
			data.addToLink(toLink);
		}
	}
	
	@Override
	public void connect(OTFDataReceiver receiver) {
		this.drawer = (OTFLaneSignalDrawer) receiver;
	}

	@Override
	public void invalidate(SceneGraph graph) {
		this.drawer.invalidate(graph);
	}

	@Override
	public void readDynData(ByteBuffer in, SceneGraph graph) throws IOException {
		// nothing to do as lanes are non dynamical data
	}
}
