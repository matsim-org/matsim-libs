/* *********************************************************************** *
 * project: org.matsim.*
 * QueryLinkById
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package org.matsim.vis.otfvis.opengl.queries;

import java.util.ArrayList;
import java.util.List;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.vis.otfvis.SimulationViewForQueries;
import org.matsim.vis.otfvis.data.OTFServerQuadTree;
import org.matsim.vis.otfvis.interfaces.OTFQuery;
import org.matsim.vis.otfvis.interfaces.OTFQueryResult;
import org.matsim.vis.otfvis.opengl.drawer.OTFGLAbstractDrawable;
import org.matsim.vis.otfvis.opengl.drawer.OTFOGLDrawer;
import org.matsim.vis.otfvis.opengl.gl.DrawingUtils;


/**
 * @author dgrether
 *
 */
public class QueryNodeById extends AbstractQuery implements OTFQuery {

	private static final Logger log = Logger.getLogger(QueryNodeById.class);
	
	private List<Id<Node>> nodeIds;

	private transient Result result;

	@Override
	public void setId(String id) {
		this.nodeIds = QueryUtils.parseIds(id, Node.class);
	}

	@Override
	public void installQuery(SimulationViewForQueries simulationView) {
		Network net = simulationView.getNetwork();
		this.fillResult(net);
	}

	private void fillResult(Network net){
		this.result = new Result();
		if (this.nodeIds.size() == 0) return;

		//get the nodes from the network
		List<Node> nodes = new ArrayList<Node>();
		for (Id<Node> id : this.nodeIds){
			Node node = net.getNodes().get(id);
			if (node != null){
				nodes.add(node);
				log.info("Node id " + id + " found in network.");
			}
			else {
				log.info("Node id " + id + " not found in network.");
			}
		}
		List<Coord> coords = new ArrayList<Coord>();
		for (Node l : nodes){
			Coord coord = OTFServerQuadTree.getOTFTransformation().transform(l.getCoord());
			coords.add(coord);
		}
		this.result.coords = coords;
	}
	
	
	@Override
	public void uninstall() {
		this.nodeIds.clear();
	}

	@Override
	public Type getType() {
		return OTFQuery.Type.LINK;
	}

	@Override
	public OTFQueryResult query() {
		return this.result;
	}

	private static final class Result implements OTFQueryResult{

		private List<Coord> coords;

		@Override
		public void draw(OTFOGLDrawer drawer) {
			GL2 gl = OTFGLAbstractDrawable.getGl();
			gl.glColor3d(1.0, 0.0, 0.0);
			gl.glEnable(GL.GL_BLEND);
			gl.glEnable(GL.GL_LINE_SMOOTH);
			for (Coord c : coords){
				DrawingUtils.drawCircle(gl, (float) c.getX(), (float) c.getY(), 100);
			}
			gl.glDisable(GL.GL_LINE_SMOOTH);
			gl.glDisable(GL.GL_BLEND);

		}

		@Override
		public void remove() {
			
		}

		@Override
		public boolean isAlive() {
			return true;
		}
		
	}
	
}
