package org.matsim.application.analysis.impact;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.matsim.application.ApplicationUtils;
import org.matsim.application.CommandSpec;
import org.matsim.application.Dependency;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.analysis.emissions.AirPollutionAnalysis;
import org.matsim.application.options.CsvOptions;
import org.matsim.application.options.InputOptions;
import org.matsim.application.options.OutputOptions;
import org.matsim.application.options.SampleOptions;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;
import picocli.CommandLine;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Creates BVWP-style tables from a MATSim run.
 * <p>
 * Values that can not be derived from standard MATSim outputs are intentionally written as placeholders.
 */
@CommandLine.Command(name = "bvwp", description = "Creates BVWP-style central effects and cost-benefit tables.")
@CommandSpec(
	requireRunDirectory = true,
	requires = {"legs.csv"},
	dependsOn = {
		@Dependency(value = AirPollutionAnalysis.class, files = "emissions_per_network_mode.csv")
	},
	produces = {
		"bvwp_central_traffic_effects.csv",
		"bvwp_emissions.csv",
		"bvwp_cost_benefit_analysis.csv",
		"bvwp_costs.csv",
		"bvwp_summary.csv"
	}
)
public class BvwpAnalysis implements MATSimAppCommand {

	private static final Locale L = Locale.US;
	private static final String PLACEHOLDER = "TODO";
	private static final String COMPUTED = "computed_from_matsim";
	private static final String MISSING_REFERENCE = "placeholder_missing_reference_run";
	private static final String NOT_STANDARD_OUTPUT = "placeholder_not_in_standard_matsim_output";
	private static final String REQUIRES_BVWP_PARAMETERS = "placeholder_requires_bvwp_parameters";
	private static final double METERS_PER_KILOMETER = 1000.0;
	private static final double HOURS_PER_SECOND = 1.0 / 3600.0;
	private static final double MILLION = 1_000_000.0;

	@CommandLine.Mixin
	private final InputOptions input = InputOptions.ofCommand(BvwpAnalysis.class);

	@CommandLine.Mixin
	private final OutputOptions output = OutputOptions.ofCommand(BvwpAnalysis.class);

	@CommandLine.Mixin
	private SampleOptions sample;

	@CommandLine.Option(names = "--reference-run-directory", description = "Optional run directory for the Bezugsfall.")
	private Path referenceRunDirectory;

	@CommandLine.Option(names = "--person-traffic-modes", split = ",", defaultValue = "car",
		description = "Network modes treated as Personenverkehr.")
	private Set<String> personTrafficModes;

	@CommandLine.Option(names = "--freight-traffic-modes", split = ",", defaultValue = "freight,truck",
		description = "Network modes treated as Gueterverkehr.")
	private Set<String> freightTrafficModes;

	@CommandLine.Option(names = "--person-traffic-days-per-year", defaultValue = "334",
		description = "Annualization factor for passenger traffic.")
	private int personTrafficDaysPerYear;

	@CommandLine.Option(names = "--freight-traffic-days-per-year", defaultValue = "302",
		description = "Annualization factor for freight traffic.")
	private int freightTrafficDaysPerYear;

	public static void main(String[] args) {
		new BvwpAnalysis().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		RunStats plan = readLegStats(Path.of(input.getPath("legs.csv")));
		RunStats reference = referenceRunDirectory == null ? null : readLegStats(ApplicationUtils.matchInput("legs.csv", referenceRunDirectory));

		Path planEmissions = Path.of(input.getPath(AirPollutionAnalysis.class, "emissions_per_network_mode.csv"));
		Path referenceEmissions = referenceRunDirectory == null ? null : findReferenceEmissions(referenceRunDirectory);
		EmissionStats planEmissionStats = readEmissionStats(planEmissions);
		EmissionStats referenceEmissionStats = referenceEmissions == null ? EmissionStats.missing() : readEmissionStats(referenceEmissions);

		writeCentralTrafficEffects(plan, reference);
		writeEmissions(planEmissionStats, referenceEmissionStats);
		writeCostBenefitAnalysis();
		writeCosts();
		writeSummary();

		return 0;
	}

