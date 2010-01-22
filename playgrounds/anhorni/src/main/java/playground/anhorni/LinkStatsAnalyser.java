/* *********************************************************************** *
 * project: org.matsim.*
 * LinkStatsAnalyser.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.anhorni;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import org.matsim.analysis.CalcLinkStats;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.counts.Count;
import org.matsim.counts.CountSimComparison;
import org.matsim.counts.Counts;
import org.matsim.counts.algorithms.CountSimComparisonKMLWriter;
import org.matsim.counts.algorithms.CountSimComparisonTableWriter;
import org.matsim.counts.algorithms.CountsComparisonAlgorithm;

/**
 * This class is able to produce a crapy kml file to compare the link volumes
 * on selected links given two linkStats files. The results are written to
 * a kmz file.
 */
public class LinkStatsAnalyser {

	//the network
	private NetworkLayer network;

	/**
	 * the number of the iteration which can be set in the config file
	 */
	private Integer iterationNumber;
	/**
	 * the CalcLinkStats read for the analysis
	 */
	private CalcLinkStats linkStats0;
	private CalcLinkStats linkStats1;

	private String coordSystem=null;

	private String linksAttributeFilename0;
	private String linksAttributeFilename1;
	private String selectedLinksFilename;
	/**
	 * the name(path) to the output file
	 */
	private String outputFilename;
	private String networkFilename;
	private double scaleFactor;
	private double vol_scale_factor;
	private List<Id> selectedLinks;

	/**
	 *
	 * @param config
	 */

	private void init(final String[] args){
		try {
			this.linksAttributeFilename0=args[0];
			this.linksAttributeFilename1=args[1];
			this.selectedLinksFilename=args[2];
			this.outputFilename=args[3];
			this.networkFilename=args[4];
			this.scaleFactor=Double.parseDouble(args[5]);
			this.vol_scale_factor= Double.parseDouble(args[6]);

			//this.iterationNumber=0;
			this.iterationNumber=new Integer(args[7]);
			//this.coordSystem="CH1903_LV03";
			this.coordSystem=args[8];

			this.selectedLinks=readSelectedLinks();

			System.out.println("  reading network...");
			this.network = loadNetwork();
			System.out.println("  done.");

			System.out.println("  Coordinate System: " + this.coordSystem);
			System.out.println("  reading LinkAttributes from: " + this.linksAttributeFilename0 + " with vol_scale factor = " + this.vol_scale_factor);
			this.linkStats0 = new CalcLinkStats(this.network,this.vol_scale_factor);
			this.linkStats0.readFile(this.linksAttributeFilename0);

			System.out.println("  reading LinkAttributes from: " + this.linksAttributeFilename1 + " with vol_scale factor = " + this.vol_scale_factor);
			this.linkStats1 = new CalcLinkStats(this.network,this.vol_scale_factor);
			this.linkStats1.readFile(this.linksAttributeFilename1);


			System.out.println("  Scale Factor set to: " + this.scaleFactor);
			System.out.println("  Iteration Number set to : " + this.iterationNumber);

			System.out.println("  done.");

		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Init aborted!");
		}
	}

	private List<Id> readSelectedLinks() {
		List<Id> links=new Vector<Id>();

		try {
			FileReader file_reader = new FileReader(this.selectedLinksFilename);
			BufferedReader buffered_reader = new BufferedReader(file_reader);

			// Skip header
			String curr_line = buffered_reader.readLine();

			while ((curr_line = buffered_reader.readLine()) != null) {
				links.add(new IdImpl(curr_line.trim()));
			}

			buffered_reader.close();
		} catch (IOException e) {
			Gbl.errorMsg(e);
		}


		return links;
	}

	/**
	 *
	 * @param calcLinkStats
	 * @return The table containing the count and sim values, link id and the
	 *         relative error.
	 */
	private List<CountSimComparison> createCountsComparisonList() {

		// create a Counts object, which holds the old sim values.
		// That is clearly a violation, but it is needed for WU
		Counts counts = new Counts();



		for (Id linkId: this.selectedLinks) {
			Count count = counts.createCount(new IdImpl(linkId.toString()), "-");
			double linkVolumes []=this.linkStats0.getAvgLinkVolumes(linkId);

			for (int i=0; i<24; i++) {
				count.createVolume(i+1, linkVolumes[i] );
				count.setCoord(new CoordImpl(this.network.getLinks().get(linkId).getCoord().getX(),
						this.network.getLinks().get(linkId).getCoord().getY()));
			}
		}


		// processing counts
		CountsComparisonAlgorithm cca = new CountsComparisonAlgorithm(this.linkStats1,
				counts, this.network);

		cca.setCountsScaleFactor(this.scaleFactor);
		cca.run();
		return cca.getComparison();
	}

	/**
	 * Writes the results of the comparison to a file
	 *
	 * @param filename
	 *          the path to the kml file
	 */
	private void writeCountsComparisonList(final String filename) {
		List<CountSimComparison> countsComparisonList = createCountsComparisonList();
		CountSimComparisonKMLWriter kmlWriter = new CountSimComparisonKMLWriter(
				countsComparisonList, this.network, TransformationFactory.getCoordinateTransformation(this.coordSystem, TransformationFactory.WGS84));
		kmlWriter.setIterationNumber(this.iterationNumber);
		kmlWriter.writeFile(filename);
		CountSimComparisonTableWriter txtWriter = new CountSimComparisonTableWriter(countsComparisonList,null);
		txtWriter.writeFile(filename+".txt");
	}

	/**
	 * load the network
	 *
	 * @return the network layer
	 */
	protected NetworkLayer loadNetwork() {
		printNote("", "  creating network layer... ");
		Scenario scenario = new ScenarioImpl();
		NetworkLayer network = (NetworkLayer) scenario.getNetwork();
		printNote("", "  done");

		printNote("", "  reading network xml file... ");
		new MatsimNetworkReader(scenario).readFile(this.networkFilename);
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
		System.out.println("This tool needs the following 8 arguments: ");
		System.out.println("  - The path to link attributes file 0");
		System.out.println("  - The path to link attributes file 1");
		System.out.println("  - The path to file which contains the selected links");
		System.out.println("  - The path to the output file");
		System.out.println("  - The path to the network file");
		System.out.println("  - The scale factor");
		System.out.println("  - The volume scale factor");
		System.out.println("  - The iteration number");
		System.out.println("  - The coordinate system");
	}

	/**
	 * See printHelp() method
	 *
	 * @param args
	 */
	public static void main(final String[] args) {
		LinkStatsAnalyser linkStatsAnalyzer = null;
		if (args.length != 9) {
			printHelp();
		}
		else {
			linkStatsAnalyzer = new LinkStatsAnalyser();
			linkStatsAnalyzer.init(args);
			linkStatsAnalyzer.writeCountsComparisonList(linkStatsAnalyzer.outputFilename);
			System.out.println("File written to " + linkStatsAnalyzer.outputFilename + "[.kmz|.txt|AWTV.txt]");
		}
	}
}
