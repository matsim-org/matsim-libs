package org.matsim.counts;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.utils.objectattributes.attributable.AttributesXmlReaderDelegate;
import org.xml.sax.Attributes;

import java.util.Stack;

/**
 * A reader for counts-files of MATSim according to <code>counts_v2.xsd</code>.
 */
public class CountsReaderMatsimV2 extends MatsimXmlParser {

	private final static String VALUE = "value";
	private final static String ATTRIBUTES = "attributes";

	private final Class<? extends Identifiable<?>> idClass;
	private final CoordinateTransformation coordinateTransformation;
	private final Counts<?> counts;
	private final AttributesXmlReaderDelegate attributesDelegate = new AttributesXmlReaderDelegate();
	private MeasurementLocation<?> currLocation;
	private Measurable currMeasurable;
	private org.matsim.utils.objectattributes.attributable.Attributes currAttributes = null;

	public CountsReaderMatsimV2(Counts<?> counts, Class<? extends Identifiable<?>> idClass) {
		this(new IdentityTransformation(), counts, idClass);
	}

	public CountsReaderMatsimV2(CoordinateTransformation coordinateTransformation, Counts<?> counts, Class<? extends Identifiable<?>> idClass) {
		super(ValidationType.NO_VALIDATION);
		this.coordinateTransformation = coordinateTransformation;
		this.counts = counts;
		this.idClass = idClass;
	}

	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {

		switch (name) {
			case Counts.ELEMENT_NAME -> startMultiModeCounts(atts);
			case MeasurementLocation.ELEMENT_NAME -> startMeasurementLocation(atts);
			case Measurable.ELEMENT_NAME -> startMeasurable(name, atts);
			case ATTRIBUTES -> attributesDelegate.startTag(name, atts, context, currAttributes);
			case VALUE -> addValuesToMeasurable(atts);
		}
	}

	@Override
	public void endTag(String name, String content, Stack<String> context) {

	}

	private void addValuesToMeasurable(Attributes atts) {
		int t = Integer.parseInt(atts.getValue("t"));
		double val = Double.parseDouble(atts.getValue("val"));
		this.currMeasurable.setAtSecond(t, val);
	}

	private void startMeasurable(String tag, Attributes atts) {
		int interval = Integer.parseInt(atts.getValue("interval"));
		currMeasurable = currLocation.createMeasurable(atts.getValue("type"), atts.getValue("networkMode"), interval);
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private void startMeasurementLocation(Attributes atts) {
		String idString = atts.getValue("refId");
		Id id = Id.create(idString, this.idClass);
		String stationName = atts.getValue("name");

		currLocation = counts.createAndAddMeasureLocation(id, stationName);
		currLocation.setId(atts.getValue("id"));

		String x = atts.getValue("x");
		String y = atts.getValue("y");
		if (x != null && y != null) {
			currLocation.setCoordinates(coordinateTransformation.transform(
				new Coord(Double.parseDouble(x), Double.parseDouble(y))
			));
		}

		currAttributes = currLocation.getAttributes();
	}

	private void startMultiModeCounts(Attributes atts) {
		currAttributes = counts.getAttributes();

		for (int i = 0; i < atts.getLength(); i++) {
			String name = atts.getQName(i);
			String value = atts.getValue(i);

			switch (name) {
				case "name" -> counts.setName(value);
				case "source" -> counts.setSource(value);
				case "year" -> counts.setYear(Integer.parseInt(value));
				case "description" -> counts.setDescription(value);
			}
		}
	}
}