	private RunStats readLegStats(Path legs) {

		RunStats result = new RunStats();

		try (CSVParser parser = new CSVParser(IOUtils.getBufferedReader(legs.toString()), csvFormat(legs))) {
			for (CSVRecord record : parser) {
				String mode = firstNonBlank(record, "network_mode", "mode");
				if (mode == null)
					continue;

				double distance = parseDouble(record, "distance");
				double travelTime = parseTime(record, "trav_time");

				if (Double.isNaN(distance) || Double.isNaN(travelTime))
					continue;

				double scaledDistance = distance * sample.getUpscaleFactor();
				double scaledTravelTime = travelTime * sample.getUpscaleFactor();

				if (personTrafficModes.contains(mode)) {
					result.personTraffic.add(scaledDistance, scaledTravelTime);
				}

				if (freightTrafficModes.contains(mode)) {
					result.freightTraffic.add(scaledDistance, scaledTravelTime);
					if (distance < 50_000) {
						result.freightBelow50KmSeconds += scaledTravelTime;
					} else {
						result.freightAtLeast50KmSeconds += scaledTravelTime;
					}
				}
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		return result;
	}

	private void writeCentralTrafficEffects(RunStats plan, RunStats reference) throws IOException {

		try (CSVPrinter printer = printer(output.getPath("bvwp_central_traffic_effects.csv"))) {
			printer.printRecord("section", "metric", "unit", "Bezugsfall", "Planfall", "difference", "status");

			placeholder(printer, "Verkehrsbelastungen auf dem Projekt", "mittlere Kfz-Belastungen im Bezugsfall", "Kfz/Tag", NOT_STANDARD_OUTPUT);
			placeholder(printer, "Verkehrsbelastungen auf dem Projekt", "mittlere Kfz-Belastungen im Planfall", "Kfz/Tag", NOT_STANDARD_OUTPUT);
			placeholder(printer, "Verkehrsbelastungen auf dem Projekt", "mittlerer Lkw-Anteil im Bezugsfall", "%", NOT_STANDARD_OUTPUT);
			placeholder(printer, "Verkehrsbelastungen auf dem Projekt", "mittlerer Lkw-Anteil im Planfall", "%", NOT_STANDARD_OUTPUT);

			comparison(printer, reference, plan,
				"Verkehrswirkungen im Planfall",
				"Veraenderung der Betriebsleistung im Personenverkehr (PV)",
				"Mio. Pkw-km/a",
				stats -> annualMillionKilometers(stats.personTraffic, personTrafficDaysPerYear)
			);
			placeholder(printer, "Verkehrswirkungen im Planfall", "davon aus induziertem Verkehr", "Mio. Pkw-km/a", NOT_STANDARD_OUTPUT);
			placeholder(printer, "Verkehrswirkungen im Planfall", "nachrichtlich aus modal-verlagertem Verkehr", "Mio. Pkw-km/a", NOT_STANDARD_OUTPUT);

			comparison(printer, reference, plan,
				"Verkehrswirkungen im Planfall",
				"Veraenderung der Fahrzeugeinsatzzeiten im PV",
				"Mio. Pkw-h/a",
				stats -> annualMillionHours(stats.personTraffic, personTrafficDaysPerYear)
			);
			placeholder(printer, "Verkehrswirkungen im Planfall", "davon aus induziertem Verkehr", "Mio. Pkw-h/a", NOT_STANDARD_OUTPUT);
			placeholder(printer, "Verkehrswirkungen im Planfall", "nachrichtlich aus modal-verlagertem Verkehr", "Mio. Pkw-h/a", NOT_STANDARD_OUTPUT);

			comparison(printer, reference, plan,
				"Verkehrswirkungen im Planfall",
				"Veraenderung der Reisezeit im PV",
				"Mio. Personen-h/a",
				stats -> annualMillionHours(stats.personTraffic, personTrafficDaysPerYear)
			);
			placeholder(printer, "Verkehrswirkungen im Planfall", "davon aus induziertem Verkehr", "Mio. Personen-h/a", NOT_STANDARD_OUTPUT);
			placeholder(printer, "Verkehrswirkungen im Planfall", "nachrichtlich aus modal-verlagertem Verkehr", "Mio. Personen-h/a", NOT_STANDARD_OUTPUT);

			comparison(printer, reference, plan,
				"Verkehrswirkungen im Planfall",
				"Veraenderung der Betriebsleistung Gueterverkehr (GV)",
				"Mio. Lkw-km/a",
				stats -> annualMillionKilometers(stats.freightTraffic, freightTrafficDaysPerYear)
			);

			comparison(printer, reference, plan,
				"Verkehrswirkungen im Planfall",
				"Veraenderung der Fahrzeugeinsatzzeiten im GV",
				"Mio. Lkw-h/a",
				stats -> annualMillionHours(stats.freightTraffic, freightTrafficDaysPerYear)
			);

			comparison(printer, reference, plan,
				"Verkehrswirkungen im Planfall",
				"Fahrzeitdifferenz im Lkw-Verkehr mit Fahrtweiten < 50 km",
				"Mio. Lkw-h/a",
				stats -> annualMillionHours(stats.freightBelow50KmSeconds, freightTrafficDaysPerYear)
			);

			comparison(printer, reference, plan,
				"Verkehrswirkungen im Planfall",
				"Fahrzeitdifferenz im Lkw-Verkehr mit Fahrtweiten >= 50 km",
				"Mio. Lkw-h/a",
				stats -> annualMillionHours(stats.freightAtLeast50KmSeconds, freightTrafficDaysPerYear)
			);

			placeholder(printer, "Veraenderung der Kraftstoffverbraeuche (PV+GV)", "Benzin", "Mio. l/a", NOT_STANDARD_OUTPUT);
			placeholder(printer, "Veraenderung der Kraftstoffverbraeuche (PV+GV)", "Diesel", "Mio. l/a", NOT_STANDARD_OUTPUT);
			placeholder(printer, "Veraenderung der Kraftstoffverbraeuche (PV+GV)", "Gas", "Mio. l/a", NOT_STANDARD_OUTPUT);
			placeholder(printer, "Veraenderung der Kraftstoffverbraeuche (PV+GV)", "Elektro", "Mio. kWh/a", NOT_STANDARD_OUTPUT);
			placeholder(printer, "Veraenderung der Zuverlaessigkeit", "Summendifferenz der Standardabweichungen der Reisezeitverluste ueber alle Routen", "Tsd. h/a", NOT_STANDARD_OUTPUT);
			placeholder(printer, "Veraenderung der Trennwirkungen", "inneroertliche Trennwirkungen", "Tsd. Personen-h/a", NOT_STANDARD_OUTPUT);
		}
	}

	private void writeEmissions(EmissionStats plan, EmissionStats reference) throws IOException {

		try (CSVPrinter printer = printer(output.getPath("bvwp_emissions.csv"))) {
			printer.printRecord("pollutant", "code", "Pkw", "Lkw", "Kfz", "unit", "status");

			emission(printer, plan, reference, "Stickoxid-Emissionen (NOx)", "NOx", "NOx");
			emission(printer, plan, reference, "Kohlenmonoxid-Emissionen (CO)", "CO", "CO");
			emission(printer, plan, reference, "Kohlendioxid-Emissionen (CO2)", "CO2", "CO2_TOTAL");
			emission(printer, plan, reference, "Kohlenwasserstoff-Emissionen (HC)", "HC", "HC");
			emission(printer, plan, reference, "Feinstaub-Emissionen (PM)", "PM", "PM");
			emission(printer, plan, reference, "Schwefeldioxid-Emissionen (SO2)", "SO2", "SO2");
		}
	}

	private void writeCostBenefitAnalysis() throws IOException {

		try (CSVPrinter printer = printer(output.getPath("bvwp_cost_benefit_analysis.csv"))) {
			printer.printRecord("description", "component", "annual_benefit_mio_eur", "present_value_mio_eur", "status");

			nkaPlaceholder(printer, "Veraenderung der Betriebskosten im Personen- und Gueterverkehr", "NB");
			nkaPlaceholder(printer, "Fahrzeugvorhaltekosten", "");
			nkaPlaceholder(printer, "Betriebsfuehrungskosten (Personal)", "");
			nkaPlaceholder(printer, "Betriebsfuehrungskosten (Betrieb)", "");
			nkaPlaceholder(printer, "Veraenderung der Instandhaltungs- und Betriebskosten der Verkehrswege", "NW");
			nkaPlaceholder(printer, "Veraenderung der Verkehrssicherheit", "NS");
			nkaPlaceholder(printer, "Veraenderung der Reisezeit im Personenverkehr", "NRZ");
			nkaPlaceholder(printer, "davon Reisezeitnutzen aus Einzelreisezeitgewinnen < 1 min", "");
			nkaPlaceholder(printer, "Veraenderung der Transportzeit der Ladung im Gueterverkehr", "NTZ");
			nkaPlaceholder(printer, "Veraenderung der impliziten Nutzen", "NI");
			nkaPlaceholder(printer, "Veraenderung der Lebenszyklusemissionen von Treibhausgasen der Infrastruktur", "NL");
			nkaPlaceholder(printer, "Veraenderung der Geraeuschbelastung", "NG");
			nkaPlaceholder(printer, "Innerorts", "NGi");
			nkaPlaceholder(printer, "Ausserorts", "NGa");
			nkaPlaceholder(printer, "Veraenderung der Abgasbelastungen", "NA");
			nkaPlaceholder(printer, "Stickoxid-Emissionen (NOx)", "NA1");
			nkaPlaceholder(printer, "Kohlenmonoxid-Emissionen (CO)", "NA2");
			nkaPlaceholder(printer, "Kohlendioxid-Emissionen (CO2)", "NA3");
			nkaPlaceholder(printer, "Kohlenwasserstoff-Emissionen (HC)", "NA4");
			nkaPlaceholder(printer, "Feinstaub-Emissionen (PM)", "NA5");
			nkaPlaceholder(printer, "Schwefeldioxid-Emissionen (SO2)", "NA6");
			nkaPlaceholder(printer, "Veraenderung der inneroertlichen Trennwirkungen", "NT");
			nkaPlaceholder(printer, "Veraenderung der Zuverlaessigkeit", "NZ");
			nkaPlaceholder(printer, "Gesamtnutzen", "");
		}
	}

	private void writeCosts() throws IOException {

		try (CSVPrinter printer = printer(output.getPath("bvwp_costs.csv"))) {
			printer.printRecord("description", "cost_mio_eur", "present_value_mio_eur", "status");
			printer.printRecord("Planungskosten", PLACEHOLDER, PLACEHOLDER, REQUIRES_BVWP_PARAMETERS);
			printer.printRecord("Aus- und Neubaukosten", PLACEHOLDER, PLACEHOLDER, REQUIRES_BVWP_PARAMETERS);
			printer.printRecord("Summe bewertungsrelevanter Investitionskosten", PLACEHOLDER, PLACEHOLDER, REQUIRES_BVWP_PARAMETERS);
		}
	}

	private void writeSummary() throws IOException {

		try (CSVPrinter printer = printer(output.getPath("bvwp_summary.csv"))) {
			printer.printRecord("metric", "value", "unit", "status");
			printer.printRecord("Barwert des Nutzens", PLACEHOLDER, "Mio. EUR", REQUIRES_BVWP_PARAMETERS);
			printer.printRecord("Barwert der bewertungsrelevanten Investitionskosten", PLACEHOLDER, "Mio. EUR", REQUIRES_BVWP_PARAMETERS);
			printer.printRecord("Nutzen-Kosten-Verhaeltnis (NKV)", PLACEHOLDER, "", REQUIRES_BVWP_PARAMETERS);
		}
	}

	private EmissionStats readEmissionStats(Path file) {

		if (file == null || !Files.exists(file))
			return EmissionStats.missing();

		EmissionStats result = new EmissionStats(true);

		try (CSVParser parser = new CSVParser(IOUtils.getBufferedReader(file.toString()), csvFormat(file))) {
			for (CSVRecord record : parser) {
				String mode = firstNonBlank(record, "vehicleType", "network_mode", "mode");
				String pollutant = firstNonBlank(record, "pollutant", "Pollutant");

				if (mode == null || pollutant == null)
					continue;

				double value = parseDouble(record, "value");
				if (Double.isNaN(value))
					value = parseDouble(record, "kg") * 1000.0;

				if (Double.isNaN(value))
					continue;

				if (personTrafficModes.contains(mode)) {
					result.addPersonTraffic(pollutant, value);
				} else if (freightTrafficModes.contains(mode)) {
					result.addFreightTraffic(pollutant, value);
				}
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		return result;
	}

	private Path findReferenceEmissions(Path runDirectory) {

		Path analysisFile = runDirectory.resolve("analysis").resolve("emissions").resolve("emissions_per_network_mode.csv");
		if (Files.exists(analysisFile))
			return analysisFile;

		try {
			return ApplicationUtils.matchInput("emissions_per_network_mode.csv", runDirectory);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	private void comparison(CSVPrinter printer, RunStats reference, RunStats plan, String section, String metric, String unit, Value value) throws IOException {

		double planValue = value.get(plan);

		if (reference == null) {
			printer.printRecord(section, metric, unit, PLACEHOLDER, format(planValue), PLACEHOLDER, MISSING_REFERENCE);
			return;
		}

		double referenceValue = value.get(reference);
		printer.printRecord(section, metric, unit, format(referenceValue), format(planValue), format(planValue - referenceValue), COMPUTED);
	}

	private void emission(CSVPrinter printer, EmissionStats plan, EmissionStats reference, String label, String code, String pollutant) throws IOException {

		if (!plan.available || !reference.available) {
			printer.printRecord(label, code, PLACEHOLDER, PLACEHOLDER, PLACEHOLDER, "t/a",
				referenceRunDirectory == null ? MISSING_REFERENCE : "placeholder_missing_emissions");
			return;
		}

		double pkw = annualTons(plan.personTraffic.getOrDefault(pollutant, 0.0), personTrafficDaysPerYear)
			- annualTons(reference.personTraffic.getOrDefault(pollutant, 0.0), personTrafficDaysPerYear);
		double lkw = annualTons(plan.freightTraffic.getOrDefault(pollutant, 0.0), freightTrafficDaysPerYear)
			- annualTons(reference.freightTraffic.getOrDefault(pollutant, 0.0), freightTrafficDaysPerYear);

		printer.printRecord(label, code, format(pkw), format(lkw), format(pkw + lkw), "t/a", COMPUTED);
	}

	private void placeholder(CSVPrinter printer, String section, String metric, String unit, String status) throws IOException {
		printer.printRecord(section, metric, unit, PLACEHOLDER, PLACEHOLDER, PLACEHOLDER, status);
	}

	private void nkaPlaceholder(CSVPrinter printer, String description, String component) throws IOException {
		printer.printRecord(description, component, PLACEHOLDER, PLACEHOLDER, REQUIRES_BVWP_PARAMETERS);
	}

	private double annualMillionKilometers(Measure measure, int daysPerYear) {
		return measure.distanceMeters * daysPerYear / METERS_PER_KILOMETER / MILLION;
	}

	private double annualMillionHours(Measure measure, int daysPerYear) {
		return annualMillionHours(measure.travelTimeSeconds, daysPerYear);
	}

	private double annualMillionHours(double seconds, int daysPerYear) {
		return seconds * HOURS_PER_SECOND * daysPerYear / MILLION;
	}

	private double annualTons(double gramsPerDay, int daysPerYear) {
		return gramsPerDay * daysPerYear / MILLION;
	}

	private String firstNonBlank(CSVRecord record, String... columns) {
		for (String column : columns) {
			if (record.isMapped(column)) {
				String value = record.get(column);
				if (value != null && !value.isBlank())
					return value.trim();
			}
		}

		return null;
	}

	private double parseDouble(CSVRecord record, String column) {
		if (!record.isMapped(column))
			return Double.NaN;

		String value = record.get(column);
		if (value == null || value.isBlank())
			return Double.NaN;

		try {
			return Double.parseDouble(value);
		} catch (NumberFormatException e) {
			return Double.NaN;
		}
	}

	private double parseTime(CSVRecord record, String column) {
		if (!record.isMapped(column))
			return Double.NaN;

		String value = record.get(column);
		if (value == null || value.isBlank())
			return Double.NaN;

		try {
			return Time.parseTime(value);
		} catch (IllegalArgumentException e) {
			return Double.NaN;
		}
	}

	private CSVFormat csvFormat(Path path) throws IOException {
		return CSVFormat.DEFAULT.builder()
			.setHeader()
			.setSkipHeaderRecord(true)
			.setDelimiter(CsvOptions.detectDelimiter(path.toString()))
			.build();
	}

	private CSVPrinter printer(Path path) throws IOException {
		return new CSVPrinter(Files.newBufferedWriter(path), CSVFormat.DEFAULT);
	}

	private String format(double value) {
		return String.format(L, "%.3f", value);
	}

	private interface Value {
		double get(RunStats stats);
	}

	private static final class RunStats {
		private final Measure personTraffic = new Measure();
		private final Measure freightTraffic = new Measure();
		private double freightBelow50KmSeconds;
		private double freightAtLeast50KmSeconds;
	}

	private static final class Measure {
		private double distanceMeters;
		private double travelTimeSeconds;

		private void add(double distanceMeters, double travelTimeSeconds) {
			this.distanceMeters += distanceMeters;
			this.travelTimeSeconds += travelTimeSeconds;
		}
	}

	private static final class EmissionStats {
		private final boolean available;
		private final Map<String, Double> personTraffic = new LinkedHashMap<>();
		private final Map<String, Double> freightTraffic = new LinkedHashMap<>();

		private EmissionStats(boolean available) {
			this.available = available;
		}

		private static EmissionStats missing() {
			return new EmissionStats(false);
		}

		private void addPersonTraffic(String pollutant, double value) {
			personTraffic.merge(Objects.toString(pollutant), value, Double::sum);
		}

		private void addFreightTraffic(String pollutant, double value) {
			freightTraffic.merge(Objects.toString(pollutant), value, Double::sum);
		}
	}
}
