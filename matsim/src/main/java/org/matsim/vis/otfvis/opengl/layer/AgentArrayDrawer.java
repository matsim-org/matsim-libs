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
import java.awt.geom.Point2D;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.media.opengl.GL;

import org.matsim.vis.otfvis.OTFClientControl;
import org.matsim.vis.otfvis.opengl.drawer.OTFGLDrawableImpl;
import org.matsim.vis.otfvis.opengl.drawer.OTFOGLDrawer.AgentDrawer;

import com.sun.opengl.util.BufferUtil;
import com.sun.opengl.util.texture.Texture;

/**
 * 
 * This is just a helper class for the AgentPointLayer. It lives only for one timestep.
 *
 */
public final class AgentArrayDrawer {

	private int count = 0;
	
	private static int alpha =200;

	private ByteBuffer colorsIN = null;
	
	private FloatBuffer vertexIN = null;

	private List<FloatBuffer> posBuffers = new LinkedList<FloatBuffer>();
	
	private List<ByteBuffer> colBuffers= new LinkedList<ByteBuffer>();

	private Map<Integer,Integer> id2coord = new HashMap<Integer,Integer>();
	
	private Texture texture;

	public AgentArrayDrawer() {
		// Empty constructor
	}

	private void setTexture() {
		this.texture = AgentDrawer.agentpng;
	}

	private void setAgentSize() {
		float agentSize = OTFClientControl.getInstance().getOTFVisConfig().getAgentSize();
		if (OTFGLDrawableImpl.getGl().isFunctionAvailable("glPointParameterf")) {
			// Query for the max point size supported by the hardware
			float [] maxSize = {0.0f};
			OTFGLDrawableImpl.getGl().glGetFloatv( GL.GL_POINT_SIZE_MAX_ARB, FloatBuffer.wrap(maxSize) );
			float quadratic[] =  { 0.0f, 0.0001f, 0.000000f };
			OTFGLDrawableImpl.getGl().glPointParameterfvARB( GL.GL_POINT_DISTANCE_ATTENUATION_ARB, FloatBuffer.wrap(quadratic ));

			OTFGLDrawableImpl.getGl().glPointSize(agentSize/10.f);
			OTFGLDrawableImpl.getGl().glPointParameterf(GL.GL_POINT_SIZE_MIN_ARB, 1.f);
			OTFGLDrawableImpl.getGl().glPointParameterf(GL.GL_POINT_SIZE_MAX_ARB, agentSize*30.f);

		} else {
			OTFGLDrawableImpl.getGl().glPointSize(agentSize/10);
		}
	}

	private void drawArray(GL gl) {
		ByteBuffer colors =  null;
		FloatBuffer vertex =  null;
		for(int i = 0; i < this.getPosBuffers().size(); i++) {
			colors = this.colBuffers.get(i);
			vertex = this.getPosBuffers().get(i);
			int remain = i == this.getPosBuffers().size()-1 ? this.count %OGLAgentPointLayer.BUFFERSIZE : OGLAgentPointLayer.BUFFERSIZE; //Math.min(vertex.limit() / 2, count - i*BUFFERSIZE);
			colors.position(0);
			vertex.position(0);
			gl.glColorPointer (4, GL.GL_UNSIGNED_BYTE, 0, colors);
			gl.glVertexPointer (2, GL.GL_FLOAT, 0, vertex);
			gl.glDrawArrays (GL.GL_POINTS, 0, remain);
		}
	}

	public int getNearestAgent(Point2D.Double point) {
		FloatBuffer vertex =  null;

		int idx = 0;
		int result = -1;
		double mindist = Double.MAX_VALUE;
		double dist = 0;

		for(int i = 0; i < this.getPosBuffers().size(); i++) {
			vertex = this.getPosBuffers().get(i);
			vertex.position(0);
			while (vertex.hasRemaining() && (idx < this.count)) {
				float x = vertex.get();
				float y = vertex.get();
				// DS We do not need z value here but need to fetch it from buffer!
				/*float z = */vertex.get();

				// Manhattan dist reicht uns hier
				dist = Math.abs(x-point.x) + Math.abs(y-point.y);
				if ( dist < mindist) {
					mindist = dist;
					result = idx;
				}
				idx++;
			}
		}
		return result;
	}

	public void addAgent(char[] id, float startX, float startY, Color mycolor, boolean saveId){
		if (this.count % OGLAgentPointLayer.BUFFERSIZE == 0) {
			this.vertexIN = BufferUtil.newFloatBuffer(OGLAgentPointLayer.BUFFERSIZE*2);
			this.colorsIN = BufferUtil.newByteBuffer(OGLAgentPointLayer.BUFFERSIZE*4);
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

	public Map<Integer,Integer> getId2coord() {
		return id2coord;
	}

	public List<FloatBuffer> getPosBuffers() {
		return posBuffers;
	}

	public void draw() {
		GL gl = OTFGLDrawableImpl.getGl();
		gl.glEnable(GL.GL_POINT_SPRITE);
		
		this.setAgentSize();
		
		gl.glEnableClientState (GL.GL_COLOR_ARRAY);
		gl.glEnableClientState (GL.GL_VERTEX_ARRAY);
		
		this.setTexture();
		
		//texture = null;
		// setting the texture to null means that agents are painted using (software-rendered?) squares.  I have made speed
		// tests and found on my computer (mac powerbook, with "slow" graphics settings) no difference at all between "null"
		// and a jpg.  So no reason to not use a jpg/png.  kai, apr'10
		
		if (this.texture != null) {
			this.texture.enable();
			gl.glEnable(GL.GL_TEXTURE_2D);
			gl.glTexEnvf(GL.GL_POINT_SPRITE, GL.GL_COORD_REPLACE, GL.GL_TRUE);
			this.texture.bind();
		}
		gl.glDepthMask(false);
		
		this.drawArray(gl);
		
		gl.glDisableClientState (GL.GL_COLOR_ARRAY);
		gl.glDisableClientState (GL.GL_VERTEX_ARRAY);
		if (this.texture != null ) {
			this.texture.disable();
		}
		
		gl.glDisable(GL.GL_POINT_SPRITE);
	}

}