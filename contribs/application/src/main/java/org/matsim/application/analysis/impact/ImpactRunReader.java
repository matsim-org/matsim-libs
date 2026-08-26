package org.matsim.application.analysis.impact;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.matsim.application.options.CsvOptions;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Set;

/** Reads and aggregates the standard trip, leg, person and optional emissions outputs of one MATSim run. */
final class ImpactRunReader {

	private final Set<String> modes;
	private final Set<String> vehicleModes;

	ImpactRunReader(Set<String> modes, Set<String> vehicleModes) {
		this.modes = modes;
		this.vehicleModes = vehicleModes;
	}

	ImpactAnalysisResult read(Path trips, Path legs, Path persons, Path emissions, double populationScaleFactor) {
		ImpactAnalysisResult result = new ImpactAnalysisResult();
		// Aggregating while reading avoids retaining potentially millions of trip and leg records.
		readTrips(trips, populationScaleFactor, result);
		readLegs(legs, populationScaleFactor, result);
		readPersons(persons, populationScaleFactor, result);
		readEmissions(emissions, result);
		return result;
	}

	private void readTrips(Path file, double scale, ImpactAnalysisResult result) {
		try (CSVParser parser = parser(file)) {
			for (CSVRecord record : parser) {
				// TripAnalysis already provides MATSim's main-mode result. Falling back supports older compatible outputs
				// without recreating trip identification or main-mode logic here.
				String mode = text(record, "main_mode", "longest_distance_mode", "mode");
				if (!consider(mode)) continue;
				double distance = number(record, "traveled_distance", "distance");
				double travelTime = time(record, "trav_time");
				ImpactAnalysisResult.ModeImpact modeImpact = result.byMode.computeIfAbsent(mode, ignored -> new ImpactAnalysisResult.ModeImpact());
				String person = text(record, "person");
				if (person != null) {
					ImpactAnalysisResult.PersonImpact personImpact = result.persons.computeIfAbsent(person, ignored -> new ImpactAnalysisResult.PersonImpact());
					personImpact.modes.add(mode);
					if (!Double.isNaN(travelTime)) personImpact.travelTimeSeconds += travelTime;
				}
				modeImpact.trips += scale;
				if (!Double.isNaN(distance)) modeImpact.personDistanceMeters += distance * scale;
				if (!Double.isNaN(travelTime)) modeImpact.personTravelTimeSeconds += travelTime * scale;
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private void readLegs(Path file, double scale, ImpactAnalysisResult result) {
		try (CSVParser parser = parser(file)) {
			for (CSVRecord record : parser) {
				// Missing network_mode denotes a passenger/non-network leg and must not become a vehicle movement.
				String mode = text(record, "network_mode");
				if (mode == null || !vehicleModes.contains(mode) || !consider(mode)) continue;
				ImpactAnalysisResult.ModeImpact impact = result.byMode.computeIfAbsent(mode, ignored -> new ImpactAnalysisResult.ModeImpact());
				double distance = number(record, "distance");
				double travelTime = time(record, "trav_time");
				impact.vehicleLegs += scale;
				if (!Double.isNaN(distance)) impact.vehicleDistanceMeters += distance * scale;
				if (!Double.isNaN(travelTime)) impact.vehicleTravelTimeSeconds += travelTime * scale;
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private void readPersons(Path file, double scale, ImpactAnalysisResult result) {
		// executed_score represents the executed plan and avoids re-scoring with potentially different assumptions.
		try (CSVParser parser = parser(file)) {
			for (CSVRecord record : parser) {
				String person = text(record, "person");
				if (person == null) continue;
				ImpactAnalysisResult.PersonImpact impact = result.persons.computeIfAbsent(person, ignored -> new ImpactAnalysisResult.PersonImpact());
				double score = number(record, "executed_score");
				if (!Double.isNaN(score)) {
					impact.score = score;
					result.scoreSum += score * scale;
					result.scoredPersons += scale;
				}
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private void readEmissions(Path file, ImpactAnalysisResult result) {
		// Emissions are optional: physical transport impacts remain useful when the emissions contrib was not run.
		if (file == null || !Files.exists(file)) return;
		result.emissionsAvailable = true;
		try (CSVParser parser = parser(file)) {
			for (CSVRecord record : parser) {
				String mode = text(record, "vehicleType", "network_mode", "mode");
				String pollutant = text(record, "pollutant", "Pollutant");
				if (mode == null || pollutant == null || !consider(mode)) continue;
				// AirPollutionAnalysis normally reports grams in value. The kg fallback handles compatible older outputs
				// while normalizing the internal representation to grams per simulated day.
				double grams = number(record, "value");
				if (Double.isNaN(grams)) {
					double kilograms = number(record, "kg");
					grams = Double.isNaN(kilograms) ? Double.NaN : kilograms * 1000.;
				}
				if (!Double.isNaN(grams)) result.emissions.computeIfAbsent(mode, ignored -> new LinkedHashMap<>()).merge(pollutant, grams, Double::sum);
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private boolean consider(String mode) {
		return mode != null && (modes == null || modes.isEmpty() || modes.contains(mode));
	}

	private CSVParser parser(Path file) throws IOException {
		CSVFormat format = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true)
			.setDelimiter(CsvOptions.detectDelimiter(file.toString())).build();
		return new CSVParser(IOUtils.getBufferedReader(file.toString()), format);
	}

	private String text(CSVRecord record, String... columns) {
		for (String column : columns) {
			if (record.isMapped(column) && !record.get(column).isBlank()) return record.get(column).trim();
		}
		return null;
	}

	private double number(CSVRecord record, String... columns) {
		for (String column : columns) {
			if (!record.isMapped(column) || record.get(column).isBlank()) continue;
			try { return Double.parseDouble(record.get(column)); } catch (NumberFormatException ignored) { }
		}
		return Double.NaN;
	}

	private double time(CSVRecord record, String column) {
		String value = text(record, column);
		if (value == null) return Double.NaN;
		try { return Time.parseTime(value); } catch (IllegalArgumentException e) { return Double.NaN; }
	}
}
