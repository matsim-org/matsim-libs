/* *********************************************************************** *
 * project: org.matsim.*
 * OTFLabelDrawer.java
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

import java.awt.Font;
import java.io.Serializable;

import javax.media.opengl.GL;
import javax.media.opengl.glu.GLU;

import org.matsim.core.gbl.MatsimResource;
import org.matsim.vis.otfvis.opengl.drawer.AbstractBackgroundDrawer;
import org.matsim.vis.otfvis.opengl.drawer.OTFOGLDrawer;
import org.matsim.vis.otfvis.opengl.gl.InfoText;

import com.sun.opengl.util.j2d.TextRenderer;
import com.sun.opengl.util.texture.Texture;
import com.sun.opengl.util.texture.TextureCoords;

/**
 * OTFScaleBarDrawer draws a scale bar as an overlay.
 * It depends on the chosen coordinates being in meters.
 *
 * @author laemmel
 *
 */
public class OTFLabelDrawer extends AbstractBackgroundDrawer implements Serializable  {

	private static final long serialVersionUID = -2278366139695432415L;
	private transient TextRenderer textRenderer = null;
	double[] modelview = new double[16];
	double[] projection = new double[16];
	int[] viewport = new int[4];

	private transient Texture back = null;

	private final String bg;
	private String label;

	public OTFLabelDrawer() {
		this.bg = "sb_background.png";
//		initTextRenderer();
	}

	private void initTextRenderer() {
		// Create the text renderer
		Font font = new Font("SansSerif", Font.PLAIN, 30);
		this.textRenderer = new TextRenderer(font, true, false);
		InfoText.setRenderer(this.textRenderer);
	}

	@Override
	public void onDraw(GL gl) {


		if (this.back == null){
			initTextRenderer();
			this.back = OTFOGLDrawer.createTexture(MatsimResource.getAsInputStream(this.bg));
		}

		updateMatrices(gl);
		float [] fl = getKoords();

		final TextureCoords tc = this.back.getImageTexCoords();
		final float tx1 = tc.left();
		final float ty1 = tc.top();
		final float tx2 = tc.right();
		final float ty2 = tc.bottom();

		final float z = 1.1f;

		float width = (float) this.textRenderer.getBounds(this.label).getWidth() * fl[3] + fl[4];
		float height =(float) this.textRenderer.getBounds(this.label).getHeight() * fl[3] + fl[4];

		gl.glEnable(GL.GL_BLEND);
		gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
		this.back.enable();
		this.back.bind();
		gl.glColor4f(1,1,1,1);

		gl.glBegin(GL_QUADS);
		gl.glTexCoord2f(tx1, ty1); gl.glVertex3f(fl[0], fl[1], z);
		gl.glTexCoord2f(tx2, ty1); gl.glVertex3f(fl[0], fl[1]-height*1.3f, z);
		gl.glTexCoord2f(tx2, ty2); gl.glVertex3f(fl[0]+width, fl[1]-height*1.3f, z);
		gl.glTexCoord2f(tx1, ty2); gl.glVertex3f(fl[0]+width, fl[1], z);
		gl.glEnd();
		this.back.disable();

		gl.glDisable(GL.GL_BLEND);

//		this.sc.enable();
//		this.sc.bind();
//
//		gl.glColor4f(1,1,1,1);
//
//		gl.glBegin(GL_QUADS);
//		gl.glTexCoord2f(tx1, ty1); gl.glVertex3f(fl[0], fl[1], z);
//		gl.glTexCoord2f(tx2, ty1); gl.glVertex3f(fl[0], fl[3], z);
//		gl.glTexCoord2f(tx2, ty2); gl.glVertex3f(fl[2], fl[3], z);
//		gl.glTexCoord2f(tx1, ty2); gl.glVertex3f(fl[2], fl[1], z);
//		gl.glEnd();
//		this.sc.disable();
//
		this.textRenderer.begin3DRendering();
//		float c = 0.f;
//		String text = ""+(int)fl[10];
//		float width = (float) this.textRenderer.getBounds(text).getWidth() * fl[8];
//		// Render the text
//		this.textRenderer.setColor(.1843f, .3922f, .6627f, 1.f);
		this.textRenderer.setColor(.0f, .0f, .0f, 1.f);
//		this.textRenderer.draw3D(text, fl[2] - width/2.f,fl[9],1.1f,fl[8]);
//
		this.textRenderer.draw3D(this.label, fl[2] ,fl[1]-height,1.1f,fl[3]);
		this.textRenderer.end3DRendering();

	}

	private float [] getKoords() {
		int scrTX = this.viewport[0];
		int scrTY = this.viewport[1];
		int scrBX = this.viewport[2];
		int scrBY = this.viewport[3];



		int scrWidth = scrBX -scrTX;
		int diagonal = (int) Math.sqrt(scrBX*scrBX + scrBY * scrBY);


		float[] tmp = getOGLPos(scrTX,scrTY);
		float glTX = tmp[0];

		tmp = getOGLPos(scrBX,scrBY);
		float glBX = tmp[0];


		float glWidth = glBX - glTX;
		float xFactor = Math.abs(glWidth/scrWidth);


		float ret[]  = new float [5];
//
//		int scTXBX = (int) (0.4 * diagonal);
//		int scTXBY = scrBY - 20; //(int) (scrBY - (0.01 * diagonal));
//		tmp = getOGLPos(scTXBX,scTXBY);

		int scTXTX = (int) (0.01 * diagonal);
		int scTXTY = (int) ( (0.01 * diagonal));
		tmp = getOGLPos(scTXTX,scTXTY);
		ret[0] = tmp[0] - 3 * xFactor;
		ret[1] = tmp[1];
		ret[2] = tmp[0];
		ret[3] = (float) (xFactor * diagonal *0.0004);
		ret[4] = 9 * xFactor;
		return ret;

	}

	public void updateMatrices(GL gl) {
		// update matrices for mouse position calculation
		gl.glGetDoublev( GL_MODELVIEW_MATRIX, this.modelview,0);
		gl.glGetDoublev( GL_PROJECTION_MATRIX, this.projection,0);
		gl.glGetIntegerv( GL_VIEWPORT, this.viewport,0 );
	}

	private float [] getOGLPos(int x, int y) {


		double[] obj_pos = new double[3];
		float winX, winY;//, winZ = cameraStart.getZ();
		float posX, posY;//, posZ;
		double[] w_pos = new double[3];
		double[] z_pos = new double[1];


		winX = x;
		winY = this.viewport[3] - y;
		z_pos[0]=1;

		GLU glu = new GLU();
		obj_pos[2]=0; // Check view relative z-coord of layer zero == visnet layer
		glu.gluProject( obj_pos[0], obj_pos[1],obj_pos[2], this.modelview,0, this.projection,0, this.viewport,0, w_pos,0);
		glu.gluUnProject( winX, winY, w_pos[2], this.modelview,0, this.projection,0, this.viewport,0, obj_pos,0);

		posX = (float)obj_pos[0];
		posY = (float)obj_pos[1];
		return new float []{posX, posY};
	}

	public void setLabel(String obj) {
		this.label = obj;

	}


}
