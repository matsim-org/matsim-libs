/* *********************************************************************** *
 * project: org.matsim.*
 * PrepareLinkLeaveEventMatrix.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

/**
 *
 */
package playground.yu.integration.cadyts.demandCalibration.withCarCounts.experiments.smearLinkUtilOffsets;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import no.uib.cipr.matrix.Vector;
import no.uib.cipr.matrix.sparse.BiCGstab;
import no.uib.cipr.matrix.sparse.CompRowMatrix;
import no.uib.cipr.matrix.sparse.IterativeSolver;
import no.uib.cipr.matrix.sparse.IterativeSolverNotConvergedException;
import no.uib.cipr.matrix.sparse.OutputIterationReporter;
import no.uib.cipr.matrix.sparse.SparseVector;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.utils.collections.Tuple;

import playground.yu.utils.io.ScoreModificationReader;
import playground.yu.utils.io.SimpleWriter;

/**
 * @author yu
 *
 */
public class PopLinksTimeBinsMatrixCreator implements LinkLeaveEventHandler {
	private final static Logger log = Logger.getLogger(PopLinksTimeBinsMatrixCreator.class);

	/** matrix[mxn] m-number of pop, n-number of links x timeBins */
	private CompRowMatrix A, AT;
	private Map<Tuple<Integer/* rowIdx */, Integer/* colIdx */>, Integer/* value */> coordNonZeros = new HashMap<Tuple<Integer, Integer>, Integer>();
	private static double TIME_BIN = 3600d;// an hour
	private Network network;
	private Population population;
	private static int CALIBRATION_START_TIMEBIN = 7,
			CALIBRATION_END_TIMEBIN = 20;
	/** Map<linkId_timeBin (String), column index in Matrix> */
	private Map<String, Integer> linkTimeBinSequence = new HashMap<String, Integer>();
	/** Map<column index in Matrix (linkIndex),linkId_timeBin (String)> */
	private Map<Integer, String> linkTimeBinSequence2 = new HashMap<Integer, String>();
	/**
	 * Map<perosnId, personIndex in Collection, which will be used as the row
	 * index in Matrix>
	 */
	private Map<Id, Integer> personSequence = new HashMap<Id, Integer>();
	// private Map<Integer/* rowIdx */, Set<Integer>/* colIdxes */> coordsA;
	private int[/* rows-pop */][/* nonzero column indices on each row */] nonZero4A;

	// private int perNum = 0;

	public PopLinksTimeBinsMatrixCreator(Network network, Population population) {
		this.network = network;
		this.population = population;
	}

	public PopLinksTimeBinsMatrixCreator(Network network,
			Population population, double timeBin) {
		this(network, population);
		TIME_BIN = timeBin;
	}

	public PopLinksTimeBinsMatrixCreator(Network network,
			Population population, int calibrationStartTimeBin,
			int calibrationEndTimeBin) {
		this(network, population);
		CALIBRATION_START_TIMEBIN = calibrationStartTimeBin;
		CALIBRATION_END_TIMEBIN = calibrationEndTimeBin;
	}

	public PopLinksTimeBinsMatrixCreator(Network network,
			Population population, double timeBin, int calibrationStartTimeBin,
			int calibrationEndTimeBin) {
		this(network, population, calibrationStartTimeBin,
				calibrationEndTimeBin);
		TIME_BIN = timeBin;
	}

	/** this must be called after the object is constructed */
	public void init() {
		int colIdx = 0;
		for (Link link : network.getLinks().values()) {
			String linkIdStr = "linkId\t" + link.getId();
			for (int tb = CALIBRATION_START_TIMEBIN; tb <= CALIBRATION_END_TIMEBIN; tb++) {
				String linkTimeBin = linkIdStr + "\ttimeBin\t" + tb;
				linkTimeBinSequence.put(linkTimeBin, colIdx++);// 1.transfers
				// value,
				// 2.++
				linkTimeBinSequence2.put(linkTimeBinSequence.get(linkTimeBin),
						linkTimeBin);
			}
		}

		int rowIdx = 0;
		for (Person person : population.getPersons().values()) {
			personSequence.put(person.getId(), rowIdx++);// 1.transfers
			// value, 2.++
		}
	}

