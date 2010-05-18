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

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

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
import Jama.Matrix;
import Jama.QRDecomposition;

/**
 * @author yu
 * 
 */
public class PopLinksTimeBinsMatrixCreator implements LinkLeaveEventHandler {
	/** matrix[mxn] m-number of pop, n-number of links */
	private Matrix matrix;
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
	private Map<Integer, String> linkTimeBinSequence2 = new HashMap<Integer, String>();
	/**
	 * Map<perosnId, personIndex in Collection, which will be used as the row
	 * index in Matrix>
	 */
	private Map<Id, Integer> personSequence = new HashMap<Id, Integer>();

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

	/** this should be called after the object is constructed */
	public void init() {
		int colIdx = 0;
		for (Link link : this.network.getLinks().values())
			for (int tb = CALIBRATION_START_TIMEBIN; tb <= CALIBRATION_END_TIMEBIN; tb++) {
				String linkTimeBin = "linkId\t" + link.getId() + "\ttimeBin\t"
						+ tb;
				this.linkTimeBinSequence.put(linkTimeBin, colIdx++);// 1.transfers
				// value,
				// 2.++
				this.linkTimeBinSequence2.put(this.linkTimeBinSequence
						.get(linkTimeBin), linkTimeBin);
			}
		int rowIdx = 0;
		for (Person person : this.population.getPersons().values())
			this.personSequence.put(person.getId(), rowIdx++);// 1.transfers
		// value, 2.++

		this.matrix = new Matrix(this.personSequence.size()/* row-pop */,
				this.linkTimeBinSequence.size()/* col-link_timeBin */, 0d);
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

	}

	public void writeMatrix(String matrixOutputFilename) {
		// SimpleWriter writer = new SimpleWriter(matrixOutputFilename);
		// writer.write("\tcol_no.");
		// for (int n = 0; n < this.matrix.getColumnDimension(); n++)
		// writer.write("\t" + n);
		// writer.writeln("\nrow_no.");
		// for (int m = 0; m < this.matrix.getRowDimension(); m++) {
		// writer.write(m + "\t");
		// for (int n = 0; n < this.matrix.getColumnDimension(); n++) {
		// writer.write("\t" + this.matrix.get(m, n));
		// }
		// writer.writeln();
		// }
		// writer.close();
		try {
			this.matrix.print(new PrintWriter(matrixOutputFilename), 100, 2);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	public Matrix getMatrix() {
		return matrix;
	}

	/** @param args */
	public static void main(String[] args) {
		String networkFilename = "../integration-demandCalibration1.0.1/test/input/calibration/CalibrationTest/testLogLikelihood/network.xml", // 
		populationFilename = "../integration-demandCalibration1.0.1/test/output/calibration/CalibrationTest/testLogLikelihood/ITERS/it.300/300.plans.xml.gz", //
		eventsFilename = "../integration-demandCalibration1.0.1/test/output/calibration/CalibrationTest/testLogLikelihood/ITERS/it.300/300.events.txt.gz", //
		matrixOutputFilename = "../integration-demandCalibration1.0.1/test/output/prepare/popLinksMatrix.log", //
		scoreModificationFilename = "../integration-demandCalibration1.0.1/test/output/calibration/CalibrationTest/testLogLikelihood/scoreModification.log", //
		matrixA_bFilename = "../integration-demandCalibration1.0.1/test/output/prepare/popLinksMatrixA_b.log", //
		linkUtilityOffsetFilename = "../integration-demandCalibration1.0.1/test/output/prepare/linkIdTimeBinX.log", //
		matrixResidualFilename = "../integration-demandCalibration1.0.1/test/output/prepare/residual.log";

		Scenario scenario = new ScenarioImpl();

		Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(networkFilename);

		Population population = scenario.getPopulation();
		new MatsimPopulationReader(scenario).readFile(populationFilename);

		PopLinksTimeBinsMatrixCreator pltbmc = new PopLinksTimeBinsMatrixCreator(
				network, population, 7, 10);
		pltbmc.init();
		EventsManager events = new EventsManagerImpl();
		events.addHandler(pltbmc);
		new MatsimEventsReader(events).readFile(eventsFilename);

		pltbmc.writeMatrix(matrixOutputFilename);

		Matrix A = pltbmc.getMatrix();
		System.out.println("rank[A] =\t" + A.rank());

		ScoreModificationReader smReader = new ScoreModificationReader(
				scoreModificationFilename);
		smReader.parse();
		// Map<Id, Double> b=smReader.get

		int m = A.getRowDimension(), n = A.getColumnDimension();
		System.out.println("A mxn:\t" + m + "\tx\t" + n);

		Matrix b = new Matrix(m, 1);
		for (Entry<Id, Integer> personIdRowIdxEntry : pltbmc
				.getPersonSequence().entrySet()) {
			int rowIdx = personIdRowIdxEntry.getValue();
			double scoreModification = smReader
					.getPersonUtilityOffset(personIdRowIdxEntry.getKey()/* personId */);
			// A_b.set(rowIdx, n, scoreModification);
			b.set(rowIdx, 0, scoreModification);
		}
		Matrix A_b = MatrixUtils.getAugmentMatrix(A, b);
		System.out.println("rank[A_b] =\t" + A_b.rank());

		A_b.print(new DecimalFormat(), 10);

		Matrix x;
		if (A.rank() == A_b.rank()) {
			if (new QRDecomposition(A).isFullRank()) {
				x = A.solve(b);

			} else {
				x = MatrixUtils.getMinimumNormSolution(A, b);
			}

			SimpleWriter writer = new SimpleWriter(linkUtilityOffsetFilename);
			for (int i = 0; i < x.getRowDimension(); i++) {
				writer.writeln(pltbmc.linkTimeBinSequence2.get(i)
						+ "\tutiliyOffset\t" + x.get(i, 0));
			}
			writer.close();

			Matrix Residual = A.times(x).minus(b);
			System.out.println("Ax-b:");
			Residual.print(new DecimalFormat("0.###E00"), 10);
			System.out.println("b:");
			b.print(new DecimalFormat("0.###E00"), 10);
			double rnorm = Residual.normInf();
			System.out.println("Matrix\tResidual Infinity norm:\t" + rnorm);
		}
	}
}
