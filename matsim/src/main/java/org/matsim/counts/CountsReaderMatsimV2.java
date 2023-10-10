package org.matsim.counts;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
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

	private final Class<? extends Identifiable<?>> identifiableClass;
	private final MultiModeCounts<?> counts;
	private final AttributesXmlReaderDelegate attributesDelegate = new AttributesXmlReaderDelegate();
	private MeasurementLocation<?> currLocation;
	private Measurable currMeasurable;
	private org.matsim.utils.objectattributes.attributable.Attributes currAttributes = null;


	public CountsReaderMatsimV2(MultiModeCounts<?> counts, Class<? extends Identifiable<?>> identifiableClass) {
		super(ValidationType.NO_VALIDATION);
		this.counts = counts;
		this.identifiableClass = identifiableClass;
	}

	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {

		switch (name) {
			case MultiModeCounts.ELEMENT_NAME -> startMultiModeCounts(atts);
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
		this.currMeasurable.setAtMinute(t, val);
	}

	private void startMeasurable(String tag, Attributes atts) {
		int interval = Integer.parseInt(atts.getValue("interval"));
		currMeasurable = currLocation.createMeasurable(atts.getValue("type"), atts.getValue("mode"), interval);
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private void startMeasurementLocation(Attributes atts) {
		String idString = atts.getValue("id");
		Id id = Id.create(idString, this.identifiableClass);
		String stationName = atts.getValue("name");

		currLocation = counts.createAndAddLocation(id, stationName);
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
