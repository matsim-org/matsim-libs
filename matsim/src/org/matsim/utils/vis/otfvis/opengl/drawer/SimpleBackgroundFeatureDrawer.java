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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.media.opengl.GL;

import org.geotools.data.FeatureSource;
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

	final List<OTFFeature> featureList = new ArrayList<OTFFeature>();
	
	protected int featureDisplList = -1;
	
	public SimpleBackgroundFeatureDrawer(final FeatureSource features, final float [] color) throws IOException{
		final Iterator<Feature> it = features.getFeatures().iterator();
		while (it.hasNext()){
			final Feature ft = it.next();
			featureList.add(getOTFFeature(ft, color));
		}

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



	public void onDraw(final GL gl) {
	
		onPrepareDraw(gl);
		gl.glCallList(featureDisplList);

	}
	
	public void onPrepareDraw(final GL gl) {
		if(featureDisplList == -1) {
			featureDisplList = gl.glGenLists(1);
			gl.glNewList(featureDisplList, GL.GL_COMPILE);

			for(OTFFeature feature : featureList) {
				if (!feature.converted) {
					feature.setOffset((float)this.offsetEast, (float)this.offsetNorth);
				}
				gl.glColor4f(feature.color[0],feature.color[1],feature.color[2],feature.color[3]);
				gl.glBegin(feature.glType);
				for (int i =  0; i < feature.npoints; i++) {
					gl.glVertex3d(feature.xpoints[i], feature.ypoints[i], 0);	
					
				}
				gl.glEnd();
			}
			gl.glEndList();
		}
		
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
			this.xpoints = xpoints; 
			this.ypoints = ypoints;
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
