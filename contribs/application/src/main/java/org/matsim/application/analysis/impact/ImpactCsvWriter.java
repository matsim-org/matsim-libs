package org.matsim.application.analysis.impact;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Writes the stable long-format CSV contract consumed by the SimWrapper impact dashboard. */
final class ImpactCsvWriter {

	private static final double METERS_PER_KILOMETER = 1000.;
	private static final double SECONDS_PER_HOUR = 3600.;
	private static final double MILLION = 1_000_000.;
	private static final String ABSOLUTE = "absolute";
	private static final String COMPARISON = "comparison";
	private static final List<String> POLLUTANTS = List.of("CO2_TOTAL", "NOx", "CO", "HC", "PM", "SO2");

	private final Set<String> configuredModes;
	private final Set<String> vehicleModes;
	private final Set<String> freightModes;
	private final int personDays;
	private final int freightDays;

	ImpactCsvWriter(Set<String> configuredModes, Set<String> vehicleModes, Set<String> freightModes,
			int personDays, int freightDays) {
		this.configuredModes = configuredModes;
		this.vehicleModes = vehicleModes;
		this.freightModes = freightModes;
		this.personDays = personDays;
		this.freightDays = freightDays;
	}

	void write(Path output, ImpactAnalysisResult policy, ImpactAnalysisResult reference,
			double policyScale, double referenceScale) throws IOException {
		// A single deterministic long-format artifact is the stable interface to SimWrapper. Analytical components
		// remain separate methods so future output files can be introduced without changing the calculations.
		try (CSVPrinter printer = new CSVPrinter(Files.newBufferedWriter(output), CSVFormat.DEFAULT)) {
			header(printer);
			traffic(printer, policy, reference);
			emissions(printer, policy, reference);
			scores(printer, policy, reference);
			persons(printer, policy, reference, policyScale, referenceScale);
			methodology(printer, policyScale, referenceScale, reference != null);
			reservedRows(printer, reference != null);
		}
	}

	private void traffic(CSVPrinter printer, ImpactAnalysisResult policy, ImpactAnalysisResult reference) throws IOException {
		Set<String> modes = union(policy.byMode.keySet(), reference == null ? Set.of() : reference.byMode.keySet());
		// Modes are discovered from both scenarios. Explicitly configured modes are also emitted so a mode absent in
		// one or both runs is visible as a computed zero rather than silently disappearing.
		if (configuredModes != null) modes.addAll(configuredModes);
		for (String mode : sorted(modes)) {
			ImpactAnalysisResult.ModeImpact policyMode = policy.byMode.getOrDefault(mode, ImpactAnalysisResult.ModeImpact.EMPTY);
			ImpactAnalysisResult.ModeImpact referenceMode = reference == null ? null : reference.byMode.getOrDefault(mode, ImpactAnalysisResult.ModeImpact.EMPTY);
			int days = days(mode);
			boolean freight = freightModes.contains(mode);
			String section = freight ? "Gueterverkehr" : "Personenverkehr";
			row(printer, section, "Fahrten", mode, "Fahrten/Tag", referenceMode, policyMode, value -> value.trips);
			row(printer, section, "Fahrten", mode, "Fahrten/a", referenceMode, policyMode, value -> value.trips * days);
			row(printer, section, freight ? "Zurueckgelegte Distanz" : "Verkehrsleistung Personen", mode,
				freight ? "km/Tag" : "Personen-km/Tag", referenceMode, policyMode,
				value -> value.personDistanceMeters / METERS_PER_KILOMETER);
			row(printer, section, freight ? "Zurueckgelegte Distanz" : "Verkehrsleistung Personen", mode,
				freight ? "Mio. km/a" : "Mio. Personen-km/a", referenceMode, policyMode,
				value -> annualKilometers(value.personDistanceMeters, days));
			row(printer, section, freight ? "Reisezeit" : "Reisezeit Personen", mode,
				freight ? "h/Tag" : "Personen-h/Tag", referenceMode, policyMode,
				value -> value.personTravelTimeSeconds / SECONDS_PER_HOUR);
			row(printer, section, freight ? "Reisezeit" : "Reisezeit Personen", mode,
				freight ? "Mio. h/a" : "Mio. Personen-h/a", referenceMode, policyMode,
				value -> annualHours(value.personTravelTimeSeconds, days));
			if (vehicleModes.contains(mode)) vehicleTraffic(printer, section, mode, days, referenceMode, policyMode);
		}
	}

