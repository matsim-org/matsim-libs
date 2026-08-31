package org.matsim.application.analysis.impact;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.matsim.application.CommandSpec;
import org.matsim.application.Dependency;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.InputOptions;
import org.matsim.application.options.OutputOptions;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Creates compact, presentation-only tables from the stable impact CSV. */
@CommandLine.Command(name = "impact-dashboard-tables", description = "Creates display tables for the impact dashboard.")
@CommandSpec(
	dependsOn = @Dependency(value = ImpactAnalysis.class, files = "impact.csv"),
	produces = {
		"impact_person_trips.csv", "impact_person_distance.csv", "impact_person_time.csv",
		"impact_freight_trips.csv", "impact_freight_distance.csv", "impact_freight_time.csv",
		"impact_emissions.csv", "impact_scores.csv", "impact_agents.csv", "impact_methodology.csv",
		"impact_cost_benefit.csv",
		"impact_person_trips_base.csv", "impact_person_trips_policy.csv", "impact_person_trips_difference.csv",
		"impact_person_distance_base.csv", "impact_person_distance_policy.csv", "impact_person_distance_difference.csv",
		"impact_person_time_base.csv", "impact_person_time_policy.csv", "impact_person_time_difference.csv",
		"impact_freight_trips_base.csv", "impact_freight_trips_policy.csv", "impact_freight_trips_difference.csv",
		"impact_freight_distance_base.csv", "impact_freight_distance_policy.csv", "impact_freight_distance_difference.csv",
		"impact_freight_time_base.csv", "impact_freight_time_policy.csv", "impact_freight_time_difference.csv",
		"impact_emissions_base.csv", "impact_emissions_policy.csv", "impact_emissions_difference.csv",
		"impact_scores_base.csv", "impact_scores_policy.csv", "impact_scores_difference.csv",
		"impact_agents_base.csv", "impact_agents_policy.csv", "impact_agents_difference.csv",
		"impact_methodology_base.csv", "impact_methodology_policy.csv", "impact_methodology_difference.csv",
		"impact_cost_benefit_base.csv", "impact_cost_benefit_policy.csv", "impact_cost_benefit_difference.csv",
		"impact_general_car.csv", "impact_emissions_car.csv", "impact_general_truck.csv", "impact_emissions_truck.csv",
		"impact_general_freight.csv", "impact_emissions_freight.csv", "impact_general_bike.csv", "impact_emissions_bike.csv",
		"impact_general_pt.csv", "impact_emissions_pt.csv", "impact_general_ride.csv", "impact_emissions_ride.csv",
		"impact_general_walk.csv", "impact_emissions_walk.csv"
		, "impact_general_car_day.csv", "impact_emissions_car_day.csv", "impact_general_car_year.csv", "impact_emissions_car_year.csv",
		"impact_general_truck_day.csv", "impact_emissions_truck_day.csv", "impact_general_truck_year.csv", "impact_emissions_truck_year.csv",
		"impact_general_freight_day.csv", "impact_emissions_freight_day.csv", "impact_general_freight_year.csv", "impact_emissions_freight_year.csv",
		"impact_general_bike_day.csv", "impact_emissions_bike_day.csv", "impact_general_bike_year.csv", "impact_emissions_bike_year.csv",
		"impact_general_pt_day.csv", "impact_emissions_pt_day.csv", "impact_general_pt_year.csv", "impact_emissions_pt_year.csv",
		"impact_general_ride_day.csv", "impact_emissions_ride_day.csv", "impact_general_ride_year.csv", "impact_emissions_ride_year.csv",
		"impact_general_walk_day.csv", "impact_emissions_walk_day.csv", "impact_general_walk_year.csv", "impact_emissions_walk_year.csv"
		, "impact_scores_day.csv", "impact_scores_year.csv"
	}
)
public class ImpactDashboardTables implements MATSimAppCommand {
	private static final String WORD_JOINER = "\u2060";

	@CommandLine.Mixin
	private final InputOptions input = InputOptions.ofCommand(ImpactDashboardTables.class);
	@CommandLine.Mixin
	private final OutputOptions output = OutputOptions.ofCommand(ImpactDashboardTables.class);

	public static void main(String[] args) {
		new ImpactDashboardTables().execute(args);
	}

