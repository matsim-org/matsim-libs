package org.matsim.contrib.msp.capability.result;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.msp.capability.structure.CapabilityTimeline;
import org.matsim.core.utils.io.IOUtils;

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Writes the capability analysis, as three levels that aggregate into one another:
 * <table border="1">
 *     <caption>the output</caption>
 *     <tr><th>file</th><th>one row per</th><th>record</th></tr>
 *     <tr><td>{@link #INSERTIONS_FILE}</td><td>person x mode x type x facility x insertion</td>
 *         <td>{@link InsertionRecord}</td></tr>
 *     <tr><td>{@link #FACILITIES_FILE}</td><td>person x mode x type x facility</td>
 *         <td>{@link FacilityRecord}</td></tr>
 *     <tr><td>{@link #PERSONS_FILE}</td><td>person x mode x type</td>
 *         <td>{@link PersonRecord}</td></tr>
 *     <tr><td>{@link #ACTIVITIES_FILE}</td><td>block of an agent's day</td>
 *         <td>{@link CapabilityTimeline.Block}</td></tr>
 *     <tr><td>{@link #FUNDAMENTAL_PLANS_FILE}</td><td>person the filter accepted</td>
 *         <td>{@link FundamentalPlanRecord}</td></tr>
 * </table>
 * Level 2 is level 1 grouped by facility and level 3 is level 2 grouped by person, so a downstream
 * analysis can rebuild the upper levels from the lower ones - every row carries the parameters its own
 * numbers were computed with. {@link #ACTIVITIES_FILE} is not a level: it is the shape of the agent's
 * day, which the other three are relative to, and neither is {@link #FUNDAMENTAL_PLANS_FILE}: it
 * records how each person's plan was reduced to its mandatory activities, which is the step every level
 * is computed on top of.
 * <p>
 * The three tables that grow with the number of facilities and insertions are gzipped. The two that stay
 * the size of the population - {@link #PERSONS_FILE} and {@link #FUNDAMENTAL_PLANS_FILE} - are plain csv,
 * because those are the ones opened by hand. Level 1 grows fastest - persons x modes x types x facilities
 * x periods - and can be left out entirely, see {@link #CapabilityWriter(Path, boolean)}.
 */
public final class CapabilityWriter implements Closeable {

	public static final String INSERTIONS_FILE = "capability_insertions.csv.gz";
	public static final String FACILITIES_FILE = "capability_facilities.csv.gz";
	public static final String PERSONS_FILE = "capability_persons.csv";
	public static final String ACTIVITIES_FILE = "capability_activities.csv.gz";
	public static final String FUNDAMENTAL_PLANS_FILE = "capability_fundamental_plans.csv";

	private final CSVPrinter insertions;
	private final CSVPrinter facilities;
	private final CSVPrinter persons;
	private final CSVPrinter activities;
	private final CSVPrinter fundamentalPlans;

	/**
	 * @param directory where the files are written to
	 * @param writeInsertions whether the insertion level is written at all. It is the largest table by
	 * far, so a run over a whole population may well want it off.
	 */
	public CapabilityWriter(Path directory, boolean writeInsertions) {
		try {
			this.insertions = writeInsertions ? open(directory, INSERTIONS_FILE, InsertionRecord.HEADER) : null;
			this.facilities = open(directory, FACILITIES_FILE, FacilityRecord.HEADER);
			this.persons = open(directory, PERSONS_FILE, PersonRecord.HEADER);
			this.activities = open(directory, ACTIVITIES_FILE,
				new String[]{"person_id", "seq", "act_type", "is_home", "from", "to",
					"soft_start", "soft_end", "x", "y"});
			this.fundamentalPlans = open(directory, FUNDAMENTAL_PLANS_FILE, FundamentalPlanRecord.HEADER);

		} catch (IOException e) {
			throw new UncheckedIOException("Could not open the capability output in " + directory, e);
		}
	}

	/**
	 * @return true if the insertion level is being written, i.e. whether {@link #write(InsertionRecord)}
	 * does anything
	 */
	public boolean writesInsertions() {
		return this.insertions != null;
	}

	public void write(InsertionRecord record) {
		if (this.insertions != null) {
			print(this.insertions, record.toRecord(), INSERTIONS_FILE);
		}
	}

	public void write(FacilityRecord record) {
		print(this.facilities, record.toRecord(), FACILITIES_FILE);
	}

	public void write(PersonRecord record) {
		print(this.persons, record.toRecord(), PERSONS_FILE);
	}

	/**
	 * Writes how one person's day was reduced to its mandatory activities.
	 */
	public void write(FundamentalPlanRecord record) {
		print(this.fundamentalPlans, record.toRecord(), FUNDAMENTAL_PLANS_FILE);
	}

	/**
	 * Writes the blocks of one agent's day, the time axis the insertions sit on.
	 */
	public void writeActivities(Person person, List<CapabilityTimeline.Block> blocks) {
		for (int i = 0; i < blocks.size(); i++) {
			CapabilityTimeline.Block block = blocks.get(i);
			print(this.activities, new Object[]{
				person.getId(), i, block.type(), block.home(),
				Format.time(block.from()), Format.time(block.to()),
				// a fixed block has nothing uncertain to draw, so its soft range is left empty
				block.isSoft() ? Format.time(block.softStart()) : "",
				block.isSoft() ? Format.time(block.softEnd()) : "",
				Format.x(block.coord()), Format.y(block.coord())
			}, ACTIVITIES_FILE);
		}
	}

	@Override
	public void close() {
		try {
			if (this.insertions != null) {
				this.insertions.close();
			}
			this.facilities.close();
			this.persons.close();
			this.activities.close();
			this.fundamentalPlans.close();

		} catch (IOException e) {
			throw new UncheckedIOException("Could not close the capability output", e);
		}
	}

	private static CSVPrinter open(Path directory, String file, String[] header) throws IOException {
		CSVPrinter printer = new CSVPrinter(
			IOUtils.getBufferedWriter(directory.resolve(file).toString()), CSVFormat.DEFAULT);
		printer.printRecord((Object[]) header);
		return printer;
	}

	private static void print(CSVPrinter printer, Object[] record, String file) {
		try {
			printer.printRecord(record);
		} catch (IOException e) {
			throw new UncheckedIOException("Could not write " + file, e);
		}
	}
}
