package org.matsim.application.analysis.noise;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.application.avro.XYTData;
import org.matsim.core.utils.io.IOUtils;
import tech.tablesaw.api.*;
import tech.tablesaw.io.csv.CsvReadOptions;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.*;

/**
 * Merges noise data from multiple files into one file.
 */
final class MergeNoiseOutput {

	private static final Logger log = LogManager.getLogger(MergeNoiseOutput.class);
	private final String[] inputPath;
	private final Path outputDirectory;
	private final String[] labels = {"immission", "emission"};
	private final double minTime = 3600.;
	private double maxTime = 24. * 3600.;
	private boolean createCSVFileForImmissions = true;

	MergeNoiseOutput(String[] inputPath, Path outputDirectory) {
		this.inputPath = inputPath;
		this.outputDirectory = outputDirectory;
	}

	/**
	 * Rounds a value to a given precision.
	 *
	 * @param value     value to round
	 * @param precision number of decimal places
	 * @return rounded value
	 */
	private static double round(double value, int precision) {
		int scale = (int) Math.pow(10, precision);
		return (double) Math.round(value * scale) / scale;
	}

	/**
	 * Returns the maximum time.
	 *
	 * @return maxTime value
	 */
	public double getMaxTime() {
		return maxTime;
	}

	/**
	 * Sets the maximum time.
	 *
	 * @param maxTime value
	 */
	public void setMaxTime(double maxTime) {
		this.maxTime = maxTime;
	}

	/**
	 * Merges noise data from multiple files into one file.
	 */
	public void run() {

		// Loop over all paths
		for (int i = 0; i < labels.length; i++) {

			// Select the correct method based on the label
			switch (labels[i]) {
				case "immission" -> {
					if (createCSVFileForImmissions) {
						mergeImmissions(inputPath[i], labels[i]);
					} else {
						mergeImissionsAvro(inputPath[i], labels[i]);
					}

				}
				case "emission" -> mergeEmissions(inputPath[i], labels[i]);
				default -> log.warn("Unknown path: " + inputPath[i]);
			}

		}
	}

