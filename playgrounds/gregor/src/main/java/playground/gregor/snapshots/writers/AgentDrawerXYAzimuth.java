/* *********************************************************************** *
 * project: org.matsim.*
 * AgentDrawerXYAzimuth.java
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
package playground.gregor.snapshots.writers;

import static javax.media.opengl.GL.GL_MODELVIEW_MATRIX;
import static javax.media.opengl.GL.GL_PROJECTION_MATRIX;
import static javax.media.opengl.GL.GL_QUADS;
import static javax.media.opengl.GL.GL_VIEWPORT;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import javax.media.opengl.GL;
import javax.media.opengl.glu.GLU;

import org.matsim.core.gbl.MatsimResource;
import org.matsim.vis.otfvis.OTFClientControl;
import org.matsim.vis.otfvis.opengl.drawer.OTFGLAbstractDrawableReceiver;
import org.matsim.vis.otfvis.opengl.drawer.OTFOGLDrawer;

import com.sun.opengl.util.texture.Texture;
import com.sun.opengl.util.texture.TextureCoords;

public class AgentDrawerXYAzimuth extends OTFGLAbstractDrawableReceiver  {


	final static OTFOGLDrawer.FastColorizer colorizer = new OTFOGLDrawer.FastColorizer(
			new double[] { 0.0, 30., 120., 255. ,256.}, new Color[] {	Color.GREEN, Color.YELLOW, Color.RED, Color.RED, Color.BLUE});


	private static final int MAX_DRAW = 15000;

	private final Stack<AgentInfo> ais = new Stack<AgentInfo>();

	private boolean initialized = false;

	protected Texture texture;

	double[] modelview = new double[16];
	double[] projection = new double[16];
	int[] viewport = new int[4];

	private static final float ALPHA = 1.f;

	private int levels;

	private float width;

	private int[] counter;

	private Rect rect;

	private float tx1;

	private float ty1;

	private float tx2;

	private float ty2;

	private float agentSize;

	@Override
	public void onDraw(GL gl) {
		if (!this.initialized) {
			throw new RuntimeException("Drawer needs to be initialized!");
		}

		if (this.texture == null) {
			this.texture = OTFOGLDrawer.createTexture(MatsimResource.getAsInputStream("ped.png"));
			final TextureCoords tc = this.texture.getImageTexCoords();
			this.tx1 = tc.left();
			this.ty1 = tc.top();
			this.tx2 = tc.right();
			this.ty2 = tc.bottom();
		}

		if (isEmpty()) {
			return;
		}

		setAgentSize();


		if (this.texture != null) {
			this.texture.enable();
			gl.glEnable(GL.GL_TEXTURE_2D);
			this.texture.bind();
		}
		gl.glDepthMask(false);

		drawArray(gl);
		if (this.texture != null ) {
			this.texture.disable();
		}

	}

	protected boolean isEmpty() {
		return false;
	}

	protected void drawArray(GL gl) {




		gl.glEnable(GL.GL_BLEND);
		gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);

		updateMatrices(gl);
		float [] top = getOGLPos(this.viewport[0], this.viewport[1]);
		float [] bottom = getOGLPos(this.viewport[2], this.viewport[3]);

		float currentWidth = bottom[0] - top[0];
		float scrRatio = currentWidth / (this.viewport[2]-this.viewport[0]);
		this.agentSize *= scrRatio;

		float ratio = Math.min(currentWidth/this.width,1);
		int level = 0;
		//		if (((OTFVisConfig)Gbl.getConfig().getModule(OTFVisConfig.GROUP_NAME)).zoomDependentLod()) {
		//		}
		for (;this.counter[level]*ratio > MAX_DRAW && level < this.counter.length-1; level++);
		//		System.out.println("level:" + level + " max agents:" + this.counter[level] + " est. agents:" + this.counter[level]*ratio);

		for (int i = level; i < this.levels; i++) {
			Buffer buff = this.rect.getBuffer(i);

			float[] vertices = buff.getVertArray();
			float[] colors = buff.getColArray();
			float [] azimuths = buff.getAziArray();

			int count = buff.getCount();
			int vPos = 0;
			int cPos = 0;
			float z = 0.f;

			for(int j = 0; j < count; j++) {


				float x = vertices[vPos++];
				float y = vertices[vPos++];

				float r = colors[cPos++];
				float g = colors[cPos++];
				float b = colors[cPos++];
				if (x < top[0] || x > bottom[0] || y > top[1] || y < bottom[1]) {
					continue;
				}

				float a = azimuths[j];

				gl.glPushMatrix();
				gl.glTranslatef(x, y,0); //center of square
				gl.glRotatef( a, 0.0f, 0.0f, 1.0f );

				gl.glColor4f(r,g,b,ALPHA);
				gl.glBegin(GL_QUADS);
				gl.glTexCoord2f(this.tx1, this.ty1); gl.glVertex3f(- this.agentSize,  this.agentSize, z);
				gl.glTexCoord2f(this.tx2, this.ty1); gl.glVertex3f( this.agentSize,this.agentSize, z);
				gl.glTexCoord2f(this.tx2, this.ty2); gl.glVertex3f( this.agentSize,- this.agentSize, z);
				gl.glTexCoord2f(this.tx1, this.ty2); gl.glVertex3f(- this.agentSize, - this.agentSize, z);
				gl.glEnd();

				gl.glPopMatrix();

			}
		}
	}

	public void updateMatrices(GL gl) {
		// update matrices for mouse position calculation
		gl.glGetDoublev( GL_MODELVIEW_MATRIX, this.modelview,0);
		gl.glGetDoublev( GL_PROJECTION_MATRIX, this.projection,0);
		gl.glGetIntegerv( GL_VIEWPORT, this.viewport,0 );
	}

	private float [] getOGLPos(int x, int y)
	{
		double[] obj_pos = new double[3];
		float winX, winY;//, winZ = cameraStart.getZ();
		float posX, posY;//, posZ;
		double[] w_pos = new double[3];
		double[] z_pos = new double[1];


		winX = x;
		winY = this.viewport[3] - y;

		z_pos[0]=1;

		GLU glu = new GLU();
		obj_pos[2]=0; // Check view relative z-koord of layer zero == visnet layer
		glu.gluProject( obj_pos[0], obj_pos[1],obj_pos[2], this.modelview,0, this.projection,0, this.viewport,0, w_pos,0);

		glu.gluUnProject( winX, winY, w_pos[2], this.modelview,0, this.projection,0, this.viewport,0, obj_pos,0);

		posX = (float)obj_pos[0];
		posY = (float)obj_pos[1];

		return new float []{posX, posY};
	}
	public void init(float width, int maxAgents) {

		this.width = width;

		List<Integer> zoomLevels = new ArrayList<Integer>();
		zoomLevels.add(1);
		zoomLevels.add(2);


		while (maxAgents / zoomLevels.get(zoomLevels.size()-1) > MAX_DRAW) {
			zoomLevels.add(zoomLevels.get(zoomLevels.size()-1)+zoomLevels.get(zoomLevels.size()-2));
			System.out.println(zoomLevels.get(zoomLevels.size()-1));
		}
		this.levels = zoomLevels.size();




		this.counter = new int [zoomLevels.size()];
		this.rect = new Rect(this.levels);
		while (!this.ais.isEmpty()) {
			AgentInfo ai = this.ais.pop();
			int level = zoomLevels.size()-1;
			while (ai.id.hashCode() % zoomLevels.get(level) != 0) {
				level--;
			}
			this.counter[level]++;
			this.rect.addAgent(level,ai);
		}
		for (int i = this.counter.length-2; i >= 0; i--) {
			this.counter[i] += this.counter[i+1];
		}


		this.initialized = true;
	}


	protected void setAgentSize() {
		this.agentSize = OTFClientControl.getInstance().getOTFVisConfig().getAgentSize()/10f;

	}

	public void addAgent(float x, float y, int type, int user, float speed, String id, float azimuth) {


		AgentInfo ai = new AgentInfo();
		ai.x = x;
		ai.y = y;
		ai.type = type;
		ai.id = id;
		ai.azimuth = azimuth;
		this.ais.push(ai);
	}



	private static class Buffer {

		private int count = 0;

		private final List<Float> tmpCol = new ArrayList<Float>();
		private final List<Float> tmpVert = new ArrayList<Float>();
		private final List<Float> tmpAzi = new ArrayList<Float>();

		private float [] colors = null;
		private float [] azimuths = null;
		private float [] vertices = null;

		public void addAgent(AgentInfo ai) {

			this.tmpVert.add(ai.x);
			this.tmpVert.add(ai.y);

			this.tmpAzi.add(ai.azimuth);

			Color mycolor = colorizer.getColor(ai.type);
			this.tmpCol.add(mycolor.getRed()/255.f);
			this.tmpCol.add(mycolor.getGreen()/255.f);
			this.tmpCol.add(mycolor.getBlue()/255.f);


			this.count++;
		}

		public float [] getAziArray() {
			if (this.azimuths == null) {
				this.azimuths = new float [this.tmpAzi.size()];
				for (int i = 0 ; i < this.tmpAzi.size(); i++) {
					this.azimuths[i] = this.tmpAzi.get(i);
				}
				this.tmpAzi.clear();
			}
			return this.azimuths;
		}


		public float [] getColArray() {
			if (this.colors == null) {
				this.colors = new float[this.tmpCol.size()];
				for (int i = 0; i < this.tmpCol.size(); i++) {
					this.colors[i] = this.tmpCol.get(i);
				}
				this.tmpCol.clear();
			}

			return this.colors;
		}

		public float [] getVertArray() {
			if (this.vertices == null) {
				this.vertices = new float[this.tmpVert.size()];
				for (int i = 0; i < this.tmpVert.size(); i++) {
					this.vertices[i] = this.tmpVert.get(i);
				}
				this.tmpVert.clear();
			}
			return this.vertices;
		}
		public int getCount() {
			return this.count;
		}
	}

	private static class Rect {
		private final Buffer [] buffers;

		public Rect(int size) {
			this.buffers = new Buffer[size];
			for (int i = 0; i < size; i++){
				this.buffers[i] = new Buffer();
			}
		}

		public void addAgent(int level, AgentInfo ai) {
			this.buffers[level].addAgent(ai);

		}

		public Buffer getBuffer(int level) {
			return this.buffers[level];
		}
	}

	private static class AgentInfo {
		float x;
		float y;
		int type;
		float azimuth;
		String id;

	}
}



