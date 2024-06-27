package org.matsim.contrib.carsharing.qsim;

import java.util.ArrayList;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.operation.distance.DistanceOp;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.gis.PointFeatureFactory;

public class FreefloatingAreas {
	private PointFeatureFactory pointFeatureFactory;

	private SimpleFeature areas = null;

	private ArrayList<String> ids = new ArrayList<String>();

	public FreefloatingAreas() {
		this.pointFeatureFactory = new PointFeatureFactory.Builder()
				.setName("point")
				.setCrs(DefaultGeographicCRS.WGS84)
				.create();
	}

	public void add(SimpleFeature area) {
		String id = (String) area.getAttribute("id");

		if (this.ids.contains(id) == false) {
			this.ids.add(id);

			if (this.areas == null) {
				this.areas = area;
			} else {
				// merge geometries!
				MultiPolygon oldPolygons = (MultiPolygon) this.areas.getAttribute("the_geom");
				MultiPolygon newPolygon = (MultiPolygon) area.getAttribute("the_geom");
				MultiPolygon unitedPolygons = (MultiPolygon) oldPolygons.union(newPolygon);
				this.areas.setAttribute("the_geom", unitedPolygons);
			}
		}
	}
	
	public synchronized boolean contains(Coord coord) {
		if (this.areas == null) {
			return true;
		}

		MultiPolygon polygons = (MultiPolygon) this.areas.getAttribute("the_geom");
		SimpleFeature pointFeature = this.pointFeatureFactory.createPoint(coord, new Object[0], null);
		Point point = (Point) pointFeature.getAttribute("the_geom");

		return polygons.contains(point);
	}

	public Coord[] nearestPoints(Coord coord) {
		if (this.areas == null) {
			return new Coord[]{coord};
		}

		MultiPolygon polygons = (MultiPolygon) this.areas.getAttribute("the_geom");
		SimpleFeature pointFeature = this.pointFeatureFactory.createPoint(coord, new Object[0], null);
		Point point = (Point) pointFeature.getAttribute("the_geom");

		Coordinate[] coordinates = DistanceOp.nearestPoints(polygons, point);
		Coord[] coords = new Coord[coordinates.length];

		int index = 0;
		for (Coordinate coordinate : coordinates) {
			Coord nearestCoord = new Coord(coordinate.x, coordinate.y);
			coords[index] = nearestCoord;
			index++;
		}

		return coords;
	}
}
