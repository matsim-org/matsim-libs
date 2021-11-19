package org.matsim.application.options;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;


public class CsvOptionsTest {

	@Rule
	public TemporaryFolder f = new TemporaryFolder();

	@Test
	public void output() throws IOException {

		CsvOptions csv = new CsvOptions(CSVFormat.Predefined.TDF);

		Path tmp = f.getRoot().toPath().resolve("test.csv");

		CSVPrinter printer = csv.createPrinter(tmp);

		printer.printRecord("header", "column");
		printer.printRecord("1", "2");
		printer.close();

		assertThat(tmp)
				.hasContent("header\tcolumn\n1\t2");

	}
}