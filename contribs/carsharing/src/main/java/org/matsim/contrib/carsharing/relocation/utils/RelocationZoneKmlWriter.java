package org.matsim.contrib.carsharing.relocation.utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.carsharing.relocation.infrastructure.RelocationZone;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.CH1903LV03toWGS84;
import org.matsim.core.utils.io.MatsimXmlWriter;
import org.matsim.core.utils.misc.Time;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.MultiPolygon;

public class RelocationZoneKmlWriter extends MatsimXmlWriter {
	protected CoordinateTransformation coordinateTransformation;

	protected Map<Id<RelocationZone>, Coord[]> coords;

	public RelocationZoneKmlWriter() {
		this.coordinateTransformation = new CH1903LV03toWGS84();
	}

	public void setPolygons(Map<Id<RelocationZone>, MultiPolygon> polygons) {
		this.coords = new HashMap<Id<RelocationZone>, Coord[]>();
		Iterator<Entry<Id<RelocationZone>, MultiPolygon>> iterator = polygons.entrySet().iterator();

		while (iterator.hasNext()) {
			Entry<Id<RelocationZone>, MultiPolygon> entry = iterator.next();

			MultiPolygon polygon = entry.getValue();
			Coord[] coords = new Coord[polygon.getCoordinates().length];
			for (int index = 0; index < polygon.getCoordinates().length; index++) {
				Coordinate coordinate = polygon.getCoordinates()[index];
				Coord coord = new Coord(coordinate.getOrdinate(Coordinate.X), coordinate.getOrdinate(Coordinate.Y));
				Coord WGS84Coord = coordinateTransformation.transform(coord);
				coords[index] = WGS84Coord;
			}

			this.coords.put(entry.getKey(), coords);
		}
	}

	public Map<Id<RelocationZone>, Coord[]> getCoords()
	{
		return this.coords;
	}

	public void writeFile(final Double time, final String filename, Map<Id<RelocationZone>, Map<String, Object>> status) {
		String[] lineColorsBelow = {"00000000", "ff1400ff", "ff1400f0", "ff1400dc", "ff1400c8", "ff1400b4", "ff1400a0", "ff14008c", "ff140078", "ff140064", "ff140050"};
		String[] polyColorsBelow = {"00000000", "991400ff", "991400f0", "991400dc", "991400c8", "991400b4", "991400a0", "9914008c", "99140078", "99140064", "99140050"};
		String[] lineColorsAbove = {"00000000", "ff00ff14", "ff00f014", "ff00dc14", "ff00c814", "ff00b414", "ff00a014", "ff008c14", "ff007814", "ff006414", "ff005014"};
		String[] polyColorsAbove = {"00000000", "9900ff14", "9900f014", "9900dc14", "9900c814", "9900b414", "9900a014", "99008c14", "99007814", "99006414", "99005014"};

		this.openFile(filename);
		this.writeStartTag("kml", Arrays.asList(createTuple("xmlns", "http://www.opengis.net/kml/2.2"), createTuple("xmlns:gx", "http://www.google.com/kml/ext/2.2"), createTuple("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance"), createTuple("xsi:schemalocation", "http://www.opengis.net/kml/2.2 https://developers.google.com/kml/schema/kml22gx.xsd")));

		this.writeStartTag("Document", Collections.<Tuple<String, String>>emptyList());
		this.writeStartTag("name", Collections.<Tuple<String, String>>emptyList());
		this.writeContent("Relocation Zones " + Time.writeTime(time), false);
		this.writeEndTag("name");

		for (Entry<Id<RelocationZone>, Map<String, Object>> relocationZoneEntry : status.entrySet()) {
			Id<RelocationZone> relocationZoneId = relocationZoneEntry.getKey();
			Map<String, Object> relocationZoneContent = relocationZoneEntry.getValue();

			String lineColor = "00000000";
			String polyColor = "00000000";

			try {
				Double level = (Double) relocationZoneContent.get("level");

				if (level < 0) {
					int stepsBelow = Math.min(lineColorsBelow.length, polyColorsBelow.length) - 1;
					level = (level < -stepsBelow) ? -stepsBelow : level;

					lineColor = lineColorsBelow[(int) Math.floor(Math.abs(level))];
					polyColor = polyColorsBelow[(int) Math.floor(Math.abs(level))];
				} else if (level > 0) {
					int stepsAbove = Math.min(lineColorsAbove.length, polyColorsAbove.length) - 1;
					level = (level > stepsAbove) ? stepsAbove : level;

					lineColor = lineColorsAbove[(int) Math.floor(level)];
					polyColor = polyColorsAbove[(int) Math.floor(level)];
				//} else if (numVehicles > 0) {
					//lineColor = "ff00FF14";
					//polyColor = "9900FF14";
				}
			} catch (Exception e) {
				// no level
			}

			this.writeStartTag("Placemark", Arrays.asList(createTuple("id", "linepolygon_" + relocationZoneId.toString())));
			this.writeStartTag("description", Collections.<Tuple<String, String>>emptyList());
			try {
				String content = (String) relocationZoneContent.get("content");
				this.writeContent(content, true);
			} catch (Exception e) {
				// no content
			}
			this.writeEndTag("description");
			this.writeStartTag("Style", Collections.<Tuple<String, String>>emptyList());
			this.writeStartTag("LineStyle", Collections.<Tuple<String, String>>emptyList());
			this.writeStartTag("color", Collections.<Tuple<String, String>>emptyList());
			this.writeContent(lineColor, false);
			this.writeEndTag("color");
			this.writeStartTag("width", Collections.<Tuple<String, String>>emptyList());
			this.writeContent("3", false);
			this.writeEndTag("width");
			this.writeEndTag("LineStyle");
			this.writeStartTag("PolyStyle", Collections.<Tuple<String, String>>emptyList());
			this.writeStartTag("color", Collections.<Tuple<String, String>>emptyList());
			this.writeContent(polyColor, false);
			this.writeEndTag("color");
			this.writeEndTag("PolyStyle");
			this.writeEndTag("Style");
			this.writePolygon(this.getCoords().get(relocationZoneId));
			this.writeEndTag("Placemark");
		}

		this.writeEndTag("Document");
		this.writeEndTag("kml");
		this.close();
	}

