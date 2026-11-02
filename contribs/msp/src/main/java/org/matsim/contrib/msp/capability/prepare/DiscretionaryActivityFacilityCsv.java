package org.matsim.contrib.msp.capability.prepare;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.msp.capability.CapabilityConfigGroup;
import org.matsim.contrib.msp.capability.facilities.DiscretionaryActivityFacilities;
import org.matsim.contrib.msp.capability.facilities.DiscretionaryActivityFacility;
import org.matsim.contrib.msp.capability.facilities.DiscretionaryActivityFacilityExtractor;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacilitiesFactory;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.OpeningTime;
import org.matsim.facilities.OpeningTimeImpl;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The csv interchange format for the places at which discretionary activities can be performed: one row per
 * place and {@link DiscretionaryActivityFacility#getType() type}.
 * <pre>
 * id,name,type,sub_type,x,y,opening_time,closing_time
 * super-7,Konsum Striesen,shop,Konsum,4621000,5820000,08:00:00,20:00:00
 * gasthaus-2,Gasthaus Am Markt,restaurant,German,4622000,5821000,11:00:00,23:00:00
 * park-1,Großer Garten,leisure,,4623000,5822000,,
 * </pre>
 * This is deliberately the <em>only</em> format the facility generation reads, so that every source of
 * facilities — a list maintained by hand today, an OpenStreetMap extract later — produces this csv and
 * then goes through the same {@link #read(String) reader}. An OSM step therefore does not have to know
 * anything about MATSim facilities: it maps OSM tags to a {@code type}, a brand or cuisine to a
 * {@code sub_type}, {@code opening_hours} to {@code opening_time} / {@code closing_time}, and
 * {@link #write writes} this csv.
 * <p>
 * <b>Columns.</b> Addressed by name, so their order does not matter. {@link #ID} and {@link #TYPE} are
 * required, and so is a location — {@link #X} and {@link #Y}, or {@link #LINK_ID}, or both. Everything
 * else is optional, and an empty cell simply means "not known". Any column that is not one of the known
 * ones is kept as a facility attribute, which is how a future reader can carry more of its source
 * through without this format having to change.
 * <p>
 * <b>Times</b> are either a MATSim time string ({@code 08:00:00}) or a number of seconds. MATSim counts
 * on past midnight, so a bar closing at 3 in the morning closes at {@code 27:00:00}. An empty
 * {@link #OPENING_TIME} means open ended at the start, an empty {@link #CLOSING_TIME} open ended at the
 * end, and two empty cells mean open around the clock.
 * <p>
 * <b>A place offering several types</b> — a mall one can both shop and eat at — gets one row per type,
 * all sharing the {@link #ID}. The rows then have to agree on name, sub-type and location; the first
 * row of an id wins and a conflict is logged. Note that distinct places are usually better off with
 * distinct ids: a sub-type belongs to the place, so a mall modelled as one id cannot have one brand for
 * its supermarket and another for its restaurant.
 */
public final class DiscretionaryActivityFacilityCsv {

	public static final String ID = "id";
	public static final String NAME = "name";
	public static final String TYPE = "type";
	public static final String SUB_TYPE = "sub_type";
	public static final String X = "x";
	public static final String Y = "y";
	public static final String LINK_ID = "link_id";
	public static final String OPENING_TIME = "opening_time";
	public static final String CLOSING_TIME = "closing_time";

	/**
	 * The columns this format knows. Every other column of a file is kept as a facility attribute.
	 */
	public static final List<String> COLUMNS =
		List.of(ID, NAME, TYPE, SUB_TYPE, X, Y, LINK_ID, OPENING_TIME, CLOSING_TIME);

	private static final Logger log = LogManager.getLogger(DiscretionaryActivityFacilityCsv.class);

	private final CapabilityConfigGroup config;
	private char delimiter = ',';

	public DiscretionaryActivityFacilityCsv(CapabilityConfigGroup config) {
		this.config = config;
	}

	/**
	 * @param delimiter the column separator, {@code ','} by default
	 */
	public DiscretionaryActivityFacilityCsv setDelimiter(char delimiter) {
		this.delimiter = delimiter;
		return this;
	}

	/**
	 * Reads the csv into a MATSim facilities container, annotated so that
	 * {@link DiscretionaryActivityFacilityExtractor} finds the types, sub-types and opening hours again. The
	 * result is an ordinary {@link ActivityFacilities} that can be written with
	 * {@link org.matsim.facilities.FacilitiesWriter} and used by the rest of MATSim.
	 *
	 * @throws IllegalArgumentException if a required column or cell is missing, or a cell is unreadable.
	 * The message names the row, since the file is usually maintained by hand.
	 */
	public ActivityFacilities read(String file) {

		ActivityFacilities facilities = FacilitiesUtils.createActivityFacilities();

		// the type attribute holds all types of a facility, so they have to be collected across rows
		Map<Id<ActivityFacility>, Set<String>> typesById = new LinkedHashMap<>();
		int rows = 0;

		CSVFormat format = CSVFormat.DEFAULT.builder()
			.setDelimiter(this.delimiter)
			.setHeader()
			.setSkipHeaderRecord(true)
			.setIgnoreSurroundingSpaces(true)
			.build();

		try (CSVParser parser = CSVParser.parse(IOUtils.getBufferedReader(file), format)) {

			List<String> header = parser.getHeaderNames();
			require(header.contains(ID), "The file has no '%s' column, found %s.", ID, header);
			require(header.contains(TYPE), "The file has no '%s' column, found %s.", TYPE, header);
			require(header.contains(X) && header.contains(Y) || header.contains(LINK_ID),
				"The file says nowhere where the facilities are: it needs '%s' and '%s' columns, or a '%s' column. "
					+ "Found %s.", X, Y, LINK_ID, header);

			List<String> extra = header.stream().filter(column -> !COLUMNS.contains(column)).toList();
			if (!extra.isEmpty()) {
				log.info("Keeping the columns {} as facility attributes.", extra);
			}

			for (CSVRecord row : parser) {
				readRow(row, facilities, typesById, extra);
				rows++;
			}

		} catch (IOException e) {
			throw new UncheckedIOException("Could not read discretionary activity facilities from " + file, e);
		}

		log.info("Read {} rows into {} facilities from {}.", rows, facilities.getFacilities().size(), file);

		return facilities;
	}

	private void readRow(CSVRecord row, ActivityFacilities facilities,
						 Map<Id<ActivityFacility>, Set<String>> typesById, List<String> extraColumns) {

		Id<ActivityFacility> id = Id.create(required(row, ID), ActivityFacility.class);

		ActivityFacility facility = facilities.getFacilities().get(id);
		if (facility == null) {
			facility = createFacility(row, facilities, id);
			facilities.addActivityFacility(facility);
			setName(facility, cell(row, NAME));
			if (cell(row, SUB_TYPE) != null) {
				facility.getAttributes().putAttribute(this.config.getFacilitySubTypeAttribute(), cell(row, SUB_TYPE));
			}
			for (String column : extraColumns) {
				if (cell(row, column) != null) {
					facility.getAttributes().putAttribute(column, cell(row, column));
				}
			}
		} else {
			warnOnConflict(row, facility, id);
		}

		Set<String> types = typesById.computeIfAbsent(id, key -> new LinkedHashSet<>());

		for (String type : required(row, TYPE).split(",")) {
			type = type.strip();
			if (type.isEmpty()) {
				continue;
			}
			types.add(type);
			addActivityOption(facilities.getFactory(), facility, type, openingTimeOf(row));
		}

		require(!types.isEmpty(), "Row %d has an empty '%s'.", row.getRecordNumber(), TYPE);

		// the capability type attribute lists every type of the facility
		facility.getAttributes().putAttribute(this.config.getFacilityTypeAttribute(), String.join(",", types));
	}

	private ActivityFacility createFacility(CSVRecord row, ActivityFacilities facilities, Id<ActivityFacility> id) {

		Coord coord = coordOf(row);
		Id<Link> linkId = cell(row, LINK_ID) == null ? null : Id.createLinkId(cell(row, LINK_ID));

		require(coord != null || linkId != null, "Row %d says neither '%s'/'%s' nor '%s', so the facility has no "
			+ "location.", row.getRecordNumber(), X, Y, LINK_ID);

		return linkId == null
			? facilities.getFactory().createActivityFacility(id, coord)
			: facilities.getFactory().createActivityFacility(id, coord, linkId);
	}

	private Coord coordOf(CSVRecord row) {
		String x = cell(row, X);
		String y = cell(row, Y);
		if (x == null || y == null) {
			require(x == null && y == null, "Row %d has only one of '%s' and '%s'.", row.getRecordNumber(), X, Y);
			return null;
		}
		return new Coord(parseDouble(row, X, x), parseDouble(row, Y, y));
	}

	/**
	 * @return the opening window of the row, null if it is open around the clock
	 */
	private OpeningTime openingTimeOf(CSVRecord row) {

		double opening = time(row, OPENING_TIME, Double.NEGATIVE_INFINITY);
		double closing = time(row, CLOSING_TIME, Double.POSITIVE_INFINITY);

		if (opening == Double.NEGATIVE_INFINITY && closing == Double.POSITIVE_INFINITY) {
			return null;
		}

		require(opening < closing, "Row %d closes at %s before or when it opens at %s. Note that MATSim counts on past "
				+ "midnight, so closing at 3 in the morning is '27:00:00'.", row.getRecordNumber(),
			Time.writeTime(closing), Time.writeTime(opening));

		return new OpeningTimeImpl(opening, closing);
	}

	/**
	 * Puts the opening window into an {@code <activity>} option named after the type, which is where
	 * MATSim keeps opening hours and where {@link DiscretionaryActivityFacilityExtractor} looks for them.
	 */
	private static void addActivityOption(ActivityFacilitiesFactory factory, ActivityFacility facility,
										  String type, OpeningTime openingTime) {

		ActivityOption option = facility.getActivityOptions().get(type);
		if (option == null) {
			option = factory.createActivityOption(type);
			facility.addActivityOption(option);
		}

		if (openingTime != null) {
			option.addOpeningTime(openingTime);
		}
	}

	private void warnOnConflict(CSVRecord row, ActivityFacility facility, Id<ActivityFacility> id) {

		Coord coord = coordOf(row);
		if (coord != null && facility.getCoord() != null && !coord.equals(facility.getCoord())) {
			log.warn("Row {} puts facility {} at {}, an earlier row put it at {}. Keeping the earlier location.",
				row.getRecordNumber(), id, coord, facility.getCoord());
		}

		String subType = cell(row, SUB_TYPE);
		Object known = facility.getAttributes().getAttribute(this.config.getFacilitySubTypeAttribute());
		if (subType != null && known != null && !subType.equals(known)) {
			log.warn("Row {} gives facility {} the sub-type '{}', an earlier row gave it '{}'. Keeping the earlier one. "
				+ "A sub-type belongs to the place, so consider giving the two types their own ids.",
				row.getRecordNumber(), id, subType, known);
		}
	}

	/**
	 * Writes the places as csv, one row per place and type. The inverse of {@link #read(String)}, and
	 * the format a facility source such as an OpenStreetMap reader should produce.
	 * <p>
	 * A place that is open in several windows a day cannot be expressed in one row: only the earliest
	 * opening and the latest closing time are written, and a warning is logged. Write the facilities
	 * themselves if the windows matter.
	 */
	public void write(DiscretionaryActivityFacilities facilities, String file) {

		CSVFormat format = CSVFormat.DEFAULT.builder().setDelimiter(this.delimiter).build();
		int withSeveralWindows = 0;

		try (CSVPrinter csv = new CSVPrinter(IOUtils.getBufferedWriter(file), format)) {

			csv.printRecord(ID, NAME, TYPE, SUB_TYPE, X, Y, LINK_ID, OPENING_TIME, CLOSING_TIME);

			for (DiscretionaryActivityFacility facility : facilities.getAll()) {

				if (facility.getOpeningTimes().size() > 1) {
					withSeveralWindows++;
				}

				Coord coord = facility.getCoord();

				csv.printRecord(
					facility.getId(),
					facility.getName().orElse(""),
					facility.getType(),
					facility.getSubType().orElse(""),
					coord == null ? "" : coord.getX(),
					coord == null ? "" : coord.getY(),
					facility.getLinkId() == null ? "" : facility.getLinkId(),
					writeTime(facility.getOpeningTime()),
					writeTime(facility.getClosingTime()));
			}

		} catch (IOException e) {
			throw new UncheckedIOException("Could not write discretionary activity facilities to " + file, e);
		}

		if (withSeveralWindows > 0) {
			log.warn("{} of {} places are open in more than one window a day. A row holds one window, so only the "
				+ "earliest opening and the latest closing time were written.", withSeveralWindows, facilities.size());
		}

		log.info("Wrote {} places to {}.", facilities.size(), file);
	}

	/**
	 * @return the time as a MATSim time string, empty if it is open ended
	 */
	private static String writeTime(double time) {
		return Double.isInfinite(time) ? "" : Time.writeTime(time);
	}

	/**
	 * The name goes into the facility description, which is where MATSim keeps it and what the facilities
	 * writer and reader round trip as the {@code desc} attribute.
	 */
	private static void setName(ActivityFacility facility, String name) {
		if (name != null && facility instanceof ActivityFacilityImpl impl) {
			impl.setDesc(name);
		}
	}

	/**
	 * @return the trimmed cell of the given column, null if the column is absent or the cell is empty
	 */
	private static String cell(CSVRecord row, String column) {
		if (!row.isMapped(column) || !row.isSet(column)) {
			return null;
		}
		String value = row.get(column).strip();
		return value.isEmpty() ? null : value;
	}

	private static String required(CSVRecord row, String column) {
		String value = cell(row, column);
		require(value != null, "Row %d has an empty '%s'.", row.getRecordNumber(), column);
		return value;
	}

	private static double parseDouble(CSVRecord row, String column, String value) {
		try {
			return Double.parseDouble(value);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Row " + row.getRecordNumber() + " has an unreadable '" + column
				+ "': '" + value + "'.", e);
		}
	}

	private static double time(CSVRecord row, String column, double fallback) {
		String value = cell(row, column);
		if (value == null) {
			return fallback;
		}
		try {
			return Time.parseTime(value);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Row " + row.getRecordNumber() + " has an unreadable '" + column
				+ "': '" + value + "'. Expected a time such as '08:00:00' or a number of seconds.", e);
		}
	}

	private static void require(boolean condition, String message, Object... arguments) {
		if (!condition) {
			throw new IllegalArgumentException(String.format(message, arguments));
		}
	}
}