	private void vehicleTraffic(CSVPrinter printer, String section, String mode, int days,
			ImpactAnalysisResult.ModeImpact reference, ImpactAnalysisResult.ModeImpact policy) throws IOException {
		row(printer, section, "Fahrzeugfahrten", mode, "Fahrzeug-Legs/Tag", reference, policy, value -> value.vehicleLegs);
		row(printer, section, "Fahrzeugverkehrsleistung", mode, "Mio. Fahrzeug-km/a", reference, policy,
			value -> annualKilometers(value.vehicleDistanceMeters, days));
		row(printer, section, "Fahrzeugverkehrsleistung", mode, "Fahrzeug-km/Tag", reference, policy,
			value -> value.vehicleDistanceMeters / METERS_PER_KILOMETER);
		row(printer, section, "Fahrzeugeinsatzzeit", mode, "Mio. Fahrzeug-h/a", reference, policy,
			value -> annualHours(value.vehicleTravelTimeSeconds, days));
		row(printer, section, "Fahrzeugeinsatzzeit", mode, "Fahrzeug-h/Tag", reference, policy,
			value -> value.vehicleTravelTimeSeconds / SECONDS_PER_HOUR);
	}

	private void emissions(CSVPrinter printer, ImpactAnalysisResult policy, ImpactAnalysisResult reference) throws IOException {
		Set<String> modes = union(policy.emissions.keySet(), reference == null ? Set.of() : reference.emissions.keySet());
		if (modes.isEmpty()) modes.addAll(vehicleModes);
		for (String mode : sorted(modes)) {
			Set<String> pollutants = new LinkedHashSet<>(POLLUTANTS);
			pollutants.addAll(policy.emissions.getOrDefault(mode, Map.of()).keySet());
			if (reference != null) pollutants.addAll(reference.emissions.getOrDefault(mode, Map.of()).keySet());
			for (String pollutant : pollutants) {
				Double policyDay = emission(policy, mode, pollutant, false);
				Double referenceDay = reference == null ? null : emission(reference, mode, pollutant, false);
				Double policyYear = emission(policy, mode, pollutant, true);
				Double referenceYear = reference == null ? null : emission(reference, mode, pollutant, true);
				String status = policyDay == null || reference != null && referenceDay == null ? "missing_emissions"
					: reference == null ? ABSOLUTE : COMPARISON;
				values(printer, "Emissionen", pollutant, mode, "kg/Tag", referenceDay, policyDay, status);
				values(printer, "Emissionen", pollutant, mode, "t/a", referenceYear, policyYear, status);
			}
		}
	}

	private void scores(CSVPrinter printer, ImpactAnalysisResult policy, ImpactAnalysisResult reference) throws IOException {
		// Scores remain utilities and are not annualized or silently interpreted as monetary values.
		Double referenceMean = reference == null || reference.scoredPersons == 0 ? null : reference.scoreSum / reference.scoredPersons;
		Double policyMean = policy.scoredPersons == 0 ? null : policy.scoreSum / policy.scoredPersons;
		String status = reference == null ? ABSOLUTE : COMPARISON;
		values(printer, "Score", "Personen mit ausgefuehrtem Score", "all", "Personen/Tag",
			reference == null ? null : reference.scoredPersons, policy.scoredPersons, status);
		values(printer, "Score", "Summe ausgefuehrter Score", "all", "utils/Tag",
			reference == null ? null : reference.scoreSum, policy.scoreSum, status);
		values(printer, "Score", "Mittlerer ausgefuehrter Score", "all", "utils/Person", referenceMean, policyMean, status);
	}

	private void persons(CSVPrinter printer, ImpactAnalysisResult policy, ImpactAnalysisResult reference,
			double policyScale, double referenceScale) throws IOException {
		if (reference == null) {
			for (String metric : List.of("Gemeinsame Personen", "Nur im Bezugsfall", "Nur im Szenario", "Verbleiber", "Wechsler",
				"Mittlere Reisezeit gemeinsamer Personen", "Mittlere Scoredifferenz gemeinsamer Personen"))
				values(printer, "Agentenvergleich", metric, "all", "", null, null, "not_applicable_without_reference");
			return;
		}
		ImpactComparison comparison = ImpactComparison.compare(reference, policy);
		values(printer, "Agentenvergleich", "Gemeinsame Personen", "all", "Personen (Stichprobe)",
			(double) comparison.matched(), (double) comparison.matched(), COMPARISON);
		// Unmatched counts are additive and therefore use each run's population scale. Matched means below remain
		// unscaled because multiplying numerator and denominator would not change a mean.
		values(printer, "Agentenvergleich", "Nur im Bezugsfall", "all", "Personen/Tag",
			comparison.referenceOnly() * referenceScale, 0., COMPARISON);
		values(printer, "Agentenvergleich", "Nur im Szenario", "all", "Personen/Tag",
			0., comparison.policyOnly() * policyScale, COMPARISON);
		values(printer, "Agentenvergleich", "Verbleiber", "all", "Personen (Stichprobe)",
			(double) comparison.remainers(), (double) comparison.remainers(), COMPARISON);
		values(printer, "Agentenvergleich", "Wechsler", "all", "Personen (Stichprobe)",
			(double) comparison.switchers(), (double) comparison.switchers(), COMPARISON);
		values(printer, "Agentenvergleich", "Mittlere Reisezeit gemeinsamer Personen", "all", "h/Person/Tag",
			comparison.matched() == 0 ? null : comparison.referenceTravelTimeSeconds() / comparison.matched() / SECONDS_PER_HOUR,
			comparison.matched() == 0 ? null : comparison.policyTravelTimeSeconds() / comparison.matched() / SECONDS_PER_HOUR, COMPARISON);
		values(printer, "Agentenvergleich", "Mittlere Scoredifferenz gemeinsamer Personen", "all", "utils/Person",
			0., Double.isNaN(comparison.meanScoreDelta()) ? null : comparison.meanScoreDelta(), COMPARISON);
	}

