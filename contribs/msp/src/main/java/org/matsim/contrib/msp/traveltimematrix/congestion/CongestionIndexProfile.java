package org.matsim.contrib.msp.traveltimematrix.congestion;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.contrib.msp.traveltimematrix.RoadClassification;
import org.matsim.core.utils.io.IOUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.net.URL;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A {@link CongestionIndex} as a step function over the day: one value per time bin and road class.
 * <p>
 * This is the whole time dimension of the matrix, and it is deliberately tiny - 96 numbers for a day at
 * 15 minute bins, times the number of road classes. It is small enough to be written as a csv, read by
 * hand, plotted, and put in a paper, which is the point: the curve <em>is</em> the model, so it has to
 * be inspectable.
 * <pre>
 * road_class,bin_start,congestion_index
 * all,0,0.0
 * all,900,0.004
 * motorway,0,0.0
 * motorway,900,0.002
 * </pre>
 * {@code bin_start} is the start of the bin in seconds after midnight, so the bin size can be read off
 * the file and does not have to be passed alongside it.
 *
 * @see CongestionIndexCalculator to measure one on a finished run
 */
public final class CongestionIndexProfile implements CongestionIndex {

	private static final Logger log = LogManager.getLogger(CongestionIndexProfile.class);

	private static final String ROAD_CLASS = "road_class";
	private static final String BIN_START = "bin_start";
	private static final String CONGESTION_INDEX = "congestion_index";

	private final int timeBinSize;
	private final Map<String, double[]> indexByRoadClass;
	private final int numberOfBins;

	/**
	 * Road classes that were asked for and are not in the profile, so that the warning about them is
	 * logged once per class rather than once per query. Concurrent because a matrix is queried from
	 * every thread that uses it.
	 */
	private final Set<String> unknownClassesWarnedAbout = ConcurrentHashMap.newKeySet();

	/**
	 * @param timeBinSize the width of a bin [s]
	 * @param indexByRoadClass one array per road class, all of the same length. Has to hold
	 * {@link RoadClassification#ALL}, which is the fallback for every class it does not hold.
	 */
	public CongestionIndexProfile(int timeBinSize, Map<String, double[]> indexByRoadClass) {

		if (timeBinSize <= 0) {
			throw new IllegalArgumentException("A time bin has to be longer than 0 s, not " + timeBinSize + " s.");
		}
		if (!indexByRoadClass.containsKey(RoadClassification.ALL)) {
			throw new IllegalArgumentException("A congestion index profile has to hold the network-wide class '"
				+ RoadClassification.ALL + "', because it is the fallback for a road class that carried no traffic. "
				+ "It holds " + indexByRoadClass.keySet() + ".");
		}

		this.timeBinSize = timeBinSize;
		this.numberOfBins = indexByRoadClass.get(RoadClassification.ALL).length;

		Map<String, double[]> copy = new LinkedHashMap<>();
		// ALL first, so that it heads the csv and the log
		copy.put(RoadClassification.ALL, indexByRoadClass.get(RoadClassification.ALL).clone());
		for (Map.Entry<String, double[]> entry : new TreeMap<>(indexByRoadClass).entrySet()) {
			if (entry.getValue().length != this.numberOfBins) {
				throw new IllegalArgumentException("Road class '" + entry.getKey() + "' has " + entry.getValue().length
					+ " time bins, but '" + RoadClassification.ALL + "' has " + this.numberOfBins
					+ ". Every class has to cover the same day.");
			}
			copy.putIfAbsent(entry.getKey(), entry.getValue().clone());
		}
		this.indexByRoadClass = copy;
	}

	@Override
	public double get(double time, String roadClass) {
		double[] index = this.indexByRoadClass.get(roadClass);
		if (index == null) {
			if (this.unknownClassesWarnedAbout.add(roadClass)) {
				log.warn("The congestion index profile does not hold road class '{}', so the network-wide index is "
					+ "used for it. It holds {}.", roadClass, this.indexByRoadClass.keySet());
			}
			index = this.indexByRoadClass.get(RoadClassification.ALL);
		}
		return index[binOf(time)];
	}

	@Override
	public Collection<String> getRoadClasses() {
		return this.indexByRoadClass.keySet();
	}

	/**
	 * The bin a time of day falls into, <b>clamped</b> to the bins the profile has.
	 * <p>
	 * Clamping rather than wrapping or refusing is the right thing for both ends. A negative time cannot
	 * happen in a MATSim run but does happen in an evaluation that asks for "an hour before the first
	 * departure", and a time past the end of the profile happens all the time: MATSim counts seconds on
	 * past midnight, so a late-evening departure plus a long travel time lands at 25:30, and a run whose
	 * events stop at 24:00 has no bin for it. The last bin of the day is the honest answer there - the
	 * traffic of the small hours resembles the traffic of the late night far more than that of the
	 * morning peak, which is what wrapping around to bin 0 would claim.
	 */
	int binOf(double time) {
		int bin = (int) Math.floor(time / this.timeBinSize);
		return Math.max(0, Math.min(this.numberOfBins - 1, bin));
	}

	/**
	 * @return the width of a time bin [s]
	 */
	public int getTimeBinSize() {
		return this.timeBinSize;
	}

	/**
	 * @return how many bins the profile covers, i.e. {@code getNumberOfBins() * getTimeBinSize()} is the
	 * end of the day it was measured over
	 */
	public int getNumberOfBins() {
		return this.numberOfBins;
	}

	/**
	 * @return the values of one road class, in bin order. A copy - the profile is immutable.
	 */
	public double[] getValues(String roadClass) {
		double[] values = this.indexByRoadClass.get(roadClass);
		if (values == null) {
			throw new IllegalArgumentException("The profile does not hold road class '" + roadClass + "'. It holds "
				+ this.indexByRoadClass.keySet() + ".");
		}
		return values.clone();
	}