	private static int getCalibrationTimeBin(double time) {
		return (int) time / 3600 + 1;
	}

	public Map<Id, Integer> getPersonSequence() {
		return personSequence;
	}

	public Map<Integer, String> getLinkTimeBinSequence2() {
		return linkTimeBinSequence2;
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		double time = event.getTime();
		int timeBin = getCalibrationTimeBin(time);
		if (timeBin >= CALIBRATION_START_TIMEBIN
				&& timeBin <= CALIBRATION_END_TIMEBIN) {
			Id persId = event.getPersonId();
			Integer rowIdx = personSequence.get(persId);
			// if (rowIdx == null) {
			// rowIdx = perNum++;
			// this.personSequence.put(persId, rowIdx);
			// }

			// int rowIdx = this.personSequence.get(persId);
			int colIdx = linkTimeBinSequence.get("linkId\t" + event.getLinkId()
					+ "\ttimeBin\t" + getCalibrationTimeBin(time));
			// this.matrix.add(rowIdx, colIdx, 1d);
			Tuple<Integer, Integer> coord = new Tuple<Integer, Integer>(rowIdx,
					colIdx);
			Integer value = coordNonZeros.get(coord);
			if (value == null) {
				value = 0;
			}
			coordNonZeros.put(coord, value + 1);
		}
	}

	public void writeCoordMatrix(String coordMatrixFilename) {
		SimpleWriter writer = new SimpleWriter(coordMatrixFilename);
		writer.writeln("rowIdx\tcolIdx\tvalue");
		for (Entry<Tuple<Integer, Integer>, Integer> coordValue : coordNonZeros
				.entrySet()) {
			Tuple<Integer, Integer> coord = coordValue.getKey();
			StringBuffer sb = new StringBuffer(coord.getFirst().toString());
			sb.append('\t');
			sb.append(coord.getSecond().toString());
			sb.append('\t');
			sb.append(coordValue.getValue().toString());
			writer.writeln(sb);
		}
		writer.close();
	}

	public void prepareMatrix() {
		// save nonzero elements indexes to nonzero-array
		Map<Integer/* rowIdx */, Set<Integer>/* colIdxes */> coordsA = new TreeMap<Integer, Set<Integer>>();
		for (Tuple<Integer, Integer> coord : coordNonZeros.keySet()) {
			Integer rowIdx = coord.getFirst();
			Set<Integer> cols = coordsA.get(rowIdx);
			if (cols == null) {
				cols = new TreeSet<Integer>();
				coordsA.put(rowIdx, cols);
			}
			cols.add(coord.getSecond()/* colIdx */);
		}
		nonZero4A = new int[personSequence.size()][];
		for (Entry<Integer, Set<Integer>> rowCols : coordsA.entrySet()) {
			Integer rowIdx = rowCols.getKey();
			Set<Integer> colIdxes = rowCols.getValue();
			nonZero4A[rowIdx] = new int[colIdxes.size()];
			int i = 0;
			for (Integer colIdx : colIdxes) {
				nonZero4A[rowIdx][i] = colIdx;
				i++;
			}
		}
		playground.yu.integration.cadyts.demandCalibration.withCarCounts.experiments.smearLinkUtilOffsets.ArrayUtils
				.complete(nonZero4A);// very important
		// initialize matrix
		A = new CompRowMatrix(nonZero4A.length, linkTimeBinSequence.size(),
				nonZero4A);

		// set elements into matrix
		for (Entry<Tuple<Integer, Integer>, Integer> rowColValue : coordNonZeros
				.entrySet()) {
			Tuple<Integer, Integer> coord = rowColValue.getKey();
			A.set(coord.getFirst(), coord.getSecond(), rowColValue.getValue());
		}
	}

