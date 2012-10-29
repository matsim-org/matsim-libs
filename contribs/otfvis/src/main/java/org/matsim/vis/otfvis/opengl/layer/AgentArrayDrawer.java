/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

/**
 * 
 */
package org.matsim.vis.otfvis.opengl.layer;

import java.awt.Color;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;

import org.apache.log4j.Logger;
import org.matsim.core.gbl.MatsimResource;
import org.matsim.vis.otfvis.OTFClientControl;
import org.matsim.vis.otfvis.caching.SceneGraph;
import org.matsim.vis.otfvis.opengl.drawer.OTFGLAbstractDrawable;
import org.matsim.vis.otfvis.opengl.drawer.OTFOGLDrawer;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.util.texture.Texture;

/**
 * 
 * This is a helper class for the AgentPointLayer. It lives only for one timestep.
 *
 */
final class AgentArrayDrawer extends OTFGLAbstractDrawable {

	private int count = 0;

	private static int alpha =200;

	private ByteBuffer colorsIN = null;

	private FloatBuffer vertexIN = null;

	private List<FloatBuffer> posBuffers = new LinkedList<FloatBuffer>();

	private List<ByteBuffer> colBuffers= new LinkedList<ByteBuffer>();

	private Map<Integer,Integer> id2coord = new HashMap<Integer,Integer>();

	private static Texture texture;

	private static final Logger log = Logger.getLogger(AgentArrayDrawer.class);

	@Override
	protected void onInit(GL2 gl) {
		texture = OTFOGLDrawer.createTexture(gl, MatsimResource.getAsInputStream("icon18.png"));
	}

	private static int infocnt = 0 ;
	private void drawArray(GL2 gl) {

		// testing if the point sprite is available.  Would be good to not do this in every time step ...
		// ... move to some earlier place in the calling hierarchy.  kai, feb'11
		if ( infocnt < 1 ) {
			infocnt++ ;
			String[] str = {"glDrawArrays", "glVertexPointer", "glColorPointer"} ;
			for ( int ii=0 ; ii<str.length ; ii++ ) {
				if ( gl.isFunctionAvailable(str[ii]) ) {
					log.info( str[ii] + " is available ") ;
				} else {
					log.warn( str[ii] + " is NOT available ") ;
				}
			}
		}

		ByteBuffer colors =  null;
		FloatBuffer vertex =  null;
		for(int i = 0; i < this.getPosBuffers().size(); i++) {
			colors = this.colBuffers.get(i);
			vertex = this.getPosBuffers().get(i);
			int remain = i == this.getPosBuffers().size()-1 ? this.count %OGLAgentPointLayer.BUFFERSIZE : OGLAgentPointLayer.BUFFERSIZE; 
			colors.position(0);
			vertex.position(0);
			gl.glColorPointer (4, GL.GL_UNSIGNED_BYTE, 0, colors);
			gl.glVertexPointer (2, GL.GL_FLOAT, 0, vertex);
			gl.glDrawArrays (GL.GL_POINTS, 0, remain);
		}
	}

	private static void setAgentSize(GL2 gl) {
		float agentSize = OTFClientControl.getInstance().getOTFVisConfig().getAgentSize() / 10.f;
		gl.glPointSize(agentSize);
	}

	void addAgent(char[] id, float startX, float startY, Color mycolor, boolean saveId){
		if (this.count % OGLAgentPointLayer.BUFFERSIZE == 0) {
			this.vertexIN = Buffers.newDirectFloatBuffer(OGLAgentPointLayer.BUFFERSIZE*2);
			this.colorsIN = Buffers.newDirectByteBuffer(OGLAgentPointLayer.BUFFERSIZE*4);
			this.colBuffers.add(this.colorsIN);
			this.getPosBuffers().add(this.vertexIN);
		}
		this.vertexIN.put(startX);
		this.vertexIN.put(startY);
		if (saveId) this.getId2coord().put(Arrays.hashCode(id),this.count);

		this.colorsIN.put( (byte)mycolor.getRed());
		this.colorsIN.put( (byte)mycolor.getGreen());
		this.colorsIN.put((byte)mycolor.getBlue());
		this.colorsIN.put( (byte)alpha);

		this.count++;
	}

	Map<Integer,Integer> getId2coord() {
		return id2coord;
	}

	List<FloatBuffer> getPosBuffers() {
		return posBuffers;
	}

	@Override
	public void onDraw(GL2 gl) {
		gl.glEnable(GL2.GL_POINT_SPRITE);

		setAgentSize(gl);

		gl.glEnableClientState (GL2.GL_COLOR_ARRAY);
		gl.glEnableClientState (GL2.GL_VERTEX_ARRAY);

		//texture = null;
		// setting the texture to null means that agents are painted using (software-rendered?) squares.  I have made speed
		// tests and found on my computer (mac powerbook, with "slow" graphics settings) no difference at all between "null"
		// and a jpg.  But it looks weird w/o some reasonable icon.  kai, jan'11

		if (texture != null) {
			texture.enable(gl);
			gl.glEnable(GL2.GL_TEXTURE_2D);
			gl.glTexEnvf(GL2.GL_POINT_SPRITE, GL2.GL_COORD_REPLACE, GL2.GL_TRUE);
			texture.bind(gl);
		}

		gl.glDepthMask(false);

		this.drawArray(gl);

		gl.glDisableClientState (GL2.GL_COLOR_ARRAY);
		gl.glDisableClientState (GL2.GL_VERTEX_ARRAY);
		if (texture != null ) {
			texture.disable(gl);
		}

		gl.glDisable(GL2.GL_POINT_SPRITE);
	}

	@Override
	public void addToSceneGraph(SceneGraph graph) {
		
	}

}