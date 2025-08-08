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
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.application.avro.XYTData;
import org.matsim.application.options.CsvOptions;
import org.matsim.core.config.Config;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;
import tech.tablesaw.api.*;
import tech.tablesaw.io.csv.CsvReadOptions;

import java.math.BigDecimal;
import java.math.RoundingMode;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.geotools.gml3.v3_2.GML.coordinateSystem;

/**
 * Merges noise data from multiple files into one file.
 */
final class MergeNoiseOutput {

	private static final Logger log = LogManager.getLogger(MergeNoiseOutput.class);


	private static final int HOUR_STEP = 3600;

	private final Path outputDirectory;
	private final String crs;
	private final int minTime = HOUR_STEP;

	private final Map<String,Float> totalReceiverPointValues = new HashMap<>();
	private final int maxTime = 24 * HOUR_STEP;

	MergeNoiseOutput(Path outputDirectory, String coordinateSystem) {
		this.outputDirectory = outputDirectory;
		this.crs = coordinateSystem;
	}

	/**
	 * Returns a formatted time string using the given time format.
	 *
	 * @param time time in seconds
	 * @return formatted time string
	 */
	private static String formatTime(double time) {
		return Time.writeTime(time, Time.TIMEFORMAT_HHMMSS);
	}

	/**
	 * Averages the given noise levels. The formula is: 10 * log10(1/n * sum(10^(L/10))),
	 * where n is the number of values and L is the noise level.
	 * Source: <a href="https://de.wikipedia.org/wiki/Mittelungspegel">Wikipedia:
	 * Mittelungspegel</a> (19.02.2025); DIN 45641 – Mittelung von Schallpegeln
	 *
	 * @param pegel nosise levels
	 * @param day
	 * @return averaged noise level
	 */
	private static double averagingLevel(double[] pegel, DaySection day) {
		double sum = 0;

		// The day is divided into three sections: day, evening, and night
		// The day is from 06:00 to 18:00, the evening from 18:00 to 22:00,
		// and the night from 22:00 to 06:00
		if (day == DaySection.DAY) {
			for (int i = 6; i < 18; i++) {
				sum += Math.pow(10, pegel[i] / 10);
			}
			return 10 * Math.log10(sum / 12);
		} else if (day == DaySection.EVENING) {
			for (int i = 18; i < 22; i++) {
				sum += Math.pow(10, pegel[i] / 10);
			}
			return 10 * Math.log10(sum / 4);
		} else if (day == DaySection.NIGHT) {
			for (int i = 0; i < 6; i++) {
				sum += Math.pow(10, pegel[i] / 10);
			}
			for (int i = 22; i < 24; i++) {
				sum += Math.pow(10, pegel[i] / 10);
			}
			return 10 * Math.log10(sum / 8);
		}
		return 0;
	}

	/**
	 * Calculates the L_DEN value based on the given noise levels.
	 * <p>
	 * Sources:
	 * <p>
	 * RICHTLINIE 2002/49/EG DES EUROPÄISCHEN PARLAMENTS UND DES RATES
	 * vom 25. Juni 2002 über die Bewertung und Bekämpfung von Umgebungslärm
	 * (Anhang 1)
	 *
	 * @param lDay     noise level during the day (L_day)
	 * @param lEvening noise level during the evening (L_evening)
	 * @param lNight   noise level during the night (L_night)
	 * @return calculated L_DEN
	 */
	private static double calculateLDen(double lDay, double lEvening, double lNight) {
		return 10 * Math.log10((12 * Math.pow(10, lDay / 10) + 4 * Math.pow(10, (lEvening + 5) / 10) + 8 * Math.pow(10, (lNight + 10) / 10)) / 24);
	}
	/**
	 * Rounds a value to a given precision.
	 *
	 * @param value     value to round
	 * @return rounded value
	 */
	private double roundToOneDecimalPlace(double value) {
		return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
	}

	/**
	 * Checks if the given time falls in the nighttime window (i.e. before 06:00 or after 22:00).
	 *
	 * @param time time in seconds
	 * @return true if time is within night range, false otherwise
	 */
	private boolean isNightTime(double time) {
		int nightBreakStart = 6 * HOUR_STEP;
		int nightBreakEnd = 22 * HOUR_STEP;
		return time <= nightBreakStart || time > nightBreakEnd;
	}

	/**
	 * Merges noise data from multiple files into one file.
	 */
	public void run() throws IOException {
		mergeReceiverPointData(outputDirectory.resolve("immissions") + File.separator, "immission");
		mergeReceiverPointData(outputDirectory.resolve("damages_receiverPoint") + File.separator, "damages_receiverPoint");
		mergeLinkData(outputDirectory.resolve("emissions") + File.separator);
	}

