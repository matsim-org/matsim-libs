package org.matsim.contrib.msp.traveltimematrix.experiments;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * A csv an experiment appends one row to at a time, writing the header on the first.
 * <p>
 * Appending rather than collecting and writing at the end, because a sweep over thirty configurations
 * takes long enough that it will sometimes be interrupted - by an out-of-memory in the expensive corner,
 * by a laptop lid, by someone who has seen enough. Every row is on disk the moment it is known, so what
 * finished is usable whatever happens to the rest. It also means the file can be watched while the sweep
 * runs.
 */
final class ResultTable {

	private static final Logger log = LogManager.getLogger(ResultTable.class);

	private final Path file;
	private final List<String> header;
	private boolean headerWritten;
	private int rows;

	ResultTable(Path file, List<String> header) {
		this.file = file;
		this.header = List.copyOf(header);
		this.headerWritten = Files.exists(file);
	}

	/**
	 * Appends one row, flushed immediately.
	 *
	 * @throws IllegalArgumentException if the row does not have as many fields as the header, which is
	 * the mistake that turns a results table into a silently misaligned one
	 */
	void append(List<?> record) {

		if (record.size() != this.header.size()) {
			throw new IllegalArgumentException("The row has " + record.size() + " fields but the header of "
				+ this.file + " has " + this.header.size() + ": " + this.header);
		}

		try (Writer writer = IOUtils.getAppendingBufferedWriter(this.file.toString());
			 CSVPrinter printer = new CSVPrinter(writer, this.headerWritten
				 ? CSVFormat.DEFAULT
				 : CSVFormat.DEFAULT.builder().setHeader(this.header.toArray(String[]::new)).build())) {
			printer.printRecord(record);
		} catch (IOException e) {
			throw new UncheckedIOException("Cannot append a row to " + this.file, e);
		}

		this.headerWritten = true;
		this.rows++;
	}

	void logSummary() {
		log.info("Wrote {} rows to {}.", this.rows, this.file);
	}
}