	/**
	 * The daily average of one road class, as the unweighted mean over the bins. Useful as the constant
	 * a {@link CongestionIndex#constant(double)} baseline is set to, and as the one number to report per
	 * scenario.
	 */
	public double getDailyMean(String roadClass) {
		double[] values = this.indexByRoadClass.get(roadClass);
		if (values == null) {
			throw new IllegalArgumentException("The profile does not hold road class '" + roadClass + "'. It holds "
				+ this.indexByRoadClass.keySet() + ".");
		}
		double sum = 0;
		for (double value : values) {
			sum += value;
		}
		return sum / values.length;
	}

	/**
	 * Writes the profile as the csv shown in the class comment. One row per road class and bin, the
	 * network-wide class first, so that the file is also readable top to bottom.
	 */
	public void write(String path) {
		try (Writer writer = IOUtils.getBufferedWriter(path);
			 CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder()
				 .setHeader(ROAD_CLASS, BIN_START, CONGESTION_INDEX)
				 .build())) {

			for (Map.Entry<String, double[]> entry : this.indexByRoadClass.entrySet()) {
				double[] values = entry.getValue();
				for (int bin = 0; bin < values.length; bin++) {
					printer.printRecord(entry.getKey(), bin * this.timeBinSize,
						String.format(Locale.ROOT, "%.6f", values[bin]));
				}
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		log.info("Wrote a congestion index profile of {} road classes over {} bins of {} s to {}.",
			this.indexByRoadClass.size(), this.numberOfBins, this.timeBinSize, path);
	}

	/**
	 * Reads back what {@link #write(String)} wrote. The bin size is taken from the file - from the
	 * smallest difference between two consecutive {@code bin_start} values of a class - so a profile is
	 * a complete input on its own and cannot be read at the wrong resolution.
	 *
	 * @throws IllegalArgumentException if the bins of a class are not a complete, equally spaced series
	 * starting at 0
	 */
	public static CongestionIndexProfile read(URL url) {

		Map<String, TreeMap<Integer, Double>> byClass = new LinkedHashMap<>();

		try (CSVParser parser = CSVFormat.DEFAULT.builder()
			.setHeader()
			.setSkipHeaderRecord(true)
			.build()
			.parse(IOUtils.getBufferedReader(url))) {

			for (CSVRecord record : parser) {
				String roadClass = record.get(ROAD_CLASS);
				int binStart = Integer.parseInt(record.get(BIN_START).trim());
				double value = Double.parseDouble(record.get(CONGESTION_INDEX).trim());
				Double previous = byClass.computeIfAbsent(roadClass, c -> new TreeMap<>()).put(binStart, value);
				if (previous != null) {
					throw new IllegalArgumentException("Road class '" + roadClass + "' has two rows for the bin "
						+ "starting at " + binStart + " s.");
				}
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		if (byClass.isEmpty()) {
			throw new IllegalArgumentException("The congestion index file " + url + " holds no rows.");
		}

		int timeBinSize = binSizeOf(byClass);
		Map<String, double[]> indexByRoadClass = new LinkedHashMap<>();
		for (Map.Entry<String, TreeMap<Integer, Double>> entry : byClass.entrySet()) {
			indexByRoadClass.put(entry.getKey(), valuesOf(entry.getKey(), entry.getValue(), timeBinSize));
		}

		CongestionIndexProfile profile = new CongestionIndexProfile(timeBinSize, indexByRoadClass);
		log.info("Read a congestion index profile of {} road classes over {} bins of {} s from {}. Daily mean of "
				+ "'{}': {}.", indexByRoadClass.size(), profile.numberOfBins, timeBinSize, url,
			RoadClassification.ALL, String.format(Locale.ROOT, "%.4f", profile.getDailyMean(RoadClassification.ALL)));
		return profile;
	}

	/**
	 * @see #read(URL)
	 */
	public static CongestionIndexProfile read(String path) {
		return read(IOUtils.resolveFileOrResource(path));
	}

	/**
	 * The bin size the file was written at: the smallest gap between two consecutive bin starts of any
	 * class. Taking the smallest rather than the first makes the file readable even when a class has a
	 * gap in it, which the check in {@link #valuesOf} then reports by name.
	 */
	private static int binSizeOf(Map<String, TreeMap<Integer, Double>> byClass) {
		int binSize = Integer.MAX_VALUE;
		for (TreeMap<Integer, Double> bins : byClass.values()) {
			Integer previous = null;
			for (Integer binStart : bins.keySet()) {
				if (previous != null) {
					binSize = Math.min(binSize, binStart - previous);
				}
				previous = binStart;
			}
		}
		if (binSize == Integer.MAX_VALUE) {
			throw new IllegalArgumentException("Every road class of the congestion index file has a single bin, so "
				+ "the bin size cannot be read off the file. A profile needs at least two bins.");
		}
		return binSize;
	}

	private static double[] valuesOf(String roadClass, TreeMap<Integer, Double> bins, int timeBinSize) {
		int numberOfBins = bins.lastKey() / timeBinSize + 1;
		double[] values = new double[numberOfBins];
		for (int bin = 0; bin < numberOfBins; bin++) {
			Double value = bins.get(bin * timeBinSize);
			if (value == null) {
				throw new IllegalArgumentException("Road class '" + roadClass + "' has no value for the bin starting "
					+ "at " + bin * timeBinSize + " s, although it has values up to " + bins.lastKey() + " s. A "
					+ "profile has to cover the day without gaps.");
			}
			values[bin] = value;
		}
		return values;
	}
}