	/**
	 * Writes the given data to the given file.
	 *
	 * @param xytData
	 * @param output
	 */
	private void writeAvro(XYTData xytData, File output) {
		DatumWriter<XYTData> datumWriter = new SpecificDatumWriter<>(XYTData.class);
		try (DataFileWriter<XYTData> dataFileWriter = new DataFileWriter<>(datumWriter)) {
			dataFileWriter.create(xytData.getSchema(), IOUtils.getOutputStream(IOUtils.getFileUrl(output.toString()), false));
			dataFileWriter.append(xytData);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private void mergeEmissions(String pathParameter, String label) {
		log.info("Merging emissions data for label {}", label);
		Object2DoubleMap<String> mergedData = new Object2DoubleOpenHashMap<>();
		Table csvOutputMerged = Table.create(TextColumn.create("Link Id"), DoubleColumn.create("value"));

		for (double time = minTime; time <= maxTime; time += 3600.) {
			String path = pathParameter + label + "_" + this.round(time, 1) + ".csv";

			// Read the file
			Table table = Table.read().csv(CsvReadOptions.builder(IOUtils.getBufferedReader(path))
				.columnTypesPartial(Map.of("Link Id", ColumnType.TEXT))
				.sample(false)
				.separator(';').build());

			for (Row row : table) {
				// index for Noise Emission xx:xx:xx -> 7
				String linkId = row.getString("Link Id");
				double value = row.getDouble(7);
				mergedData.mergeDouble(linkId, value, Double::max);

			}
		}

		for (Object2DoubleMap.Entry<String> entry : mergedData.object2DoubleEntrySet()) {
			if (entry.getDoubleValue() >= 0.0) {
				Row writeRow = csvOutputMerged.appendRow();
				writeRow.setString("Link Id", entry.getKey());
				writeRow.setDouble("value", entry.getDoubleValue());
			}
		}

		File out = outputDirectory.getParent().resolve(label + "_per_day.csv").toFile();
		csvOutputMerged.write().csv(out);
		log.info("Merged noise data written to {} ", out);
	}

	/**
	 * Merges the immissions data
	 *
	 * @param pathParameter path to the immissions data
	 * @param label         label for the immissions data
	 */
	private void mergeImissionsAvro(String pathParameter, String label) {

		Object2DoubleMap<Coord> mergedData = new Object2DoubleOpenHashMap<>();

		Set<Float> xCoords = new HashSet<>();
		Set<Float> yCoords = new HashSet<>();
		List<Integer> times = new ArrayList<>();
		List<Float> values = new ArrayList<>();
		Map<CharSequence, List<Float>> valuesMap = new HashMap<>();


		// Loop over all files
		for (double time = minTime; time <= maxTime; time += 3600.) {

			times.add((int) time);
			String path = pathParameter + label + "_" + round(time, 1) + ".csv";

			// Read the file
			Table table = Table.read().csv(CsvReadOptions.builder(IOUtils.getBufferedReader(path))
				.columnTypesPartial(Map.of("x", ColumnType.FLOAT, "y", ColumnType.FLOAT, "Receiver Point Id", ColumnType.INTEGER, "t", ColumnType.DOUBLE))
				.sample(false)
				.separator(';').build());

			// Loop over all rows in the file
			for (Row row : table) {
				float x = row.getFloat("x");
				float y = row.getFloat("y");
				float value = (float) row.getDouble(1); // 1
				Coord coord = new Coord(x, y);

				xCoords.add(x);
				yCoords.add(y);
				values.add(value);

				mergedData.mergeDouble(coord, value, Double::max);
			}
		}
		valuesMap.put("imissions", values);

		// hour data
		XYTData xytHourData = new XYTData();

		xytHourData.setTimestamps(times);
		xytHourData.setXCoords(new ArrayList<>(xCoords));
		xytHourData.setYCoords(new ArrayList<>(yCoords));
		xytHourData.setData(valuesMap);
		xytHourData.setCrs("EPSG:25832");

		File out = outputDirectory.getParent().resolve(label + "_per_hour.avro.gz").toFile();

		writeAvro(xytHourData, out);

		// day data
		XYTData xytDayData = new XYTData();
		Set<Float> xCoordsDay = new HashSet<>();
		Set<Float> yCoordsDay = new HashSet<>();
		List<Float> valuesDay = new ArrayList<>();
		List<Integer> timesDay = new ArrayList<>();
		Map<CharSequence, List<Float>> valuesMapDay = new HashMap<>();

		// Create the merged data
		for (Object2DoubleMap.Entry<Coord> entry : mergedData.object2DoubleEntrySet()) {
			xCoordsDay.add((float) entry.getKey().getX());
			yCoordsDay.add((float) entry.getKey().getY());
			valuesDay.add((float) entry.getDoubleValue());
		}

		timesDay.add(0);

		valuesMapDay.put("imissions_day", valuesDay);
		xytDayData.setTimestamps(timesDay);
		xytDayData.setXCoords(new ArrayList<>(xCoordsDay));
		xytDayData.setYCoords(new ArrayList<>(yCoordsDay));
		xytDayData.setData(valuesMapDay);
		xytDayData.setCrs("EPSG:25832");
		File outDay = outputDirectory.getParent().resolve(label + "_per_day.avro.gz").toFile();
		writeAvro(xytDayData, outDay);

	}

	// Merges the immissions data
	private void mergeImmissions(String pathParameter, String label) {
		log.info("Merging immissions data for label {}", label);
		Object2DoubleMap<Coord> mergedData = new Object2DoubleOpenHashMap<>();

		Table csvOutputPerHour = Table.create(DoubleColumn.create("time"), DoubleColumn.create("x"), DoubleColumn.create("y"), DoubleColumn.create("value"));
		Table csvOutputMerged = Table.create(DoubleColumn.create("time"), DoubleColumn.create("x"), DoubleColumn.create("y"), DoubleColumn.create("value"));

		// Loop over all files
		for (double time = minTime; time <= maxTime; time += 3600.) {

			String path = pathParameter + label + "_" + round(time, 1) + ".csv";

			// Read the file
			Table table = Table.read().csv(CsvReadOptions.builder(IOUtils.getBufferedReader(path))
				.columnTypesPartial(Map.of("x", ColumnType.DOUBLE, "y", ColumnType.DOUBLE, "Receiver Point Id", ColumnType.INTEGER, "t", ColumnType.DOUBLE))
				.sample(false)
				.separator(';').build());

			// Loop over all rows in the file
			for (Row row : table) {
				double x = row.getDouble("x");
				double y = row.getDouble("y");
				Coord coord = new Coord(x, y);
				double value = row.getDouble(1); // 1

				mergedData.mergeDouble(coord, value, Double::max);

				Row writeRow = csvOutputPerHour.appendRow();
				writeRow.setDouble("time", time);
				writeRow.setDouble("x", coord.getX());
				writeRow.setDouble("y", coord.getY());
				writeRow.setDouble("value", value);
			}
		}

		// Create the merged data
		for (Object2DoubleMap.Entry<Coord> entry : mergedData.object2DoubleEntrySet()) {
			Row writeRow = csvOutputMerged.appendRow();
			writeRow.setDouble("time", 0.0);
			writeRow.setDouble("x", entry.getKey().getX());
			writeRow.setDouble("y", entry.getKey().getY());
			writeRow.setDouble("value", entry.getDoubleValue());
		}

		// Write the merged data (per hour) to a file
		File out = outputDirectory.getParent().resolve(label + "_per_hour.csv").toFile();
		csvOutputPerHour.write().csv(out);
		log.info("Merged noise data written to {} ", out);

		// Write the merged data (per day) to a file
		File outPerDay = outputDirectory.getParent().resolve(label + "_per_day.csv").toFile();
		csvOutputMerged.write().csv(outPerDay);
		log.info("Merged noise data written to {} ", outPerDay);

	}

}
