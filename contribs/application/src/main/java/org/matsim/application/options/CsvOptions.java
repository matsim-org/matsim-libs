package org.matsim.application.options;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.matsim.core.utils.io.IOUtils;
import picocli.CommandLine;

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
	 * Get the CSV format defined by the options.
	 */
	public CSVFormat getFormat() {
		CSVFormat format = this.csvFormat.getFormat().withFirstRecordAsHeader();
		if (csvDelimiter != null)
			format = format.withDelimiter(csvDelimiter);

		return format;
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
		return new CSVPrinter(IOUtils.getBufferedWriter(path.toUri().toURL(),  csvCharset, false), getFormat());
	}

}
