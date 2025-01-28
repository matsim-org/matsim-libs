package org.matsim.application.avro;

import it.unimi.dsi.fastutil.objects.Object2FloatAVLTreeMap;
import it.unimi.dsi.fastutil.objects.Object2FloatSortedMap;
import org.apache.avro.file.CodecFactory;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.matsim.core.utils.io.IOUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public class CSVToAvroConverter {

	public static void main(String[] args) throws IOException {
		String projection = args.length > 2 ? args[2] : null;
		String name = args.length > 3 ? args[3] : "Emissions";

		XYTData avroData = readCSV(args[0], projection, name);
		writeAvroFile(avroData, Path.of(args[1]));
	}

	/**
	 * Reads a CSV file, processes its data, and returns the corresponding Avro object.
	 *
	 * @param csvFilePath the path to the CSV file
	 * @param projection  the projection (CRS)
	 * @param name        the name for the data series (defaults is "Emissions")
	 * @throws IOException if an error occurs during reading the file
	 */
	public static XYTData readCSV(String csvFilePath, String projection, String name) throws IOException {
		List<CSVEntries> entries = new ArrayList<>();
		List<Float> xCoords = new ArrayList<>();
		List<Float> yCoords = new ArrayList<>();
		List<Integer> timestamps = new ArrayList<>();
		Object2FloatSortedMap<XYT> valuesMap = new Object2FloatAVLTreeMap<>(Comparator.comparing((XYT e) -> e.t)
			.thenComparing(e -> e.x)
			.thenComparing(e -> e.y));

		try (CSVParser csvReader = new CSVParser(IOUtils.getBufferedReader(csvFilePath), CSVFormat.DEFAULT.builder()
			.setCommentMarker('#').setSkipHeaderRecord(true).setHeader().build())) {

			String comment = csvReader.getHeaderComment();

			if (comment != null && (projection == null || projection.isEmpty())) {
				projection = comment;
			} else if (projection == null) {
				projection = "";
			}

			for (CSVRecord record : csvReader) {
				try {
					int time = (int) Double.parseDouble(record.get(0));
					float x = Float.parseFloat(record.get(1));
					float y = Float.parseFloat(record.get(2));
					float value = Float.parseFloat(record.get(3));

					entries.add(new CSVEntries(time, x, y, value));

				} catch (NumberFormatException e) {
					System.out.println("Skipping invalid line: " + String.join(",", record));
				}
			}
		}

		// Sort entries by time -> x -> y
		entries.sort(Comparator.comparing((CSVEntries e) -> e.time)
			.thenComparing(e -> e.x)
			.thenComparing(e -> e.y));

		for (CSVEntries entry : entries) {
			if (!xCoords.contains(entry.x)) {
				xCoords.add(entry.x);
			}
			if (!yCoords.contains(entry.y)) {
				yCoords.add(entry.y);
			}
			if (!timestamps.contains(entry.time)) {
				timestamps.add(entry.time);
			}

			valuesMap.put(new XYT(entry.x, entry.y, entry.time), entry.value);
		}

		// Check if all combinations of x, y, and time exist
		for (int time : timestamps) {
			for (float x : xCoords) {
				for (float y : yCoords) {
					XYT key = new XYT(x, y, time);
					if (!valuesMap.containsKey(key)) {
						valuesMap.put(key, 0f);
					}
				}
			}
		}

		// Create Avro data object
		XYTData avroData = new XYTData();
		avroData.setCrs(projection);
		avroData.setXCoords(xCoords);
		avroData.setYCoords(yCoords);
		avroData.setTimestamps(timestamps);

		List<Float> valuesList = new ArrayList<>(valuesMap.values());
		Map<CharSequence, List<Float>> result = new HashMap<>();
		result.put(name != null && !name.isEmpty() ? name : "Emissions", valuesList);

		avroData.setData(result);

		return avroData;
	}

	/**
	 * Writes the Avro data
	 *
	 * @param avroData the Avro data
	 * @param avroFile the path to the output Avro file
	 * @throws IOException if an error occurs during writing the file
	 */
	public static void writeAvroFile(XYTData avroData, Path avroFile) throws IOException {
		DatumWriter<XYTData> datumWriter = new SpecificDatumWriter<>(XYTData.class);
		try (DataFileWriter<XYTData> dataFileWriter = new DataFileWriter<>(datumWriter)) {
			dataFileWriter.setCodec(CodecFactory.deflateCodec(9));
			dataFileWriter.create(XYTData.getClassSchema(), avroFile.toFile());
			dataFileWriter.append(avroData);
		}
	}

	private record CSVEntries(int time, float x, float y, float value) {
	}

	private record XYT(float x, float y, float t) {
	}
}
