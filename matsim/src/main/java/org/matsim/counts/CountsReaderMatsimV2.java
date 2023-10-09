package org.matsim.counts;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.utils.objectattributes.attributable.AttributesXmlReaderDelegate;
import org.xml.sax.Attributes;

import java.util.Arrays;
import java.util.Map;
import java.util.Stack;

/**
 * A reader for counts-files of MATSim according to <code>counts_v2.xsd</code>.
 */
public class CountsReaderMatsimV2 extends MatsimXmlParser {

	private final static String VALUE = "val";
	private final static String ATTRIBUTES = "attributes";

	private final Class<? extends Identifiable<?>> identifiableClass;
	private final MultiModeCounts<?> counts;
	private final Map<? extends Id<?>, ? extends MeasurementLocation<?>> locations;
	private Id<?> currCount;
	private Measurable currMeasurable;
	private org.matsim.utils.objectattributes.attributable.Attributes currAttributes = null;

	private final AttributesXmlReaderDelegate attributesDelegate = new AttributesXmlReaderDelegate();


	public CountsReaderMatsimV2(MultiModeCounts<?> counts, Class<? extends Identifiable<?>> identifiableClass) {
		super(ValidationType.NO_VALIDATION);
		this.counts = counts;
		this.locations = counts.getMeasureLocations();
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
		// TODO interval should be irrelevant?
		if (this.currMeasurable.getInterval() == 1440) {
			this.currMeasurable.setDailyValue(Double.parseDouble(atts.getValue(0)));
		} else {
			int h = Integer.parseInt(atts.getValue("h"));
			double v = Double.parseDouble(atts.getValue("v"));
			this.currMeasurable.setAtMinute(h, v);
		}
	}

	private void startMeasurable(String tag, Attributes atts) {
		MeasurementLocation<?> count = locations.get(currCount);
		int interval = Integer.parseInt(atts.getValue("interval"));
		this.currMeasurable = count.addMeasurable(tag, atts.getValue("mode"), interval);
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private void startMeasurementLocation(Attributes atts) {
		String idString = atts.getValue("id");
		Id id = Id.create(idString, this.identifiableClass);
		String stationName = atts.getValue("stationName");

		counts.createAndAddCount(id, stationName);
		currCount = id;
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
