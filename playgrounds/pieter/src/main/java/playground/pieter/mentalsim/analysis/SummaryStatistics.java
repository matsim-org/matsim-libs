package playground.pieter.mentalsim.analysis;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.JFileChooser;
import javax.swing.JPanel;

import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.jfree.chart.plot.XYPlot;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.utils.charts.XYLineChart;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;

public class SummaryStatistics {

	/**
	 * @param simulationPath
	 *            the root of the simulation
	 * @param startIter
	 * @param endIter
	 * @param dailyVolumes
	 *            Whether the RMSD should track daily or hourly volumes on links
	 */
	public void linkStatsRMSDeltaPlot(String simulationPath, int startIter,
			int endIter, boolean dailyVolumes) {
		ArrayList<String> files = new ArrayList<String>();
		// create a list of file names to use in the comparison
		ArrayList<Integer> relevantIters = new ArrayList<Integer>();
		for (int iter = startIter; iter < endIter; iter++) {
			String fileName = getLinkStatsFileName(simulationPath, iter);
			if (fileName == null)
				continue;
			else {
				files.add(fileName);
				relevantIters.add(iter);
			}
		}
		// now iterate through the list and get their rmses for plotting
		double[] iters = new double[relevantIters.size() - 1];
		for (int iter = 0; iter < relevantIters.size() - 1; iter++) {
			// iters[iter] = relevantIters.get(iter+1);
			iters[iter] = iter + 1;
		}
		double[] valuesByIter = new double[relevantIters.size() - 1];
		double[] valuesVsStart = new double[relevantIters.size() - 1];
		for (int i = 0; i < files.size() - 1; i++) {
			String firstFileName = files.get(i);
			String secondFileName = files.get(i + 1);
			valuesByIter[i] = getLinkStatsRMSDelta(firstFileName,
					secondFileName, dailyVolumes);
			valuesVsStart[i] = getLinkStatsRMSDelta(files.get(0),
					secondFileName, dailyVolumes);
		}
		String[] pathComponents = simulationPath.split("/");
		String runID = pathComponents[pathComponents.length - 1];
		plotLinkStatsRMSDelta(iters, valuesByIter, simulationPath,
				"Linkstats change over iterations for " + runID
						+ (dailyVolumes ? " (daily volumes)" : ""),
				dailyVolumes, false);
		plotLinkStatsRMSDelta(iters, valuesVsStart, simulationPath,
				"Linkstats change vs start for " + runID
						+ (dailyVolumes ? " (daily volumes)" : ""),
				dailyVolumes, true);
	}

	public void departuresAndModeShareRMSDeltaPlot(String simulationPath,
			int startIter, int endIter, boolean modeShare) {
		ArrayList<String> files = new ArrayList<String>();
		// create a list of file names to use in the comparison
		ArrayList<Integer> relevantIters = findQsimIters(simulationPath);
		for (int iter : relevantIters) {
			String fileName = getDepartureStatsFileName(simulationPath, iter);
			if (fileName == null)
				continue;
			else {
				files.add(fileName);
			}
		}
		// now iterate through the list and get their rmses for plotting
		double[] iters = new double[relevantIters.size()];
		for (int iter = 0; iter < relevantIters.size(); iter++) {
			iters[iter] = relevantIters.get(iter);
		}
		double[] rmsd = new double[relevantIters.size()];
		rmsd[0] = 0;
		double[] modeshare = new double[relevantIters.size()];
		modeshare[0] = 100;
		for (int i = 1; i < files.size(); i++) {
			String secondFileName = files.get(i);
			Tuple<Double, Double> deltaAndModeShare = getDeparturesRMSDeltaAndModeShare(files.get(0), secondFileName);
			rmsd[i] = deltaAndModeShare.getFirst();
			if (modeShare)
				modeshare[i] = deltaAndModeShare.getSecond();
		}
		String[] pathComponents = simulationPath.split("/");
		String runID = pathComponents[pathComponents.length - 1];
		plotDepartureStatsRMSDelta(iters, rmsd, simulationPath,
				"Departures change vs start for " + runID);
		if (modeShare)
			plotModeShare(iters, modeshare, simulationPath,
					"Mode share of car for " + runID);
	}

