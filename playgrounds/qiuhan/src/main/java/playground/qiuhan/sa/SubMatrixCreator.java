/* *********************************************************************** *
 * project: org.matsim.*
 * SubMatrixCreator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.qiuhan.sa;

import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

import org.matsim.core.gbl.MatsimRandom;
import org.matsim.matrices.Entry;
import org.matsim.matrices.Matrix;
import org.matsim.visum.VisumMatrixWriter;

public class SubMatrixCreator {
	private Matrix m, rounded;
	private Map<String, Matrix> ms;
	private Map<String, DailyTrafficLoadCurve> ds;
	private Map<String, Integer> zoneIdTypes;

	public SubMatrixCreator(final Matrix m,
			final Map<String, DailyTrafficLoadCurve> ds,
			Map<String, Integer> zoneIdTypes) {
		this.m = m;
		this.rounded = new Matrix(
				"rounded",
				"a matrix whose elements are the sum of all the rounded value of traffic volume in each hour");
		this.ds = ds;
		this.zoneIdTypes = zoneIdTypes;
		this.ms = new TreeMap<String, Matrix>();
	}

	public void createMatrices() {
		for (int i = 1; i <= 24; i++) {

			String time = Integer.toString(i);

			Matrix smallMatrix = this.createMatrix(time);

			ms.put(time, smallMatrix);
		}
		modifyMatrices();
	}

	private void modifyMatrices() {
		// compare m and rounded
		Random r = MatsimRandom.getRandom();
		for (String from : this.m.getFromLocations().keySet()) {
			DailyTrafficLoadCurve d = this
					.getSuitableDailyTrafficLoadCurve(from.toString());
			for (Entry entry : this.m.getFromLocEntries(from)) {
				String to = entry.getToLocation();
				int origVal = (int) this.m.getEntry(from, to).getValue();
				int roundedVal = (int) this.rounded.getEntry(from, to)
						.getValue();
				int diff = origVal - roundedVal;

				if (diff > 0) {// to add
					for (int diffIdx = 0; diffIdx < diff; diffIdx++) {
						double shareSum = 0;
						double rnd = r.nextDouble();
						for (int time = 1; time <= 24; time++) {
							shareSum += d.getTrafficShare(time) / 100d;
							if (shareSum > rnd) {
								Matrix smallM = this.ms.get(Integer
										.toString(time));
								smallM.setEntry(from, to, ((int) smallM
										.getEntry(from, to).getValue()) + 1);
								break;
							}
						}
					}
				} else if (diff < 0) {// to reduce, auction: use the absolute
										// value of diff
					for (int diffIdx = 0; diffIdx < -diff; diffIdx++) {
						double shareSum = 0;
						double rnd = r.nextDouble();

						double shareSumDenominator = 0d;
						Set<Integer> times = new HashSet<Integer>();
						for (int time = 1; time <= 24; time++) {
							Matrix smallM = this.ms.get(Integer.toString(time));
							int value = (int) smallM.getEntry(from, to)
									.getValue();
							if (value != 0) {
								shareSumDenominator += d.getTrafficShare(time);
								times.add(time);
							}
						}

						for (int time : times) {
							shareSum += d.getTrafficShare(time)
									/ shareSumDenominator;
							if (shareSum > rnd) {
								Matrix smallM = this.ms.get(Integer
										.toString(time));
								smallM.setEntry(
										from,
										to,
										smallM.getEntry(from, to).getValue() - 1);
								break;
							}
						}
					}
				}
				if (diff != 0) {
					// TODO check the consistancy of small matrices
					// Matrix sumMatrix = new Matrix("new sum",
					// "to check if the small matrices were well calculated");
					int sum = 0;
					for (int time = 1; time <= 24; time++) {
						Matrix smallMatrix = ms.get(Integer.toString(time));
						sum += smallMatrix.getEntry(from, to).getValue();
					}
					if (origVal != sum) {
						System.err.println("There is something wrong on from\t"
								+ from + "\tto\t" + to + "\torginal value =\t"
								+ origVal + ", and the new sum =\t" + sum
								+ ", and the diff =\t" + diff
								+ ", and the rounded value =\t" + roundedVal);
					}
				}
			}
		}
	}

	public Matrix createMatrix(String time) {
		Matrix matrix = new Matrix(time, "from " + (Integer.parseInt(time) - 1)
				+ " to " + time);
		for (String from : this.m.getFromLocations().keySet()) {
			for (Entry entry : this.m.getFromLocEntries(from)) {
				String to = entry.getToLocation();
				int value = (int) (entry.getValue()
						* getShare(from.toString(), time) / 100d + 0.5);
				matrix.createEntry(from/* O */, to/* D */, value);

				Entry roundedEntry = this.rounded.getEntry(from, to);
				int oldValue = roundedEntry == null ? 0 : (int) roundedEntry
						.getValue();

				this.rounded.setEntry(from, to, oldValue + value);
			}
		}
		return matrix;
	}

	private DailyTrafficLoadCurve getSuitableDailyTrafficLoadCurve(
			String fromLocId) {
		int typeNb = this.zoneIdTypes.get(fromLocId);
		if (typeNb < 5) {
			return this.ds.get("inside");
		} else {
			return this.ds.get("outside");
		}
	}

	private double getShare(String fromLocId, String time) {
		return this.getSuitableDailyTrafficLoadCurve(fromLocId)
				.getTrafficShare(Integer.parseInt(time));

	}

	public void writeMatrices(String path) {
		for (String time : this.ms.keySet()) {
			new VisumMatrixWriter(ms.get(time)).writeFile(path + time + ".mtx");
		}
	}

	public static void main(String[] args) {
		String matrixFilename = "input/OEV_O_Matrix/QZ-Matrix 5 oev_00.mtx"//
		, dailyTrafficLoadCurveFilename = "input/tagsganglinie/tagsganglinien.txt"//
		, dailyTrafficLoadCurveFilename2 = "input/tagsganglinie/tagsganglinien_ausserhalb.txt"//
		, zoneFilename = "output/matsimNetwork/Zone2.log"//
		, outputPath = "output/matrices2/"//
		;

		// read matrix
		Matrix m = new Matrix("5oev_o",
				"from QZ-Matrix 5 oev_00.mtx of Sonja's DA");
		new VisumMatrixReaderWithIntrazonalTraffic(m).readFile(matrixFilename);
		// System.out.println("--------------------------");
		// System.out.println("entry from 10011 to 10010:\t"
		// + m.getEntry(new IdImpl(10011), new IdImpl(10010)));
		// System.out.println("entry from 10011 to 10011:\t"
		// + m.getEntry(new IdImpl(10011), new IdImpl(10011)));
		// System.out.println("entry from 10011 to 10012:\t"
		// + m.getEntry(new IdImpl(10011), new IdImpl(10012)));
		// System.out.println("entry from 10013 to 10013:\t"
		// + m.getEntry(new IdImpl(10013), new IdImpl(10013)));
		// System.out.println("entry from 10011 to 1001x:\t"
		// + m.getEntry(new IdImpl(10011), new IdImpl("1001x")));
		//
		// System.out.println("--------------------------");
		// read daily traffic load curves
		DailyTrafficLoadCurve inside = new DailyTrafficLoadCurve();
		new DailyTrafficLoadCurveReader(inside)
				.readFile(dailyTrafficLoadCurveFilename);

		DailyTrafficLoadCurve outside = new DailyTrafficLoadCurve();
		new DailyTrafficLoadCurveReader(outside)
				.readFile(dailyTrafficLoadCurveFilename2);

		Map<String, DailyTrafficLoadCurve> ds = new TreeMap<String, DailyTrafficLoadCurve>();
		ds.put("inside", inside);
		ds.put("outside", outside);

		// read zonefile
		ZoneReader zones = new ZoneReader();
		zones.readFile(zoneFilename);
		Map<String, Integer> zoneIdTypes = zones.getZoneIdTypes();

		SubMatrixCreator creator = new SubMatrixCreator(m, ds, zoneIdTypes);
		creator.createMatrices();
		creator.writeMatrices(outputPath);
	}
}
