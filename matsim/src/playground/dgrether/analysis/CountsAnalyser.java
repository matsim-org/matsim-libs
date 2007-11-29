/* *********************************************************************** *
 * project: org.matsim.*
 * CountsAnalyser.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.dgrether.analysis;

import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.matsim.analysis.CalcLinkStats;
import org.matsim.config.Config;
import org.matsim.counts.CountSimComparison;
import org.matsim.counts.Counts;
import org.matsim.counts.MatsimCountsReader;
import org.matsim.counts.algorithms.CountSimComparisonKMLWriter;
import org.matsim.counts.algorithms.CountSimComparisonTableWriter;
import org.matsim.counts.algorithms.CountsComparisonAlgorithm;
import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.network.NetworkLayerBuilder;
import org.matsim.utils.geometry.transformations.TransformationFactory;

/**
 * This class is able to compare traffic counts with traffic in the simulation.
 * The results are written to file in a format which has to be specified.
 *
 * @author dgrether
 *
 */
public class CountsAnalyser {

	/**
	 * name of the counts module in config
	 */
	public static final String COUNTS = "counts";

	/**
	 * name of the linkattribute parameter in config
	 */
	public static final String LINKATTS = "linkattributes";

	/**
	 * name of the output format parameter in config
	 */
	public static final String OUTPUTFORMAT = "outputformat";

	/**
	 * name of the output file parameter in config
	 */
	public static final String OUTFILE = "outputCountsFile";

	/**
	 * name of the timefilter parameter in config
	 */
	public static final String TIMEFILTER = "timeFilter";

	/**
	 * name of the distancefilter parameter in config
	 */
	public static final String DISTANCEFILTER = "distanceFilter";

	/**
	 * name of the distancefilterCenterNode parameter in config
	 */
	public static final String DISTANCEFITLERCENTERNODE = "distanceFilterCenterNode";
	/**
	 * the network
	 */
	private NetworkLayer network;

	/**
	 * the name(path) to the output file
	 */
	private String outputFile;

	/**
	 * the output format
	 */
	private String outputFormat;

	/**
	 * the time filter in h
	 *  A value in 1..24, 1 for 0 a.m. to 1 a.m., 2 for 1 a.m. to 2 a.m.
	 */
	private Integer timeFilter;

	/**
	 * the distance filter in m
	 */
	private Double distanceFilter;

	/**
	 * the id of the node used as center for the distance filter
	 */
	private String distanceFilterCenterNode;
	/**
	 * the timestep for which counts are initially drawn in the 3d viewer
	 *  A value in 1..24, 1 for 0 a.m. to 1 a.m., 2 for 1 a.m. to 2 a.m.
	 */
	private Integer visibleTimeStep;
	/**
	 * the number of the iteration which can be set in the config file
	 */
	private Integer iterationNumber;
	/**
	 * the CalcLinkStats read for the analysis
	 */
	private CalcLinkStats linkStats;

	private String coordSystem;

	/**
	 *
	 * @param config
	 */
	public CountsAnalyser(final String config) {
		try {
			this.readConfig(config);
			this.writeCountsComparisonList(this.outputFile, this.outputFormat);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(
					"Something wrong in config file, execution aborted!");
		}
	}

	/**
	 * Reads the parameters from the config file
	 *
	 * @param config
	 * @throws Exception
	 */
	private void readConfig(final String configFile) throws Exception {
		Config config = Gbl.createConfig(new String[] { configFile, "config_v1.dtd" });
		System.out.println("  reading counts xml file... ");
		MatsimCountsReader counts_parser = new MatsimCountsReader(Counts.getSingleton());
		counts_parser.readFile(config.counts().getCountsFileName());
		System.out.println("  reading counts done.");
		System.out.println("  reading network...");
		this.network = loadNetwork();
		System.out.println("  done.");
		// reading config parameters
		System.out.println("Reading parameters...");
		String linksAttributeFilename = config.getParam(COUNTS, LINKATTS);
		System.out.println("  Linkattribute File: " + linksAttributeFilename);
		this.outputFormat = config.getParam(COUNTS, OUTPUTFORMAT);
		System.out.println("  Output format: " + this.outputFormat);
		this.outputFile = config.getParam(COUNTS, OUTFILE);
		System.out.println("  Output file: " + this.outputFile);
		this.timeFilter = config.counts().getTimeFilter();
		System.out.println("  Time filter: " + this.timeFilter);
		this.visibleTimeStep = config.counts().getVisibleTimeStep();

		System.out.println("  Visible time step: " + this.visibleTimeStep);
		this.iterationNumber = config.counts().getIterationNumber();
		System.out.println("  Iteration Number set to : " + this.iterationNumber);
		this.distanceFilterCenterNode = config.counts().getDistanceFilterCenterNode();
		System.out.println("  Distance filter center node: " + this.distanceFilterCenterNode);
		this.distanceFilter = config.counts().getDistanceFilter();
		System.out.println("  Distance filter: " + this.distanceFilter);
		System.out.println("  Scale Factor: " + config.counts().getCountsScaleFactor());
		this.coordSystem = config.global().getCoordinateSystem();
		System.out.println("  Coordinate System: " + this.coordSystem);
		System.out.println("  reading LinkAttributes from: " + linksAttributeFilename);
		this.linkStats = new CalcLinkStats(this.network);
		this.linkStats.readFile(linksAttributeFilename);

		System.out.println("  done.");
	}

