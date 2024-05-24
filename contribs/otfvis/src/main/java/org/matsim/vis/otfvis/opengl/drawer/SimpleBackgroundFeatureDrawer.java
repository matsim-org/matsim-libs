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

package org.matsim.vis.otfvis.opengl.drawer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;

/**
 * SimpleBackgroundDrawer can draw a geotools feature (e.g. shape file?) on screen.
 * @author dstrippgen
 *
 */
public class SimpleBackgroundFeatureDrawer implements GLEventListener {

	private static class OTFFeature {
		final int npoints;
		float [] xpoints;
		float [] ypoints;
		final float[] color;
		final  int glType;

		OTFFeature(final float[] xpoints, final float[] ypoints, final int npoints, final float[] color, final int glType) {
			this.npoints = npoints;
			this.color = color;
			this.glType = glType;
			this.xpoints = xpoints;
			this.ypoints = ypoints;
		}
	}
	private final List<OTFFeature> featureList = new ArrayList<>();

	public SimpleBackgroundFeatureDrawer(final SimpleFeatureSource features, final float[] color) throws IOException{
		SimpleFeatureIterator fIt = features.getFeatures().features();
		while (fIt.hasNext()) {
			final SimpleFeature ft = fIt.next();
			featureList.add(toOTFFeature(ft, color));
		}
		fIt.close();
	}
	
	private OTFFeature toOTFFeature(final SimpleFeature feature, final float[] color) {
		final Geometry geo = (Geometry) feature.getDefaultGeometry();
		final LineString ls;
		final int glType;
		if (geo instanceof Polygon) {
			ls = ((Polygon) geo).getExteriorRing();
			glType = GL2.GL_POLYGON;
		} else if (geo instanceof MultiPolygon) {
			ls = ((Polygon) geo.getGeometryN(0)).getExteriorRing();
			glType = GL2.GL_POLYGON;
		} else if (geo instanceof LineString) {
			ls = (LineString) geo;
			glType = GL2.GL_LINE_STRIP;
		} else if (geo instanceof MultiLineString) {
			ls = (LineString) geo.getGeometryN(0);
			glType = GL2.GL_LINE_STRIP;
		} else if (geo instanceof Point) {
			final GeometryFactory geofac  = new GeometryFactory();
			ls = geofac.createLineString(new Coordinate[]{geo.getCoordinate()});
			glType = GL.GL_POINTS;
		} else {
			throw new RuntimeException("Could not read Geometry from Feature!!");
		}
		final int npoints = ls.getNumPoints();
		final float [] xpoints = new float[npoints];
		final float [] ypoints = new float[npoints];
		for (int i = 0; i < npoints; i++) {
			xpoints[i] = (float) (ls.getPointN(i).getCoordinate().x);
			ypoints[i] = (float) (ls.getPointN(i).getCoordinate().y);
		}
		return new OTFFeature(xpoints, ypoints, npoints, color, glType);
	}

	@Override
	public void init(GLAutoDrawable glAutoDrawable) {}

	@Override
	public void reshape(GLAutoDrawable glAutoDrawable, int i, int i1, int i2, int i3) {}

	@Override
	public void display(GLAutoDrawable glAutoDrawable) {
		GL2 gl = (GL2) glAutoDrawable.getGL();
		for(OTFFeature feature : featureList) {
			gl.glColor4f(feature.color[0],feature.color[1],feature.color[2],feature.color[3]);
			gl.glBegin(feature.glType);
			for (int i =  0; i < feature.npoints; i++) {
				gl.glVertex3d(feature.xpoints[i], feature.ypoints[i], 0);	
			}
			gl.glEnd();
		}
	}

	@Override
	public void dispose(GLAutoDrawable glAutoDrawable) {}

}
