package org.matsim.contrib.drt.extension.alonso_mora.algorithm.assignment;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.log4j.Logger;
import org.matsim.contrib.drt.extension.alonso_mora.algorithm.AlonsoMoraTrip;
import org.matsim.contrib.drt.extension.alonso_mora.algorithm.assignment.AssignmentSolver.Solution.Status;

/**
 * Solves the assignment problem as described by Alonso-Mora et al. using the
 * GLPK solver via file transmission. GLPK must be avaialble on the system to
 * use this solver.
 * 
 * @author sebhoerl
 */
public class GlpkMpsAssignmentSolver implements AssignmentSolver {
	static public final String TYPE = "GlpkMps";
	
	private static final Logger logger = Logger.getLogger(GlpkMpsAssignmentSolver.class);

	private final double unassignmentPenalty;
	private final double rejectionPenalty;

	private final File problemPath;
	private final File solutionPath;

	private final int timeLimit;

	public GlpkMpsAssignmentSolver(double unassignmentPenalty, double rejectionPenalty, int timeLimit, File problemPath,
			File solutionPath) {
		this.unassignmentPenalty = unassignmentPenalty;
		this.rejectionPenalty = rejectionPenalty;

		this.problemPath = problemPath;
		this.solutionPath = solutionPath;

		this.timeLimit = timeLimit;
	}

	@Override
	public Solution solve(Stream<AlonsoMoraTrip> candidates) {
		try {
			List<AlonsoMoraTrip> tripList = candidates.collect(Collectors.toList());
			new MpsAssignmentWriter(tripList, unassignmentPenalty, rejectionPenalty).write(problemPath);

			new ProcessBuilder("glpsol", "--tmlim", String.valueOf(timeLimit), "-w", solutionPath.toString(),
					problemPath.toString()).start().waitFor();

			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(solutionPath)));

			String line = null;
			boolean isOptimal = false;
			List<AlonsoMoraTrip> solution = new LinkedList<>();

			while ((line = reader.readLine()) != null) {
				if (line.startsWith("c Status")) {
					isOptimal = line.contains("OPTIMAL");
				} else if (line.startsWith("j")) {
					String[] parts = line.split(" ");

					if (parts[2].equals("1")) {
						int tripIndex = Integer.parseInt(parts[1]) - 1;

						if (tripIndex < tripList.size()) {
							solution.add(tripList.get(tripIndex));
						}
					}
				}
			}

			reader.close();

			if (!isOptimal) {
				logger.warn("GLPK MPS solution is not optimal");
			}

			return new Solution(isOptimal ? Status.OPTIMAL : Status.FEASIBLE, solution);
		} catch (FileNotFoundException e) {
			logger.warn("GLPK MPS solver did not finish successfully");
			return new Solution(Status.FAILURE, Collections.emptySet());
		} catch (IOException | InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	static public boolean checkAvailability() {
		try {
			Process process = new ProcessBuilder("glpsol", "--version").start();

			BufferedInputStream inputStream = new BufferedInputStream(process.getInputStream());
			BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

			String line = null;
			boolean messageFound = false;

			while ((line = reader.readLine()) != null) {
				if (line.contains("GLPK LP/MIP Solver")) {
					messageFound = true;
				}
			}

			if (messageFound) {
				return true;
			}
		} catch (IOException e) {
		}

		logger.error( //
				"GLPK MILP solver was not found in the system."
						+ " Make sure you are able to call the 'glpsol' executable from the command line.");

		return false;
	}
}
