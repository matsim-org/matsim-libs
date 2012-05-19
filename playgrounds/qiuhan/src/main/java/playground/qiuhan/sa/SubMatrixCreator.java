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

import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.matrices.Entry;
import org.matsim.matrices.Matrix;
import org.matsim.visum.VisumMatrixReader;
import org.matsim.visum.VisumMatrixWriter;

public class SubMatrixCreator {
	private Matrix m;
	private Map<String, Matrix> ms;
	private Map<String, DailyTrafficLoadCurve> ds;
	private Map<String, Integer> zoneIdTypes;

	public SubMatrixCreator(final Matrix m,
			final Map<String, DailyTrafficLoadCurve> ds,
			Map<String, Integer> zoneIdTypes) {
		this.m = m;
		this.ds = ds;
		this.zoneIdTypes = zoneIdTypes;
		this.ms = new TreeMap<String, Matrix>();
	}

	public void createMatrixes() {
		for (int i = 1; i <= 24; i++) {

			String time = Integer.toString(i);

			Matrix smallMatrix = this.createMatrix(time);

			ms.put(time, smallMatrix);
		}
	}

	public Matrix createMatrix(String time) {
		Matrix matrix = new Matrix(time, "from " + (Integer.parseInt(time) - 1)
				+ " to " + time);
		for (Id from : this.m.getFromLocations().keySet()) {
			for (Entry entry : this.m.getFromLocEntries(from)) {
				matrix.createEntry(from/* O */, entry.getToLocation()/* D */,
						entry.getValue() * getShare(from.toString(), time)
								/ 100d);
			}
		}
		return matrix;
	}

	private double getShare(String fromLocId, String time) {
		int typeNb = this.zoneIdTypes.get(fromLocId);
		if (typeNb < 5) {
			return this.ds.get("inside")
					.getTrafficShare(Integer.parseInt(time));
		} else {
			DailyTrafficLoadCurve outside = this.ds.get("outside");
			return outside.getTrafficShare(Integer.parseInt(time));
		}
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
		, zoneFilename = "output/matsimNetwork/testZone.log"//
		, outputPath = "output/matrices/"//
		;

		// read matrix
		Matrix m = new Matrix("5oev_o",
				"from QZ-Matrix 5 oev_00.mtx of Sonja's DA");
		new VisumMatrixReaderWithIntrazonalTraffic(m).readFile(matrixFilename);
//		System.out.println("--------------------------");
//		System.out.println("entry from 10011 to 10010:\t"
//				+ m.getEntry(new IdImpl(10011), new IdImpl(10010)));
//		System.out.println("entry from 10011 to 10011:\t"
//				+ m.getEntry(new IdImpl(10011), new IdImpl(10011)));
//		System.out.println("entry from 10011 to 10012:\t"
//				+ m.getEntry(new IdImpl(10011), new IdImpl(10012)));
//		System.out.println("entry from 10013 to 10013:\t"
//				+ m.getEntry(new IdImpl(10013), new IdImpl(10013)));
//		System.out.println("entry from 10011 to 1001x:\t"
//				+ m.getEntry(new IdImpl(10011), new IdImpl("1001x")));
//		
//		System.out.println("--------------------------");
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
		creator.createMatrixes();
		creator.writeMatrices(outputPath);
	}
}
