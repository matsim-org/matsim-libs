/* *********************************************************************** *
 * project: org.matsim.*
 * InundationData.java
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
package playground.gregor.otf.readerwriter;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import javax.media.opengl.GL;

import org.matsim.core.utils.collections.QuadTree;

import com.sun.opengl.util.texture.TextureCoords;

public class InundationData implements Serializable{

	private static final long serialVersionUID = 6819500627032639679L;

	public double [] powerLookUp;
	public Map<Double,QuadTree<InundationGeometry>> floodingData = new HashMap<Double,QuadTree<InundationGeometry>>();

	public float[][] walshs;

	public float[] xcoords;

	public float[] ycoords;

	static final float [] empty = new float [] {1,1,1,0};
	
	public static final int RES = 4;
	static final byte [][] walsh16 = new byte [][] {
		{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},
		{1,1,1,1,1,1,1,1,-1,-1,-1,-1,-1,-1,-1,-1},
		{1,1,1,1,-1,-1,-1,-1,-1,-1,-1,-1,1,1,1,1},
		{1,1,1,1,-1,-1,-1,-1,1,1,1,1,-1,-1,-1,-1},
		{1,1,-1,-1,-1,-1,1,1,1,1,-1,-1,-1,-1,1,1},
		{1,1,-1,-1,-1,-1,1,1,-1,-1,1,1,1,1,-1,-1},
		{1,1,-1,-1,1,1,-1,-1,-1,-1,1,1,-1,-1,1,1},
		{1,1,-1,-1,1,1,-1,-1,1,1,-1,-1,1,1,-1,-1},
		{1,-1,-1,1,1,-1,-1,1,1,-1,-1,1,1,-1,-1,1},
		{1,-1,-1,1,1,-1,-1,1,-1,1,1,-1,-1,1,1,-1},
		{1,-1,-1,1,-1,1,1,-1,-1,1,1,-1,1,-1,-1,1},
		{1,-1,-1,1,-1,1,1,-1,1,-1,-1,1,-1,1,1,-1},
		{1,-1,1,-1,-1,1,-1,1,1,-1,1,-1,-1,1,-1,1},
		{1,-1,1,-1,-1,1,-1,1,-1,1,-1,1,1,-1,1,-1},
		{1,-1,1,-1,1,-1,1,-1,-1,1,-1,1,-1,1,-1,1},
		{1,-1,1,-1,1,-1,1,-1,1,-1,1,-1,1,-1,1,-1},
	};
	public static final byte [][] walsh8 = new byte [][] {
		{1,1,1,1,1,1,1,1},
		{1,1,1,1,-1,-1,-1,-1},
		{1,1,-1,-1,-1,-1,1,1},
		{1,1,-1,-1,1,1,-1,-1},
		{1,-1,-1,1,1,-1,-1,1},
		{1,-1,-1,1,-1,1,1,-1},
		{1,-1,1,-1,-1,1,-1,1},
		{1,-1,1,-1,1,-1,1,-1},
	};	
	
	static final byte [][] walsh4 = new byte [][] {
		{1,1,1,1},
		{1,1,-1,-1},
		{1,-1,-1,1},
		{1,-1,1,-1},
	};	
	static final byte [][] walsh2 = new byte [][] {
		{1,1},
		{1,-1},
	};
	static final byte [][] walsh1 = new byte [][] {
		{1}
	};
	

	public  TreeMap<Double,float[]> colorMapping;
	
	public int seriesLength;
	
	
	public interface InundationGeometry {
		
		public void draw(GL gl, int time);
		public int [] getCoords();
		
	}
	public static class Quad implements Serializable, InundationGeometry   {

		private final InundationData data;

		public Quad(InundationData data) {
			this.data = data;
		}

		/**
		 * 
		 */
		private static final long serialVersionUID = -4366232796811829198L;

		
		
		public int [] coordsIdx = new int [4];
		
		@Override
		public void draw(GL gl, int time) {
			byte count = 0;
			float [] acol = null;
			if (time < this.data.walshs[this.coordsIdx[0]][InundationData.RES]) {
				acol = InundationData.empty;
				count++;
			} else {
				acol = this.data.getColor(time,this.data.walshs[this.coordsIdx[0]]);
			}
			float [] bcol = null;
			if (time < this.data.walshs[this.coordsIdx[1]][InundationData.RES]) {
				bcol = InundationData.empty;
				count++;
			} else {
				bcol = this.data.getColor(time,this.data.walshs[this.coordsIdx[1]]);
			}
			float [] ccol = null;
			if (time < this.data.walshs[this.coordsIdx[2]][InundationData.RES]) {
				ccol = InundationData.empty;
				count++;
			} else {
				ccol = this.data.getColor(time,this.data.walshs[this.coordsIdx[2]]);
			}
			float [] dcol = null;
			if (time < this.data.walshs[this.coordsIdx[3]][InundationData.RES]) {
				dcol = InundationData.empty;
				if(count == 3) {
					return;
				}
			} else {
				dcol = this.data.getColor(time,this.data.walshs[this.coordsIdx[3]]);
			}
			
			TextureCoords co = new TextureCoords(0,0,1,1);
			
			gl.glBegin(GL.GL_QUADS);

			gl.glColor4f(acol[0],acol[1],acol[2],acol[3]);
			gl.glTexCoord2f(co.right(),co.bottom()); gl.glVertex3f(this.data.xcoords[this.coordsIdx[0]],this.data.ycoords[this.coordsIdx[0]],1.f);
			gl.glColor4f(bcol[0],bcol[1],bcol[2],bcol[3]);
			gl.glTexCoord2f(co.right(),co.top()); gl.glVertex3f(this.data.xcoords[this.coordsIdx[1]],this.data.ycoords[this.coordsIdx[1]],1.f);
			gl.glColor4f(ccol[0],ccol[1],ccol[2],ccol[3]);
			gl.glTexCoord2f(co.left(), co.top()); gl.glVertex3f(this.data.xcoords[this.coordsIdx[2]],this.data.ycoords[this.coordsIdx[2]],1.f);
			gl.glColor4f(dcol[0],dcol[1],dcol[2],dcol[3]);
			gl.glTexCoord2f(co.left(), co.bottom()); gl.glVertex3f(this.data.xcoords[this.coordsIdx[3]],this.data.ycoords[this.coordsIdx[3]],1.f);
			gl.glEnd();	
			
		}

		@Override
		public int[] getCoords() {
			return this.coordsIdx;
		}


		
	}
	
	public static class Polygon implements Serializable, InundationGeometry {

		/**
		 * 
		 */
		private static final long serialVersionUID = 2464806262945866749L;
		private final InundationData data;
		public int [] coordsIdx;
		public Polygon(InundationData data) {
			this.data = data;
		}
		
		@Override
		public void draw(GL gl, int time) {
			int count = 0; 
			for (int i = 0; i < this.coordsIdx.length; i++) {
				if (time < this.data.walshs[this.coordsIdx[i]][InundationData.RES]) {
					count++;
					if (count == this.coordsIdx.length-1) {
						return;
					}
				}
			}
			gl.glBegin(GL.GL_POLYGON);
			for (int i = 0; i < this.coordsIdx.length; i++) {
				float [] col = null;
				if (time < this.data.walshs[this.coordsIdx[i]][InundationData.RES]) {
					col = empty;
				} else {
					col = this.data.getColor(time,this.data.walshs[this.coordsIdx[i]]);
//					col[3] = 1;
				}
				gl.glColor4f(col[0],col[1],col[2],col[3]);
				gl.glVertex3f(this.data.xcoords[this.coordsIdx[i]],this.data.ycoords[this.coordsIdx[i]],1.f);
			}
			gl.glEnd();	
		}

		@Override
		public int[] getCoords() {
			return this.coordsIdx;
		}
		
	}
	
	public static class Triangle  implements Serializable,  InundationGeometry  {
		/**
		 * 
		 */
		private static final long serialVersionUID = 6218207491133983652L;
		private final InundationData data;
		
		public Triangle(InundationData data) {
			this.data = data;
		}
		
		public int [] coordsIdx = new int [3];

		@Override
		public void draw(GL gl, int time) {
			
			byte count = 0;
			float [] acol = null;
			if (time < this.data.walshs[this.coordsIdx[0]][InundationData.RES]) {
				acol = InundationData.empty;
				count++;
			} else {
				acol = this.data.getColor(time,this.data.walshs[this.coordsIdx[0]]);
			}
			float [] bcol = null;
			if (time < this.data.walshs[this.coordsIdx[1]][InundationData.RES]) {
				bcol = InundationData.empty;
				count++;
			} else {
				bcol = this.data.getColor(time,this.data.walshs[this.coordsIdx[1]]);
			}
			float [] ccol = null;
			if (time < this.data.walshs[this.coordsIdx[2]][InundationData.RES]) {
				ccol = InundationData.empty;
				if (count == 2) {
					return;
				}
			} else {
				ccol = this.data.getColor(time,this.data.walshs[this.coordsIdx[2]]);
			}
			TextureCoords co = new TextureCoords(0,0,1,1);
			
			gl.glBegin(GL.GL_TRIANGLES);

			gl.glColor4f(acol[0],acol[1],acol[2],acol[3]);
			gl.glTexCoord2f(co.right(),co.bottom()); gl.glVertex3f(this.data.xcoords[this.coordsIdx[0]],this.data.ycoords[this.coordsIdx[0]],1.f);
			gl.glColor4f(bcol[0],bcol[1],bcol[2],bcol[3]);
			gl.glTexCoord2f(co.right(),co.top()); gl.glVertex3f(this.data.xcoords[this.coordsIdx[1]],this.data.ycoords[this.coordsIdx[1]],1.f);
			gl.glColor4f(ccol[0],ccol[1],ccol[2],ccol[3]);
			gl.glTexCoord2f(co.left(), co.top()); gl.glVertex3f(this.data.xcoords[this.coordsIdx[2]],this.data.ycoords[this.coordsIdx[2]],1.f);
			gl.glEnd();	
			
		}

		@Override
		public int[] getCoords() {
			return this.coordsIdx;
		}

	}
	

	public float[] getColor(int timeSlotIdx, float[] coef) {
		
		double pos = ((timeSlotIdx-coef[RES])/(this.seriesLength-coef[RES])) * (RES-1);
		int idx1 = (int) pos;
		int idx2 = (int) pos+1;
		double c1 = idx2 - pos;
		double c2 = 1 - c1;
		double restored = 0;
		for (int k = 0; k < RES; k++) {
			restored += coef[k] * (c1*walsh4[k][idx1] + c2 *walsh4[k][idx2]); 
		}
		
		if (restored < 0) {
			return empty;
		}
		if (Double.isNaN(restored)) {
			return empty;
		}
		
//		System.out.println("restored:" + restored);
		Entry<Double,float[]> e = this.colorMapping.floorEntry(restored);
		if (e == null) {
			return empty;
		}
		float [] ret = e.getValue(); 
		if (ret == null) {
			return empty;
		}
		return ret;
	}
	
}
