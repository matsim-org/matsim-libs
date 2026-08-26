package org.matsim.application.analysis.impact;

import org.matsim.application.ApplicationUtils;
import org.matsim.application.CommandSpec;
import org.matsim.application.Dependency;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.analysis.emissions.AirPollutionAnalysis;
import org.matsim.application.options.InputOptions;
import org.matsim.application.options.OutputOptions;
import org.matsim.application.options.SampleOptions;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

/** Orchestrates absolute and reference-policy impact analysis for the SimWrapper dashboard. */
@CommandLine.Command(name = "impact", description = "Calculates absolute and optionally comparative MATSim impacts.")
@CommandSpec(requireRunDirectory = true, requires = {"legs.csv", "trips.csv", "persons.csv"},
	dependsOn = {@Dependency(value = AirPollutionAnalysis.class, files = "emissions_per_network_mode.csv")},
	produces = "impact.csv")
public class ImpactAnalysis implements MATSimAppCommand {

	@CommandLine.Mixin
	private final InputOptions input = InputOptions.ofCommand(ImpactAnalysis.class);
	@CommandLine.Mixin
	private final OutputOptions output = OutputOptions.ofCommand(ImpactAnalysis.class);
	@CommandLine.Mixin
	private SampleOptions sample;

	@CommandLine.Option(names = "--reference-run-directory", description = "Optional run directory used as reference case.")
	private Path referenceRunDirectory;
	@CommandLine.Option(names = "--reference-sample-size", description = "Sample fraction of the reference run. Defaults to the policy sample size.")
	private Double referenceSampleSize;
	@CommandLine.Option(names = "--modes", split = ",", description = "Modes included in the analysis. Defaults to modes found in trips.csv.")
	private Set<String> modes;
	@CommandLine.Option(names = "--vehicle-modes", split = ",", defaultValue = "car,freight,truck", description = "Network modes used for vehicle metrics.")
	private Set<String> vehicleModes;
	@CommandLine.Option(names = "--freight-modes", split = ",", defaultValue = "freight,truck", description = "Modes treated as freight transport.")
	private Set<String> freightModes;
	@CommandLine.Option(names = "--person-traffic-days-per-year", defaultValue = "334", description = "Annualization factor for passenger transport.")
	private int personTrafficDaysPerYear;
	@CommandLine.Option(names = "--freight-traffic-days-per-year", defaultValue = "302", description = "Annualization factor for freight transport.")
	private int freightTrafficDaysPerYear;

	public static void main(String[] args) { new ImpactAnalysis().execute(args); }

	@Override
	public Integer call() throws Exception {
		validateOptions();
		// SampleOptions describes the simulated population share. Only additive quantities are upscaled;
		// person-level means are calculated later from the unscaled matched observations.
		double policyScale = sample.getUpscaleFactor();
		double referenceScale = referenceSampleSize == null ? policyScale : 1. / referenceSampleSize;
		ImpactRunReader reader = new ImpactRunReader(modes, vehicleModes);

		// The policy files are supplied by the standard MATSim application input mechanism. Reference files are
		// matched independently because the reference directory is read-only and outside the current run context.
		ImpactAnalysisResult policy = reader.read(Path.of(input.getPath("trips.csv")), Path.of(input.getPath("legs.csv")),
			Path.of(input.getPath("persons.csv")), scenarioEmissions(), policyScale);
		ImpactAnalysisResult reference = referenceRunDirectory == null ? null : reader.read(
			ApplicationUtils.matchInput("trips.csv", referenceRunDirectory), ApplicationUtils.matchInput("legs.csv", referenceRunDirectory),
			ApplicationUtils.matchInput("persons.csv", referenceRunDirectory), findEmissions(referenceRunDirectory), referenceScale);
		new ImpactCsvWriter(modes, vehicleModes, freightModes, personTrafficDaysPerYear, freightTrafficDaysPerYear)
			.write(output.getPath("impact.csv"), policy, reference, policyScale, referenceScale);
		return 0;
	}

	private void validateOptions() {
		if (referenceSampleSize != null && (referenceSampleSize <= 0. || referenceSampleSize > 1.))
			throw new IllegalArgumentException("--reference-sample-size must be in (0, 1].");
		if (personTrafficDaysPerYear <= 0 || freightTrafficDaysPerYear <= 0)
			throw new IllegalArgumentException("Annualization factors must be positive.");
	}

	private Path scenarioEmissions() {
		// Prefer the dependency output resolved by SimWrapper. A directory lookup also supports invoking the command
		// manually against a run for which the emissions analysis has already been generated.
		String configured = input.getPath(AirPollutionAnalysis.class, "emissions_per_network_mode.csv");
		Path path = configured == null ? null : Path.of(configured);
		return path != null && Files.exists(path) ? path : findEmissions(input.getRunDirectory());
	}

	private Path findEmissions(Path runDirectory) {
		// AirPollutionAnalysis normally writes to this analysis directory. matchInput retains compatibility with
		// prefixed MATSim output names and alternative analysis layouts.
		Path analysis = runDirectory.resolve("analysis/emissions/emissions_per_network_mode.csv");
		if (Files.exists(analysis)) return analysis;
		try {
			Path matched = ApplicationUtils.matchInput("emissions_per_network_mode.csv", runDirectory);
			return Files.exists(matched) ? matched : null;
		} catch (IllegalArgumentException e) {
			return null;
		}
	}
}
