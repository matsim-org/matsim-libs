package org.matsim.freight.carriers.jsprit;

import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
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
	private JFreeChart createGraphic(NavigableMap<Integer, Double> sumSelectedCostBefore, NavigableMap<Integer, Integer> runCarrierCountBefore) {
		XYSeries sumSelectedSeries = new XYSeries("Sum selected cost", false, true);
		XYSeries runSeries = new XYSeries("Running carriers", false, true);

		// add the series of the jsprit run before this analyzed run to the graphic, so that the series from before and after aggregation are shown in one graphic without overlapping
		if (!sumSelectedCostBefore.isEmpty()) {
			for (Integer iter : sumSelectedCostBefore.navigableKeySet()) {
				Double sum = sumSelectedCostBefore.get(iter);
				Integer run = runCarrierCountBefore.get(iter);

				sumSelectedSeries.add(iter, sum);
				if (run != null) {
					runSeries.add(iter, run);
				}
			}
		}
		for (Integer iter : sumSelectedCost.navigableKeySet()) {
			Double sum = sumSelectedCost.get(iter);
			Integer run = runCarrierCount.get(iter);
			iter = iter + sumSelectedCostBefore.size(); // shift iter to the right, so that the series from before and after aggregation are shown in one graphic without overlapping
			sumSelectedSeries.add(iter, sum);
			if (run != null) {
				runSeries.add(iter, run);
			}
		}
		int maxValue = sumSelectedCost.values().stream().max(Double::compareTo).orElse(0.0).intValue();
		int minorValue = sumSelectedCost.values().stream().min(Double::compareTo).orElse(1.0).intValue();
		int maxValueBefore = sumSelectedCostBefore.values().stream().max(Double::compareTo).orElse(0.0).intValue();
		int minorValueBefore = sumSelectedCostBefore.values().stream().min(Double::compareTo).orElse(1.0).intValue();
		minorValue = Math.min(minorValue, minorValueBefore);
		maxValue = Math.max(maxValue, maxValueBefore);

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
	 * @param sumSelectedCostBefore          selected costs of the jsprit run before this analyzed run.
	 * @param runCarrierCountBefore          running carrier count of the jsprit run before this analyzed run.
	 */
	public void writeGraphic(Path aggegatedJspritAnalysisCSVPath, NavigableMap<Integer, Double> sumSelectedCostBefore,
							 NavigableMap<Integer, Integer> runCarrierCountBefore) {
		Path aggegatedJspritAnalysisPNGPath = Path.of(aggegatedJspritAnalysisCSVPath.toString().replace(".csv", ".png"));

		try {
			Files.createDirectories(aggegatedJspritAnalysisPNGPath.getParent());
			ChartUtils.saveChartAsPNG(aggegatedJspritAnalysisPNGPath.toFile(), createGraphic(sumSelectedCostBefore, runCarrierCountBefore), 1200, 800);
		} catch (IOException e) {
			log.warn("IOException occurred while writing jsprit iteration graphic to {}", aggegatedJspritAnalysisPNGPath, e);
		}
	}

	/**
	 * Writes the aggregated jsprit analysis to a CSV file.
	 *
	 * @param aggegatedJspritAnalysisCSVPath Path to the CSV file where the aggregated jsprit analysis is written to.
	 * @param sumSelectedCostBefore          selected costs of the jsprit run before this analyzed run.
	 * @param delimiter                      Delimiter used in the CSV file.
	 */
	public void writeAggregatedCsv(Path aggegatedJspritAnalysisCSVPath, NavigableMap<Integer, Double> sumSelectedCostBefore, String delimiter) {
		try {

			Path dir = aggegatedJspritAnalysisCSVPath.getParent();
			if (dir != null) {
				Files.createDirectories(dir);
			}

			try (BufferedWriter writer = IOUtils.getAppendingBufferedWriter(aggegatedJspritAnalysisCSVPath.toString())) {
				if (sumSelectedCostBefore.isEmpty()) {
					writer.write(String.join(delimiter, "jsprit_iteration", "runCarrier", "sumJspritScores"));
					writer.newLine();
				}
				for (int iter = 0; iter < sumSelectedCost.size(); iter++) {
					int writtenIter = iter + sumSelectedCostBefore.size();
					writer.write(writtenIter + delimiter + runCarrierCount.get(iter) + delimiter + sumSelectedCost.get(iter));
					writer.newLine();
				}
			}
		} catch (IOException e) {
			log.error("Error writing aggregated jsprit analysis CSV to {}", aggegatedJspritAnalysisCSVPath, e);
		}
	}
}
