/* *********************************************************************** *
 * project: org.matsim.*
 * SimpleBackgroundPolygonDrawer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.utils.vis.otfvis.opengl.drawer;

import java.util.Arrays;

import javax.media.opengl.GL;

import org.geotools.feature.Feature;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;



public class SimpleBackgroundPolygonDrawer extends AbstractBackgroundDrawer {

	final OTFPolygon polygon;
	
	public SimpleBackgroundPolygonDrawer(final Feature feature){
		this.polygon = getPolygon(feature);
	}
	
	
	private OTFPolygon getPolygon(final Feature feature) {
		final Geometry geo = feature.getDefaultGeometry();
		final Polygon p;
		if (geo instanceof Polygon) {
			p = (Polygon) geo;
		}else if (geo instanceof MultiPolygon) {
			p = (Polygon)((MultiPolygon)geo).getGeometryN(0);
		} else {
			throw new RuntimeException("Could not read Polygon from Feature!!");
		}
		final LineString ls = p.getExteriorRing();
		final int npoints = ls.getNumPoints();
		final float [] xpoints = new float[npoints];
		final float [] ypoints = new float[npoints];
		final float [] color = new float [] {.5f,.1f,.1f,.8f};
		for (int i = 0; i < npoints; i++) {
			xpoints[i] = (float) (ls.getPointN(i).getCoordinate().x);
			ypoints[i] = (float) (ls.getPointN(i).getCoordinate().y);
		}
		return new OTFPolygon(xpoints,ypoints,npoints, color);
	}


	@Override
	public void onDraw(final GL gl) {
		
		if (!this.polygon.converted) {
			this.polygon.setOffset((float)this.offsetEast, (float)this.offsetNorth);
		}
		gl.glColor4f(this.polygon.color[0],this.polygon.color[1],this.polygon.color[2],this.polygon.color[3]);
		gl.glBegin(GL.GL_POLYGON);
		for (int i =  0; i < this.polygon.npoints; i++) {
			gl.glVertex3d(this.polygon.xpoints[i], this.polygon.ypoints[i], 10);	
			
		}
		gl.glEnd();
		
	}

	
	private static class OTFPolygon {
		
		boolean converted = false;
		final int npoints;
		float [] xpoints;
		float [] ypoints;
		final float[] color;
		public OTFPolygon(final float [] xpoints, final float [] ypoints, final int npoints, final float[] color) {
			this.npoints = npoints;
			this.color = color;
			this.xpoints = Arrays.copyOf(xpoints, npoints);
			this.ypoints = Arrays.copyOf(ypoints, npoints);
		}
		
		public void setOffset(final float offsetEast, final float offsetNorth) {
			for (int i = 0; i < this.npoints; i++) {
				this.xpoints[i] -= offsetEast;
				this.ypoints[i] -= offsetNorth;
			}
			this.converted = true;
		}
	}

}
