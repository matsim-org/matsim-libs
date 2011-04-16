/* *********************************************************************** *
 * project: org.matsim.*
 * OTFInundationDrawer.java
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
import static javax.media.opengl.GL.GL_VIEWPORT;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import javax.media.opengl.GL;
import javax.media.opengl.glu.GLU;


import playground.gregor.otf.readerwriter.InundationData;
import playground.gregor.otf.readerwriter.InundationData.InundationGeometry;




public class OTFInundationDrawer extends OTFTimeDependentDrawer {


	double[] modelview = new double[16];
	double[] projection = new double[16];
	int[] viewport = new int[4];







	private double zoom;

	private  short timeSlotIdx = -1;
	private InundationData data;
	private double startTime = 3 * 3600;




	@Override
	public void onDraw(GL gl, int time) {



		this.timeSlotIdx = (((short) ((time - this.startTime)/60)));

		if (this.timeSlotIdx < 0) {
			return;
		}

		updateMatrices(gl);
		calcCurrentZoom();


		float [] top = getOGLPos(-50,-50);
		float [] bot = getOGLPos(this.viewport[2]+50, this.viewport[3]+50);
		Collection<InundationGeometry> fic = new ArrayList<InundationGeometry>();
		double mxX = top[0] > bot[0] ? top[0] : bot[0];
		double miX = top[0] < bot[0] ? top[0] : bot[0];
		double mxY = top[1] > bot[1] ? top[1] : bot[1];
		double miY = top[1] < bot[1] ? top[1] : bot[1];

		double zoom = this.zoom;
		this.data.floodingData.get(zoom).get(miX,miY,mxX,mxY,fic);
		while (fic.size() > 40000) {
			fic.clear();
			zoom *= 4;
			this.data.floodingData.get(zoom).get(miX,miY,mxX,mxY,fic);
		}
		for (InundationGeometry t : fic) {

			t.draw(gl, this.timeSlotIdx);

		}

	}


	private void calcCurrentZoom() {
		float scrWidth = this.viewport[2] - this.viewport[0];
		float [] top = getOGLPos(this.viewport[0], this.viewport[1]);
		float [] bottom = getOGLPos(this.viewport[2], this.viewport[3]);
		float glWidth = Math.abs(top[0]-bottom[0])/2;
		double ratio = glWidth/scrWidth;

		int idx = -Arrays.binarySearch(this.data.powerLookUp, ratio);
		if (idx > this.data.powerLookUp.length-1) {
			this.zoom = this.data.powerLookUp[this.data.powerLookUp.length-1];
		} else {
			this.zoom = this.data.powerLookUp[idx-1];
		}
		//		System.out.println("zoom:" + this.zoom);

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


	public void setData(InundationData data) {
		this.data = data;

	}


	public void setStartTime(double startTime) {
		this.startTime  = startTime;
		
	}


}
