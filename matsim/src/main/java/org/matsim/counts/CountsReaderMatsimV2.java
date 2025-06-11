package org.matsim.counts;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.core.scenario.ProjectionUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.utils.objectattributes.attributable.AttributesXmlReaderDelegate;
import org.xml.sax.Attributes;

import java.util.Stack;

import static org.matsim.utils.objectattributes.attributable.AttributesXmlReaderDelegate.TAG_ATTRIBUTE;
import static org.matsim.utils.objectattributes.attributable.AttributesXmlReaderDelegate.TAG_ATTRIBUTES;

/**
 * A reader for counts-files of MATSim according to <code>counts_v2.xsd</code>.
 */
public class CountsReaderMatsimV2 extends MatsimXmlParser {
	private static final Logger log = LogManager.getLogger( CountsReaderMatsimV2.class );
	private final static String VALUE = "value";
	private final Class<? extends Identifiable<?>> idClass;
	private CoordinateTransformation coordinateTransformation;
	private final Counts<?> counts;
	private final AttributesXmlReaderDelegate attributesDelegate = new AttributesXmlReaderDelegate();
	private final String externalInputCRS;
	private final String targetCRS;
	private MeasurementLocation<?> currLocation;
	private Measurable currMeasurable;
	private org.matsim.utils.objectattributes.attributable.Attributes currAttributes = null;

	public CountsReaderMatsimV2(Counts<?> counts, Class<? extends Identifiable<?>> idClass) {
		this( null, null, counts, idClass );
	}

	public CountsReaderMatsimV2( String externalInputCRS, String targetCRS, Counts<?> counts, Class<? extends Identifiable<?>> idClass ) {
		super(ValidationType.NO_VALIDATION);
		this.externalInputCRS = externalInputCRS;
		this.targetCRS = targetCRS;
		this.counts = counts;
		this.idClass = idClass;

		if (externalInputCRS != null && targetCRS != null) {
			this.coordinateTransformation = TransformationFactory.getCoordinateTransformation(externalInputCRS, targetCRS);
			ProjectionUtils.putCRS(this.counts, targetCRS);
		} else if ( externalInputCRS==null && targetCRS==null ){
			this.coordinateTransformation = new IdentityTransformation();
		} else {
			log.warn("finding a coordinate spec on one side but not on the other: inputCRS=" + externalInputCRS + "; targetCRS=" + targetCRS + ".  We are assuming that things are consistent, and are continuing anyways." );
			this.coordinateTransformation = new IdentityTransformation();
			// yy this is the logic that I fould.  One could alternatively fail here. kai, feb'24
		}

	}

	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {
		switch (name) {
			case Counts.ELEMENT_NAME -> startMultiModeCounts(atts);
			case MeasurementLocation.ELEMENT_NAME -> startMeasurementLocation(atts);
			case Measurable.ELEMENT_NAME -> startMeasurable(name, atts);
			case TAG_ATTRIBUTES, TAG_ATTRIBUTE -> attributesDelegate.startTag(name, atts, context, currAttributes);
			case VALUE -> addValuesToMeasurable(atts);
		}
	}

	@Override
	public void endTag(String name, String content, Stack<String> context) {
		switch( name ) {
			case TAG_ATTRIBUTES:
				if (context.peek().equals(Counts.ELEMENT_NAME)) {
					String inputCRS = (String) counts.getAttributes().getAttribute( ProjectionUtils.INPUT_CRS_ATT );
					if (inputCRS != null && targetCRS != null) {
						if (externalInputCRS != null) {
							// warn or crash?
							log.warn("coordinate transformation defined both in config and in input file: setting from input file will be used");
						}
						coordinateTransformation = TransformationFactory.getCoordinateTransformation(inputCRS, targetCRS );
						ProjectionUtils.putCRS(counts, targetCRS);
					}
				}
				/* fall-through */
			case TAG_ATTRIBUTE:
				attributesDelegate.endTag(name, content, context);
				break;
		}
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
