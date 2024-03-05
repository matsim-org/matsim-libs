package org.matsim.application.options;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang.StringUtils;
import org.matsim.core.utils.io.IOUtils;
import picocli.CommandLine;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/**
 * Options for reading a general csv file.
 *
 * @see picocli.CommandLine.Mixin
 */
public final class CsvOptions {

	@CommandLine.Option(names = "--csv-format", description = "CSV Format", defaultValue = "Default")
	private CSVFormat.Predefined csvFormat;

	@CommandLine.Option(names = "--csv-delimiter", description = "CSV Delimiter", required = false)
	private Character csvDelimiter;

	@CommandLine.Option(names = "--csv-charset", description = "CSV input encoding", defaultValue = "UTF8")
	private Charset csvCharset = StandardCharsets.UTF_8;

	/**
	 * Default constructor.
	 */
	public CsvOptions() {
	}

	/**
	 * Constructor for a default CSV Format.
	 */
	public CsvOptions(CSVFormat.Predefined csvFormat) {
		this.csvFormat = csvFormat;
	}

	/**
	 * Constructor with all available options.
	 */
	public CsvOptions(CSVFormat.Predefined csvFormat, Character csvDelimiter, Charset csvCharset) {
		this.csvFormat = csvFormat;
		this.csvDelimiter = csvDelimiter;
		this.csvCharset = csvCharset;
	}

	/**
	 * Detects possibly used delimiter from the header of a csv or tsv file.
	 */
	public static Character detectDelimiter(String path) throws IOException {
        try (BufferedReader reader = IOUtils.getBufferedReader(path)) {
			String firstLine = reader.readLine();

			int comma = StringUtils.countMatches(firstLine, ",");
			int semicolon = StringUtils.countMatches(firstLine, ";");
			int tab = StringUtils.countMatches(firstLine, "\t");

			if (comma == 0 && semicolon == 0 && tab == 0) {
				throw new IllegalArgumentException("No delimiter found in the first line of the file.");
			}

			// Comma is preferred as the more likely format
			if (comma >= semicolon && comma >= tab) {
				return ',';
			} else if (tab >= semicolon)
				return '\t';
			else
				return ';';
        }
	}

	/**
	 * Get the CSV format defined by the options.
	 */
	public CSVFormat getFormat() {
		CSVFormat.Builder format = this.csvFormat.getFormat().builder().setSkipHeaderRecord(true);
		if (csvDelimiter != null)
			format = format.setDelimiter(csvDelimiter);

		return format.build();
	}

	/**
	 * Creates a new csv parser from specified options.
	 */
	public CSVParser createParser(Path path) throws IOException {
		return new CSVParser(IOUtils.getBufferedReader(path.toUri().toURL(), csvCharset), getFormat());
	}

	/**
	 * Creates a new csv writer.
	 */
	public CSVPrinter createPrinter(Path path) throws IOException {
		return new CSVPrinter(IOUtils.getBufferedWriter(path.toUri().toURL(), csvCharset, false), getFormat());
	}

}
