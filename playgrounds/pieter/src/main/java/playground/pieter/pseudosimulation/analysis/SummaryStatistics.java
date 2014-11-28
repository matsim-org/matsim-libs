package playground.pieter.pseudosimulation.analysis;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.JFileChooser;
import javax.swing.JPanel;

import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.jfree.chart.title.TextTitle;
import org.matsim.core.utils.charts.XYLineChart;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;

public class SummaryStatistics {

	public class LinkStatsComparison {

		private final double dailyRmsd;
		private final double hourlyRmsd;
		private final ArrayList<Double> dailyDeltas;
		public double getDailyRmsd() {
			return dailyRmsd;
		}

		public double getHourlyRmsd() {
			return hourlyRmsd;
		}

		public ArrayList<Double> getDailyDeltas() {
			return dailyDeltas;
		}

		public ArrayList<Double> getHourlyDeltas() {
			return hourlyDeltas;
		}

		private final ArrayList<Double> hourlyDeltas;

		public LinkStatsComparison(double dailyRmsd, double hourlyRmsd,
				ArrayList<Double> dailyDeltas, ArrayList<Double> hourlyDeltas) {
			this.dailyRmsd = dailyRmsd;
			this.hourlyRmsd = hourlyRmsd;
			this.dailyDeltas = dailyDeltas;
			this.hourlyDeltas = hourlyDeltas;
			
		}

	}

	private int referenceIter;
	private String referencePath;

