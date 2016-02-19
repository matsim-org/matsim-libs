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

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.vividsolutions.jts.geom.*;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.matsim.vis.otfvis.caching.SceneGraph;
import org.opengis.feature.simple.SimpleFeature;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * SimpleBackgroundDrawer can draw a geotools feature (e.g. shape file?) on screen.
 * @author dstrippgen
 *
 */
public class SimpleBackgroundFeatureDrawer extends AbstractBackgroundDrawer implements Serializable {

	private static final long serialVersionUID = 3686886023386144106L;

	final List<OTFFeature> featureList = new ArrayList<OTFFeature>();
	
	public SimpleBackgroundFeatureDrawer(final SimpleFeatureSource features, final float [] color) throws IOException{
		SimpleFeatureIterator fIt = features.getFeatures().features();
		while (fIt.hasNext()){
			final SimpleFeature ft = fIt.next();
			featureList.add(getOTFFeature(ft, color));
		}
		fIt.close();
	}
	
	private OTFFeature getOTFFeature(final SimpleFeature feature, final float[] color) {
		final Geometry geo = (Geometry) feature.getDefaultGeometry();
		final LineString ls;
		final int glType;
		if (geo instanceof Polygon) {
			ls = ((Polygon) geo).getExteriorRing();
			glType = GL2.GL_POLYGON;
		}else if (geo instanceof MultiPolygon) {
			ls = ((Polygon)((MultiPolygon)geo).getGeometryN(0)).getExteriorRing();
			glType = GL2.GL_POLYGON;
		} else if (geo instanceof LineString) {
				ls = (LineString) geo;
				glType = GL2.GL_LINE_STRIP;
		} else if (geo instanceof MultiLineString) {
			ls = (LineString)((MultiLineString) geo).getGeometryN(0);
			glType = GL2.GL_LINE_STRIP;
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
	public void onDraw(final GL2 gl) {
		
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

	}
	
	
	private static class OTFFeature implements Serializable{
		
		/**
		 * 
		 */
		private static final long serialVersionUID = 5647675629518893490L;
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
	
	@Override
	public void addToSceneGraph(SceneGraph graph) {
		graph.addItem(this);
	}

}