	/**
	 *
	 * @param calcLinkStats
	 * @return The table containing the count and sim values, link id and the
	 *         relative error.
	 */
	private List<CountSimComparison> createCountsComparisonList(
			final CalcLinkStats calcLinkStats) {
		// processing counts
		CountsComparisonAlgorithm cca = new CountsComparisonAlgorithm(this.linkStats,
				Counts.getSingleton(), this.network);
		if ((this.distanceFilter != null) && (this.distanceFilterCenterNode != null))
			cca.setDistanceFilter(this.distanceFilter, this.distanceFilterCenterNode);
		cca.setCountsScaleFactor(Gbl.getConfig().counts().getCountsScaleFactor());
		cca.run(Counts.getSingleton());
		return cca.getComparison();
	}

	/**
	 * Writes the results of the comparison to a file
	 *
	 * @param filename
	 *          the path to the kml file
	 * @param format
	 *          the format kml or txt
	 */
	private void writeCountsComparisonList(final String filename, final String format) {
		List<CountSimComparison> countsComparisonList = createCountsComparisonList(this.linkStats);
		if (format.compareToIgnoreCase("kml") == 0) {
			CountSimComparisonKMLWriter kmlWriter = new CountSimComparisonKMLWriter(
					countsComparisonList, this.network, TransformationFactory.getCoordinateTransformation(this.coordSystem, TransformationFactory.WGS84));
			kmlWriter.setTimeFilter(this.timeFilter);
			kmlWriter.setIterationNumber(this.iterationNumber);
			kmlWriter.write(filename);
		}
		else if (format.compareToIgnoreCase("txt") == 0) {
			CountSimComparisonTableWriter writer = new CountSimComparisonTableWriter(countsComparisonList, Locale.US);
			writer.setTimeFilter(this.timeFilter);
			writer.write(filename);
		}
		else {
			throw new IllegalArgumentException("Output format must be txt or kml");
		}
	}

	/**
	 * load the network
	 *
	 * @return the network layer
	 */
	protected NetworkLayer loadNetwork() {
		// - read network: which buildertype??
		printNote("", "  creating network layer... ");
		NetworkLayerBuilder
				.setNetworkLayerType(NetworkLayerBuilder.NETWORK_SIMULATION);
		NetworkLayer network = (NetworkLayer) Gbl.getWorld().createLayer(
				NetworkLayer.LAYER_TYPE, null);
		printNote("", "  done");

		printNote("", "  reading network xml file... ");
		new MatsimNetworkReader(network).readFile(Gbl.getConfig().network().getInputFile());
		printNote("", "  done");

		return network;
	}

	/**
	 * an internal routine to generated some (nicely?) formatted output. This
	 * helps that status output looks about the same every time output is written.
	 *
	 * @param header
	 *          the header to print, e.g. a module-name or similar. If empty
	 *          <code>""</code>, no header will be printed at all
	 * @param action
	 *          the status message, will be printed together with a timestamp
	 */
	private final void printNote(final String header, final String action) {
		if (header != "") {
			System.out.println();
			System.out.println("===============================================================");
			System.out.println("== " + header);
			System.out.println("===============================================================");
		}
		if (action != "") {
			System.out.println("== " + action + " at " + (new Date()));
		}
		if (header != "") {
			System.out.println();
		}
	}

	/**
	 * help output
	 *
	 */
	private static void printHelp() {
		// String ls = System.getProperty("line.separator");
		System.out.println("This tool needs one config argument. "
				+ "The config file must contain the following parameters: ");
		System.out
				.println("  - The path to the file with the link attributes (mandatory)");
		System.out
				.println("  - The path to the file to which the output is written (mandatory)");
		System.out.println("  - The output format, can be kml or txt (mandatory)");
		System.out
				.println("  - The time filter (mandatory) 0 for 0 to 1 am, 1 for 1 to 2 am...");
		System.out
				.println("  - The distance filter (optional) the distance in km to filter the counts "
						+ "around a node that must be given in the subsequent argument.");
		System.out
				.println("  - The node id for the center of the distance filter (optinal, however mandatory if distance filter is set)");
	}

	/**
	 * See printHelp() method
	 *
	 * @param args
	 */
	public static void main(final String[] args) {
		CountsAnalyser ca = null;
		if (args.length != 1) {
			printHelp();
		}
		else {
			ca = new CountsAnalyser(args[0]);
			System.out.println("File written to " + ca.outputFile);
		}
	}
}
