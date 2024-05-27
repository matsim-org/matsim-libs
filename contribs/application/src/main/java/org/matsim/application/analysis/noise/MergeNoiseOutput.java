package org.matsim.application.analysis.noise;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatFloatPair;
import it.unimi.dsi.fastutil.floats.FloatList;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2FloatMap;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import org.apache.avro.file.CodecFactory;
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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Merges noise data from multiple files into one file.
 */
final class MergeNoiseOutput {

	private static final Logger log = LogManager.getLogger(MergeNoiseOutput.class);


	/**
	 * If true, a CSV file is created for immissions. Deprecated, this code will be removed.
	 */
	private static final boolean CREATE_CSV_FILES = false;

	private final String[] inputPath;
	private final Path outputDirectory;
	private final String crs;
	private final String[] labels = {"immission", "emission"};
	private final int minTime = 3600;
	private int maxTime = 24 * 3600;

	MergeNoiseOutput(String[] inputPath, Path outputDirectory, String crs) {
		this.inputPath = inputPath;
		this.outputDirectory = outputDirectory;
		this.crs = crs;
	}

	/**
	 * Rounds a value to a given precision.
	 *
	 * @param value     value to round
	 * @param precision number of decimal places
	 * @return rounded value
	 */
	private static double round(double value, int precision) {
		return BigDecimal.valueOf(value).setScale(precision, RoundingMode.HALF_UP).doubleValue();
	}

	/**
	 * Returns the maximum time.
	 *
	 * @return maxTime value
	 */
	public int getMaxTime() {
		return maxTime;
	}

	/**
	 * Sets the maximum time.
	 *
	 * @param maxTime value
	 */
	public void setMaxTime(int maxTime) {
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
					if (CREATE_CSV_FILES) {
						mergeImmissionsCSV(inputPath[i], labels[i]);
					} else {
						mergeImissions(inputPath[i], labels[i]);
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
			dataFileWriter.setCodec(CodecFactory.deflateCodec(9));
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
	private void mergeImissions(String pathParameter, String label) {

		// data per time step, maps coord to value
		Int2ObjectMap<Object2FloatMap<FloatFloatPair>> data = new Int2ObjectOpenHashMap<>();

		// Loop over all files
		for (int time = minTime; time <= maxTime; time += 3600) {

			String path = pathParameter + label + "_" + round(time, 1) + ".csv";
			Object2FloatOpenHashMap<FloatFloatPair> values = new Object2FloatOpenHashMap<>();

			if (!Files.exists(Path.of(path))) {
				log.warn("File {} does not exist", path);
				continue;
			}

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
				FloatFloatPair coord = FloatFloatPair.of(x, y);
				values.put(coord, value);
			}

			data.put(time, values);
		}

		// hour data
		XYTData xytHourData = new XYTData();

		xytHourData.setTimestamps(data.keySet().intStream().boxed().toList());
		List<Float> xCoords = data.values().stream().flatMap(m -> m.keySet().stream().map(FloatFloatPair::firstFloat)).distinct().sorted().toList();
		List<Float> yCoords = data.values().stream().flatMap(m -> m.keySet().stream().map(FloatFloatPair::secondFloat)).distinct().sorted().toList();

		xytHourData.setXCoords(xCoords);
		xytHourData.setYCoords(yCoords);

		FloatList raw = new FloatArrayList();

		Object2FloatMap<FloatFloatPair> perDay = new Object2FloatOpenHashMap<>();

		for (Integer ts : xytHourData.getTimestamps()) {
			Object2FloatMap<FloatFloatPair> d = data.get((int) ts);

			for (Float x : xytHourData.getXCoords()) {
				for (Float y : xytHourData.getYCoords()) {
					FloatFloatPair coord = FloatFloatPair.of(x, y);
					float v = d.getOrDefault(coord, 0);
					raw.add(v);
					if (v > 0)
						perDay.mergeFloat(coord, v, Float::sum);
				}
			}
		}

		xytHourData.setData(Map.of("imissions", raw));
		xytHourData.setCrs(crs);

		File out = outputDirectory.getParent().resolve(label + "_per_hour.avro").toFile();

		writeAvro(xytHourData, out);

		raw = new FloatArrayList();
		// day data
		XYTData xytDayData = new XYTData();

		for (Float x : xytHourData.getXCoords()) {
			for (Float y : xytHourData.getYCoords()) {
				FloatFloatPair coord = FloatFloatPair.of(x, y);
				float v = perDay.getOrDefault(coord, 0);
				raw.add(v);
			}
		}

		xytDayData.setTimestamps(List.of(0));
		xytDayData.setXCoords(xCoords);
		xytDayData.setYCoords(yCoords);
		xytDayData.setData(Map.of("imissions", raw));
		xytDayData.setCrs(crs);

		File outDay = outputDirectory.getParent().resolve(label + "_per_day.avro").toFile();

		writeAvro(xytDayData, outDay);
	}

	// Merges the immissions data
	@Deprecated
	private void mergeImmissionsCSV(String pathParameter, String label) {
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
