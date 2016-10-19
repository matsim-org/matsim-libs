package org.matsim.contrib.carsharing.qsim;

import java.util.ArrayList;

import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.gis.PointFeatureFactory;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;

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

	public boolean contains(Coord coord) {
		if (this.areas == null) {
			return true;
		}

		MultiPolygon polygons = (MultiPolygon) this.areas.getAttribute("the_geom");
		SimpleFeature pointFeature = this.pointFeatureFactory.createPoint(coord, new Object[0], null);
		Point point = (Point) pointFeature.getAttribute("the_geom");

		return polygons.contains(point);
	}
}
