package org.matsim.counts;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

import java.util.Arrays;
import java.util.Map;
import java.util.Stack;

/**
 * Reader class to parse MulitModeCounts from file.
 * */
public class MultiModeCountsReader extends MatsimXmlParser {

	private final String MULTIMODECOUNTS = "multiModeCounts";
	private final String MULTIMODECOUNT = "multiModeCount";
	private final String ATTRIBUTES = "attributes";
	private final String VALUE = "value";

	private final MultiModeCounts counts;
	private final Map<Id<? extends Identifiable>, MultiModeCount> countMap;
	private Id<? extends Identifiable> currCount;
	private Measurable currMeasurable;

	public MultiModeCountsReader(MultiModeCounts counts) {
		super(ValidationType.NO_VALIDATION);
		this.counts = counts;
		this.countMap = counts.getCounts();
	}

	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {

		switch (name) {
			case MULTIMODECOUNTS -> startMultiModeCounts(atts, context);
			case MULTIMODECOUNT -> startMultiModeCount(atts, context);
			case ATTRIBUTES -> startAttributes(atts, context);
			case VALUE -> addValuesToMeasurable(atts);

			default -> {
				if (counts.getMeasurableTags().contains(name))
					startMeasurable(name, atts);
			}
		}
	}

	private void addValuesToMeasurable(Attributes atts) {
		if (this.currMeasurable.hasOnlyDailyValues()) {
			this.currMeasurable.setDailyValue(Double.parseDouble(atts.getValue(0)));
		} else {
			int h = Integer.parseInt(atts.getValue("h"));
			double v = Double.parseDouble(atts.getValue("v"));
			this.currMeasurable.addAtHour(h, v);
		}
	}

	private void startAttributes(Attributes atts, Stack<String> context) {

		String peek = context.peek();
		org.matsim.utils.objectattributes.attributable.Attributes attributes;

		if (peek.equals(MULTIMODECOUNTS)) {
			attributes = counts.getAttributes();

		} else {
			attributes = countMap.get(this.currCount).getAttributes();
		}

		for (int i = 0; i < atts.getLength(); i++) {
			String name = atts.getQName(i);
			String value = atts.getValue(i);

			attributes.putAttribute(name, value);
		}

	}

	private void startMeasurable(String tag, Attributes atts) {
		MultiModeCount count = countMap.get(currCount);
		this.currMeasurable = count.addMeasurable(tag, atts.getValue("mode"), atts.getValue("hasOnlyDailyValues").equals("t"));
	}

	private void startMultiModeCount(Attributes atts, Stack<String> context) {
		String idString = atts.getValue("id");
		Id<? extends Identifiable> id = Id.create(idString, counts.identifiable);
		String stationName = atts.getValue("stationName");
		int year = Integer.parseInt(atts.getValue("year"));

		counts.createAndAddCount(id, stationName, year);
		currCount = id;
	}

	private void startMultiModeCounts(Attributes atts, Stack<String> context) {
		org.matsim.utils.objectattributes.attributable.Attributes attributes = counts.getAttributes();

		for (int i = 0; i < atts.getLength(); i++) {

			String name = atts.getQName(i);
			String value = atts.getValue(i);

			switch (name) {
				case "name" -> counts.setName(value);
				case "source" -> counts.setSource(value);
				case "year" -> counts.setYear(Integer.parseInt(value));
				case "description" -> counts.setDescription(value);
				case "identifiable" -> {
					if (!counts.identifiable.getName().equals(value))
						throw new RuntimeException("Counts from file are matched onto " + value + " objects, but + " + counts.identifiable.getName() + " were specified before!");
				}
				case "measurableTags" -> Arrays.stream(value.split(",")).forEach(s -> counts.getMeasurableTags().add(s));

				default -> attributes.putAttribute(name, value);
			}
		}
	}

	@Override
	public void endTag(String name, String content, Stack<String> context) {

	}
}