	public int[][] prepareNonZeroATmultA() {
		log.info("prepare nonZeroArray for ATA BEGAN!");
		int n = linkTimeBinSequence.size(), m = personSequence.size();
		int[][] nonZero4ATA = new int[n][];

		Map<Integer, Set<Integer>> coordsATA = new HashMap<Integer, Set<Integer>>();
		int tmpK = 1;
		for (int k = 0; k < m; k++) {
			int kLength = nonZero4A[k].length;
			if (k == 2 * tmpK) {
				tmpK = k;
				log.info("prepareNonZeroATmultA k =\t" + tmpK);
			}
			if (kLength > 0) {
				for (int i = 0; i < kLength; i++) {
					for (int j = i; j < kLength; j++) {
						// add row - {...cols...}
						int rowIdx = nonZero4A[k][i], colIdx = nonZero4A[k][j];
						{
							Set<Integer> colsATAi = coordsATA.get(rowIdx);
							if (colsATAi == null) {
								colsATAi = new TreeSet<Integer>();
								coordsATA.put(rowIdx, colsATAi);
							}
							colsATAi.add(colIdx);
						}
						if (i != j) {
							Set<Integer> colsATAi = coordsATA.get(colIdx);
							if (colsATAi == null) {
								colsATAi = new TreeSet<Integer>();
								coordsATA.put(colIdx, colsATAi);
							}
							colsATAi.add(rowIdx);
						}
					}
				}
			}
		}
		for (Integer rowIdx : coordsATA.keySet()) {
			Set<Integer> colIdxes = coordsATA.get(rowIdx);
			nonZero4ATA[rowIdx] = new int[colIdxes.size()];
			int i = 0;
			for (Integer colIdx : colIdxes) {
				nonZero4ATA[rowIdx][i] = colIdx;
				i++;
			}
		}
		ArrayUtils.complete(nonZero4ATA);
		log.info("prepare nonZeroArray for ATA ENDED!");
		return nonZero4ATA;
	}

	public CompRowMatrix ATmultA() {
		int[][] nonZero4ATA = prepareNonZeroATmultA();
		log.info("Multiplication ATA = AT * A BEGAN!");
		int n = A.numColumns();
		CompRowMatrix ATA = new CompRowMatrix(n, n, nonZero4ATA);
		int m = A.numRows(), tmpK = 1;

		for (int k = 0; k < m; k++) {
			int kLength = nonZero4A[k].length;
			if (k == 2 * tmpK) {
				tmpK = k;
				log.info("multiplication ATA=AT*A k =\t" + tmpK);
			}
			if (kLength > 0) {
				for (int i = 0; i < kLength; i++) {
					for (int j = i; j < kLength; j++) {
						// add row - {...cols...}
						int rowIdx = nonZero4A[k][i], colIdx = nonZero4A[k][j];
						double product = A.get(k, rowIdx) * A.get(k, colIdx);
						ATA.add(rowIdx, colIdx, product);
						if (i != j) {
							ATA.add(colIdx, rowIdx, product);
						}
					}
				}
			}
		}
		log.info("Multiplication ATA = AT * A ENDED!");
		return ATA;
	}

	public int[][] prepareNonzeroArray4Transpose() {
		// save nonzero elements indexes to nonzero-array
		Map<Integer, Set<Integer>> coords = new TreeMap<Integer, Set<Integer>>();
		for (Tuple<Integer, Integer> coord : coordNonZeros.keySet()) {
			Integer rowIdx = coord.getSecond();// ////////////////CHANGED/////////////////
			Set<Integer> cols = coords.get(rowIdx);
			if (cols == null) {
				cols = new TreeSet<Integer>();
				coords.put(rowIdx, cols);
			}
			cols.add(coord.getFirst()/*
									 * colIdx//////////////////CHANGED///////////
									 * //////
									 */);
		}
		int[/* nonzero column indices on each row */][] nonZero4AT = new int[linkTimeBinSequence
				.size()][];
		for (Entry<Integer, Set<Integer>> rowCols : coords.entrySet()) {
			Integer rowIdx = rowCols.getKey();
			Set<Integer> colIdxes = rowCols.getValue();
			nonZero4AT[rowIdx] = new int[colIdxes.size()];
			int i = 0;
			for (Integer colIdx : colIdxes) {
				nonZero4AT[rowIdx][i] = colIdx;
				i++;
			}
		}

		playground.yu.integration.cadyts.demandCalibration.withCarCounts.experiments.smearLinkUtilOffsets.ArrayUtils
				.complete(nonZero4AT);// very important

		return nonZero4AT;
	}

