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
package playground.yu.linkUtilOffset.hourVersion;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

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

import playground.yu.utils.io.ScoreModificationReader;
import playground.yu.utils.io.SimpleWriter;
import playground.yu.utils.math.MatrixUtils;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.doublealgo.Transform;
import cern.colt.matrix.impl.SparseDoubleMatrix2D;
import cern.colt.matrix.linalg.Algebra;
import cern.colt.matrix.linalg.QRDecomposition;

/**
 * @author yu
 * 
 */
public class PopLinksTimeBinsMatrixCreator2 implements LinkLeaveEventHandler {
	/** matrix[mxn] m-number of pop, n-number of links */
	private SparseDoubleMatrix2D matrix;
	private static double TIME_BIN = 3600d;// an hour
	private Network network;
	private Population population;
	private static int CALIBRATION_START_TIMEBIN = 7,
			CALIBRATION_END_TIMEBIN = 20;
	/**
	 * Map<linkId_timeBin (String), linkIndex in Collection, which will be used
	 * as the column index in Matrix>
	 */
	private Map<String, Integer> linkTimeBinSequence = new HashMap<String, Integer>();
	/** Map<column index in Matrix (linkIndex),linkId_timeBin (String)> */
	private Map<Integer, String> linkTimeBinSequence2 = new HashMap<Integer, String>();
	/**
	 * Map<perosnId, personIndex in Collection, which will be used as the row
	 * index in Matrix>
	 */
	private Map<Id, Integer> personSequence = new HashMap<Id, Integer>();

	public PopLinksTimeBinsMatrixCreator2(Network network, Population population) {
		this.network = network;
		this.population = population;
	}

	public PopLinksTimeBinsMatrixCreator2(Network network,
			Population population, double timeBin) {
		this(network, population);
		TIME_BIN = timeBin;
	}

	public PopLinksTimeBinsMatrixCreator2(Network network,
			Population population, int calibrationStartTimeBin,
			int calibrationEndTimeBin) {
		this(network, population);
		CALIBRATION_START_TIMEBIN = calibrationStartTimeBin;
		CALIBRATION_END_TIMEBIN = calibrationEndTimeBin;
	}

	public PopLinksTimeBinsMatrixCreator2(Network network,
			Population population, double timeBin, int calibrationStartTimeBin,
			int calibrationEndTimeBin) {
		this(network, population, calibrationStartTimeBin,
				calibrationEndTimeBin);
		TIME_BIN = timeBin;
	}

	/** this must be called after the object is constructed */
	public void init() {
		int colIdx = 0;
		for (Link link : this.network.getLinks().values()) {
			String linkIdStr = "linkId\t" + link.getId();
			for (int tb = CALIBRATION_START_TIMEBIN; tb <= CALIBRATION_END_TIMEBIN; tb++) {
				String linkTimeBin = linkIdStr + "\ttimeBin\t" + tb;
				this.linkTimeBinSequence.put(linkTimeBin, colIdx++);// 1.transfers
				// value,
				// 2.++
				this.linkTimeBinSequence2.put(this.linkTimeBinSequence
						.get(linkTimeBin), linkTimeBin);
			}
		}
		int rowIdx = 0;
		for (Person person : this.population.getPersons().values())
			this.personSequence.put(person.getId(), rowIdx++);// 1.transfers
		// value, 2.++

		this.matrix = new SparseDoubleMatrix2D(this.personSequence.size()/*
																		 * row-pop
																		 */,
				this.linkTimeBinSequence.size()/* col-link_timeBin */);
	}

