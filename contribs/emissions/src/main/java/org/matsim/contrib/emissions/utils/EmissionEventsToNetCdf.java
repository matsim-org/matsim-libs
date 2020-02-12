package org.matsim.contrib.emissions.utils;

import org.apache.log4j.Logger;
import ucar.ma2.Array;
import ucar.ma2.ArrayChar;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class EmissionEventsToNetCdf {

	private static final Logger logger = Logger.getLogger(EmissionEventsToNetCdf.class);

	public static void main(String[] args) {

		Path path = Paths.get("C:\\Users\\Janek\\repos\\shared-svn\\projects\\mosaik-2\\data\\emission-driver-input\\erp_itm_chemistry.nc");
		try (NetcdfFile file = NetcdfFile.open(path.toString())) {

			List<Integer> times = toIntList(file.findVariable("time"));
			List<Double> x = toDoubleArray(file.findVariable("x"));
			List<Double> y = toDoubleArray(file.findVariable("y"));
			List<String> emissionNames = toStringArray(file.findVariable("emission_name"));
			List<String> timestamps = toStringArray(file.findVariable("timestamp"));

			Variable emissionValues = file.findVariable("emission_values");

			Dimension zDimention = new Dimension("z", 1);
			emissionValues = emissionValues.reduce(Collections.singletonList(zDimention)); // remove z dimension, since it is not used

			List<XYTPollutantValue> valuesGreaterZero = new ArrayList<>();

			for (Integer time : times) {
				Array timeSlice = getTimeStep(time, x.size(), y.size(), emissionNames.size(), emissionValues);
				for (int xi = 0; xi < x.size(); xi++) {
					Array xValues = getXValues(xi, y.size(), emissionNames.size(), timeSlice);
					for (int yi = 0; yi < y.size(); yi++) {
						Array yValues = getYValues(yi, emissionNames.size(), xValues);
						float[] a = (float[]) yValues.copyTo1DJavaArray();
						for (int ei = 0; ei < a.length; ei++) {
							if (a[ei] > 0) {
								XYTPollutantValue value = new XYTPollutantValue(x.get(xi), y.get(yi), timestamps.get(time - 1), emissionNames.get(ei), a[ei]);
								valuesGreaterZero.add(value);
							}
						}
					}
				}
			}

			logger.info(valuesGreaterZero.size());
		} catch (IOException | InvalidRangeException e) {
			e.printStackTrace();
		}

	}

	private static Array getXValues(int xValue, int size, int size1, Array timeSlice) throws InvalidRangeException {
		return timeSlice.section(new int[]{xValue, 0, 0}, new int[]{1, size, size1}).reduce();
	}

	private static Array getYValues(int yValue, int size1, Array timeSlice) throws InvalidRangeException {
		return timeSlice.section(new int[]{yValue, 0,}, new int[]{1, size1}).reduce();
	}

	private static Array getTimeStep(int timestep, int xSize, int ySize, int namesSize, Variable data) throws IOException, InvalidRangeException {

		return data.read(new int[]{timestep - 1, 0, 0, 0}, new int[]{1, xSize, ySize, namesSize}).reduce();
	}

	private static List<Integer> toIntList(Variable oneDimensionalVariable) throws IOException {

		if (oneDimensionalVariable.getRank() != 1 || oneDimensionalVariable.getDataType() != DataType.INT)
			throw new IllegalArgumentException("only 1 dimensional variables in this method");

		int[] values = (int[]) oneDimensionalVariable.read().copyTo1DJavaArray();
		return Arrays.stream(values).boxed().collect(Collectors.toList());
	}

	private static List<Double> toDoubleArray(Variable oneDimensionalVariable) throws IOException {

		if (oneDimensionalVariable.getRank() != 1 || oneDimensionalVariable.getDataType() != DataType.DOUBLE)
			throw new IllegalArgumentException("only 1 dimensional variables in this method");

		double[] values = (double[]) oneDimensionalVariable.read().copyTo1DJavaArray();
		return Arrays.stream(values).boxed().collect(Collectors.toList());
	}

	private static List<String> toStringArray(Variable oneDimensionalVariable) throws IOException {

		if (oneDimensionalVariable.getRank() != 2 || oneDimensionalVariable.getDataType() != DataType.CHAR)
			throw new IllegalArgumentException("only 1 dimensional variables in this method");

		ArrayChar stringArray = (ArrayChar) oneDimensionalVariable.read();
		List<String> result = new ArrayList<>();
		for (String s : stringArray) {
			result.add(s);
		}

		return result;
	}

	static class XYTPollutantValue {

		private final double x;
		private final double y;
		private final String timestamp;
		private final String name;
		private final float value;


		public XYTPollutantValue(double x, double y, String timestamp, String name, float value) {
			this.x = x;
			this.y = y;
			this.timestamp = timestamp;
			this.value = value;
			this.name = name;
		}

		public double getX() {
			return x;
		}

		public double getY() {
			return y;
		}

		public String getTimestamp() {
			return timestamp;
		}

		public float getValue() {
			return value;
		}

		public String getName() {
			return name;
		}
	}
}