	// ///////////////////////////////////////////////////////////////////////////////////////////
	public void prepareMatrixTranspose(int[][] nonZero4AT) {
		// initialize matrix
		AT = new CompRowMatrix(nonZero4AT.length, personSequence.size(),
				nonZero4AT);

		// set elements into matrix
		for (Entry<Tuple<Integer, Integer>, Integer> rowColValue : coordNonZeros
				.entrySet()) {
			Tuple<Integer, Integer> coord = rowColValue.getKey();
			AT.set(coord.getSecond()/* /////////////CHANGED/////////// */,
					coord.getFirst()/* /////////////CHANGED/////////// */,
					rowColValue.getValue());
		}
	}

	@Override
	public void reset(int iteration) {
		linkTimeBinSequence.clear();
		linkTimeBinSequence2.clear();
		personSequence.clear();
		A = null;
		coordNonZeros.clear();
	}

	public CompRowMatrix getMatrix() {
		return A;
	}

	public CompRowMatrix getMatrixTranspose() {
		return AT;
	}

	/** @param args */
	public static void runMTJ(String[] args) {

		// String networkFilename =
		// "../schweiz-ivtch-SVN/baseCase/network/ivtch-osm.xml", //
		// populationFilename =
		// "../integration-demandCalibration1.0.1/test/input/um1/1000.plans.xml.gz",
		// //
		// eventsFilename =
		// "../integration-demandCalibration1.0.1/test/input/um1/1000.events.txt.gz",
		// //
		// coordMatrixOutputFilename =
		// "../integration-demandCalibration1.0.1/test/input/um1/popLinksMatrix.log",
		// //
		// scoreModificationFilename =
		// "../integration-demandCalibration1.0.1/test/input/um1/700.scoreModification.log",
		// //
		// AFilename =
		// "../integration-demandCalibration1.0.1/test/input/um1/A.log", //
		// ATFilename =
		// "../integration-demandCalibration1.0.1/test/input/um1/AT.log", //
		// linkUtilityOffsetFilename =
		// "../integration-demandCalibration1.0.1/test/input/um1/linkIdTimeBinX.log";

		String networkFilename = "/work/chen/data/ivtch/input/ivtch-osm.xml", //
		populationFilename = "../matsim-bse/outputs/4SE_DC/middle_um1/ITERS/it.1000/1000.plans.xml.gz", //
		eventsFilename = "../matsim-bse/outputs/4SE_DC/middle_um1/ITERS/it.1000/1000.events.txt.gz", //
		scoreModificationFilename = "../matsim-bse/outputs/4SE_DC/middle_um1/scoreModification.log", //

		coordMatrixOutputFilename = "../matsim-bse/outputs/4SE_DC/middle_um1/ITERS/it.1000/test/1000.popLinksMatrix.log.gz", //
		AFilename = "../matsim-bse/outputs/4SE_DC/middle_um1/ITERS/it.1000/test/1000.A.log.gz", //
		ATFilename = "../matsim-bse/outputs/4SE_DC/middle_um1/ITERS/it.1000/test/1000.AT.log.gz", //
		ATAFilename = "../matsim-bse/outputs/4SE_DC/middle_um1/ITERS/it.1000/test/1000.ATA.log.gz", //
		bFilename = "../matsim-bse/outputs/4SE_DC/middle_um1/ITERS/it.1000/test/1000.b.log.gz", //
		ATbFilename = "../matsim-bse/outputs/4SE_DC/middle_um1/ITERS/it.1000/test/1000.ATb.log.gz", //
		xFilename = "../matsim-bse/outputs/4SE_DC/middle_um1/ITERS/it.1000/test/1000.x.log.gz"; //
		// linkUtilityOffsetFilename =
		// "../matsim-bse/old_runs/um1/ITERS/it.1000/linkIdTimeBinX.log";

		Scenario scenario = new ScenarioImpl();

		Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(networkFilename);

		Population population = scenario.getPopulation();
		new MatsimPopulationReader(scenario).readFile(populationFilename);

		PopLinksTimeBinsMatrixCreator pltbmc = new PopLinksTimeBinsMatrixCreator(
				network, population, 7, 20);
		pltbmc.init();
		EventsManager events = new EventsManagerImpl();
		events.addHandler(pltbmc);
		new MatsimEventsReader(events).readFile(eventsFilename);

		pltbmc.writeCoordMatrix(coordMatrixOutputFilename);
		pltbmc.prepareMatrix();
		CompRowMatrix A = pltbmc.getMatrix();
		MatrixUtils.writeMatrix(A, AFilename);

		int[][] nonZero4AT = pltbmc.prepareNonzeroArray4Transpose();
		CompRowMatrix AT = new CompRowMatrix(A.numColumns(), A.numRows(),
				nonZero4AT);
		// /////////////////////////////////
		log.info("Transpose (A -> AT) BEGAN!");
		AT = (CompRowMatrix) A.transpose(AT);
		log.info("Transpose (A -> AT) ENDED!");
		MatrixUtils.writeMatrix(AT, ATFilename);

		// //////////////////////////////////////////
		CompRowMatrix ATA = pltbmc.ATmultA();
		MatrixUtils.writeMatrix(ATA, ATAFilename);
		// ///////////////////////////////////

		ScoreModificationReader smReader = new ScoreModificationReader(
				scoreModificationFilename);
		smReader.parse();

		int m = A.numRows(), n = A.numColumns();
		System.out.println("A mxn:\t" + m + "\tx\t" + n);

		Vector b = new SparseVector(m);
		for (Entry<Id, Integer> persIdRowPair : pltbmc.getPersonSequence()
				.entrySet()) {
			int rowIdx = persIdRowPair.getValue();
			Double scoreMf = smReader.getPersonUtilityOffset(persIdRowPair
					.getKey()/* personId */);
			if (scoreMf == null) {
				scoreMf = 0d;
			}
			if (scoreMf > 0) {
				b.set(rowIdx, scoreMf);
			}
		}
		VectorUtils.writeVector(b, bFilename);

		Vector ATb = new SparseVector(n);

		log.info("multiplication ATb = AT * b BEGAN!");
		ATb = AT.mult(b, ATb);
		log.info("multiplication ATb = AT * b ENDED!");
		VectorUtils.writeVector(ATb, ATbFilename);

		// /////////////////////////////////////////////////////////////
		log.info("CG (Conjugate Gradients) Method BEGAN!");
		Vector x = new SparseVector(n);

		log.info("\"Allocate storage for Conjugate Gradients\" BEGAN!");
		// Allocate storage for Conjugate Gradients
		IterativeSolver solver = new BiCGstab(x);
		log.info("\"Allocate storage for Conjugate Gradients\" ENDED!");

		// log.info(
		// "\"Create a Cholesky preconditioner\" BEGAN!");
		// // Create a Cholesky preconditioner
		// Preconditioner M = new ICC(ATA.copy());
		// log.info(
		// "\"Create a Cholesky preconditioner\" ENDED!");
		//
		// log.info(
		// "\"Set up the preconditioner, and attach it\" BEGAN!");
		// // Set up the preconditioner, and attach it
		// M.setMatrix(ATA);
		// solver.setPreconditioner(M);
		// log.info(
		// "\"Set up the preconditioner, and attach it\" ENDED!");

		log.info("\"Add a convergence monitor\" BEGAN!");
		// Add a convergence monitor
		solver.getIterationMonitor().setIterationReporter(
				new OutputIterationReporter());
		log.info("\"Add a convergence monitor\" ENDED!");

		log.info("\"Start the solver, and check for problems\" BEGAN!");
		// Start the solver, and check for problems
		try {
			solver.solve(ATA, ATb, x);
		} catch (IterativeSolverNotConvergedException e) {
			System.err.println("Iterative solver failed to converge");
		}
		log.info("\"Start the solver, and check for problems\" ENDED!");

		log.info("CG (Conjugate Gradients) Method ENDED!");
		VectorUtils.writeVector(x, xFilename);
	}

	public static void main(String[] args) {
		log.info(
				"----------------->STARTED-------------------------");
		runMTJ(args);
		log.info(
				"----------------->ENDED-------------------------");
	}
}
