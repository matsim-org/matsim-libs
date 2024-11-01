package org.matsim.application.avro;

import org.apache.avro.file.DataFileReader;
import org.apache.avro.specific.SpecificDatumReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.testcases.MatsimTestUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.matsim.core.utils.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CSVToAvroConverterTest {

	@RegisterExtension
	public final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void conversion() throws IOException {
		String input = utils.getInputDirectory() + "exampleCSV.csv";
		String output = utils.getOutputDirectory() + "exampleAvro.avro";

		CSVToAvroConverter.main(new String[]{input, output});

		// Verify the output Avro file exists
		Path outputPath = Path.of(output);
		assertTrue(Files.exists(outputPath), "The Avro output file should exist.");

		Set<Double> uniqueTimes = new HashSet<>();
		Set<Double> uniqueX = new HashSet<>();
		Set<Double> uniqueY = new HashSet<>();

		try (CSVParser csvParser = new CSVParser(IOUtils.getBufferedReader(input), CSVFormat.DEFAULT.builder()
			.setCommentMarker('#')
			.setSkipHeaderRecord(true)
			.setHeader("time", "x", "y", "value")
			.build())) {
			for (CSVRecord record : csvParser) {
				uniqueTimes.add(Double.parseDouble(record.get("time")));
				uniqueX.add(Double.parseDouble(record.get("x")));
				uniqueY.add(Double.parseDouble(record.get("y")));
			}
		}

		// Check if the avro file has the expected number of unique entries
		int expectedTimeCount = uniqueTimes.size();
		int expectedXCount = uniqueX.size();
		int expectedYCount = uniqueY.size();
		int expectedEmissionsSize = expectedTimeCount * expectedXCount * expectedYCount;

		// Verify the avro data
		SpecificDatumReader<XYTData> datumReader = new SpecificDatumReader<>(XYTData.class);
		try (DataFileReader<XYTData> dataFileReader = new DataFileReader<>(new File(output), datumReader)) {
			assertTrue(dataFileReader.hasNext(), "There should be at least one record in the Avro file.");

			XYTData avroData = dataFileReader.next();

			// Verify the number of unique entries in the Avro file matches the CSV data
			assertEquals(expectedTimeCount, avroData.getTimestamps().size(), "The number of unique time entries should match.");
			assertEquals(expectedXCount, avroData.getXCoords().size(), "The number of unique x-coordinates should match.");
			assertEquals(expectedYCount, avroData.getYCoords().size(), "The number of unique y-coordinates should match.");

			// Check if the data map has the expected number of entries
			Map<CharSequence, List<Float>> emissionsData = avroData.getData();

			for (Map.Entry<CharSequence, List<Float>> entry : emissionsData.entrySet()) {
				assertNotNull(entry.getValue(), "The Emissions data should not be null.");
				assertEquals(expectedEmissionsSize, entry.getValue().size(), "The size of the Emissions data should be timeCount * xCount * yCount.");
			}
		}

		Files.delete(outputPath);
	}
}