	/**
	 * @param simulationPath
	 *            the root of the simulation
	 * @param startIter
	 * @param endIter
     */
    void linkStatsRMSDeltaPlot(String simulationPath, int startIter,
                               int endIter) {
		ArrayList<String> files = new ArrayList<>();
		// create a list of file names to use in the comparison
		ArrayList<Integer> relevantIters = new ArrayList<>();
		for (int iter = startIter; iter < endIter; iter++) {
			String fileName = getLinkStatsFileName(simulationPath, iter);
			if (fileName == null) {
            }
			else {
				files.add(fileName);
				relevantIters.add(iter);
			}
		}
		String reference;
		if (this.referencePath == null) {
			reference = files.get(0);
		} else {
			reference = getLinkStatsFileName(referencePath, referenceIter);
		}
		// now iterate through the list and get their rmses for plotting
		double[] iters = new double[relevantIters.size()];

		for (int iter = 0; iter < relevantIters.size(); iter++) {
			iters[iter] = relevantIters.get(iter);
		}
		BufferedWriter dailyWriter = IOUtils.getBufferedWriter(simulationPath+"/dailyDeltas.txt");
		BufferedWriter hourlyWriter = IOUtils.getBufferedWriter(simulationPath+"/hourlyDeltas.txt");
		ArrayList<ArrayList<Double>> dailyDeltas = new ArrayList<>();
		ArrayList<ArrayList<Double>> hourlyDeltas = new ArrayList<>();
		double[] dailyValuesVsReference = new double[relevantIters.size()];
		double[] hourlyValuesVsReference = new double[relevantIters.size()];
		try {
			for (int i = 0; i < files.size(); i++) {
				String secondFileName = files.get(i);

				LinkStatsComparison linkStatsComparison = getLinkStatsComparison(
						reference, secondFileName);
				dailyValuesVsReference[i] = linkStatsComparison.getDailyRmsd();
				hourlyValuesVsReference[i] = linkStatsComparison
						.getHourlyRmsd();
				dailyDeltas.add( linkStatsComparison.getDailyDeltas());
				hourlyDeltas.add(linkStatsComparison.getHourlyDeltas());
			}
			writeDeltas(iters, dailyWriter, dailyDeltas);
			writeDeltas(iters, hourlyWriter, hourlyDeltas);
			dailyWriter.close();
			hourlyWriter.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		String[] pathComponents = simulationPath.split("/");
		String runID = pathComponents[pathComponents.length - 1];

		plotLinkStatsRMSDelta(iters, dailyValuesVsReference, simulationPath,
				"Linkstats change vs ref. for " + runID
						+  " (daily volumes)" ,
				true, true);
		plotLinkStatsRMSDelta(iters, hourlyValuesVsReference, simulationPath,
				"Linkstats change vs ref. for " + runID
						+  " (hourly volumes)" ,
				false, true);
	}

	private void writeDeltas(double[] iters, BufferedWriter writer,
			ArrayList<ArrayList<Double>> allDeltas) throws IOException {
		String outString = "";
		for (double iter : iters) {
			outString += String.format("it_%0000d\t", (int)iter);
		}
		outString = outString.trim();
		outString += "\n";
		writer.write(outString);
		writer.flush();
		for (int i = 0; i < allDeltas.get(0).size(); i++) {
			outString = "";
			for (ArrayList<Double> delta : allDeltas) {
				outString += String.format("%f\t", delta.get(i));
			}
			outString = outString.trim();
			outString += "\n";
			writer.write(outString);
			writer.flush();
		}
	}

	void departuresAndModeShareRMSDeltaPlot(String simulationPath,
                                            int startIter, int endIter, boolean modeShare) {
		ArrayList<String> files = new ArrayList<>();
		// create a list of file names to use in the comparison
		ArrayList<Integer> relevantIters = findQsimIters(simulationPath);
		for (int iter : relevantIters) {
			String fileName = getDepartureStatsFileName(simulationPath, iter);
			if (fileName == null) {
            }
			else {
				files.add(fileName);
			}
		}
		String reference;
		if (this.referencePath == null) {
			reference = files.get(0);
		} else {
			reference = getDepartureStatsFileName(referencePath, referenceIter);
		}
		// now iterate through the list and get their rmses for plotting
		double[] iters = new double[relevantIters.size()];
		for (int iter = 0; iter < relevantIters.size(); iter++) {
			iters[iter] = relevantIters.get(iter);
		}
		double[] rmsd = new double[relevantIters.size()];
		double[] modeshare = new double[relevantIters.size()];
		for (int i = 0; i < files.size(); i++) {
			String secondFileName = files.get(i);
			Tuple<Double, Double> deltaAndModeShare = getDeparturesRMSDeltaAndModeShare(
					reference, secondFileName);
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
		xyAbsolute.getChart().addSubtitle(
				new TextTitle("Reference: run " + this.referencePath + " iter "
						+ this.referenceIter));
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
		xyAbsolute.getChart().addSubtitle(
				new TextTitle("Reference: run " + this.referencePath + " iter "
						+ this.referenceIter));
		xyAbsolute.saveAsPng(simulationPath + "/departuresRMSDelta.png", 800,
				600);
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
	 * @return a Tuple of the RMSD of the second vs the first filename, and the
	 *         mode share of the second file
	 */
	private Tuple<Double, Double> getDeparturesRMSDeltaAndModeShare(
			String firstFileName, String secondFileName) {
		double rmsd = 0;
		double modeShare = 0;
		Tuple<Double, Double> rmsModeShare = null;
		try {
			BufferedReader reader1 = IOUtils.getBufferedReader(firstFileName);
			BufferedReader reader2 = IOUtils.getBufferedReader(secondFileName);
			String[] headers1 = reader1.readLine().split("\t");
			String[] headers2 = reader2.readLine().split("\t");
			int totalDeparturesColumn1 = 0;
			int totalDeparturesColumn2 = 0;
			int carDeparturesColumn1 = 0;
			int carDeparturesColumn2 = 0;
			for (int column = 0; column < headers1.length; column++) {
				String heading1 = headers1[column];
				if (heading1.startsWith("departures_all"))
					totalDeparturesColumn1 = column;
				if (heading1.startsWith("departures_car"))
					carDeparturesColumn1 = column;
			}
			for (int column = 0; column < headers2.length; column++) {
				String heading2 = headers2[column];
				if (heading2.startsWith("departures_all"))
					totalDeparturesColumn2 = column;
				if (heading2.startsWith("departures_car"))
					carDeparturesColumn2 = column;
			}
			// read the lines, and perform the rmsdelta calc
			int rowCount = 0;
			double sumDeltaSquared = 0;
			double totalDepartures1 = 0;
			double totalCar1 = 0;
			double totalDepartures2 = 0;
			double totalCar2 = 0;
			while (rowCount >= 0) {
				String line1 = reader1.readLine();
				String line2 = reader2.readLine();
				if (line1 == null || line2 == null)
					break;
				else
					rowCount++;
				String[] bits1 = line1.split("\t");
				String[] bits2 = line2.split("\t");
				// go through all columns of counts
				sumDeltaSquared += Math.pow(Double
						.parseDouble(bits1[totalDeparturesColumn1])
						- Double.parseDouble(bits2[totalDeparturesColumn2]), 2);
				totalDepartures1 += Double
						.parseDouble(bits1[totalDeparturesColumn1]);
				totalCar1 += Double.parseDouble(bits1[carDeparturesColumn1]);
				totalDepartures2 += Double
						.parseDouble(bits2[totalDeparturesColumn2]);
				totalCar2 += Double.parseDouble(bits2[carDeparturesColumn2]);

			}
			rmsd = Math.pow(sumDeltaSquared / rowCount, 0.5);
			modeShare = (totalCar2 / totalDepartures2)
					/ (totalCar1 / totalDepartures1);
			rmsModeShare = new Tuple<>(rmsd, modeShare);
			reader1.close();
			reader2.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println(rmsd);
		return rmsModeShare;
	}

	private ArrayList<Integer> findQsimIters(String simulationPath) {
		ArrayList<Integer> qsimIters = new ArrayList<>();
		try {
			BufferedReader reader = IOUtils.getBufferedReader(simulationPath
					+ "/qsimstats.txt");
			reader.readLine();
			while (true) {
				String line = reader.readLine();
				if (line == null)
					break;
				qsimIters.add(Integer.parseInt(line.split("\t")[0]));
			}
			int maxIter = this.getMaxIter(simulationPath);
			qsimIters.add(maxIter);
			System.out.println(qsimIters);
			return qsimIters;
		} catch (UncheckedIOException fe) {
			for (int iter = 0; iter < 20000; iter++) {
				String fileName = getDepartureStatsFileName(simulationPath, iter);
				if (fileName == null) {
                }
				else {
					qsimIters.add(iter);
				}
			}
			// the last iteration is not recorded in qsimiters.txt
			// because the history object closes before the writer can write it
			return qsimIters;
		} catch (IOException e) {
			System.err.println("No qsimstats.txt found");
		}
		return null;
	}

	private LinkStatsComparison getLinkStatsComparison(String reference,
			String secondFileName) {
		ArrayList<Double> dailyDeltas = new ArrayList<>();
		ArrayList<Double> hourlyDeltas = new ArrayList<>();
		double dailyRmsd = 0;
		double hourlyRmsd = 0;
		try {
			BufferedReader refReader = IOUtils
					.getBufferedReader(reference);
			BufferedReader secondReader = IOUtils
					.getBufferedReader(secondFileName);
			String[] headings = refReader.readLine().split("\t");
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
			double sumDailyDeltaSquared = 0;
			double sumHourlyDeltaSquared = 0;
			while (rowCount >= 0) {
				String refLine = refReader.readLine();
				String secondLine = secondReader.readLine();
				if (refLine == null || secondLine == null)
					break;
				else
					rowCount++;
				String[] refBits = refLine.split("\t");
				String[] secondBits = secondLine.split("\t");
				// go through all columns of counts or only through the total
				// daily volume
					double refDaily = Double.parseDouble(refBits[dailyVolumeColumn]);
					double secondDaily = Double.parseDouble(secondBits[dailyVolumeColumn]);
					sumDailyDeltaSquared += Math.pow(refDaily-secondDaily,2);
					dailyDeltas.add(refDaily-secondDaily);
				
					for (int column : countColumns) {
						double refHourly = Double.parseDouble(refBits[column]);
						double secondHourly = Double.parseDouble(secondBits[column]);
						sumHourlyDeltaSquared += Math.pow(refHourly-secondHourly, 2);
						hourlyDeltas.add(refHourly-secondHourly);
					}
				
			}
			dailyRmsd = Math.pow(sumDailyDeltaSquared / dailyDeltas.size(), 0.5);
			hourlyRmsd = Math.pow(sumHourlyDeltaSquared / hourlyDeltas.size(), 0.5);
			refReader.close();
			secondReader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Daily link RMSD: "+dailyRmsd+", Hourly RMSD: "+hourlyRmsd);
		return new LinkStatsComparison(dailyRmsd,hourlyRmsd,dailyDeltas,hourlyDeltas);

	}
	private double getLinkStatsDelta(String firstFileName,
			String secondFileName, boolean dailyVolumes) {
		ArrayList<Double> deltas = new ArrayList<>();
		
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
							.pow(
									Double
											.parseDouble(firstBits[dailyVolumeColumn])
											- Double
													.parseDouble(secondBits[dailyVolumeColumn]),
									2);
				} else {
					for (int column : countColumns) {
						// firstCounts.add(Double.parseDouble(firstBits[column]));
						// secondCounts.add(Double.parseDouble(secondBits[column]));
						// System.out.println(String.format("%s , %s",firstBits[column],secondBits[column]));
						sumDeltaSquared += Math.pow(Double
								.parseDouble(firstBits[column])
								- Double.parseDouble(secondBits[column]), 2);
						deltas.add(Double
								.parseDouble(firstBits[column])
								- Double.parseDouble(secondBits[column]));
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
			boolean vIter) {
		double[] relativeValues = valuesRelativeToMax(values);
		XYLineChart xyAbsolute = new XYLineChart(title, "iterations",
				"RMSD (absolute)");
		xyAbsolute.addSeries("RMSD", iters, values);
		xyAbsolute.getChart().addSubtitle(
				new TextTitle("Reference: run " + this.referencePath + " iter "
						+ this.referenceIter));
		xyAbsolute.saveAsPng(simulationPath + "/linkstatsRMSD_abs"
				+ (vIter ? "_vRef" : "") + (dailyVolumes ? "_daily" : "")
				+ ".png", 800, 600);
		BufferedWriter writer = IOUtils.getBufferedWriter(simulationPath
				+ "/linkstatsRMSD" + (dailyVolumes ? "_daily" : "") + ".txt");
		try {
			writer
					.write("ITERATION\tRMSD (absolute)\tRMSD (relative to max)\n");
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

	private void getReferenceIter(String iterationPath) {
		// TODO Auto-generated method stub
		this.referenceIter = Integer.parseInt(iterationPath
				.substring(iterationPath.lastIndexOf(".") + 1));
		this.referencePath = new File(new File(iterationPath).getParent())
				.getParent();

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

	private int getMaxIter(String simulationPath) {
		// TODO Auto-generated method stub
		File dir = new File(simulationPath);
		FileFilter fileFilter = new WildcardFileFilter("*instrumentation.txt");
		File[] files = dir.listFiles(fileFilter);
		if (files == null || files.length == 0)
			return 0;
		int max = 0;
		BufferedReader reader = IOUtils.getBufferedReader(files[0].toString());
		try {
			reader.readLine();
			while (true) {
				String line = reader.readLine();
				if (line == null)
					break;
				max = Integer.parseInt(line.split("\t")[0]);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// System.out.println(files[0].toString());
		return max;
	}

	void iterateThroughSimulationFolders(String simPath) {
		File dir = new File(simPath);
		FileFilter fileFilter = DirectoryFileFilter.DIRECTORY;
		File[] files = dir.listFiles(fileFilter);
		if (files == null || files.length == 0)
			return;
		boolean useOnlyCurrentDir = false;
		for (File file : files) {
			if (file.toString().endsWith("ITERS"))
				// use the current dir
				useOnlyCurrentDir = true;
		}

		JFileChooser chooser = new JFileChooser("./data/zoutput");
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		chooser.showOpenDialog(new JPanel());
		this.getReferenceIter(chooser.getSelectedFile().getPath());
		// if we managed to get this far, I suppose we can go ahead and iterate
		// through the subdirs
		if (useOnlyCurrentDir) {
			this.linkStatsRMSDeltaPlot(simPath, 0, 10000);
//			this.linkStatsRMSDeltaPlot(simPath, 0, 10000, false);
			this.departuresAndModeShareRMSDeltaPlot(simPath, 0, 10000, true);
		} else {
			for (File file : files) {
				this.linkStatsRMSDeltaPlot(file.toString(), 0, 10000);
//				this.linkStatsRMSDeltaPlot(file.toString(), 0, 10000, false);
				this.departuresAndModeShareRMSDeltaPlot(file.toString(), 0, 10000, true);
			}
		}
	}

	public static void main(String[] args) {
		String simPath = null;

		JFileChooser chooser = new JFileChooser("./data/zoutput");
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		chooser.showOpenDialog(new JPanel());
		simPath = chooser.getSelectedFile().getPath();

		// new SummaryStatistics().linkStatsRMSDeltaPlot(simPath, 0, 5000,
		// true);
		// new SummaryStatistics().linkStatsRMSDeltaPlot(simPath, 0, 5000,
		// false);
		// new SummaryStatistics().departuresAndModeShareRMSDeltaPlot(simPath,
		// 0, 5000, true);
		new SummaryStatistics().iterateThroughSimulationFolders(simPath);
	}
}
