/* *********************************************************************** *
 * project: org.matsim.*
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

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.analysis.CalcLinkStats;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.counts.ComparisonErrorStatsCalculator;
import org.matsim.counts.CountSimComparison;
import org.matsim.counts.CountSimComparisonImpl;

import playground.dgrether.DgPaths;
import playground.dgrether.utils.DoubleArrayTableWriter;


/**
 * @author dgrether
 *
 */
public class SimSimTrafficAnalyser {


	private static final Logger log = Logger.getLogger(SimSimTrafficAnalyser.class);

	private NetworkLayer network;
	private CalcLinkStats linkStats;
	private CalcLinkStats linkStats2;

	private List<CountSimComparison> countSimComp;

	private String coordSystem;

	public SimSimTrafficAnalyser() {
	}


	private void loadData(String networkFile, String linkAttributes1, String linkAttributes2) {

		ScenarioImpl scenario = new ScenarioImpl();
		scenario.getConfig().addCoreModules();
		scenario.getConfig().network().setInputFile(networkFile);
		ScenarioLoaderImpl loader = new ScenarioLoaderImpl(scenario);
		loader.loadNetwork();

		network = scenario.getNetwork();

		this.linkStats = new CalcLinkStats(this.network);
		this.linkStats.readFile(linkAttributes1);

		this.linkStats2 = new CalcLinkStats(this.network);
		this.linkStats2.readFile(linkAttributes2);

		log.info("read data successfully...");
	}


	public void runAnalysis(String networkFile, String linkAttributes1, String linkAttributes2, String srs, String outfile) {
		loadData(networkFile, linkAttributes1, linkAttributes2);

//		NetworkFilterManager netFilter = new NetworkFilterManager(this.network);
//		Node center = this.network.getNode(new IdImpl("2531"));
//		NetworkLinkDistanceFilter distFilter = new NetworkLinkDistanceFilter(30000.0, center);
//		netFilter.addLinkFilter(distFilter);
//		this.network = (NetworkLayer) netFilter.applyFilters();

		this.coordSystem = srs;

		this.countSimComp = new ArrayList<CountSimComparison>(this.network.getLinks().size());

		for (Link l : this.network.getLinks().values()) {
			double[] volumes = this.linkStats.getAvgLinkVolumes(l.getId());
			double[] volumes2 = this.linkStats2.getAvgLinkVolumes(l.getId());

			if ((volumes.length == 0) || (volumes2.length == 0)) {
				log.warn("No volumes for link: " + l.getId().toString());
				continue;
			}
			for (int hour = 1; hour <= 24; hour++) {
				double sim1Value=volumes[hour-1];
				double sim2Value=volumes2[hour-1];
				this.countSimComp.add(new CountSimComparisonImpl(l.getId(), hour, sim1Value, sim2Value));
			}
		}

//		CountSimComparisonKMLWriter kmlWriter = new CountSimComparisonKMLWriter(
//				countSimComp, this.network, TransformationFactory.getCoordinateTransformation(this.coordSystem, TransformationFactory.WGS84));
//		kmlWriter.writeFile(outfile);

		ComparisonErrorStatsCalculator errorStats = new ComparisonErrorStatsCalculator(countSimComp);

		double[] hours = new double[24];
		for (int i = 1; i < 25; i++) {
			hours[i-1] = i;
		}
		DoubleArrayTableWriter tableWriter = new DoubleArrayTableWriter();
		tableWriter.addColumn(hours);
		tableWriter.addColumn(errorStats.getMeanRelError());
		tableWriter.writeFile(outfile + "errortable.txt");


	}


	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String net = DgPaths.IVTCHNET;
//		String linkstats1 = DgPaths.VSPCVSBASE + "runs/run610/it.100/100.linkstats.txt.gz";
//		String linkstats2 = DgPaths.VSPCVSBASE + "runs/run612/it.100/100.linkstats.txt.gz";
//		String linkstats1 = DgPaths.VSPCVSBASE + "runs/run610/it.200/200.linkstats.txt.gz";
//		String linkstats2 = DgPaths.VSPCVSBASE + "runs/run612/it.200/200.linkstats.txt.gz";
//		String linkstats1 = DgPaths.VSPCVSBASE + "runs/run610/it.500/500.linkstats.txt.gz";
//		String linkstats2 = DgPaths.VSPCVSBASE + "runs/run612/it.500/500.linkstats.txt.gz";
//		String linkstats1 = DgPaths.VSPCVSBASE + "runs/run610/it.550/550.linkstats.txt.gz";
//		String linkstats2 = DgPaths.VSPCVSBASE + "runs/run612/it.550/550.linkstats.txt.gz";
//		String outfile = DgPaths.VSPCVSBASE + "runs/run612/traffic612vs610.550";
//		String linkstats1 = DgPaths.VSPCVSBASE + "runs/run612/it.500/500.linkstats.txt.gz";
//		String linkstats2 = DgPaths.VSPCVSBASE + "runs/run620/it.500/500.linkstats.txt.gz";
//		String outfile = DgPaths.VSPCVSBASE + "runs/run620/traffic612vs620.500";

//		String linkstats1 = DgPaths.VSPCVSBASE + "runs/run465/it.500/500.linkstats.txt.gz";
//		String linkstats2 = DgPaths.VSPCVSBASE + "runs/run568/it.500/500.linkstats.txt.gz";
//		String outfile = DgPaths.VSPCVSBASE + "runs/run568/traffic465vs568.500";
		String linkstats1 = DgPaths.RUNBASE + "run709/it.1000/1000.linkstats.txt.gz";
		String linkstats2 = DgPaths.RUNBASE + "run710/it.1000/1000.linkstats.txt.gz";
		String outfile = DgPaths.RUNBASE + "run710/traffic709vs710.500";





		String srs = TransformationFactory.CH1903_LV03;

		Gbl.createConfig(null);

		SimSimTrafficAnalyser analyser = new SimSimTrafficAnalyser();
		analyser.runAnalysis(net, linkstats1, linkstats2, srs, outfile);


	}

}
