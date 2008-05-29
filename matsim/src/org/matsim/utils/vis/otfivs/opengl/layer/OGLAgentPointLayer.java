/* *********************************************************************** *
 * project: org.matsim.*
 * OGLAgentPointLayer.java
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

package org.matsim.utils.vis.otfivs.opengl.layer;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.media.opengl.GL;

import org.matsim.gbl.Gbl;
import org.matsim.utils.vis.otfivs.caching.DefaultSceneLayer;
import org.matsim.utils.vis.otfivs.caching.SceneGraph;
import org.matsim.utils.vis.otfivs.data.OTFDataSimpleAgent;
import org.matsim.utils.vis.otfivs.gui.OTFVisConfig;
import org.matsim.utils.vis.otfivs.opengl.drawer.OTFGLDrawableImpl;
import org.matsim.utils.vis.otfivs.opengl.drawer.OTFOGLDrawer;
import org.matsim.utils.vis.otfivs.opengl.drawer.OTFOGLDrawer.AgentDrawer;
import org.matsim.utils.vis.otfivs.opengl.drawer.OTFOGLDrawer.RandomColorizer;

import com.sun.opengl.util.BufferUtil;

public class OGLAgentPointLayer extends DefaultSceneLayer {
	private final static OTFOGLDrawer.FastColorizer colorizer = new OTFOGLDrawer.FastColorizer(
			new double[] { 0.0, 30., 50.}, new Color[] {
					Color.RED, Color.YELLOW, Color.GREEN});
	// for Padang time-based agents
	private final static OTFOGLDrawer.FastColorizer colorizer3 = new OTFOGLDrawer.FastColorizer(
			new double[] { 0.0, 30., 120.}, new Color[] {
					Color.GREEN, Color.YELLOW, Color.RED});
	
	public static class AgentArrayDrawer extends OTFGLDrawableImpl {
		public final static RandomColorizer colorizer2 = new RandomColorizer(256);


		private static final byte[] colBuf = new byte[5000000*4];
		private static final float[] vertBuf = new float [5000000*2];
		
		public int count = 0;
		
		private final ByteBuffer colorsIN =  ByteBuffer.wrap(colBuf);
		private final FloatBuffer vertexIN =  FloatBuffer.wrap(vertBuf);
		
		private ByteBuffer colors =  null;
		private FloatBuffer vertex =  null;
				
		private final Map<Integer,Integer> id2coord = new HashMap<Integer,Integer>();
		
		public AgentArrayDrawer() {
		}
		
		
		public void onDraw(GL gl) {
			if ( vertex == null) return ;
			
			float agentSize = ((OTFVisConfig)Gbl.getConfig().getModule(OTFVisConfig.GROUP_NAME)).getAgentSize();
			
			//System.out.println("Count veh = " + count);

			colors.position(0);
			vertex.position(0);
			
			gl.glEnable(GL.GL_POINT_SPRITE_ARB);


		    if (gl.isFunctionAvailable("glPointParameterfvARB")) {
				// Query for the max point size supported by the hardware
			    float [] maxSize = {0.0f};
			    gl.glGetFloatv( GL.GL_POINT_SIZE_MAX_ARB, FloatBuffer.wrap(maxSize) );
			    float quadratic[] =  { 0.0f, 0.0f, 0.0001f };
			    gl.glPointParameterfvARB( GL.GL_POINT_DISTANCE_ATTENUATION_ARB, FloatBuffer.wrap(quadratic ));

			    gl.glPointSize(agentSize);
		        gl.glPointParameterf(GL.GL_POINT_SIZE_MIN_ARB, 10);
		        gl.glPointParameterf(GL.GL_POINT_SIZE_MAX_ARB, 100);
		    	
		    } else {
		    	gl.glPointSize(agentSize/10);
		    }
			
	        gl.glEnableClientState (GL.GL_COLOR_ARRAY);
	        gl.glEnableClientState (GL.GL_VERTEX_ARRAY);   
	        
			AgentDrawer.carjpg.enable();
			gl.glEnable(GL.GL_TEXTURE_2D);
			gl.glTexEnvf(GL.GL_POINT_SPRITE_ARB, GL.GL_COORD_REPLACE_ARB, GL.GL_TRUE);
			AgentDrawer.carjpg.bind();
			

	        gl.glColorPointer (4, GL.GL_UNSIGNED_BYTE, 0, colors);
	        gl.glVertexPointer (2, GL.GL_FLOAT, 0, vertex);      
	        gl.glDrawArrays (GL.GL_POINTS, 0, count);

	        gl.glDisableClientState (GL.GL_COLOR_ARRAY);
	        gl.glDisableClientState (GL.GL_VERTEX_ARRAY); 

	        AgentDrawer.carjpg.disable();
	        
	        gl.glDisable(GL.GL_POINT_SPRITE_ARB);
		
		}

		public void addAgent(char[] id, float startX, float startY, Color mycolor, boolean saveId){
			vertexIN.put(startX);
			vertexIN.put(startY);
			//vertexIN.put(0.f);
			if (saveId) id2coord.put(Arrays.hashCode(id),count);
			
			colorsIN.put( (byte)mycolor.getRed());
			colorsIN.put( (byte)mycolor.getGreen());
			colorsIN.put((byte)mycolor.getBlue());
			colorsIN.put( (byte)120);
			
			count++;
			
		}
		
		public void addAgent(char[] id, float startX, float startY, int state, float color) {
//			Color mycolor = colorizer.getColor(0.1 + 0.9*color);
			Color mycolor = colorizer2.getColor(state);
			addAgent(id, startX, startY, mycolor, true);
		}

		public void compress() {
			if (vertexIN.position() == 0) return;
			
			int newVSize = 0, newCSize = 0;
			if (vertex != null) {
				newVSize = vertex.capacity();
				newCSize = colors.capacity();
			}

			newVSize += vertexIN.position();
			newCSize += colorsIN.position();
			
			FloatBuffer newVertex = BufferUtil.newFloatBuffer(newVSize);
			ByteBuffer newColor = BufferUtil.newByteBuffer(newCSize);
			if (vertex != null) {
				vertex.position(0);
				newVertex.put(vertex);
				colors.position(0);
				newColor.put(colors);
			}
			vertexIN.limit(vertexIN.position());
			vertexIN.position(0);
			newVertex.put(vertexIN);
			colorsIN.limit(colorsIN.position());
			colorsIN.position(0);
			newColor.put(colorsIN);
			
			colors = newColor;
			vertex = newVertex;
			vertexIN.clear();
			colorsIN.clear();
		}
		
	}
	
	public class AgentPointDrawer extends OTFGLDrawableImpl implements OTFDataSimpleAgent.Receiver {
	
		public void setAgent(char[] id, float startX, float startY, int state, int user, float color) {
			drawer.addAgent(id, startX, startY, colorizer.getColor(0.1 + 0.9*color), true);
		}
	
		@Override
		public void invalidate(SceneGraph graph) {
		}
	
		public void onDraw(GL gl) {
		}
		
	}

	public class AgentPadangRegionDrawer extends AgentPointDrawer {
		@Override
		public void setAgent(char[] id, float startX, float startY, int state, int user, float color) {
			drawer.addAgent(id, startX, startY, AgentArrayDrawer.colorizer2.getColor(user), false);
		}
	
	}

	public class AgentPadangTimeDrawer extends AgentPointDrawer {
		@Override
		public void setAgent(char[] id, float startX, float startY, int state, int user, float color) {
			drawer.addAgent(id, startX, startY, colorizer3.getColor(state),false);
		}
	
	}

	private final AgentArrayDrawer drawer = new AgentArrayDrawer();
	private final AgentPointDrawer pointdrawer = this.new AgentPointDrawer();
	private final AgentPadangTimeDrawer timedrawer = this.new AgentPadangTimeDrawer();
	private final AgentPadangRegionDrawer regiondrawer = this.new AgentPadangRegionDrawer();

	@Override
	public void draw() {
		drawer.draw();
	}

	@Override
	public void finish() {
		drawer.compress();
	}

	@Override
	public void init(SceneGraph graph) {
	}

	/* (non-Javadoc)
	 * @see playground.david.vis.data.DefaultSceneLayer#newInstance(java.lang.Class)
	 */
	@Override
	public Object newInstance(Class clazz) throws InstantiationException, IllegalAccessException {
//		AgentPointDrawer drawer = (AgentPointDrawer)clazz.newInstance(this);
		if (clazz == AgentPadangTimeDrawer.class) return timedrawer;
		else if (clazz == AgentPadangRegionDrawer.class) return regiondrawer;
		else return pointdrawer;
	}

	public int getDrawOrder() {
		// TODO Auto-generated method stub
		return 100;
	}

	public int getAgentIndex(Point2D.Double point) {
		int count = 0;
		int result = -1;
		double mindist = Double.MAX_VALUE;
		double dist = 0;
		
		drawer.vertex.position(0);
		while (drawer.vertex.hasRemaining()) {
			float x = drawer.vertex.get();
			float y = drawer.vertex.get();
			// DS We do not need z value here but need to fetch it from buffer!
			float z =  drawer.vertex.get();
			
			// Manhattan dist reicht uns hier
			dist = Math.abs(x-point.x) + Math.abs(y-point.y);
			if ( dist < mindist) {
				mindist = dist;
				result = count;
			}
			count++;
		}
		return result;
	}
	
	
	
	public Point2D.Double getAgentCoords(char [] id) {
		Point2D.Double point = null;
		int idNr = Arrays.hashCode(id);
		Integer i = drawer.id2coord.get(idNr);
		if (i != null) {
			float x = drawer.vertex.get(i*3);
			float y = drawer.vertex.get(i*3+1);
			return new Point2D.Double(x,y);
		} else return null;
		
	}
}