	public void writeIconStyleTag(String style, String iconHref)
	{
		this.writeStartTag("Style", Arrays.asList(createTuple("id", style)));
		this.writeStartTag("IconStyle", Collections.<Tuple<String, String>>emptyList());
		this.writeStartTag("Icon", Collections.<Tuple<String, String>>emptyList());
		this.writeStartTag("href", Collections.<Tuple<String, String>>emptyList());
		this.writeContent(iconHref, false);
		this.writeEndTag("href");
		this.writeEndTag("Icon");
		this.writeEndTag("IconStyle");
		this.writeEndTag("Style");
	}

	public void writePlacemark(double x, double y, String style, String description)
	{
		this.writeStartTag("Placemark", Collections.<Tuple<String, String>>emptyList());
		this.writeStartTag("name", Collections.<Tuple<String, String>>emptyList());
		this.writeEndTag("name");
		this.writeStartTag("description", Collections.<Tuple<String, String>>emptyList());
		this.writeContent(description, true);
		this.writeEndTag("description");
		this.writeStartTag("styleUrl", Collections.<Tuple<String, String>>emptyList());
		this.writeContent("#" + style, false);
		this.writeEndTag("styleUrl");
		this.writeStartTag("Point", Collections.<Tuple<String, String>>emptyList());
		this.writeStartTag("coordinates", Collections.<Tuple<String, String>>emptyList());
		this.writeContent(x + "," + y + ",0", true);
		this.writeEndTag("coordinates");
		this.writeEndTag("Point");
		this.writeEndTag("Placemark");
	}

	public void writePolygon(Coord[] coords)
	{
		String[] coordStrings = new String[coords.length];

		for (int index = 0; index < coords.length; index++) {
			Coord coord = coords[index];
			coordStrings[index] = coord.getX() + ", " + coord.getY();
		}

		this.writeStartTag("Polygon", Collections.<Tuple<String, String>>emptyList());
		this.writeStartTag("outerBoundaryIs", Collections.<Tuple<String, String>>emptyList());
		this.writeStartTag("LinearRing", Collections.<Tuple<String, String>>emptyList());
		this.writeStartTag("coordinates", Collections.<Tuple<String, String>>emptyList());
		this.writeContent(String.join(" ", coordStrings), true);
		this.writeEndTag("coordinates");
		this.writeEndTag("LinearRing");
		this.writeEndTag("outerBoundaryIs");
		this.writeEndTag("Polygon");
	}
}