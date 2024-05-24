package org.matsim.application.options;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


public class CsvOptionsTest {

	@TempDir
	public Path f;

	@Test
	void output() throws IOException {

		List<CsvOptions> delimiters = new ArrayList<>();

		delimiters.add(new CsvOptions(CSVFormat.Predefined.TDF));
		delimiters.add(new CsvOptions(CSVFormat.Predefined.Default));
		delimiters.add(new CsvOptions(CSVFormat.Predefined.Default, ';', StandardCharsets.UTF_8));

		for (CsvOptions csv : delimiters) {
			Path tmp = f.resolve("test.csv");

			CSVPrinter printer = csv.createPrinter(tmp);

			String delimiter = csv.getFormat().getDelimiterString();

			printer.printRecord("header", "column");
			printer.printRecord("1", "2");
			printer.printRecord("3", "4");
			printer.printRecord("5", "6");
			printer.close();

			assertThat(tmp)
				.hasContent("header" + delimiter + "column\n1" + delimiter + "2" + "\n3" + delimiter + "4" + "\n5" + delimiter + "6");

			assertThat(delimiter).isEqualTo(CsvOptions.detectDelimiter(tmp.toString()).toString());
		}
	}
}
