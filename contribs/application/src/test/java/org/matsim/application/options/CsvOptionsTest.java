package org.matsim.application.options;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;


public class CsvOptionsTest {

	@TempDir
	public Path f;

	@Test
	void output() throws IOException {

		CsvOptions csv = new CsvOptions(CSVFormat.Predefined.TDF);

		Path tmp = f.resolve("test.csv");

		CSVPrinter printer = csv.createPrinter(tmp);

		printer.printRecord("header", "column");
		printer.printRecord("1", "2");
		printer.close();

		assertThat(tmp)
				.hasContent("header\tcolumn\n1\t2");

	}
}