	@Override
	public Integer call() throws Exception {
		List<ImpactRecord> records = read(Path.of(input.getPath(ImpactAnalysis.class, "impact.csv")));
		writeModeTables(records, output.getPath("impact_person_trips.csv").getParent());

		writePeriodTables(records, "Personenverkehr", "Fahrten", output.getPath("impact_person_trips.csv"));
		writePeriodTables(records, "Personenverkehr", "Verkehrsleistung Personen", output.getPath("impact_person_distance.csv"));
		writePeriodTables(records, "Personenverkehr", "Reisezeit Personen", output.getPath("impact_person_time.csv"));
		writePeriodTables(records, "Gueterverkehr", "Fahrten", output.getPath("impact_freight_trips.csv"));
		writePeriodTables(records, "Gueterverkehr", "Zurueckgelegte Distanz", output.getPath("impact_freight_distance.csv"));
		writePeriodTables(records, "Gueterverkehr", "Reisezeit", output.getPath("impact_freight_time.csv"));
		writePeriodTables(records, "Emissionen", null, output.getPath("impact_emissions.csv"));

		writeSectionTables(records, "Score", output.getPath("impact_scores.csv"));
		writeScoreTable(records, output.getPath("impact_scores.csv").resolveSibling("impact_scores_day.csv"), "day");
		writeScoreTable(records, output.getPath("impact_scores.csv").resolveSibling("impact_scores_year.csv"), "year");
		writeSectionTables(records, "Agentenvergleich", output.getPath("impact_agents.csv"));
		writeSectionTables(records, "Methodik", output.getPath("impact_methodology.csv"));
		writeSectionTables(records, "Nutzen-Kosten-Analyse", output.getPath("impact_cost_benefit.csv"));
		return 0;
	}

	private void writeScoreTable(List<ImpactRecord> records, Path path, String period) throws IOException {
		try (CSVPrinter printer = new CSVPrinter(Files.newBufferedWriter(path), CSVFormat.DEFAULT)) {
			printer.printRecord("Factor", "Base", "Policy", "Diff", "%", "Unit");
			for (ImpactRecord r : records) if (r.section.equals("Score") && r.period.equals(period))
				printer.printRecord(scoreFactor(r.metric), blankOrDash(r.reference), blankOrDash(r.scenario), blankOrDash(r.difference), percent(r.relativeChange), r.unit);
		}
	}

	private void writeModeTables(List<ImpactRecord> records, Path directory) throws IOException {
		Map<String, List<ImpactRecord>> modes = new LinkedHashMap<>();
		for (ImpactRecord record : records) {
			if (record.mode == null || record.mode.equals("all")) continue;
			modes.computeIfAbsent(record.mode, ignored -> new ArrayList<>()).add(record);
		}
		for (Map.Entry<String, List<ImpactRecord>> entry : modes.entrySet()) {
			String mode = entry.getKey();
			for (String period : List.of("day", "year")) {
				writeModeTable(entry.getValue(), directory.resolve("impact_general_" + mode + "_" + period + ".csv"), false, period);
				writeModeTable(entry.getValue(), directory.resolve("impact_emissions_" + mode + "_" + period + ".csv"), true, period);
			}
		}
	}

	private void writeModeTable(List<ImpactRecord> records, Path path, boolean emissions, String period) throws IOException {
		try (CSVPrinter printer = new CSVPrinter(Files.newBufferedWriter(path), CSVFormat.DEFAULT)) {
			printer.printRecord("Factor", "Scenario", "Diff", "%", "Unit");
			List<ImpactRecord> ordered = records.stream().sorted(java.util.Comparator.comparingInt(r -> factorOrder(factorName(r.metric)))).toList();
			for (ImpactRecord record : ordered) {
				if (emissions != record.section.equals("Emissionen") || !record.period.equals(period)) continue;
				String factor = emissions ? pollutantName(record.metric) : factorName(record.metric);
				if (factor == null) continue;
				printer.printRecord(factor, blankOrDash(record.scenario), blankOrDash(record.difference), percent(record.relativeChange), englishUnit(record.unit));
			}
		}
	}

