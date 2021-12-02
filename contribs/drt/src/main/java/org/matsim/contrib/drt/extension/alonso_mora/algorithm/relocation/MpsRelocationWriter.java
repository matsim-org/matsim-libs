package org.matsim.contrib.drt.extension.alonso_mora.algorithm.relocation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.stream.Collectors;

import org.matsim.contrib.drt.extension.alonso_mora.algorithm.relocation.RelocationSolver.Relocation;

/**
 * Writes the relocation problem from Alonso-Mora et al. in MPS format which can
 * be solved using external tools.
 * 
 * @author sebhoerl
 */
public class MpsRelocationWriter {
	private final List<Relocation> relocations;

	public MpsRelocationWriter(List<Relocation> relocations) {
		this.relocations = relocations;
	}

	public void write(File path) throws IOException {
		int numberOfVariables = relocations.size();

		int numberOfVehicles = relocations.stream().map(t -> t.vehicle).collect(Collectors.toSet()).size();
		int numberOfDestinations = relocations.stream().map(t -> t.destination).collect(Collectors.toSet()).size();
		int numberOfAssignments = Math.min(numberOfVehicles, numberOfDestinations);

		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path)));

		writer.write("NAME AlonsoMoraRelocation\n");
		writer.write("ROWS\n");

		// Objective
		writer.write(String.format(" N R%07d\n", 0));

		// Constraint (equality)
		writer.write(String.format(" E R%07d\n", 1));

		writer.write("COLUMNS\n");
		writer.write(" M0000001 'MARKER' 'INTORG'\n");

		for (int i = 0; i < numberOfVariables; i++) {
			// Objective
			Relocation relocation = relocations.get(i);
			writer.write(String.format(" T%d R%07d %f", i, 0, relocation.cost));

			// Constraint
			writer.write(String.format(" R%07d 1\n", 1));
		}

		writer.write(" M0000002 'MARKER' 'INTEND'\n");
		writer.write("RHS\n");
		writer.write(String.format(" RHS1 R%07d %d\n", 1, numberOfAssignments));

		writer.write("BOUNDS\n");

		for (int i = 0; i < numberOfVariables; i++) {
			writer.write(String.format(" UP BND1 T%d 1\n", i));
			writer.write(String.format(" LO BND1 T%d 0\n", i));
		}

		writer.write("ENDATA\n");

		writer.close();
	}
}