	private void methodology(CSVPrinter printer, double policyScale, double referenceScale, boolean comparison) throws IOException {
		values(printer, "Methodik", "Hochrechnungsfaktor Szenario", "all", "Faktor", null, policyScale, "configured");
		values(printer, "Methodik", "Hochrechnungsfaktor Bezugsfall", "all", "Faktor",
			comparison ? referenceScale : null, null, comparison ? "configured" : "not_applicable_without_reference");
		values(printer, "Methodik", "Jahrestage Personenverkehr", "all", "Tage/a",
			comparison ? (double) personDays : null, (double) personDays, "configured");
		values(printer, "Methodik", "Jahrestage Gueterverkehr", "all", "Tage/a",
			comparison ? (double) freightDays : null, (double) freightDays, "configured");
	}

	private void reservedRows(CSVPrinter printer, boolean comparison) throws IOException {
		values(printer, "Nutzen-Kosten-Analyse", "Rule-of-Half-Nutzen", "all", "EUR/a", null, null,
			comparison ? "not_implemented" : "not_applicable_without_reference");
		values(printer, "Nutzen-Kosten-Analyse", "Verkehrssicherheit", "all", "EUR/a", null, null, "missing_accident_rates");
		values(printer, "Nutzen-Kosten-Analyse", "Investitionskosten", "all", "EUR", null, null, "missing_investment_cost");
	}

	private Double emission(ImpactAnalysisResult result, String mode, String pollutant, boolean annual) {
		if (!result.emissionsAvailable) return null;
		double grams = result.emissions.getOrDefault(mode, Map.of()).getOrDefault(pollutant, 0.);
		// Emission totals are grams per represented day. Annual values use the same explicit passenger/freight
		// annualization factors as the corresponding traffic metrics.
		return annual ? grams * days(mode) / MILLION : grams / 1000.;
	}

	private void row(CSVPrinter printer, String section, String metric, String mode, String unit,
			ImpactAnalysisResult.ModeImpact reference, ImpactAnalysisResult.ModeImpact policy, MetricValue value) throws IOException {
		values(printer, section, metric, mode, unit, reference == null ? null : value.get(reference), value.get(policy),
			reference == null ? ABSOLUTE : COMPARISON);
	}

	private void values(CSVPrinter printer, String section, String metric, String mode, String unit,
			Double reference, Double policy, String status) throws IOException {
		// Physical deltas always use policy minus reference. A zero reference has no meaningful relative change,
		// therefore the relative field stays empty instead of emitting infinity or an invented zero.
		String difference = reference == null || policy == null ? "" : format(policy - reference);
		String relative = reference == null || policy == null || reference == 0. ? "" : format((policy - reference) / reference);
		printer.printRecord(section, metric, metric, mode, unit.contains("/a") ? "year" : "day", unit,
			reference == null ? "" : format(reference), policy == null ? "" : format(policy), difference, relative,
			status, "MATSim standard output");
	}

	private void header(CSVPrinter printer) throws IOException {
		printer.printRecord("section", "metric", "component", "mode", "period", "unit", "reference", "scenario",
			"difference", "relative_change", "status", "source");
	}

	private int days(String mode) { return freightModes.contains(mode) ? freightDays : personDays; }
	private double annualKilometers(double meters, int days) { return meters * days / METERS_PER_KILOMETER / MILLION; }
	private double annualHours(double seconds, int days) { return seconds * days / SECONDS_PER_HOUR / MILLION; }
	private String format(double value) { return String.format(Locale.US, "%.6f", value); }
	private Set<String> union(Set<String> first, Set<String> second) { Set<String> result = new LinkedHashSet<>(first); result.addAll(second); return result; }
	private List<String> sorted(Set<String> values) { List<String> result = new ArrayList<>(values); result.sort(Comparator.naturalOrder()); return result; }

	@FunctionalInterface
	private interface MetricValue { double get(ImpactAnalysisResult.ModeImpact impact); }
}