	private int factorOrder(String factor) {
		if (factor == null) return 99;
		return switch (factor) {
			case "Vehicle Volume" -> 0;
			case "Vehicle Operating Times (≤ 50 km)" -> 1;
			case "Vehicle Operating Times (> 50 km)" -> 2;
			case "Vehicle Operating Times" -> 3;
			case "Travel Distance" -> 4;
			default -> 99;
		};
	}

	private String factorName(String metric) {
		return switch (metric) {
			case "Fahrzeugfahrten" -> "Vehicle Volume";
			case "Fahrzeugverkehrsleistung" -> "Travel Distance";
		case "Vehicle Operating Times (≤ 50 km)", "Vehicle Operating Times (> 50 km)", "Vehicle Operating Times" -> metric;
		default -> null;
		};
	}

	private String pollutantName(String pollutant) {
		return switch (pollutant) {
			case "CO2_TOTAL" -> "Total Carbon Dioxide (CO₂)";
			case "NOx" -> "Nitrogen Oxides (NOₓ)";
			case "CO" -> "Carbon Monoxide (CO)";
			case "HC" -> "Hydrocarbons (HC)";
			case "PM" -> "Particulate Matter (PM)";
			case "SO2" -> "Sulfur Dioxide (SO₂)";
			default -> pollutant;
		};
	}

	private String blankOrDash(String value) { return value == null || value.isBlank() ? "" : value; }

	private String percent(String value) {
		return value == null || value.isBlank() ? "" : String.format(Locale.US, "%.2f%%", Double.parseDouble(value) * 100.);
	}

	private String scoreFactor(String metric) {
		return switch (metric) {
			case "Personen mit ausgefuehrtem Score" -> "Persons with executed score";
			case "Summe ausgefuehrter Score" -> "Total executed score";
			case "Mittlerer ausgefuehrter Score" -> "Mean executed score";
			default -> metric;
		};
	}

	private String englishUnit(String unit) {
		return unit.replace("Fahrzeug", "vehicle").replace("Personen", "person").replace("/Tag", "/day")
			.replace("/a", "/year").replace("Mio.", "mio.").replace("million", "mio.").replace("Hours", "hours");
	}

	private List<ImpactRecord> read(Path input) throws IOException {
		List<ImpactRecord> records = new ArrayList<>();
		CSVFormat format = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build();
		try (CSVParser parser = new CSVParser(Files.newBufferedReader(input), format)) {
			for (CSVRecord record : parser) records.add(new ImpactRecord(record));
		}
		return records;
	}

	private void writePeriodTables(List<ImpactRecord> records, String section, String metric, Path path) throws IOException {
		for (String kind : List.of("base", "policy", "difference"))
			writePeriodTable(records, section, metric, casePath(path, kind), kind);
	}

	private void writePeriodTable(List<ImpactRecord> records, String section, String metric, Path path, String kind) throws IOException {
		Map<String, PeriodValues> rows = new LinkedHashMap<>();
		int decimals = decimalPlaces(section, metric);
		for (ImpactRecord record : records) {
			if (!record.section.equals(section) || metric != null && !record.metric.equals(metric)) continue;
			String label = metric == null ? record.metric + " · " + record.mode : record.mode;
			PeriodValues values = rows.computeIfAbsent(label, ignored -> new PeriodValues());
				if (record.period.equals("day")) {
					values.day = displayValue(record, decimals, kind);
				values.dayUnit = record.unit;
				} else if (record.period.equals("year")) {
					values.year = displayValue(record, decimals, kind);
				values.yearUnit = record.unit;
			}
		}

		try (CSVPrinter printer = new CSVPrinter(Files.newBufferedWriter(path), CSVFormat.DEFAULT)) {
			if (metric == null) {
				printer.printRecord("Schadstoff und Modus", "Einheit", "pro Tag", "pro Jahr");
				for (Map.Entry<String, PeriodValues> row : rows.entrySet())
					printer.printRecord(row.getKey(), unitLabel(row.getValue()),
						valueOrPlaceholder(row.getValue().day), valueOrPlaceholder(row.getValue().year));
			} else {
				// Keep the period and metric explicit in the column names:
				// Modus | Fahrten pro Tag | Fahrten pro Jahr.
				String dayUnit = rows.values().stream().map(values -> values.dayUnit).filter(java.util.Objects::nonNull)
					.findFirst().orElse("");
				String yearUnit = rows.values().stream().map(values -> values.yearUnit).filter(java.util.Objects::nonNull)
					.findFirst().orElse("");
				printer.printRecord("Modus", periodHeader(metric, dayUnit, "Tag"), periodHeader(metric, yearUnit, "Jahr"));
				for (Map.Entry<String, PeriodValues> row : rows.entrySet())
					printer.printRecord(row.getKey(), valueOrPlaceholder(row.getValue().day),
						valueOrPlaceholder(row.getValue().year));
			}
		}
	}

