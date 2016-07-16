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

import java.awt.Color;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.vis.otfvis.OTFClientControl;
import org.matsim.vis.otfvis.SimulationViewForQueries;
import org.matsim.vis.otfvis.data.OTFServerQuadTree;
import org.matsim.vis.otfvis.interfaces.OTFQuery;
import org.matsim.vis.otfvis.interfaces.OTFQueryResult;
import org.matsim.vis.otfvis.opengl.drawer.OTFGLAbstractDrawable;
import org.matsim.vis.otfvis.opengl.drawer.OTFOGLDrawer;

import com.jogamp.common.nio.Buffers;


/**
 * @author dgrether
 *
 */
public class QueryLinkById extends AbstractQuery implements OTFQuery {

	private static final Logger log = Logger.getLogger(QueryLinkById.class);
	
	private List<Id<Link>> linkIds;

	private transient Result result;

	@Override
	public void setId(String id) {
		this.linkIds = QueryUtils.parseIds(id, Link.class);
	}

	@Override
	public void installQuery(SimulationViewForQueries simulationView) {
		Network net = simulationView.getNetwork();
		this.fillResult(net);
	}

	private void fillResult(Network net){
		this.result = new Result();
		if (this.linkIds.size() == 0) return;

		//get the links from the network
		List<Link> links = new ArrayList<Link>();
		for (Id<Link> linkid : this.linkIds){
			Link link = net.getLinks().get(linkid);
			if (link != null){
				links.add(link);
				log.info("link id " + linkid + " found in network.");
			}
			else {
				log.info("link id " + linkid + " not found in network.");
			}
		}
		//fill the result
		float[] vertex = new float[links.size()*4];
		byte[] colors = new byte[links.size()*8];
		
		int pos = 0;
		Coord fromCoord, toCoord;
		Color arielUltra = Color.RED;
		for (Link l : links){
			fromCoord = OTFServerQuadTree.getOTFTransformation().transform(l.getFromNode().getCoord());
			toCoord = OTFServerQuadTree.getOTFTransformation().transform(l.getToNode().getCoord());
			vertex[pos  * 4 + 0] = (float)fromCoord.getX();
			vertex[pos  * 4 + 1] = (float)fromCoord.getY();
			vertex[pos  * 4 + 2] = (float)toCoord.getX();
			vertex[pos  * 4 + 3] = (float)toCoord.getY();
			colors[pos * 8 + 0] = (byte) arielUltra.getRed();
			colors[pos * 8 + 1] = (byte) arielUltra.getGreen();
			colors[pos * 8 + 2] = (byte) arielUltra.getBlue();
			colors[pos * 8 + 3] = (byte) 32;
			colors[pos * 8 + 4] = (byte) arielUltra.getRed();
			colors[pos * 8 + 5] = (byte) arielUltra.getGreen();
			colors[pos * 8 + 6] = (byte) arielUltra.getBlue();
			colors[pos * 8 + 7] = (byte) 128;
			pos++;
		}
		this.result.vertex = vertex;
		this.result.colors = colors;
		
	}
	
	
	@Override
	public void uninstall() {
		this.linkIds.clear();
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
		
		float[] vertex = null;
		byte[] colors = null;
		private transient ByteBuffer cols;
		private transient FloatBuffer vert;

		@Override
		public void draw(OTFOGLDrawer drawer) {
			if(this.vertex == null) return;
			GL2 gl = OTFGLAbstractDrawable.getGl();
			this.vert = Buffers.copyFloatBuffer(FloatBuffer.wrap(this.vertex));
			this.cols = Buffers.copyByteBuffer(ByteBuffer.wrap(this.colors));
			rewindGLBuffers();
			prepare(gl);
			unPrepare(gl);

		}

		private void prepare(GL2 gl) {
			gl.glEnable(GL2.GL_BLEND);
			gl.glEnable(GL2.GL_LINE_SMOOTH);
			gl.glEnableClientState(GL2.GL_COLOR_ARRAY);
			gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
			gl.glLineWidth(1.f * getLineWidth());
			gl.glColorPointer(4, GL2.GL_UNSIGNED_BYTE, 0, this.cols);
			gl.glVertexPointer(2, GL2.GL_FLOAT, 0, this.vert);
			gl.glDrawArrays(GL2.GL_LINES, 0, this.vertex.length);
			gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
			gl.glDisableClientState(GL2.GL_COLOR_ARRAY);
			gl.glDisable(GL2.GL_LINE_SMOOTH);
		}

		private void unPrepare(GL gl) {
			gl.glDisable(GL.GL_BLEND);
		}

		private float getLineWidth() {
			return OTFClientControl.getInstance().getOTFVisConfig()
					.getLinkWidth();
		}
		
		private void rewindGLBuffers() {
			vert.position(0);
			cols.position(0);
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