	private void plotModeShare(double[] iters, double[] values,
			String simulationPath, String title) {
		
		XYLineChart xyAbsolute = new XYLineChart(title, "iterations",
				"Mode share of car (%)");

		xyAbsolute.addSeries("Car", iters, values);
		xyAbsolute.saveAsPng(simulationPath + "/modeShare.png", 800, 600);
		BufferedWriter writer = IOUtils.getBufferedWriter(simulationPath
				+ "/modeShare.txt");
		try {
			writer.write("ITERATION\tcarShare\n");
			writer.flush();
			for (int i = 0; i < iters.length; i++) {
				writer.write(String.format("%f\t%f\n", iters[i], values[i]));
				writer.flush();
			}
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void plotDepartureStatsRMSDelta(double[] iters, double[] values,
			String simulationPath, String title) {
		
		XYLineChart xyAbsolute = new XYLineChart(title, "iterations",
				"RMSD (absolute)");

		xyAbsolute.addSeries("RMSD", iters, values);
		xyAbsolute.saveAsPng(simulationPath + "/departuresRMSDelta.png", 800, 600);
		BufferedWriter writer = IOUtils.getBufferedWriter(simulationPath
				+ "/departuresRMSDelta.txt");
		try {
			writer.write("ITERATION\tdeparturesRMSD\n");
			writer.flush();
			for (int i = 0; i < iters.length; i++) {
				writer.write(String.format("%f\t%f\n", iters[i], values[i]));
				writer.flush();
			}
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}



	/**
	 * @param firstFileName
	 * @param secondFileName
	 * @return a Tuple of the RMSD of the second vs the first filename, and the mode share of the second file
	 */
	private Tuple<Double, Double> getDeparturesRMSDeltaAndModeShare(String firstFileName,
			String secondFileName) {
		double rmsd = 0;
		double modeShare =0;
		Tuple<Double, Double> rmsModeShare =  null;
		try {
			BufferedReader firstReader = IOUtils
					.getBufferedReader(firstFileName);
			BufferedReader secondReader = IOUtils
					.getBufferedReader(secondFileName);
			String[] headings = firstReader.readLine().split("\t");
			// need the secondReader to progress past the headings as well
			secondReader.readLine();
			int totalDeparturesColumn = 0;
			int carDeparturesColumn = 0;
			for (int column = 0; column < headings.length; column++) {
				String heading = headings[column];
				if (heading.startsWith("departures_all"))
					totalDeparturesColumn = column;
				if (heading.startsWith("departures_car"))
					carDeparturesColumn = column;
			}
			// read the lines, and perform the rmsdelta calc
			int rowCount = 0;
			double sumDeltaSquared = 0;
			double totalDepartures = 0;
			double totalCar = 0;
			while (rowCount >= 0) {
				String firstLine = firstReader.readLine();
				String secondLine = secondReader.readLine();
				if (firstLine == null || secondLine == null)
					break;
				else
					rowCount++;
				String[] firstBits = firstLine.split("\t");
				String[] secondBits = secondLine.split("\t");
				// go through all columns of counts
				sumDeltaSquared += Math
						.pow(Double.parseDouble(firstBits[totalDeparturesColumn])
								- Double.parseDouble(secondBits[totalDeparturesColumn]),
								2);
				totalDepartures+=Double.parseDouble(secondBits[totalDeparturesColumn]);
				totalCar+=Double.parseDouble(secondBits[carDeparturesColumn]);

			}
			rmsd = Math.pow(sumDeltaSquared / rowCount, 0.5);
			modeShare = totalCar/totalDepartures*100;
			rmsModeShare=new Tuple<Double, Double>(rmsd, modeShare);
			firstReader.close();
			secondReader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println(rmsd);
		return rmsModeShare;
	}

	private ArrayList<Integer> findQsimIters(String simulationPath) {
		ArrayList<Integer> qsimIters = new ArrayList<Integer>();
		try{
			BufferedReader reader = IOUtils.getBufferedReader(simulationPath
					+ "/qsimstats.txt");
			reader.readLine();
			while (true) {
				String line = reader.readLine();
				if (line == null)
					break;
				qsimIters.add(Integer.parseInt(line.split("\t")[0]));
			}
			System.out.println(qsimIters);
			return qsimIters;
		}catch(UncheckedIOException fe){
			for (int iter = 0; iter < 20000; iter++) {
				String fileName = getLinkStatsFileName(simulationPath, iter);
				if (fileName == null)
					continue;
				else {
					qsimIters.add(iter);
				}
			}
			return qsimIters;
		} catch (IOException e) {
			System.err.println("No qsimstats.txt found");
		}
		return null;
	}

	private double getLinkStatsRMSDelta(String firstFileName,
			String secondFileName, boolean dailyVolumes) {
		double rmsd = 0;
		try {
			BufferedReader firstReader = IOUtils
					.getBufferedReader(firstFileName);
			BufferedReader secondReader = IOUtils
					.getBufferedReader(secondFileName);
			String[] headings = firstReader.readLine().split("\t");
			// need the secondReader to progress past the headings as well
			secondReader.readLine();
			int[] countColumns = new int[24];
			int dailyVolumeColumn = 0;
			int indexCounter = 0;
			for (int column = 0; column < headings.length; column++) {
				String heading = headings[column];
				if (heading.startsWith("HRS") && heading.endsWith("avg")) {
					if (indexCounter < 24)
						countColumns[indexCounter] = column;
					else
						dailyVolumeColumn = column;
					indexCounter++;
				}
			}
			// read the lines, and perform the rmsdelta calc
			int rowCount = 0;
			double sumDeltaSquared = 0;
			while (rowCount >= 0) {
				String firstLine = firstReader.readLine();
				String secondLine = secondReader.readLine();
				if (firstLine == null || secondLine == null)
					break;
				else
					rowCount++;
				String[] firstBits = firstLine.split("\t");
				String[] secondBits = secondLine.split("\t");
				// go through all columns of counts or only through the total
				// daily volume
				if (dailyVolumes) {
					sumDeltaSquared += Math
							.pow(Double
									.parseDouble(firstBits[dailyVolumeColumn])
									- Double.parseDouble(secondBits[dailyVolumeColumn]),
									2);
				} else {
					for (int column : countColumns) {
						// firstCounts.add(Double.parseDouble(firstBits[column]));
						// secondCounts.add(Double.parseDouble(secondBits[column]));
						// System.out.println(String.format("%s , %s",firstBits[column],secondBits[column]));
						sumDeltaSquared += Math
								.pow(Double.parseDouble(firstBits[column])
										- Double.parseDouble(secondBits[column]),
										2);
					}
				}
			}
			rmsd = Math.pow(sumDeltaSquared / rowCount, 0.5);
			firstReader.close();
			secondReader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println(rmsd);
		return rmsd;

	}

	private void plotLinkStatsRMSDelta(double[] iters, double[] values,
			String simulationPath, String title, boolean dailyVolumes,
			boolean vStart) {
		double[] relativeValues = valuesRelativeToMax(values);
		XYLineChart xyAbsolute = new XYLineChart(title, "iterations",
				"RMSD (absolute)");
		XYLineChart xyRelative = new XYLineChart(title, "iterations",
				"RMSD (pct of max)");
		xyAbsolute.addSeries("RMSD", iters, values);
		xyRelative.addSeries("RMSD pct", iters, relativeValues);
		xyAbsolute.saveAsPng(simulationPath + "/linkstatsRMSD_abs"
				+ (vStart ? "_vstart" : "") + (dailyVolumes ? "_daily" : "")
				+ ".png", 800, 600);
		xyRelative.saveAsPng(simulationPath + "/linkstatsRMSD_rel"
				+ (vStart ? "_vstart" : "") + (dailyVolumes ? "_daily" : "")
				+ ".png", 800, 600);
		BufferedWriter writer = IOUtils.getBufferedWriter(simulationPath
				+ "/linkstatsRMSD" + (dailyVolumes ? "_daily" : "") + ".txt");
		try {
			writer.write("ITERATION\tRMSD (absolute)\tRMSD (relative to max)\n");
			writer.flush();
			for (int i = 0; i < iters.length; i++) {
				writer.write(String.format("%f\t%f\t%f\n", iters[i], values[i],
						relativeValues[i]));
				writer.flush();
			}
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private double[] valuesRelativeToMax(double[] values) {
		double max = 0d;
		double[] result = new double[values.length];
		for (double value : values) {
			if (value > max)
				max = value;
		}
		if (max == 0d)
			return result;
		for (int i = 0; i < values.length; i++) {
			result[i] = values[i] / max * 100;
		}
		return result;
	}

	private String getLinkStatsFileName(String simulationPath, int iter) {
		// TODO Auto-generated method stub
		File dir = new File(simulationPath + "/ITERS/it." + iter);
		FileFilter fileFilter = new WildcardFileFilter("*linkstats.txt*");
		File[] files = dir.listFiles(fileFilter);
		if (files == null || files.length == 0)
			return null;
		System.out.println(files[0].toString());
		return files[0].toString();
	}

	private String getDepartureStatsFileName(String simulationPath, int iter) {
		// TODO Auto-generated method stub
		File dir = new File(simulationPath + "/ITERS/it." + iter);
		FileFilter fileFilter = new WildcardFileFilter("*legHistogram.txt*");
		File[] files = dir.listFiles(fileFilter);
		if (files == null || files.length == 0)
			return null;
		// System.out.println(files[0].toString());
		return files[0].toString();
	}

	public static void main(String[] args) {
		String simPath = null;

		JFileChooser chooser = new JFileChooser("./data");
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		chooser.showOpenDialog(new JPanel());
		simPath = chooser.getSelectedFile().getPath();
//		new SummaryStatistics().linkStatsRMSDeltaPlot(simPath, 0, 5000, true);
		new SummaryStatistics().linkStatsRMSDeltaPlot(simPath, 0, 5000, false);
		new SummaryStatistics().departuresAndModeShareRMSDeltaPlot(simPath, 0, 5000, false);
	}
}
