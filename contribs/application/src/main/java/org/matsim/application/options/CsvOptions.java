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

	@CommandLine.Option(names = "--csv-delimiter", description = "CSV Delimiter")
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
			int[] comma = new int[5];
			int[] semicolon = new int[5];
			int[] tab = new int[5];
			String[] lines = new String[5];

//			check five first lines for separator chars. It might be that the csv file has additional info in the first x lines (e.g. EPSG)
			for (int i = 0; i < 5; i++) {
				lines[i] = reader.readLine();
				if (lines[i] == null) {
					comma[i] = 0;
					semicolon[i] = 0;
					tab[i] = 0;
				} else {
					comma[i] = StringUtils.countMatches(lines[i], ",");
					semicolon[i] = StringUtils.countMatches(lines[i], ";");
					tab[i] = StringUtils.countMatches(lines[i], "\t");
				}
			}

			Integer index = null;

			for (int i = 0; i < comma.length - 1; i++) {
//				only check next index if line with separators was not found
				if (index == null) {
					if (!(comma[i] == 0 && semicolon[i] == 0 && tab[i] == 0)) {
						index = i;
					}
				}
			}

			if (index == null) {
				throw new IllegalArgumentException("No delimiter found in the first line of the file.");
			} else {
				// Comma is preferred as the more likely format
				if (comma[index] >= semicolon[index] && comma[index] >= tab[index]) {
					return ',';
				} else if (tab[index] >= semicolon[index])
					return '\t';
				else
					return ';';
			}
		}
	}

	/**
	 * Get the CSV format defined by the options.
	 */
	public CSVFormat getFormat() {
		CSVFormat.Builder format = this.csvFormat.getFormat().builder().setHeader().setSkipHeaderRecord(true);
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
