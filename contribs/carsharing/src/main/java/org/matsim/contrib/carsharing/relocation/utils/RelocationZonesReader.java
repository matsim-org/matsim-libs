package org.matsim.contrib.carsharing.relocation.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.carsharing.relocation.infrastructure.RelocationZone;
import org.matsim.core.utils.gis.PolygonFeatureFactory;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.core.utils.misc.Counter;
import org.xml.sax.Attributes;

public class RelocationZonesReader extends MatsimXmlParser {
	private PolygonFeatureFactory polygonFeatureFactory;

	private Map<String, List<RelocationZone>> relocationZones;

	private String companyId;

	private Counter counter;

	private String idString = null;

	private ArrayList<Coord> coords;

	public RelocationZonesReader() {
		super(ValidationType.DTD_ONLY);
		this.polygonFeatureFactory = new PolygonFeatureFactory.Builder()
				.setName("carsharing_relocation_zone")
				.setCrs(DefaultGeographicCRS.WGS84)
				.addAttribute("id", String.class)
				.create();
	}

	@Override
	public void startTag(final String name, final Attributes atts, final Stack<String> context) {
		if (name.equals("relocationZones")) {
			this.relocationZones = new HashMap<String, List<RelocationZone>>();

			counter = new Counter("reading car sharing relocation zone # ");
		}

		if (name.equals("company")) {
			this.companyId = atts.getValue("name");
			this.relocationZones.put(this.companyId, new ArrayList<RelocationZone>());
		}

		if (name.equals("relocationZone")) {
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
		if (name.equals("relocationZone")) {
			Coord[] coordsArray = this.coords.toArray(new Coord[this.coords.size()]);
			SimpleFeature relocationZonePolygon = this.polygonFeatureFactory.createPolygon(coordsArray);
			RelocationZone relocationZone = new RelocationZone(Id.create(this.idString, RelocationZone.class), relocationZonePolygon);
			this.relocationZones.get(this.companyId).add(relocationZone);
		}

		if (name.equals("company")) {
			Collections.sort(this.relocationZones.get(this.companyId), new Comparator<RelocationZone>() {
		        @Override
				public int compare(RelocationZone relocationZone1, RelocationZone relocationZone2) {
		            return relocationZone1.getId().compareTo(relocationZone2.getId());
		        }
		    });
		}

		if (name.equals("relocationZones")) {
			counter.printCounter();
		}
	}

	public Map<String, List<RelocationZone>> getRelocationZones() {
		return this.relocationZones;
	}
}
