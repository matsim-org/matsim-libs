/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.gregor.gis.visibilitygraph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import playground.gregor.sim2denvironment.GisDebugger;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

public class VisibilityGraph {

	private final Collection<SimpleFeature> fts;

	public VisibilityGraph(Collection<SimpleFeature> featureSet) {
		this.fts = featureSet;
	}

	public void run() {
		List<Coordinate> coords = new ArrayList<Coordinate>();
		for (SimpleFeature ft : this.fts) {
			Geometry geo = (Geometry) ft.getDefaultGeometry();
			for (int i = 0; i < geo.getCoordinates().length; i++) {
				coords.add(geo.getCoordinates()[i]);
			}

		}

		GeometryFactory geofac = new GeometryFactory();
		for (int i = 0; i < coords.size()-1; i++) {
			for (int j = i+1; j < coords.size(); j++) {
				Coordinate [] cc = {coords.get(i),coords.get(j)};
				LineString ls = geofac.createLineString(cc);
				if (!crosses(ls)) {
					GisDebugger.addGeometry(ls);
				}
			}
		}
		GisDebugger.dump("/Users/laemmel/devel/sim2dDemoII/raw_input/visibilityGraph.shp");
	}

	private boolean crosses(LineString ls) {
		for (SimpleFeature ft : this.fts) {
			Geometry geo = (Geometry) ft.getDefaultGeometry();
			if (ls.crosses(geo)) {
				return true;
			}
		}
		return false;
	}

	public static void main(String [] args) {
		String file = "/Users/laemmel/devel/sim2dDemoII/raw_input/floorplan.shp";
		ShapeFileReader reader = new ShapeFileReader();
		reader.readFileAndInitialize(file);
		new VisibilityGraph(reader.getFeatureSet()).run();
	}

}
