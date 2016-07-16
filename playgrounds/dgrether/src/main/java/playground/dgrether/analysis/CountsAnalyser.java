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

import java.util.List;
import java.util.Locale;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.Logger;
import org.matsim.analysis.CalcLinkStats;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.counts.ComparisonErrorStatsCalculator;
import org.matsim.counts.CountSimComparison;
import org.matsim.counts.Counts;
import org.matsim.counts.MatsimCountsReader;
import org.matsim.counts.algorithms.CountSimComparisonKMLWriter;
import org.matsim.counts.algorithms.CountSimComparisonTableWriter;
import org.matsim.counts.algorithms.CountsComparisonAlgorithm;

import playground.dgrether.utils.DoubleArrayTableWriter;

/**
 * This class is able to compare traffic counts with traffic in the simulation.
 * The results are written to file in a format which has to be specified.
 *
 * http://stackoverflow.com/questions/367706/is-there-a-good-command-line-argument-parser-for-java
 * http://commons.apache.org/proper/commons-cli/usage.html
 * @author dgrether
 *
 */
public class CountsAnalyser {
	
	private static final Logger log = Logger.getLogger(CountsAnalyser.class);
	
	private String networkFilename;
	private String countsFilename;
	private String linkStatsFilename;
	private double scaleFactor;
	private String coordinateSystem;

	private Network network;

	private String outputFile;

	private CalcLinkStats linkStats;

	final private Counts counts =  new Counts();

	private List<CountSimComparison> countsComparisonList;

	/**
	 *
	 * @param config
	 */
	public CountsAnalyser() {}

	/**
	 * Reads the parameters from the config file
	 *
	 * @param configFile
	 * @throws Exception
	 */
	public void loadData() {
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(this.networkFilename);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		this.network = scenario.getNetwork();
		MatsimCountsReader counts_parser = new MatsimCountsReader(counts);
		counts_parser.readFile(this.countsFilename);
		this.linkStats = new CalcLinkStats(this.network);
		this.linkStats.readFile(this.linkStatsFilename);
		countsComparisonList = createCountsComparisonList(this.linkStats);
		log.info("  done.");
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
		CountsComparisonAlgorithm cca = new CountsComparisonAlgorithm(new CountsComparisonAlgorithm.VolumesForId() {
		
			@Override
			public double[] getVolumesForStop(Id locationId) {
				return calcLinkStats.getAvgLinkVolumes(locationId);
			}
		
		}, counts, this.network,
				this.scaleFactor);
		cca.run();
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
	public void writeCountsComparisonList(final String filename, final String format) {
		if (format.compareToIgnoreCase("kml") == 0) {
			CountSimComparisonKMLWriter kmlWriter = new CountSimComparisonKMLWriter(
					countsComparisonList, this.network, TransformationFactory.getCoordinateTransformation(this.coordinateSystem, TransformationFactory.WGS84));
			kmlWriter.writeFile(filename);
		}
		else if (format.compareToIgnoreCase("txt") == 0) {
			CountSimComparisonTableWriter writer = new CountSimComparisonTableWriter(countsComparisonList, Locale.US);
			writer.writeFile(filename);
		}
		else {
			throw new IllegalArgumentException("Output format must be txt or kml");
		}
		ComparisonErrorStatsCalculator errorStats = new ComparisonErrorStatsCalculator(countsComparisonList);

		double[] hours = new double[24];
		for (int i = 1; i < 25; i++) {
			hours[i-1] = i;
		}
		DoubleArrayTableWriter tableWriter = new DoubleArrayTableWriter();
		tableWriter.addColumn(hours);
		tableWriter.addColumn(errorStats.getMeanRelError());
		tableWriter.writeFile(filename + "errortable.txt");
	}
	public String getNetworkFilename() {
		return networkFilename;
	}

	
	public void setNetworkFilename(String networkFilename) {
		this.networkFilename = networkFilename;
	}

	
	public String getLinkStatsFilename() {
		return linkStatsFilename;
	}

	
	public void setLinkStatsFilename(String linkStatsFilename) {
		this.linkStatsFilename = linkStatsFilename;
	}

	
	public double getScaleFactor() {
		return scaleFactor;
	}

	
	public void setScaleFactor(double scaleFactor) {
		this.scaleFactor = scaleFactor;
	}

	
	public String getCoordinateSystem() {
		return coordinateSystem;
	}

	
	public void setCoordinateSystem(String coordinateSystem) {
		this.coordinateSystem = coordinateSystem;
	}

	public String getCountsFilename() {
		return countsFilename;
	}
	
	
	public void setCountsFilename(String countsFilename) {
		this.countsFilename = countsFilename;
	}

	private static void printHelp() {
		log.info("This tool needs one config argument. The config file must contain the following parameters: ");
		log.info("  - The path to the file with the link attributes (mandatory)");
		log.info("  - The path to the file to which the output is written (mandatory)");
		log.info("  - The output format, can be kml or txt (mandatory)");
		log.info("  - The time filter (mandatory) 0 for 0 to 1 am, 1 for 1 to 2 am...");
		log.info("  - The distance filter (optional) the distance in km to filter the counts around a node that must be given in the subsequent argument.");
		log.info("  - The node id for the center of the distance filter (optinal, however mandatory if distance filter is set)");
	}

	private static void printHelp(Options options){
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp( "CountsAnalyzer", options );
	}
	
	/**
	 * See printHelp() method
	 */
	public static void main(String[] args) {
		CountsAnalyser ca = new CountsAnalyser();
		Options options = new Options();

		Option networkOption = OptionBuilder.withArgName( "PATH" ).hasArg().withDescription(  "path to network file" )
        .withLongOpt("network").create( "n" );
		Option crsOption = OptionBuilder.withArgName( "CRSID" ).hasArg().withDescription(  "coordinate reference system identifier" )
				.create( "crs" );
		Option countsOption = OptionBuilder.withArgName( "PATH" ).hasArg().withDescription(  "path to counts xml file" )
        .withLongOpt("counts").create( "c" );
		Option linkstatsOption = OptionBuilder.withArgName( "PATH" ).hasArg().withDescription(  "path to linkstats file" )
        .withLongOpt("linkstats").create( "ls" );
		Option scaleOption = OptionBuilder.withArgName( "double" ).hasArg().withDescription(  "scale factor" )
        .withLongOpt("scale").create( "s" );

		options.addOption(networkOption);
		options.addOption(crsOption);
		options.addOption(countsOption);
		options.addOption(linkstatsOption);
		options.addOption(crsOption);
		options.addOption(scaleOption);
		
		CommandLineParser parser = new BasicParser();
    try {
        CommandLine line = parser.parse( options, args );
        if (line.hasOption("n")){
        	ca.setNetworkFilename(line.getOptionValue("n"));
        }
        else {
        	System.err.println("No network file specified");
        	printHelp(options);
        }
        if (line.hasOption("c")) {
        	ca.setCountsFilename(line.getOptionValue("c"));
        }
        else {
        	System.err.println("No counts file specified");
        }
        if (line.hasOption("crs")){
        	ca.setCoordinateSystem(line.getOptionValue("crs"));
        }
        else {
        	System.err.println("No crs file specified");
        }
        if (line.hasOption("ls")){
        	ca.setLinkStatsFilename(line.getOptionValue("ls"));
        }
        else {
        	System.err.println("No counts file specified");
        }
        if (line.hasOption("sf")){
        	double scale = Double.parseDouble(line.getOptionValue("sf"));
        	ca.setScaleFactor(scale);
        }
    }
    catch( ParseException exp ) {
        log.error( "Parsing failed.  Reason: " + exp.getMessage());
    }
	}
}
