package org.matsim.counts;

import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.MatsimXmlWriter;
import org.matsim.utils.objectattributes.attributable.Attributable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Writer class for MultiModeCounts Object.
 */
public class MultiModeCountsWriter extends MatsimXmlWriter {

	Logger logger = LogManager.getLogger(MultiModeCountsWriter.class);

	private final MultiModeCounts<?> counts;

	/**
	 * Default Constructor, takes the MultiModeCounts object as argument.
	 */
	public MultiModeCountsWriter(MultiModeCounts<?> counts) {
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

		List<Tuple<String, String>> variables = new ArrayList();

		variables.add(new Tuple<>("measurableTags", counts.getMeasurableTags().stream().reduce((s, s2) -> s.concat(",").concat(s2)).orElse("")));
		variables.add(new Tuple<>("source", counts.getSource()));
		variables.add(new Tuple<>("name", counts.getName()));
		variables.add(new Tuple<>("year", String.valueOf(counts.getYear())));
		variables.add(new Tuple<>("description", counts.getDescription()));

		List<Tuple<String, String>> filtered = variables.stream().filter(tuple -> tuple.getSecond() != null).collect(Collectors.toList());

		super.writeStartTag(MultiModeCounts.ELEMENT_NAME, filtered, false, true);
		super.writeStartTag("attributes", attributesAsTupleList(counts), true, false);

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

		for (MultiModeCount<?> count : counts.getCounts().values()) {

			List<Tuple<String, String>> variables = new ArrayList();
			variables.add(new Tuple<>("id", count.getId().toString()));
			variables.add(new Tuple<>("stationName", count.getStationName()));
			variables.add(new Tuple<>("year", String.valueOf(count.getYear())));
			variables.add(new Tuple<>("description", counts.getDescription()));

			List<Tuple<String, String>> filtered = variables.stream().filter(tuple -> tuple.getSecond() != null).toList();

			writeStartTag(MultiModeCount.ELEMENT_NAME, filtered, false, false);

			writeStartTag("attributes", attributesAsTupleList(count), true, false);

			//write volumes for each mode
			writeMeasurables(count);

			writeEndTag(MultiModeCount.ELEMENT_NAME);
			writeEmptyLine();
		}

		writeEndTag("counts");

	}

	private void writeMeasurables(MultiModeCount<?> count) {

		Map<String, Map<String, Measurable>> measurables = count.getMeasurables();

		for (Map.Entry<String, Map<String, Measurable>> entry : measurables.entrySet()) {

			//write type of measurable data
			for (Measurable m : entry.getValue().values()) {
				int interval = m.getInterval();

				writeStartTag(m.getMeasurableType(), List.of(new Tuple<>("mode", m.getMode()), new Tuple<>("interval",
						String.valueOf(interval))));

				//write values
				Int2DoubleMap hourlyValues = m.getValues();
				try {
					for (Integer hour : hourlyValues.keySet()) {
						double v = hourlyValues.get(hour.intValue());
						writeEmptyLine();

						//start tag
						super.writer.write("\t\t\t\t\t<value ");

						super.writer.write("t=\"" + hour + "\" ");

						super.writer.write("v=\"" + v + "\" ");

						//end tag
						super.writer.write("/>");
					}
				} catch (IOException e) {
					logger.error("Error writing Measurables", e);
				}

				writeEndTag(m.getMeasurableType());
			}
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
