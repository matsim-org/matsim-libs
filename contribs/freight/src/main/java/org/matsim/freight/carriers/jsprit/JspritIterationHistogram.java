package org.matsim.freight.carriers.jsprit;

import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.freight.carriers.Carrier;
import org.matsim.freight.carriers.Carriers;
import org.matsim.freight.carriers.CarriersUtils;

import java.awt.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class JspritIterationHistogram {

	private static final Logger log = LogManager.getLogger(JspritIterationHistogram.class);

	private final NavigableMap<Integer, Double> sumSelectedCost = new TreeMap<>();
	private final NavigableMap<Integer, Integer> runCarrierCount = new TreeMap<>();

	private final String title;

	/**
	 * @param carriers                       the carriers
	 * @param bestJspritSolutionCollector    Map: CarrierId -> (Iteration -> selectedCost)
	 */
	public JspritIterationHistogram(Carriers carriers, Map<Id<Carrier>, ? extends NavigableMap<Integer, VehicleRoutingProblemSolution>> bestJspritSolutionCollector,
									String title) {
		this.title = title;
		aggregate(carriers, bestJspritSolutionCollector);
	}

	/**
	 * Analyze and aggregate the selected cost series of all carriers.
	 *
	 * @param carriers                       the carriers
	 * @param bestJspritSolutionCollector    results of all solved VRPs
	 */
	private void aggregate(Carriers carriers, Map<Id<Carrier>, ? extends NavigableMap<Integer, VehicleRoutingProblemSolution>> bestJspritSolutionCollector) {
		// get global max iterations for jsprit
		int globalMaxIteration = carriers.getCarriers().values().stream()
			.mapToInt(CarriersUtils::getJspritIterations)
			.max()
			.orElse(0);
		for (int iter = 0; iter <= globalMaxIteration; iter++) {
			double sum = 0.0;
			int runCount = 0;

			for (Id<Carrier> carrierId : bestJspritSolutionCollector.keySet()) {
				NavigableMap<Integer, VehicleRoutingProblemSolution> series = bestJspritSolutionCollector.get(carrierId);
				if (series.isEmpty()) continue;

				Map.Entry<Integer, VehicleRoutingProblemSolution> floor = series.floorEntry(iter);
				// Sum: last known selected cost
				sum += floor.getValue().getCost();

				// count carriers that are still running at iteration iter
				if (CarriersUtils.getJspritIterations(carriers.getCarriers().get(carrierId)) >= iter) {
					runCount++;
				}
			}
			sumSelectedCost.put(iter, sum);
			runCarrierCount.put(iter, runCount);
		}
	}

	/**
	 * Creates the graphic including two Y-Axes: one for sum selected cost and one for running carriers.
	 *
	 * @return the created graphic
	 */
	private JFreeChart createGraphic() {
		XYSeries sumSelectedSeries = new XYSeries("Sum selected cost", false, true);
		XYSeries runSeries = new XYSeries("Running carriers", false, true);

		for (Integer iter : sumSelectedCost.navigableKeySet()) {
			Double sum = sumSelectedCost.get(iter);
			Integer run = runCarrierCount.get(iter);

			sumSelectedSeries.add(iter, sum);
			if (run != null) {
				runSeries.add(iter, run);
			}
		}
		int maxValue = sumSelectedCost.values().stream().max(Double::compareTo).orElse(0.0).intValue();
		int minorValue = sumSelectedCost.values().stream().min(Double::compareTo).orElse(1.0).intValue();
		XYSeriesCollection costDataset = new XYSeriesCollection();
		costDataset.addSeries(sumSelectedSeries);

		JFreeChart chart = ChartFactory.createXYLineChart(
			title,
			"Jsprit Iteration",
			"Sum selected cost",
			costDataset,
			PlotOrientation.VERTICAL,
			true,
			false,
			false
		);

		Font titleFont = new Font("SansSerif", Font.BOLD, 20);
		Font axisLabelFont = new Font("SansSerif", Font.PLAIN, 18);
		Font tickFont = new Font("SansSerif", Font.PLAIN, 15);
		Font legendFont = new Font("SansSerif", Font.PLAIN, 16);

		chart.setTitle(new TextTitle(title, titleFont));
		if (chart.getLegend() != null) {
			chart.getLegend().setItemFont(legendFont);
		}
		XYPlot plot = chart.getXYPlot();
		plot.setDomainGridlinesVisible(true);
		plot.setRangeGridlinesVisible(true);

		XYLineAndShapeRenderer costRenderer = new XYLineAndShapeRenderer(true, false);
		plot.setRenderer(0, costRenderer);

		NumberAxis xAxis = (NumberAxis) plot.getDomainAxis();
		xAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
		xAxis.setLabelFont(axisLabelFont);
		xAxis.setTickLabelFont(tickFont);

		NumberAxis costAxis = (NumberAxis) plot.getRangeAxis();
		costAxis.setLabelFont(axisLabelFont);
		costAxis.setTickLabelFont(tickFont);
		costAxis.setRange(minorValue * 0.8, maxValue * 1.2);
		// second axis for running carriers
		NumberAxis carrierAxis = new NumberAxis("Running carriers");
		plot.setRangeAxis(1, carrierAxis);
		carrierAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
		carrierAxis.setLabelFont(axisLabelFont);
		carrierAxis.setTickLabelFont(tickFont);

		plot.setRangeAxis(1, carrierAxis);
		XYSeriesCollection carrierDataset = new XYSeriesCollection();
		carrierDataset.addSeries(runSeries);

		int carrierDatasetIndex = 1;
		plot.setDataset(carrierDatasetIndex, carrierDataset);
		plot.mapDatasetToRangeAxis(carrierDatasetIndex, 1);

		XYLineAndShapeRenderer carrierRenderer = new XYLineAndShapeRenderer(true, false);
		plot.setRenderer(carrierDatasetIndex, carrierRenderer);

		return chart;
	}

	/**
	 * Writes the graphic to a PNG file.
	 *
	 * @param aggegatedJspritAnalysisCSVPath Path to the CSV file where the aggregated jsprit analysis is written to. The PNG file will be created in the same directory.
	 */
	public void writeGraphic(Path aggegatedJspritAnalysisCSVPath) {
		Path aggegatedJspritAnalysisPNGPath = Path.of(aggegatedJspritAnalysisCSVPath.toString().replace(".csv", ".png"));

		try {
			Files.createDirectories(aggegatedJspritAnalysisPNGPath.getParent());
			ChartUtils.saveChartAsPNG(aggegatedJspritAnalysisPNGPath.toFile(), createGraphic(), 1200, 800);
		} catch (IOException e) {
			log.warn("IOException occurred while writing jsprit iteration graphic to {}", aggegatedJspritAnalysisPNGPath, e);
		}
	}

	/**
	 * Writes the aggregated jsprit analysis to a CSV file.
	 *
	 * @param aggegatedJspritAnalysisCSVPath Path to the CSV file where the aggregated jsprit analysis is written to.
	 * @param delimiter                      Delimiter used in the CSV file.
	 */
	public void writeAggregatedCsv(Path aggegatedJspritAnalysisCSVPath, String delimiter) {
		try {

			// If the file already exists, we assume that this is a VRP solving loop and that the file should be extended. In this case, we read the existing file to get the number of iterations before and adjust the iteration numbers accordingly.
			// An example is the generation of the small scale commercial traffic demand. Here we have a loop of solving the VRPS a agin if not all job have been assigned. In this case, we want to have one aggregated jsprit analysis CSV file that contains the results of all iterations of the loop and not one file per iteration of the loop.

			int numberOfIterationsBefore = 0;
			if (Files.exists(aggegatedJspritAnalysisCSVPath)) {
				log.warn(
					"Aggregated jsprit analysis CSV already exists at {}. The file will be extended and the number of iterations will adjusted, because we assume that this is a VRP solving loop.",
					aggegatedJspritAnalysisCSVPath);
				try (BufferedReader reader = IOUtils.getBufferedReader(aggegatedJspritAnalysisCSVPath.toString())) {
					CSVParser parse = CSVFormat.Builder.create(CSVFormat.DEFAULT).setDelimiter('\t').setHeader()
						.setSkipHeaderRecord(true).get().parse(reader);
					numberOfIterationsBefore = parse.getRecords().size();
				}
			}

			Path dir = aggegatedJspritAnalysisCSVPath.getParent();
			if (dir != null) {
				Files.createDirectories(dir);
			}

			try (BufferedWriter writer = IOUtils.getAppendingBufferedWriter(aggegatedJspritAnalysisCSVPath.toString())) {

				if (numberOfIterationsBefore == 0) {
					writer.write(String.join(delimiter, "jsprit_iteration", "runCarrier", "sumJspritScores"));
					writer.newLine();
				}
				for (int iter = 0; iter < sumSelectedCost.size(); iter++) {
					int writtenIter = iter + numberOfIterationsBefore;
					writer.write(writtenIter + delimiter + runCarrierCount.get(iter) + delimiter + sumSelectedCost.get(iter));
					writer.newLine();
				}
			}
		} catch (IOException e) {
			log.error("Error writing aggregated jsprit analysis CSV to {}", aggegatedJspritAnalysisCSVPath, e);
		}
	}
}
