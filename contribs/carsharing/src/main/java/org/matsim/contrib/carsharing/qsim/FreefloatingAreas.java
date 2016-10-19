package org.matsim.contrib.carsharing.qsim;

import java.io.IOException;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.carsharing.config.FreefloatingAreasReader;
import org.matsim.contrib.carsharing.config.FreeFloatingConfigGroup;
import org.matsim.core.utils.gis.PointFeatureFactory;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;

public class FreefloatingAreas {
	private Scenario scenario;

	private PointFeatureFactory pointFeatureFactory;

	public static final String ELEMENT_NAME = "carSharingAreas";

	private SimpleFeature carsharingAreas = null;

	public FreefloatingAreas(Scenario scenario) {
		this.pointFeatureFactory = new PointFeatureFactory.Builder()
				.setName("point")
				.setCrs(DefaultGeographicCRS.WGS84)
				.create();

		this.scenario = scenario;
	}

	public void readCarsharingAreas() throws IOException {
		final FreeFloatingConfigGroup configGroupff = (FreeFloatingConfigGroup)
				this.scenario.getConfig().getModule( FreeFloatingConfigGroup.GROUP_NAME );

		FreefloatingAreasReader reader = new FreefloatingAreasReader();
		reader.readFile(configGroupff.getAreas());
		this.carsharingAreas = reader.getCarsharingAreas();
	}

	public boolean contains(Coord coord) {
		MultiPolygon area = (MultiPolygon) this.carsharingAreas.getAttribute("the_geom");
		SimpleFeature pointFeature = this.pointFeatureFactory.createPoint(coord, new Object[0], null);
		Point point = (Point) pointFeature.getAttribute("the_geom");

		return area.contains(point);
	}
}
