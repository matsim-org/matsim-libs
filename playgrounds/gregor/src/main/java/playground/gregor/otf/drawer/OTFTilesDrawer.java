/* *********************************************************************** *
 * project: org.matsim.*
 * OTFTilesDrawer.java
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
package playground.gregor.otf.drawer;

import static javax.media.opengl.GL.GL_MODELVIEW_MATRIX;
import static javax.media.opengl.GL.GL_PROJECTION_MATRIX;
import static javax.media.opengl.GL.GL_QUADS;
import static javax.media.opengl.GL.GL_VIEWPORT;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import javax.media.opengl.GL;
import javax.media.opengl.glu.GLU;

import org.apache.log4j.Logger;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.evacuation.otfvis.drawer.Tile;
import org.matsim.evacuation.otfvis.readerwriter.TileLoader;
import org.matsim.vis.otfvis.opengl.drawer.AbstractBackgroundDrawer;

import com.sun.opengl.util.texture.TextureCoords;
import com.sun.opengl.util.texture.TextureIO;


public class OTFTilesDrawer extends AbstractBackgroundDrawer implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -8732378231241446615L;
	
	private static final Logger log = Logger.getLogger(OTFTilesDrawer.class);
	double[] modelview = new double[16];
	double[] projection = new double[16];
	int[] viewport = new int[4];
	
	
	
	double [] powerLookUp;
	
	Map<String,Tile> tilesMap = Collections.synchronizedMap(new HashMap<String,Tile>(769,0.75f));
	private final TreeSet<Tile> currentView = new TreeSet<Tile>(new Comper());
	
//	int numX = 0;
//	int numY = 0;
	private double zoom;
	private final TileLoader loader;
	private final double initZoom = 32;
	private boolean viewLocked;
	private float oTy;
	private float oTx;
	private boolean stopped = false;
	
	
	public OTFTilesDrawer() {
		
		this.loader = new TileLoader(this.tilesMap);
		this.loader.start();
		this.powerLookUp = new double [16];
		double z = 0.125;
		for (int i = 0; i < 16; i++) {
			this.powerLookUp[i] = (z *= 2);
		}
	}
	
	public void onDraw(GL gl) {
		if (this.stopped) {
			return;
		}
		updateMatrices(gl);
		calcCurrentZoom();
				
		recalcView(gl);
//		log.debug("on:" + this.offsetNorth + " oe:" + this.offsetEast);
		
		for (Tile t : this.currentView) {
			drawTile(t,gl);
		}


	}
	
	
	
	private void recalcView(GL gl) {
		if (this.viewLocked) {
			return;
		}
		this.viewLocked = true;
		this.currentView.clear();
//		System.out.println("==============================");
//		for (int r =1; r < Math.max(this.viewport[2], this.viewport[3]); r += Tile.LENGTH/2) {
//			for (double i = 0; i < 2*Math.PI; i += Math.PI/(4*r/(Tile.LENGTH/2))) {
//				double c = Math.cos(i);
//				double s = Math.sin(i);
//				int x = (int) (this.viewport[2]/2 + r * c);
//				int y = (int) (this.viewport[3]/2 + r * s);
//				System.out.println("x:" + x + " y:" + y + " r:" +r + " c:" + c + " s:" + s);
//				drawPED(x,y,gl);
//				double zoom = this.zoom;
//				
//				
//				Tile t = getTile(x, y, this.zoom);
//				while (t == null && zoom  <= this.initZoom ) {
//					zoom *= 2;
//					t = getTile(x,y,zoom);
//					this.viewLocked = false;
//				}
//				if (t == null) {
//					continue;
//				}
//				this.currentView.add(t);
//			
//			}
//			
//		}
//		System.out.println("==============================");
//		
		for (int x = this.viewport[0]; x < this.viewport[2] + Tile.LENGTH/2; x += Tile.LENGTH*0.33) {
			for (int y = this.viewport[1]; y < this.viewport[3] +Tile.LENGTH/2 ; y += Tile.LENGTH*0.33) {
				double zoom = this.zoom;
				
				
				Tile t = getTile(x, y, this.zoom);
				while (t == null && zoom  <= this.initZoom ) {
					zoom *= 2;
					t = getTile(x,y,zoom);
					this.viewLocked = false;
				}
				if (t == null) {
					continue;
				}
				this.currentView.add(t);
			}
		}
		
	}




	private void calcCurrentZoom() {
		float scrWidth = this.viewport[2] - this.viewport[0];
		float [] top = getOGLPos(this.viewport[0], this.viewport[1]);
		float [] bottom = getOGLPos(this.viewport[2], this.viewport[3]);
		float glWidth = Math.abs(top[0]-bottom[0]);
		double ratio = glWidth/scrWidth;
		
		int idx = -Arrays.binarySearch(this.powerLookUp, ratio);
		double zoom = this.powerLookUp[idx-1];
		
		
		if (zoom != this.zoom){
			this.viewLocked = false;
//			System.out.println("ratio:" + ratio + " zoom:" + zoom);
		}
		if (this.oTx != top[0] || this.oTy != top[1]){
			this.viewLocked = false;
			this.oTx = top[0];
			this.oTy = top[1];
		}
		
		this.zoom = zoom;
	}



	private void drawTile(Tile t, GL gl) {
		if (t.getTex() == null) {
			if (t.getTx() == null) {
				this.loader.kill();
				this.stopped  = true;
				log.error("Could not fetch tile from server. Most probably there is no tiles server running on localhost:8080.\nOTFVis will continue without background image drawing.");
				return;
			}
			t.setTex(TextureIO.newTexture(t.getTx()));
			t.setTx(null);
		}
		
		if (this.stopped) {
			return;
		}
		final TextureCoords tc = t.getTex().getImageTexCoords();
		final float tx1 = tc.left();
		final float ty1 = tc.top();
		final float tx2 = tc.right();
		final float ty2 = tc.bottom();

		final float z = 1.1f;
		t.getTex().enable();
		t.getTex().bind();
		
		gl.glColor4f(1,1,1,1);

		gl.glBegin(GL_QUADS);
		gl.glTexCoord2f(tx2, ty2); gl.glVertex3f(t.tX+t.sX, t.tY, z);
		gl.glTexCoord2f(tx2, ty1); gl.glVertex3f(t.tX+t.sX,t.tY+t.sY, z);
		gl.glTexCoord2f(tx1, ty1); gl.glVertex3f(t.tX, t.tY+t.sY, z);
		gl.glTexCoord2f(tx1, ty2); gl.glVertex3f(t.tX, t.tY, z);
//		log.debug("(" + (t.tX +t.sX) + "," + t.tY + ")  " + "(" + (t.tX +t.sX) + "," + (t.tY+t.sY) + ")  " + "(" + (t.tX) + "," + (t.tY+t.sY) + ")  " +  "(" + (t.tX) + "," + (t.tY) + ")  ");
		
		gl.glEnd();
		t.getTex().disable();
	

	}

	private Tile getTile(int x, int y, double zoom) {
		float [] glPos = getOGLPos(x, y);
		double geoX = Math.abs(glPos[0]) + this.offsetEast;
		double geoY = Math.abs(glPos[1]) + this.offsetNorth;
		double refCoordX = ((int)(geoX / zoom / Tile.LENGTH)) * zoom * Tile.LENGTH;
		double refCoordY = ((int)(geoY / zoom / Tile.LENGTH)) * zoom * Tile.LENGTH;		
		String tileId =  zoom + "_" + refCoordX + "_" + refCoordY;
		Tile t =  this.tilesMap.get(tileId);
		if (t != null) {
			return t; 		
		}
		
		
		float signX = glPos[0] < 0 ? -1.f : 1.f;
		float signY = glPos[1] < 0 ? -1.f : 1.f;
		
		float glX = (float) (signX * (refCoordX - this.offsetEast));
		float glY = (float) (signY * (refCoordY - this.offsetNorth));
		double geoWidth = zoom * Tile.LENGTH;
		t = new Tile();
		t.id = tileId;
		t.tX = glX;//(glX - zoom*Tile.LENGTH);
		t.tY = (glY); //(glY - zoom * Tile.LENGTH);
		t.sX = (float) (signX * zoom * (Tile.LENGTH ));
		t.sY = (float) (signY * zoom * (Tile.LENGTH ));
		t.x = x;
		t.y = y;
		t.zoom = zoom;
		t.key = zoom + MatsimRandom.getRandom().nextDouble();
		
		this.loader.addRequest(t,refCoordX,refCoordY + zoom * Tile.LENGTH,geoWidth);
		
		return null;
	}
	
	



	public void updateMatrices(GL gl) { //TODO give me the drawer!!
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
	
	private static class Comper implements Comparator<Tile>, Serializable {

		/**
		 * 
		 */
		private static final long serialVersionUID = -2522220167848088766L;

		public int compare(Tile o1, Tile o2) {
			if (o1.key < o2.key) {
				return 1;
			} else if (o1.key > o2.key) {
				return -1;
			}
			
			return 0;
		}
		
	}

}
