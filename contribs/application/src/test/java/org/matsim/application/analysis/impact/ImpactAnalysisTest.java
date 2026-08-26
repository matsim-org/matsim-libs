package org.matsim.application.analysis.impact;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.testcases.MatsimTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

class ImpactAnalysisTest {

	@RegisterExtension
	private final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void comparesPolicyAgainstReferenceAndReportsUnmatchedPersons() throws IOException {

		Path directory = Path.of(utils.getOutputDirectory());
		Path reference = directory.resolve("reference");
		Path policy = directory.resolve("policy");
		Files.createDirectories(reference);
		Files.createDirectories(policy);
		writeRun(reference, "p1,car,1000,00:10:00\np2,car,1000,00:10:00", "p1,10\np2,10");
		writeRun(policy, "p1,pt,2000,00:08:00\np3,car,1000,00:10:00", "p1,12\np3,10");

		Path output = policy.resolve("impact.csv");
		new ImpactAnalysis().execute(
			"--input-trips", policy.resolve("trips.csv").toString(),
			"--input-legs", policy.resolve("legs.csv").toString(),
			"--input-persons", policy.resolve("persons.csv").toString(),
			"--input-emissions-per-network-mode", policy.resolve("missing-emissions.csv").toString(),
			"--run-directory", policy.toString(),
			"--reference-run-directory", reference.toString(),
			"--sample-size", "0.5",
			"--output-impact", output.toString()
		);

		Assertions.assertThat(Files.readString(output))
			.contains("relative_change")
			.contains("Nur im Bezugsfall")
			.contains("Nur im Szenario")
			.contains("Hochrechnungsfaktor Szenario")
			.contains("Mittlere Scoredifferenz gemeinsamer Personen,all,day,utils/Person,0.000000,2.000000,2.000000");
	}

	private void writeRun(Path directory, String tripRows, String personRows) throws IOException {
		Files.writeString(directory.resolve("trips.csv"),
			"person,main_mode,traveled_distance,trav_time\n" + tripRows + "\n");
		Files.writeString(directory.resolve("legs.csv"),
			"person,mode,network_mode,distance,trav_time\n");
		Files.writeString(directory.resolve("persons.csv"),
			"person,executed_score\n" + personRows + "\n");
	}
}
