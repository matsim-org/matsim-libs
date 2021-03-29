package org.matsim.application.options;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Options for reading a general csv file.
 *
 * @see picocli.CommandLine.Mixin
 */
public final class CsvOptions {

    @CommandLine.Option(names = "--csv-format", description = "CSV Format", defaultValue = "Default")
    private CSVFormat.Predefined csvFormat;

    @CommandLine.Option(names = "--csv-delimiter", description = "CSV Delimiter", defaultValue = ";")
    private Character csvDelimiter;

    @CommandLine.Option(names = "--csv-charset", description = "CSV input encoding", defaultValue = "ISO-8859-1")
    private Charset csvCharset;

    /**
     * Creates a new csv parser from specified options.
     */
    public CSVParser createParser(Path path) throws IOException {
        return new CSVParser(Files.newBufferedReader(path, csvCharset), csvFormat.getFormat().withFirstRecordAsHeader().withDelimiter(csvDelimiter));
    }

    /**
     * Creates a new csv writer.
     */
    public CSVPrinter createPrinter(Path path) throws IOException {
        return new CSVPrinter(Files.newBufferedWriter(path, csvCharset), csvFormat.getFormat().withFirstRecordAsHeader().withDelimiter(csvDelimiter));
    }


}
