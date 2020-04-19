package org.matsim.contrib.emissions.utils;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.contrib.analysis.time.TimeBinMap;
import org.matsim.contrib.emissions.Pollutant;
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
import java.util.*;
import java.util.stream.Collectors;

public class EmissionEventsToNetCdf {

	private static final Logger logger = Logger.getLogger(EmissionEventsToNetCdf.class);
	private static final Map<String, Pollutant> pollutantMapping = Map.of(
			"NO", Pollutant.NOx,
			"NO2", Pollutant.NO2,
			"PM25", Pollutant.PM2_5,
			"PM10", Pollutant.PM_non_exhaust,
			"CO", Pollutant.CO,
			"CO2", Pollutant.CO2_TOTAL,
			"CH4", Pollutant.CH4,
			"SO2", Pollutant.SO2,
			"NH3", Pollutant.NH3
	);


	public static void main(String[] args) {

		Path path = Paths.get("C:\\Users\\Janekdererste\\repos\\shared-svn\\projects\\mosaik-2\\data\\emission-driver-input\\erp_itm_chemistry.nc");
		var timeGrid = readEmissionsFromNetCdf(path);
		writeToNetCdf(Paths.get("C:\\Users\\Janekdererste\\Desktop\\test-netcdf.nc"), timeGrid);
	}

	public static TimeBinMap<EmissionRaster> readEmissionsFromNetCdf(Path netCdfFile) {

		try (NetcdfFile file = NetcdfFile.open(netCdfFile.toString())) {

			List<Integer> times = toIntList(file.findVariable("time"));
			List<Double> x = toDoubleArray(file.findVariable("x"));
			List<Double> y = toDoubleArray(file.findVariable("y"));
			List<String> emissionNames = toStringArray(file.findVariable("emission_name"));
			List<String> timestamps = toStringArray(file.findVariable("timestamp"));

			Variable emissionValues = file.findVariable("emission_values");

			Dimension zDimension = new Dimension("z", 1);
			emissionValues = emissionValues.reduce(Collections.singletonList(zDimension)); // remove z dimension, since it is not used

			// use one second as time bin size
			TimeBinMap<EmissionRaster> timeBins = new TimeBinMap<>(1, 1);

			for (int ti = 0; ti < times.size(); ti++) {

				logger.info("writing things for timestep: " + timestamps.get(ti));
				var raster = new EmissionRaster();
				var currentTimeStep = times.get(ti);
				timeBins.getTimeBin(currentTimeStep).setValue(raster);

				for (int xi = 0; xi < x.size(); xi++) {
					for (int yi = 0; yi < y.size(); yi++) {

						Map<Pollutant, Double> pollutionMap = new HashMap<>();
						Array pollution = emissionValues.read(new int[]{ti, xi, yi, 0}, new int[]{1, 1, 1, emissionNames.size()});
						float[] values = (float[]) pollution.copyTo1DJavaArray();

						// write the different pollutants into a map
						for (int ei = 0; ei < values.length; ei++) {
							if (values[ei] > 0 && pollutantMapping.containsKey(emissionNames.get(ei))) {
								double doubleValue = values[ei];
								pollutionMap.put(pollutantMapping.get(emissionNames.get(ei)), doubleValue);
							}
						}
						var coord = new Coord(x.get(xi), y.get(yi));
						raster.addCell(coord, pollutionMap);
					}
				}
			}
			return timeBins;
		} catch (IOException | InvalidRangeException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	public static void writeToNetCdf(Path outputFile, TimeBinMap<EmissionRaster> data) {

		try (var writer = new EmissionNetcdfWriter(outputFile)) {

			writer.write(data);
		} catch (IOException | InvalidRangeException e) {
			e.printStackTrace();
		}
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
