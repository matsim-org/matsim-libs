package org.matsim.contrib.carsharing.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.matsim.api.core.v01.Coord;
import org.matsim.contrib.carsharing.qsim.FreefloatingAreas;
import org.matsim.core.utils.gis.PolygonFeatureFactory;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.core.utils.misc.Counter;
import org.xml.sax.Attributes;

public class FreefloatingAreasReader extends MatsimXmlParser {
	private PolygonFeatureFactory polygonFeatureFactory;

	private Map<String, FreefloatingAreas> freefloatingAreas = new HashMap<String, FreefloatingAreas>();

	private Counter counter;

	private String companyName;

	private String idString = null;

	private ArrayList<Coord> coords;

	public FreefloatingAreasReader() {
		super(ValidationType.DTD_ONLY);
		this.polygonFeatureFactory = new PolygonFeatureFactory.Builder()
				.setName("freefloating_area")
				.setCrs(DefaultGeographicCRS.WGS84)
				.addAttribute("id", String.class)
				.create();
	}

	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {
		if (name.equals("areas")) {
			counter = new Counter("reading freefloating area # ");
		}

		if (name.equals("company")) {
			this.companyName = atts.getValue("name");
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
			SimpleFeature area = this.polygonFeatureFactory.createPolygon(coordsArray);
			area.setAttribute("id", this.idString);

			this.addArea(area);
		}

		if (name.equals("areas")) {
			counter.printCounter();
		}
	}

	public Map<String, FreefloatingAreas> getFreefloatingAreas() {
		return this.freefloatingAreas;
	}

	public void addArea(SimpleFeature area) {
		// TODO: what was the purpose of this check?
		if (area.getName().getLocalPart() == "freefloating_area") {
			if (this.getFreefloatingAreas().containsKey(this.companyName) == false) {
				this.getFreefloatingAreas().put(this.companyName, new FreefloatingAreas());
			}

			this.getFreefloatingAreas().get(this.companyName).add(area);
		}
	}
}