	/**
	 * Writes the given data to the given file.
	 *
	 * @param xytData data to write
	 * @param output  target file
	 */
	private void writeAvro(XYTData xytData, File output) {
		log.info("Start writing avro file to {}", output);
		DatumWriter<XYTData> datumWriter = new SpecificDatumWriter<>(XYTData.class);
		try (DataFileWriter<XYTData> dataFileWriter = new DataFileWriter<>(datumWriter)) {
			dataFileWriter.setCodec(CodecFactory.deflateCodec(9));
			dataFileWriter.create(xytData.getSchema(), IOUtils.getOutputStream(IOUtils.getFileUrl(output.toString()), false));
			dataFileWriter.append(xytData);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Creates the L_DEN, L_day, L_evening, and L_night values for each link.
	 *
	 * @param basePath Directory containing emissions CSV files
	 * @throws IOException if an I/O error occurs
	 */
	private void mergeLinkData(String basePath) throws IOException {

		Map<String, double[]> linkNoiseValues = new LinkedHashMap<>();

		for (int i = 0; i < 24; i++) {

			double time = minTime + i * HOUR_STEP;
			String fileName = basePath + "emission_" + roundToOneDecimalPlace(time) + ".csv";
			if (!Files.exists(Path.of(fileName))) {
				log.warn("File {} does not exist", fileName);
				continue;
			}

			String columnName = "Noise Emission " + formatTime(time);
			Table table = Table.read().csv(
				CsvReadOptions.builder(IOUtils.getBufferedReader(fileName))
					.columnTypesPartial(Map.of("Link Id", ColumnType.TEXT, columnName, ColumnType.DOUBLE))
				.sample(false)
					.separator(CsvOptions.detectDelimiter(fileName))
					.build()
			);

			for (Row row : table) {
				String linkId = row.getString("Link Id");
				double value = row.getDouble(row.columnCount() - 1);
				linkNoiseValues.computeIfAbsent(linkId, k -> new double[24])[i] = value;

			}
		}

		Path out = outputDirectory.getParent().resolve("emissions.csv");

		try (CSVPrinter csv = new CSVPrinter(Files.newBufferedWriter(out), CSVFormat.DEFAULT)) {
			csv.printRecord("Link Id", "L_day (dB(A))", "L_evening (dB(A))", "L_night (dB(A))", "L_DEN (dB(A))");
			for (Map.Entry<String, double[]> entry : linkNoiseValues.entrySet()) {
				double[] values = entry.getValue();
				double lDay = averagingLevel(values, DaySection.DAY);
				double lEvening = averagingLevel(values, DaySection.EVENING);
				double lNight = averagingLevel(values, DaySection.NIGHT);
				double lDEN = calculateLDen(lDay, lEvening, lNight);
				csv.printRecord(entry.getKey(), lDay, lEvening, lNight, lDEN);
			}
		}

		log.info("Merged noise data written to {}", out);
	}

	/**
	 * Merges receiver point data (written by {@link org.matsim.contrib.noise.NoiseWriter}).
	 *
	 * @param outputDir Directory path containing receiver point CSV files
	 * @param label     Label for the receiver point data (which kind of data)
	 * @throws IOException if an I/O error occurs
	 */
	private void mergeReceiverPointData(String outputDir, String label) throws IOException {

		// Map of time-step to data mapping (coord -> value)
		Int2ObjectMap<Object2FloatMap<FloatFloatPair>> data = new Int2ObjectOpenHashMap<>();

		// Compute the capitalized base label once.
		String baseLabel = label.contains("_") ? label.substring(0, label.lastIndexOf("_")) : label;
		String capitalizedLabel = StringUtils.capitalize(baseLabel);

		// Process each time step.
		for (int time = minTime; time <= maxTime; time += HOUR_STEP) {
			String timeDataFile = outputDir + label + "_" + roundToOneDecimalPlace(time) + ".csv";

			if (!Files.exists(Path.of(timeDataFile))) {
				log.warn("File {} does not exist", timeDataFile);
				continue;
			}

			String valueHeader = capitalizedLabel + " " + Time.writeTime(time, Time.TIMEFORMAT_HHMMSS);
			Object2FloatOpenHashMap<FloatFloatPair> values = new Object2FloatOpenHashMap<>();

			Table dataTable = Table.read().csv(CsvReadOptions.builder(IOUtils.getBufferedReader(timeDataFile))
				.columnTypesPartial(Map.of("x", ColumnType.FLOAT,
					"y", ColumnType.FLOAT,
					"Receiver Point Id", ColumnType.INTEGER,
					"t", ColumnType.DOUBLE,
					valueHeader, ColumnType.DOUBLE))
				.sample(false)
					.separator(CsvOptions.detectDelimiter(timeDataFile))
					.build()
			);

			for (Row row : dataTable) {
				float x = row.getFloat("x");
				float y = row.getFloat("y");
				float value = (float) row.getDouble(valueHeader);
				FloatFloatPair coord = FloatFloatPair.of(x, y);
				values.put(coord, value);
			}

			data.put(time, values);
		}

		// Build per-hour XYTData.
		XYTData xytHourData = new XYTData();
		List<Integer> timestamps = data.keySet().intStream().boxed().sorted().toList();
		xytHourData.setTimestamps(timestamps);

		List<Float> xCoords = data.values().stream().flatMap(m -> m.keySet().stream().map(FloatFloatPair::firstFloat)).distinct().sorted().toList();
		List<Float> yCoords = data.values().stream().flatMap(m -> m.keySet().stream().map(FloatFloatPair::secondFloat)).distinct().sorted().toList();

		xytHourData.setXCoords(xCoords);
		xytHourData.setYCoords(yCoords);

		FloatList rawHourData = new FloatArrayList();

		Object2FloatMap<FloatFloatPair> perDay = new Object2FloatOpenHashMap<>();

		for (Integer ts : timestamps) {
			Object2FloatMap<FloatFloatPair> timeData = data.get(ts);
			for (Float x : xCoords) {
				for (Float y : yCoords) {
					FloatFloatPair coord = FloatFloatPair.of(x, y);
					float v = timeData.getOrDefault(coord, 0);
					rawHourData.add(v);
					if (v > 0) {
						perDay.mergeFloat(coord, v, Float::sum);
				}
			}
		}
		}

		xytHourData.setData(Map.of(label, rawHourData));
		xytHourData.setCrs(crs);

		File outHour = outputDirectory.getParent().resolve(label + "_per_hour.avro").toFile();
		writeAvro(xytHourData, outHour);

		// Build per-day XYTData.
		FloatList rawDayData = new FloatArrayList();
		for (Float x : xCoords) {
			for (Float y : yCoords) {
				FloatFloatPair coord = FloatFloatPair.of(x, y);
				float v = perDay.getOrDefault(coord, 0);
				rawDayData.add(v);
			}
		}

		XYTData xytDayData = new XYTData();
		xytDayData.setTimestamps(List.of(0));
		xytDayData.setXCoords(xCoords);
		xytDayData.setYCoords(yCoords);
		xytDayData.setData(Map.of(label, rawDayData));
		xytDayData.setCrs(crs);

		File outDay = outputDirectory.getParent().resolve(label + "_per_day.avro").toFile();

		writeAvro(xytDayData, outDay);
		// Cache the overall sum.
		float totalSum = rawDayData.stream().reduce(0f, Float::sum);
		totalReceiverPointValues.put(baseLabel, totalSum);
	}

	// Merges the immissions data (deprecated)

	@Deprecated
	private void mergeImmissionsCSV(String pathParameter, String label) throws IOException {
		log.info("Merging immissions data for label {}", label);
		Object2DoubleMap<Coord> mergedData = new Object2DoubleOpenHashMap<>();

		Table csvOutputPerHour = Table.create(
			DoubleColumn.create("time"),
			DoubleColumn.create("x"),
			DoubleColumn.create("y"),
			DoubleColumn.create("value")
		);
		Table csvOutputMerged = Table.create(
			DoubleColumn.create("time"),
			DoubleColumn.create("x"),
			DoubleColumn.create("y"),
			DoubleColumn.create("value")
		);

		for (double time = minTime; time <= maxTime; time += HOUR_STEP) {
			String filePath = pathParameter + label + "_" + roundToOneDecimalPlace(time) + ".csv";
			Table table = Table.read().csv(
				CsvReadOptions.builder(IOUtils.getBufferedReader(filePath))
				.columnTypesPartial(Map.of("x", ColumnType.DOUBLE,
					"y", ColumnType.DOUBLE,
					"Receiver Point Id", ColumnType.INTEGER,
					"t", ColumnType.DOUBLE))
				.sample(false)
					.separator(CsvOptions.detectDelimiter(filePath))
					.build()
			);

			for (Row row : table) {
				double x = row.getDouble("x");
				double y = row.getDouble("y");
				Coord coord = new Coord(x, y);
				double value = row.getDouble(1);

				mergedData.mergeDouble(coord, value, Double::max);

				Row writeRow = csvOutputPerHour.appendRow();
				writeRow.setDouble("time", time);
				writeRow.setDouble("x", coord.getX());
				writeRow.setDouble("y", coord.getY());
				writeRow.setDouble("value", value);
			}
		}

		for (Object2DoubleMap.Entry<Coord> entry : mergedData.object2DoubleEntrySet()) {
			Row writeRow = csvOutputMerged.appendRow();
			writeRow.setDouble("time", 0.0);
			writeRow.setDouble("x", entry.getKey().getX());
			writeRow.setDouble("y", entry.getKey().getY());
			writeRow.setDouble("value", entry.getDoubleValue());
		}

		File outHour = outputDirectory.getParent().resolve(label + "_per_hour.csv").toFile();
		csvOutputPerHour.write().csv(outHour);
		log.info("Merged noise data written to {}", outHour);

		File outDay = outputDirectory.getParent().resolve(label + "_per_day.csv").toFile();
		csvOutputMerged.write().csv(outDay);
		log.info("Merged noise data written to {}", outDay);
	}

	public Map<String, Float> getTotalReceiverPointValues() {
		return totalReceiverPointValues;
	}
	private enum DaySection {
		DAY,
		EVENING,
		NIGHT
	}
}
