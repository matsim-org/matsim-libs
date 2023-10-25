package org.matsim.counts;

import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.io.MatsimXmlWriter;
import org.matsim.utils.objectattributes.attributable.AttributesXmlWriterDelegate;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Writer class for <code>counts_v2.xsd</code>.
 */
final class CountsWriterV2 extends MatsimXmlWriter {

	private static final Logger logger = LogManager.getLogger(CountsWriterV2.class);

	private final Counts<?> counts;

	private final AttributesXmlWriterDelegate attributesWriter = new AttributesXmlWriterDelegate();
	private final CoordinateTransformation coordinateTransformation;


	/**
	 * Default Constructor, takes the Counts object as argument.
	 */
	public CountsWriterV2(CoordinateTransformation coordinateTransformation, Counts<?> counts) {
		this.coordinateTransformation = coordinateTransformation;
		this.counts = counts;
	}

	/**
	 * Writes the counts to a file.
	 *
	 * @param filename output file path
	 */
	public void write(String filename) throws IOException {
		logger.info("Write Counts to {}", filename);

		this.openFile(filename);

		this.writeXmlHead();
		this.writeRootElement();

		super.close();
	}

	/**
	 * Writes the counts to a file.
	 *
	 * @param filename output file path
	 */
	public void write(Path filename) throws IOException {
		write(filename.toString());
	}

	private void writeRootElement() {

		List<Tuple<String, String>> atts = new ArrayList<>();

		atts.add(createTuple(XMLNS, MatsimXmlWriter.MATSIM_NAMESPACE));
		atts.add(createTuple(XMLNS + ":xsi", DEFAULTSCHEMANAMESPACELOCATION));
		atts.add(createTuple("xsi:schemaLocation", MATSIM_NAMESPACE + " " + DEFAULT_DTD_LOCATION + "counts_v2.xsd"));

		if (counts.getSource() != null)
			atts.add(createTuple("source", counts.getSource()));

		if (counts.getName() != null)
			atts.add(createTuple("name", counts.getName()));

		if (counts.getDescription() != null)
			atts.add(createTuple("description", counts.getDescription()));

		atts.add(createTuple("year", String.valueOf(counts.getYear())));

		this.writeStartTag(Counts.ELEMENT_NAME, atts, false, true);

		attributesWriter.writeAttributes("\t", this.writer, counts.getAttributes());

		writeCounts();

		this.writeContent("\n", true);
		this.writeEndTag(Counts.ELEMENT_NAME);
	}

	private void writeCounts() {

		for (MeasurementLocation<?> count : counts.getMeasureLocations().values()) {

			List<Tuple<String, String>> attributes = new ArrayList<>();

			attributes.add(createTuple("refId", count.getRefId().toString()));

			if (count.getId() != null)
				attributes.add(createTuple("id", count.getId()));

			if (count.getStationName() != null)
				attributes.add(createTuple("name", count.getStationName()));

			if (count.getDescription() != null)
				attributes.add(createTuple("description", counts.getDescription()));

			if (count.getCoordinates() != null) {
				Coord c = coordinateTransformation.transform(count.getCoordinates());
				attributes.add(createTuple("x", c.getX()));
				attributes.add(createTuple("y", c.getY()));
			}

			writeStartTag(MeasurementLocation.ELEMENT_NAME, attributes, false, true);

			attributesWriter.writeAttributes("\t\t", this.writer, count.getAttributes());

			//write volumes for each mode
			writeMeasurables(count);

			writeEndTag(MeasurementLocation.ELEMENT_NAME);
			writeEmptyLine();
		}

	}

	private void writeMeasurables(MeasurementLocation<?> count) {

		Map<MeasurementLocation.TypeAndMode, Measurable> measurables = count.getMeasurables();

		for (Map.Entry<MeasurementLocation.TypeAndMode, Measurable> entry : measurables.entrySet()) {

			Measurable m = entry.getValue();

			int interval = m.getInterval();

			writeStartTag(Measurable.ELEMENT_NAME, List.of(
				new Tuple<>("type", m.getMeasurableType()),
				new Tuple<>("networkMode", m.getMode()),
				new Tuple<>("interval", String.valueOf(interval)))
			);

			//write values
			Int2DoubleMap values = m.getValues();
			try {
				for (int second : values.keySet()) {
					double v = values.get(second);
					writeEmptyLine();

					//start tag
					super.writer.write("\t\t\t\t<value ");

					super.writer.write("t=\"" + second + "\" ");

					super.writer.write("val=\"" + v + "\" ");

					//end tag
					super.writer.write("/>");
				}
			} catch (IOException e) {
				logger.error("Error writing Measurables", e);
			}

			writeEndTag(Measurable.ELEMENT_NAME);
		}
	}

	private void writeEmptyLine() {
		try {
			this.writer.write(NL);
		} catch (IOException e) {
			logger.warn("Error writing counts", e);
		}
	}
}
