package playground.dziemke.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.entity.StandardEntityCollection;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.counts.CountSimComparison;
import org.matsim.counts.CountSimComparisonImpl;
import org.matsim.counts.algorithms.graphs.BiasErrorGraph;

public class CreateBiasErrorGraph {
	private final static Logger log = Logger.getLogger(CreateBiasErrorGraph.class);
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		// parameters
		String runId = "run_193";
		int iterationNumber = 300;
		int width=440;
		int height=330;
		String filename = "biasErrorGraph.png";
		
		// input file and output directory
		String inputFile = "D:/Workspace/runs-svn/cemdapMatsimCadyts/" + runId + "/ITERS/it." + iterationNumber
				+ "/" + runId + "." + iterationNumber + ".countscompare.txt";
		//String outputDirectory = "D:/VSP/cemdapMatsimCadyts/Images/" + runId + "/";
		String outputDirectory = "D:/Workspace/shared-svn/papers/2014/cemdapMatsimCadyts/Images/" + runId + "/";
		
		// other objects
		List<CountSimComparison> countSimComparisonList = new ArrayList<CountSimComparison>();
			
		
		// collect the data for the graph
		try {
			BufferedReader bufferedReader = IOUtils.getBufferedReader(inputFile);
			String currentLine = bufferedReader.readLine();
			
			while ((currentLine = bufferedReader.readLine()) != null) {
				String[] entries = currentLine.split("\t", -1);
								
				Id<Link> linkId = Id.create(entries[0], Link.class);
				int hour = Integer.parseInt(entries[1]);
				double matsimVolume = Double.parseDouble(entries[2]);
				double countVolume = Double.parseDouble(entries[3]);
								
				CountSimComparison countSimComparison = new CountSimComparisonImpl(linkId, hour, countVolume, matsimVolume);
				countSimComparisonList.add(countSimComparison);
			}
		} catch (IOException e) {
			log.error(new Exception(e));
			//Gbl.errorMsg(e);
		}
		log.info("Done collecting data for files.");
		
		// following taken from "package org.matsim.counts.algorithms.CountSimComparisonKMLWriter"
		// BiasErrorGraph ep = new BiasErrorGraph(this.countComparisonFilter.getCountsForHour(null), this.iterationNumber, null, "error graph");
		
		// create the graph
		BiasErrorGraph ep = new BiasErrorGraph(countSimComparisonList, iterationNumber, outputDirectory + null, "error graph");
		ep.createChart(0);
		
		// The following is (partially) taken from "org.matsim.counts.algorithms.graphs.helper.OutputDelegate.java"
		ChartRenderingInfo info = new ChartRenderingInfo(new StandardEntityCollection());
		new File(outputDirectory).mkdir();
		File graphicsFile = new File(outputDirectory + filename);
		ChartUtilities.saveChartAsPNG(graphicsFile, ep.getChart(), width, height, info);
		log.info("Done creating graphics file.");
	}
}
