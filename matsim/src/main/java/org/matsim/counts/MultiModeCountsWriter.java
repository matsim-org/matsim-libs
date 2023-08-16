package org.matsim.counts;

import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.MatsimXmlWriter;
import org.matsim.utils.objectattributes.attributable.Attributable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Writer class for MultiModeCounts Object.
 */
public class MultiModeCountsWriter extends MatsimXmlWriter {

	Logger logger = LogManager.getLogger(MultiModeCountsWriter.class);

	private final MultiModeCounts counts;

	/**
	 * Default Constructor, takes the MultiModeCounts object as argument.
	 */
	public MultiModeCountsWriter(MultiModeCounts counts) {
		this.counts = counts;
	}

	/**
	 * Writes the counts to a file.
	 *
	 * @param filename output file path
	 */
	public void write(String filename) {
		boolean b = filename.endsWith(".gz");

		if (b)
			super.useCompression = true;

		logger.info("Write MultiModeCounts to {}", filename);

		super.openFile(filename);

		super.writeXmlHead();

		super.writeStartTag(MultiModeCounts.ELEMENT_NAME, attributesAsTupleList(counts), false, true);
		super.writeElement("name", counts.getName());
		super.writeElement("description", counts.getDescription());
		super.writeElement("source", counts.getSource());
		super.writeElement("year", String.valueOf(counts.getYear()));
		super.writeElement("identifiable", counts.identifiable.getName());

		writeElement("measurableTags", counts.getMeasurableTags().stream().reduce((s, s2) -> s.concat(",").concat(s2)).orElse(""));

		writeCounts();
		super.writeEndTag(MultiModeCounts.ELEMENT_NAME);
		super.close();
	}

	/**
	 * Writes the counts to a file.
	 *
	 * @param filename output file path
	 */
	public void write(Path filename) {
		write(filename.toString());
	}

	private void writeCounts() {

		writeStartTag("counts", null, false, false);

		for (MultiModeCount count : counts.getCounts().values()) {

			writeStartTag(MultiModeCount.ELEMENT_NAME, attributesAsTupleList(count), false, false);

			writeElement("id", count.getId().toString());
			writeElement("stationName", count.getStationName());
			writeElement("year", String.valueOf(count.getYear()));

			String description = count.getDescription();
			if (description != null)
				writeElement("description", description);

			//write volumes for each mode
			writeMeasurables(count);

			writeEndTag(MultiModeCount.ELEMENT_NAME);
			writeEmptyLine();
		}

		writeEndTag("counts");

	}

	private void writeMeasurables(MultiModeCount count) {

		Map<String, Map<String, Measurable>> measurables = count.getMeasurables();

		for (Map.Entry<String, Map<String, Measurable>> entry : measurables.entrySet()) {

			//write type of measurable data
			writeStartTag(Measurable.ELEMENT_NAME, null);

			for (Measurable m : entry.getValue().values()) {
				boolean onlyDailyValues = m.hasOnlyDailyValues();

				writeStartTag(m.getMeasurableType(), List.of(new Tuple<>("mode", m.getMode()), new Tuple<>("hasOnlyDailyValues",
						String.valueOf(String.valueOf(onlyDailyValues).charAt(0)))));

				//write either aggregated or disaggregated values
				if (onlyDailyValues) {
					writeElement("daily", String.valueOf(m.getDailyValue()));
				} else {
					Int2DoubleMap hourlyValues = m.getHourlyValues();
					try {
						for (Integer hour : hourlyValues.keySet()) {
							double v = hourlyValues.get(hour.intValue());
							writeEmptyLine();

							//start tag
							super.writer.write("\t\t\t\t\t<value ");

							super.writer.write("h=\"" + hour + "\" ");

							super.writer.write("v=\"" + v + "\" ");

							//end tag
							super.writer.write("/>");
						}
					} catch (IOException e){
						logger.error("Error writing Measurables", e);
					}
				}

				writeEndTag(m.getMeasurableType());
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

	private List<Tuple<String, String>> attributesAsTupleList(Attributable attributable) {
		List<Tuple<String, String>> result = new ArrayList<>();

		for (Map.Entry<String, Object> entry : attributable.getAttributes().getAsMap().entrySet()) {
			Tuple<String, String> tuple = new Tuple<>(entry.getKey(), entry.getValue().toString());
			result.add(tuple);
		}

		return result;
	}
}
