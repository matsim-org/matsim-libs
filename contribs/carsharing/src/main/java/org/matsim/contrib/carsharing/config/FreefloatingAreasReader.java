package org.matsim.contrib.carsharing.config;

import java.util.ArrayList;
import java.util.Stack;

import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.gis.PolygonFeatureFactory;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.core.utils.misc.Counter;
import org.opengis.feature.simple.SimpleFeature;
import org.xml.sax.Attributes;

import com.vividsolutions.jts.geom.MultiPolygon;

public class FreefloatingAreasReader extends MatsimXmlParser {
	private PolygonFeatureFactory polygonFeatureFactory;

	private SimpleFeature carsharingAreas = null;

	private ArrayList<String> ids = new ArrayList<String>();

	private Counter counter;

	private String idString = null;

	private ArrayList<Coord> coords;

	public FreefloatingAreasReader() {
		this.polygonFeatureFactory = new PolygonFeatureFactory.Builder()
				.setName("carsharing_area")
				.setCrs(DefaultGeographicCRS.WGS84)
				.addAttribute("id", String.class)
				.create();
	}

	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {
		if (name.equals("areas")) {
			counter = new Counter("reading freefloating area # ");
		}

		if (name.equals("area")) {
			counter.incCounter();
			this.idString = atts.getValue("id");
			this.coords = new ArrayList<Coord>();
		}

		if (name.equals("node")) {
			final String x = atts.getValue("x");
			final String y = atts.getValue("y");

			Coord coord = new Coord(
					Double.parseDouble(x),
					Double.parseDouble(y)
			);

			this.coords.add(coord);
		}
	}

	@Override
	public void endTag(final String name, final String content, final Stack<String> context) {
		if (name.equals("area")) {
			Coord[] coordsArray = this.coords.toArray(new Coord[this.coords.size()]);
			SimpleFeature carsharingArea = this.polygonFeatureFactory.createPolygon(coordsArray);
			carsharingArea.setAttribute("id", this.idString);

			// create union of local and instance variable carsharingArea
			this.add(carsharingArea);
		}

		if (name.equals("areas")) {
			counter.printCounter();
		}
	}

	public SimpleFeature getCarsharingAreas() {
		return this.carsharingAreas;
	}

	public void add(SimpleFeature carsharingArea) {
		if (carsharingArea.getName().getLocalPart() == "carsharing_area") {
			String id = (String) carsharingArea.getAttribute("id");

			if (this.ids.contains(id) == false) {
				this.ids.add(id);

				if (this.carsharingAreas == null) {
					this.carsharingAreas = carsharingArea;
				} else {
					// merge geometries!
					MultiPolygon oldArea = (MultiPolygon) this.carsharingAreas.getAttribute("the_geom");
					MultiPolygon newArea = (MultiPolygon) carsharingArea.getAttribute("the_geom");
					MultiPolygon unitedArea = (MultiPolygon) oldArea.union(newArea);
					this.carsharingAreas.setAttribute("the_geom", unitedArea);
				}
			}
		}
	}
}
