package org.matsim.freight.carriers.jsprit;

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
	 * @param selectedSeries Map: CarrierId -> (Iteration -> selectedCost)
	 */
	public JspritIterationHistogram(Map<Id<Carrier>, ? extends NavigableMap<Integer, Double>> selectedSeries,
									String title) {
		this.title = title;
		aggregate(selectedSeries);
	}

	/**
	 * Analyze and aggregate the selected cost series of all carriers.
	 *
	 * @param selectedSeries results of all solved VRPs
	 */
	private void aggregate(Map<Id<Carrier>, ? extends NavigableMap<Integer, Double>> selectedSeries) {
		// get global max iterations for jsprit
		int globalMaxIteration = selectedSeries.values().stream()
			.filter(m -> !m.isEmpty())
			.mapToInt(SortedMap::lastKey)
			.max()
			.orElse(0);

		for (int iter = 0; iter <= globalMaxIteration; iter++) {
			double sum = 0.0;
			int runCount = 0;

			for (NavigableMap<Integer, Double> series : selectedSeries.values()) {
				if (series.isEmpty()) continue;

				Map.Entry<Integer, Double> floor = series.floorEntry(iter);
				// Sum: last known selected cost
				sum += floor.getValue();

				// count carriers that are still running at iteration iter
				if (series.ceilingKey(iter) != null) {
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

		XYSeriesCollection costDataset = new XYSeriesCollection();
		costDataset.addSeries(sumSelectedSeries);

		JFreeChart chart = ChartFactory.createXYLineChart(
			title,
			"Iteration",
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
			Path dir = aggegatedJspritAnalysisCSVPath.getParent();
			if (dir != null) {
				Files.createDirectories(dir);
			}

			try (BufferedWriter writer = IOUtils.getBufferedWriter(aggegatedJspritAnalysisCSVPath.toString())) {

				writer.write(String.join(delimiter, "jsprit_iteration", "runCarrier", "sumJspritScores"));
				writer.newLine();

				for (int iter = 0; iter <= sumSelectedCost.size(); iter++) {
					writer.write(iter + delimiter + runCarrierCount.get(iter) + delimiter + sumSelectedCost.get(iter));
					writer.newLine();
				}
			}
		} catch (IOException e) {
			log.error("Error writing aggregated jsprit analysis CSV to {}", aggegatedJspritAnalysisCSVPath, e);
		}
	}
}
