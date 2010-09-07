/* *********************************************************************** *
 * project: org.matsim.*
 * CutFlooding.java
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
package playground.gregor.flooding;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import ucar.ma2.Array;
import ucar.ma2.ArrayDouble;
import ucar.ma2.ArrayInt;
import ucar.ma2.DataType;
import ucar.ma2.Index1D;
import ucar.ma2.Index2D;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.Variable;
import ucar.nc2.iosp.IOServiceProvider;

public class CutFlooding {

	protected static final Logger log = Logger.getLogger(CutFlooding.class);
	// private final static double xoff = 632968.529;
	// private final static double yoff = 9880201.726;
	// private final static double MAX_X = 658541.;
	// private final static double MAX_Y = 9902564.;
	// private final static double MIN_X = 648520.;
	// private final static double MIN_Y = 988294.;

	private final static double xoff = 0.;
	private final static double yoff = 0.;
	private static double MAX_X = 651438.;
	private static double MAX_Y = 9893911.;
	private final static double MIN_X = 650346.;
	private final static double MIN_Y = 9893041.;

	private static final double DISTANCE = 1.;
	private static final int TIME_RES_DOWNSCALE1 = 2;
	private static final int TIME_RES_DOWNSCALE2 = 6;
	private static final int MAX_TIME = 60;
	private NetcdfFile in;
	protected NetcdfFileWriteable out;

	protected Array aX;

	protected Array aY;

	protected Array aZ;

	protected Array aStage;

	protected Index2D idxStage;

	protected Index1D idx;

	protected Index2D idxTri;
	private Array aTri;

	private final Map<Integer, Integer> idxMapping = new HashMap<Integer, Integer>();

	public CutFlooding(String netcdf, String out) {
		try {
			this.in = NetcdfFile.open(netcdf);
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			this.out = NetcdfFileWriteable.createNew(out, false);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected void run() {

		try {
			init();
		} catch (IOException e1) {
			throw new RuntimeException(e1);
		} catch (InvalidRangeException e1) {
			throw new RuntimeException(e1);
		}

		List<Integer> indexes = getIndexesToInclude();
		List<Integer> triIndexes = getTriIndexToInclude();
		createNetcdf(indexes, triIndexes);

	}

	private void init() throws IOException, InvalidRangeException {
		log.info("initializing netcdf");

		IOServiceProvider ios = this.in.getIosp();
		Section sX = new Section();
		Variable varX = this.in.findVariable("x");
		sX.appendRange(varX.getRanges().get(0));
		this.aX = ios.readData(varX, sX);

		Section sY = new Section();
		Variable varY = this.in.findVariable("y");
		sY.appendRange(varY.getRanges().get(0));
		this.aY = ios.readData(varY, sY);

		Section sZ = new Section();
		Variable varZ = this.in.findVariable("elevation");
		sZ.appendRange(varZ.getRanges().get(0));
		this.aZ = ios.readData(varZ, sY);

		Section sStage = new Section();
		Variable varStage = this.in.findVariable("stage");
		sStage.appendRange(varStage.getRanges().get(0));
		sStage.appendRange(varStage.getRanges().get(1));
		this.aStage = ios.readData(varStage, sStage);

		Section tri = new Section();
		Variable varTri = this.in.findVariable("volumes");
		tri.appendRange(varTri.getRanges().get(0));
		tri.appendRange(varTri.getRanges().get(1));
		this.aTri = ios.readData(varTri, tri);

		this.idxTri = new Index2D(tri.getShape());
		this.idxStage = new Index2D(this.aStage.getShape());
		this.idx = new Index1D(this.aX.getShape());

		log.info("finished init.");
	}

	protected void createNetcdf(List<Integer> indexes, List<Integer> triIndexes) {
		Dimension number_of_points = this.out.addDimension("number_of_points",
				indexes.size());
		Dimension number_of_timesteps = this.out.addDimension(
				"number_of_timesteps", MAX_TIME);
		Dimension number_of_volumes = this.out.addDimension(
				"number_of_volumes", triIndexes.size());
		Dimension number_of_vertices = this.out.addDimension(
				"number_of_vertices", this.idxTri.getShape()[1]);

		Dimension[] count = { number_of_points };
		Dimension[] stages = { number_of_timesteps, number_of_points };
		Dimension[] volumes = { number_of_volumes, number_of_vertices };

		int pos = 0;

		ArrayDouble.D1 aX = new ArrayDouble.D1(indexes.size());
		ArrayDouble.D1 aY = new ArrayDouble.D1(indexes.size());
		ArrayDouble.D1 aZ = new ArrayDouble.D1(indexes.size());

		ArrayDouble.D2 aStage = new ArrayDouble.D2(MAX_TIME, indexes.size());

		ArrayInt.D2 vol = new ArrayInt.D2(triIndexes.size(), this.idxTri
				.getShape()[1]);

		for (int i : triIndexes) {
			this.idxTri.set0(i);
			for (int j = 0; j < this.idxTri.getShape()[1]; j++) {
				this.idxTri.set1(j);
				Integer idx = this.aTri.getInt(this.idxTri);
				vol.set(pos, j, this.idxMapping.get(idx));
			}
			pos++;
		}

		pos = 0;
		for (int i : indexes) {
			this.idx.set(i);
			this.idxStage.set(0, i);
			int j = 0;
			for (int time = 0; time < MAX_TIME; time++) {
				double scale = 1;
				// if (time < 33) {
				// scale = TIME_RES_DOWNSCALE1;
				// } else {
				// scale = TIME_RES_DOWNSCALE2;
				// }
				j += scale;
				this.idxStage.set0(j);
				double s = this.aStage.getFloat(this.idxStage);
				if (s != 0) {
					// System.out.println("s:" + s);
				}
				aStage.set(time, pos, s);
			}
			double x = this.aX.getDouble(this.idx) + xoff;
			double y = this.aY.getDouble(this.idx) + yoff;
			double z = this.aZ.getDouble(this.idx);
			aX.setDouble(pos, x);
			aY.setDouble(pos, y);
			// if (z != 0 ) {
			// System.out.println("z:" + z);
			// }
			aZ.setDouble(pos++, z);

		}

		this.out.addVariable("x", DataType.DOUBLE, count);
		this.out.addVariable("y", DataType.DOUBLE, count);
		this.out.addVariable("elevation", DataType.DOUBLE, count);
		this.out.addVariable("stage", DataType.DOUBLE, stages);
		this.out.addVariable("volumes", DataType.DOUBLE, volumes);

		// create the file
		try {
			this.out.create();
			this.out.write("x", new int[] { 0 }, aX);
			this.out.write("y", new int[] { 0 }, aY);
			this.out.write("elevation", new int[] { 0 }, aZ);
			this.out.write("stage", new int[] { 0, 0 }, aStage);
			this.out.write("volumes", new int[] { 0, 0 }, vol);
			this.out.close();
		} catch (IOException e) {
			System.err.println("ERROR creating file " + this.out.getLocation()
					+ "\n" + e);
		} catch (InvalidRangeException e) {
			e.printStackTrace();
		}

	}

	private List<Integer> getTriIndexToInclude() {
		log.info("found " + this.idxTri.getShape()[0] + " triangles");
		List<Integer> indexes = new ArrayList<Integer>();
		for (int i = 0; i < this.idxTri.getShape()[0]; i++) {
			this.idxTri.set0(i);
			boolean include = true;
			for (int j = 0; j < this.idxTri.getShape()[1]; j++) {
				this.idxTri.set1(j);
				Integer id = this.aTri.getInt(this.idxTri);
				if (this.idxMapping.get(id) == null) {
					include = false;
					break;
				}
			}
			if (include) {
				indexes.add(i);
			}
		}
		log.info(indexes.size() + " triangles to include.");
		return indexes;
	}

	protected List<Integer> getIndexesToInclude() {
		// QuadTree<Coordinate> coords = new QuadTree<Coordinate>(MIN_X / 2,
		// MIN_Y / 2, 2 * MAX_X, 2 * MAX_Y);

		log.info("found " + this.idx.getSize() + " coordinates and "
				+ this.idxStage.getShape()[0] + " time steps");

		int next = 0;
		List<Integer> indexes = new ArrayList<Integer>();

		for (int i = 0; i < this.idx.getSize(); i++) {
			if (i >= next) {
				log.info(i + " coordinates to processed.");
				next = i * 2;
			}
			this.idx.set(i);
			this.idxStage.set(0, i);
			double x = this.aX.getDouble(this.idx) + xoff;
			double y = this.aY.getDouble(this.idx) + yoff;
			double z = this.aZ.getDouble(this.idx);

			if (x > MAX_X || x < MIN_X || y > MAX_Y || y < MIN_Y) {
				continue;
			}
			// Collection<Coordinate> coll = coords.get(x, y, DISTANCE);
			// if (coll.size() > 0) {
			// continue;
			// }
			// Coordinate c = new Coordinate(x, y, z);
			// coords.put(x, y, c);
			this.idxMapping.put(i, indexes.size());
			indexes.add(i);
		}

		log.info(indexes.size() + " coordinates to include");

		return indexes;
	}

	public static void main(String[] args) {
		String in = "../../inputs/flooding/flooding_old.sww";
		String out = "./flooding.sww";
		new CutFlooding(in, out).run();

		// for (int i = 0; i <= 8; i++) {
		// String in =
		// "../../inputs/flooding/SZ_r018M_m003_092_12_mw9.00_03h__P"
		// + i + "_8.sww";
		// String out = "../../inputs/flooding/flooding0" + i + ".sww";
		// new CutFlooding(in, out).run();
		// }

	}

}