	private void writeSectionTables(List<ImpactRecord> records, String section, Path path) throws IOException {
		for (String kind : List.of("base", "policy", "difference"))
			writeSectionTable(records, section, casePath(path, kind), kind);
	}

	private void writeSectionTable(List<ImpactRecord> records, String section, Path path, String kind) throws IOException {
		try (CSVPrinter printer = new CSVPrinter(Files.newBufferedWriter(path), CSVFormat.DEFAULT)) {
			String header = switch (kind) {
				case "base" -> "Base";
				case "policy" -> "Policy";
				default -> "Diff";
			};
			printer.printRecord("Kennzahl", "Einheit", header);
			for (ImpactRecord record : records) {
				if (!record.section.equals(section)) continue;
				String value = switch (kind) {
					case "base" -> record.reference;
					case "policy" -> record.scenario;
					default -> record.difference;
				};
				printer.printRecord(record.metric, record.unit, value == null || value.isBlank() ? "–" : value);
			}
		}
	}

	private String displayValue(ImpactRecord record, int decimals, String kind) {
		if (record.scenario.isBlank()) return "– (" + record.status + ")";
		String value = switch (kind) {
			case "base" -> record.reference;
			case "policy" -> record.scenario;
			default -> record.difference;
		};
		return value == null || value.isBlank() ? "–" : format(value, decimals);
	}

	private Path casePath(Path path, String kind) {
		String name = path.getFileName().toString();
		int dot = name.lastIndexOf('.');
		return path.resolveSibling(name.substring(0, dot) + "_" + kind + name.substring(dot));
	}

	private String periodHeader(String metric, String unit, String period) {
		if ("Fahrten".equals(metric)) return "Trips " + period;
		String displayUnit = unit.replace("person-km", "pkm").replace("person-hours", "person-hours")
			.replace("million", "mio.");
		String displayMetric = metric.replace(" Personen", "").replace("Zurueckgelegte Distanz", "Travel Distance");
		return displayMetric + " (" + displayUnit + ")";
	}

	private int decimalPlaces(String section, String metric) {
		if ("Fahrten".equals(metric)) return 0;
		if ("Verkehrsleistung Personen".equals(metric) || "Zurueckgelegte Distanz".equals(metric)) return 3;
		if (metric != null && metric.contains("Reisezeit")) return 2;
		return "Emissionen".equals(section) ? 3 : 2;
	}

	private String format(String value, int decimals) {
		// Keep trailing zeroes in SimWrapper. Its CSV reader otherwise converts the display value back to a number.
		return String.format(Locale.US, "%." + decimals + "f", Double.parseDouble(value)) + WORD_JOINER;
	}

	private String valueOrPlaceholder(String value) {
		return value == null ? "–" : value;
	}

	private String unitLabel(PeriodValues values) {
		if (values.dayUnit == null) return valueOrPlaceholder(values.yearUnit);
		if (values.yearUnit == null || values.dayUnit.equals(values.yearUnit)) return values.dayUnit;
		return values.dayUnit + " | " + values.yearUnit;
	}

	private static final class PeriodValues {
		private String day;
		private String year;
		private String dayUnit;
		private String yearUnit;
	}

	private record ImpactRecord(String section, String metric, String mode, String period, String unit,
			String reference, String scenario, String difference, String relativeChange, String status) {

		private ImpactRecord(CSVRecord record) {
			this(record.get("section"), record.get("metric"), record.get("mode"), record.get("period"), record.get("unit"),
				record.get("reference"), record.get("scenario"), record.get("difference"), record.get("relative_change"),
				record.get("status"));
		}
	}
}
