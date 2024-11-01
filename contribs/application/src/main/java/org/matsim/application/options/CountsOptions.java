package org.matsim.application.options;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.io.IOUtils;
import picocli.CommandLine;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.*;

/**
 * Common options when working with counts data.
 */
public final class CountsOptions {

	private static final Logger log = LogManager.getLogger(CountsOptions.class);

	@CommandLine.Option(names = "--counts-mapping", description = "Path to csv with count station ids to ignore")
	private String input;

	private Map<String, Id<Link>> manualMatchedCounts = null;
	private Set<String> ignoredCounts = null;

	public CountsOptions() {
	}

	public CountsOptions(@Nullable String input) {
		this.input = input;
	}

	/**
	 * Get list of ignored count ids.
	 */
	public Set<String> getIgnored() {
		readMapping();
		return ignoredCounts;
	}

	/**
	 * Return mapping of count id to specified link id.
	 */
	public Map<String, Id<Link>> getManualMatched() {
		readMapping();
		return manualMatchedCounts;
	}

	private synchronized void readMapping() {

		// Already read
		if (manualMatchedCounts != null)
			return;

		manualMatchedCounts = new HashMap<>();
		ignoredCounts = new HashSet<>();

		// No input file
		if (input == null)
			return;

		try (var reader = IOUtils.getBufferedReader(input)) {
			CSVFormat format = CSVFormat.Builder.create()
				.setAllowMissingColumnNames(true)
				.setDelimiter(CsvOptions.detectDelimiter(input))
				.setHeader()
				.setSkipHeaderRecord(true)
				.build();

			try (CSVParser csv = new CSVParser(reader, format)) {
				Schema schema = parseSchema(csv.getHeaderNames());

				log.info("Using schema for counts mapping: {}", schema);

				for (CSVRecord row : csv) {

					String stationId = row.get(schema.stationColumn);
					manualMatchedCounts.put(stationId, Id.createLinkId(row.get(schema.linkColumn)));

					if (schema.usingColumn != null) {

						String value = row.get(schema.usingColumn).strip().toLowerCase();
						boolean val = value.equals("y") || value.equals("x") || value.equals("true");

						if (schema.isWhiteList && !val)
							ignoredCounts.add(stationId);
						else if (!schema.isWhiteList && val)
							ignoredCounts.add(stationId);
					}
				}
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

	}

	/**
	 * Check whether station id should be ignored.
	 */
	public boolean isIgnored(String stationId) {
		readMapping();
		return ignoredCounts.contains(stationId);
	}

	/**
	 * Return manually matched link id.
	 *
	 * @return null if not matched
	 */
	public Id<Link> isManuallyMatched(String stationId) {
		readMapping();

		if (manualMatchedCounts.isEmpty() || !manualMatchedCounts.containsKey(stationId))
			return null;

		return manualMatchedCounts.get(stationId);
	}

	private Schema parseSchema(List<String> header) {

		List<String> names = header.stream()
			.map(String::toLowerCase)
			.map(String::strip)
			.map(s -> s.replace("_", ""))
			.toList();

		int linkId = names.indexOf("linkid");

		if (linkId < 0)
			throw new IllegalArgumentException("Link id column not found in csv: " + header);

		int using = names.indexOf("using");
		int ignore = names.indexOf("ignore");

		// first or second column for station id
		int station = linkId == 0 ? 1 : 0;

		if (using > 0)
			return new Schema(header.get(station), header.get(linkId), header.get(using), true);

		if (ignore > 0)
			return new Schema(header.get(station), header.get(linkId), header.get(ignore), false);

		return new Schema(header.get(station), header.get(linkId), null, false);
	}

	private record Schema(String stationColumn, String linkColumn, String usingColumn, boolean isWhiteList) {
	}
}