	private static int getCalibrationTimeBin(double time) {
		return ((int) time) / 3600 + 1;
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
			int rowIdx = this.personSequence.get(event.getPersonId()), //
			colIdx = this.linkTimeBinSequence.get("linkId\t"
					+ event.getLinkId() + "\ttimeBin\t"
					+ getCalibrationTimeBin(time));
			this.matrix.set(rowIdx, colIdx,
					this.matrix.get(rowIdx, colIdx) + 1d);
		}
	}

	@Override
	public void reset(int iteration) {
		this.linkTimeBinSequence.clear();
		this.linkTimeBinSequence2.clear();
		this.personSequence.clear();
		this.matrix = null;
	}

	public void writeMatrix(String matrixOutputFilename) {
		SimpleWriter writer = new SimpleWriter(matrixOutputFilename);
		writer.write("row_no.\tcol_no.\tvalue");
		for (int row = 0; row < this.matrix.rows(); row++)
			for (int col = 0; col < this.matrix.columns(); col++) {
				double value = this.matrix.get(row, col);
				if (value != 0d)
					writer.writeln(row + "\t" + col + "\t" + value);
			}
		writer.close();
	}

	public SparseDoubleMatrix2D getMatrix() {
		return matrix;
	}

	/** @param args */
	public static void runColt(String[] args) {
		// String networkFilename =
		// "../integration-demandCalibration1.0.1/test/input/calibration/CalibrationTest/testLogLikelihood/network.xml",
		// //
		// populationFilename =
		// "../integration-demandCalibration1.0.1/test/output/calibration/CalibrationTest/testLogLikelihood/ITERS/it.300/300.plans.xml.gz",
		// //
		// eventsFilename =
		// "../integration-demandCalibration1.0.1/test/output/calibration/CalibrationTest/testLogLikelihood/ITERS/it.300/300.events.txt.gz",
		// //
		// matrixOutputFilename =
		// "../integration-demandCalibration1.0.1/test/output/prepare/popLinksMatrix.log",
		// //
		// scoreModificationFilename =
		// "../integration-demandCalibration1.0.1/test/output/calibration/CalibrationTest/testLogLikelihood/scoreModification.log",
		// //
		//
		// linkUtilityOffsetFilename =
		// "../integration-demandCalibration1.0.1/test/output/prepare/linkIdTimeBinX.log";

		String networkFilename = "../schweiz-ivtch-SVN/baseCase/network/ivtch-osm.xml", // 
		populationFilename = "../integration-demandCalibration1.0.1/test/input/um1/1000.plans.xml.gz", //
		eventsFilename = "../integration-demandCalibration1.0.1/test/input/um1/1000.events.txt.gz", //
		matrixOutputFilename = "../integration-demandCalibration1.0.1/test/input/um1/popLinksMatrix.log", //
		scoreModificationFilename = "../integration-demandCalibration1.0.1/test/input/um1/scoreModification.log", //

		linkUtilityOffsetFilename = "../integration-demandCalibration1.0.1/test/input/um1/linkIdTimeBinX.log";

		Scenario scenario = new ScenarioImpl();

		Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(networkFilename);

		Population population = scenario.getPopulation();
		new MatsimPopulationReader(scenario).readFile(populationFilename);

		PopLinksTimeBinsMatrixCreator2 pltbmc = new PopLinksTimeBinsMatrixCreator2(
				network, population, 7, 10);
		pltbmc.init();
		EventsManager events = new EventsManagerImpl();
		events.addHandler(pltbmc);
		new MatsimEventsReader(events).readFile(eventsFilename);

		// pltbmc.writeMatrix(matrixOutputFilename);

		SparseDoubleMatrix2D A = pltbmc.getMatrix();
		Algebra algebra = new Algebra();
		System.out.println("rank[A] =\t" + algebra.rank(A));

		ScoreModificationReader smReader = new ScoreModificationReader(
				scoreModificationFilename);
		smReader.parse();
		// Map<Id, Double> b=smReader.get

		int m = A.rows(), n = A.columns();
		System.out.println("A mxn:\t" + m + "\tx\t" + n);

		SparseDoubleMatrix2D b = new SparseDoubleMatrix2D(m, 1);
		for (Entry<Id, Integer> personIdRowIdxEntry : pltbmc
				.getPersonSequence().entrySet()) {
			int rowIdx = personIdRowIdxEntry.getValue();
			double scoreModification = smReader
					.getPersonUtilityOffset(personIdRowIdxEntry.getKey()/* personId */);
			b.set(rowIdx, 0, scoreModification);
		}
		SparseDoubleMatrix2D A_b = (SparseDoubleMatrix2D) MatrixUtils
				.getAugmentMatrix(A, b);
		System.out.println("rank[A_b] =\t" + algebra.rank(A_b));

		// A_b.print(new DecimalFormat(), 10);

		DoubleMatrix2D x;
		if (algebra.rank(A) == algebra.rank(A_b)) {
			if (new QRDecomposition(A).hasFullRank()) {
				x = algebra.solve(A, b);
			} else {
				x = MatrixUtils.getMinimumNormSolution(A, b);
			}

			SimpleWriter writer = new SimpleWriter(linkUtilityOffsetFilename);
			for (int i = 0; i < x.rows(); i++) {
				writer.writeln(pltbmc.linkTimeBinSequence2.get(i)
						+ "\tutiliyOffset\t" + x.get(i, 0));
			}
			writer.close();

			SparseDoubleMatrix2D Residual = (SparseDoubleMatrix2D) Transform
					.minus(algebra.mult(A, x), b);
			System.out.println("Ax-b:\n" + Residual);
			System.out.println("b:\n" + b);
			double rnorm = algebra.normInfinity(Residual);
			System.out.println("Matrix\tResidual Infinity norm:\t" + rnorm);
		}
	}

	public static void main(String[] args) {
		Logger.getLogger("Start time").info(
				"----------------->STARTED-------------------------");
		runColt(args);
		Logger.getLogger("End time").info(
				"----------------->ENDED-------------------------");
	}
}
