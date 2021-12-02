package org.matsim.contrib.drt.extension.alonso_mora.algorithm.relocation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.contrib.drt.extension.alonso_mora.algorithm.assignment.GlpkMpsAssignmentSolver;

/**
 * Solves the relocation problem as described by Alonso-Mora et al. using the
 * GLPK solver via file transmission. GLPK must be avaialble on the system to
 * use this solver.
 * 
 * @author sebhoerl
 */
public class GlpkMpsRelocationSolver implements RelocationSolver {
	static public final String TYPE = "GlpkMps";

	private static final Logger logger = Logger.getLogger(GlpkMpsRelocationSolver.class);

	private final File problemPath;
	private final File solutionPath;

	private final int timeLimit;

	public GlpkMpsRelocationSolver(int timeLimit, File problemPath, File solutionPath) {
		this.problemPath = problemPath;
		this.solutionPath = solutionPath;

		this.timeLimit = timeLimit;
	}

	@Override
	public Collection<Relocation> solve(Collection<Relocation> candidates) {
		try {
			List<Relocation> relocations = new ArrayList<>(candidates);
			new MpsRelocationWriter(relocations).write(problemPath);

			new ProcessBuilder("glpsol", "--tmlim", String.valueOf(timeLimit), "-w", solutionPath.toString(),
					problemPath.toString()).start().waitFor();

			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(solutionPath)));

			String line = null;
			boolean isOptimal = false;
			List<Relocation> solution = new LinkedList<>();

			while ((line = reader.readLine()) != null) {
				if (line.startsWith("c Status")) {
					isOptimal = line.contains("OPTIMAL");
				} else if (line.startsWith("j")) {
					String[] parts = line.split(" ");

					if (parts[2].equals("1")) {
						int candidateIndex = Integer.parseInt(parts[1]) - 1;
						solution.add(relocations.get(candidateIndex));
					}
				}
			}

			reader.close();

			if (!isOptimal) {
				logger.warn("GLPK MPS solution is not optimal");
			}

			return solution;
		} catch (FileNotFoundException e) {
			logger.warn("GLPK MPS solver did not finish successfully");
			return Collections.emptySet();
		} catch (IOException | InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	static public boolean checkAvailability() {
		return GlpkMpsAssignmentSolver.checkAvailability();
	}
}
