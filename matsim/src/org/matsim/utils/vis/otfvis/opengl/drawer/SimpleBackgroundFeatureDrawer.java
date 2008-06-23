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

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;



public class SimpleBackgroundFeatureDrawer extends AbstractBackgroundDrawer {

	final OTFFeature feature;
		
	public SimpleBackgroundFeatureDrawer(final Feature feature, final float [] color){
		this.feature = getOTFFeature(feature, color);
	}
	
	
	private OTFFeature getOTFFeature(final Feature feature, final float[] color) {
		final Geometry geo = feature.getDefaultGeometry();
		final LineString ls;
		final int glType;
		if (geo instanceof Polygon) {
			ls = ((Polygon) geo).getExteriorRing();
			glType = GL.GL_POLYGON;
		}else if (geo instanceof MultiPolygon) {
			ls = ((Polygon)((MultiPolygon)geo).getGeometryN(0)).getExteriorRing();
			glType = GL.GL_POLYGON;
		} else if (geo instanceof LineString) {
				ls = (LineString) geo;
				glType = GL.GL_LINE_STRIP;
		} else if (geo instanceof MultiLineString) {
			ls = (LineString)((MultiLineString) geo).getGeometryN(0);
			glType = GL.GL_LINE_STRIP;
		}else if (geo instanceof Point) {
				final GeometryFactory geofac  = new GeometryFactory();
				ls = geofac.createLineString(new Coordinate [] {geo.getCoordinate()});
				glType = GL.GL_POINTS;
		} else {
			throw new RuntimeException("Could not read Geometry from Feature!!");
		}
		final int npoints = ls.getNumPoints();
		final float [] xpoints = new float[npoints];
		final float [] ypoints = new float[npoints];
//		final float [] color = new float [] {.5f,.1f,.1f,.8f};
		for (int i = 0; i < npoints; i++) {
			xpoints[i] = (float) (ls.getPointN(i).getCoordinate().x);
			ypoints[i] = (float) (ls.getPointN(i).getCoordinate().y);
		}
		return new OTFFeature(xpoints,ypoints,npoints, color,glType);
	}


	@Override
	public void onDraw(final GL gl) {
		
		if (!this.feature.converted) {
			this.feature.setOffset((float)this.offsetEast, (float)this.offsetNorth);
		}
		gl.glColor4f(this.feature.color[0],this.feature.color[1],this.feature.color[2],this.feature.color[3]);
		gl.glBegin(this.feature.glType);
		for (int i =  0; i < this.feature.npoints; i++) {
			gl.glVertex3d(this.feature.xpoints[i], this.feature.ypoints[i], 10);	
			
		}
		gl.glEnd();
		
	}

	
	private static class OTFFeature {
		
		boolean converted = false;
		final int npoints;
		float [] xpoints;
		float [] ypoints;
		final float[] color;
		final  int glType;
		public OTFFeature(final float [] xpoints, final float [] ypoints, final int npoints, final float[] color, final int glType) {
			this.npoints = npoints;
			this.color = color;
			this.glType = glType;
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
