package org.matsim.contrib.util.stats;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import one.util.streamex.EntryStream;
import org.apache.commons.lang3.tuple.Pair;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.DefaultTableXYDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.matsim.contrib.common.csv.CSVLineBuilder;
import org.matsim.contrib.common.csv.CompactCSVWriter;
import org.matsim.contrib.common.timeprofile.TimeProfileCharts;
import org.matsim.contrib.common.util.ChartSaveUtils;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.dvrp.util.TimeDiscretizer;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Based on {@link VehicleOccupancyProfileWriter}
 *
 * @author nkuehnel / MOIA
 */
public class VehicleTaskProfileWriter implements IterationEndsListener {

	private static final String OUTPUT_FILE = "task_time_profiles";

	private final MatsimServices matsimServices;
	private final String mode;
	private final VehicleTaskProfileCalculator calculator;

	private final Comparator<Task.TaskType> taskTypeComparator;
	private final Map<Task.TaskType, Paint> taskTypePaints;

	public VehicleTaskProfileWriter(MatsimServices matsimServices, String mode,
										 VehicleTaskProfileCalculator calculator, Comparator<Task.TaskType> taskTypeComparator,
										 Map<Task.TaskType, Paint> taskTypePaints) {
		this.matsimServices = matsimServices;
		this.mode = mode;
		this.calculator = calculator;
		this.taskTypeComparator = taskTypeComparator;
		this.taskTypePaints = taskTypePaints;
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		TimeDiscretizer timeDiscretizer = calculator.getTimeDiscretizer();

		// stream tasks which are not related to passenger (unoccupied vehicle)
		var taskProfiles = calculator.getTaskProfiles()
				.entrySet()
				.stream()
				.sorted(Map.Entry.comparingByKey(taskTypeComparator))
				.map(e -> Pair.of(e.getKey().name(), e.getValue()))
				.collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));

		String file = filename(OUTPUT_FILE);
		String timeFormat = timeDiscretizer.getTimeInterval() % 60 == 0 ? Time.TIMEFORMAT_HHMM : Time.TIMEFORMAT_HHMMSS;

		try (CompactCSVWriter writer = new CompactCSVWriter(IOUtils.getBufferedWriter(file + ".txt"))) {
			String[] profileHeader = taskProfiles.keySet().toArray(new String[0]);
			writer.writeNext(new CSVLineBuilder().add("time").addAll(profileHeader));
			timeDiscretizer.forEach((bin, time) -> writer.writeNext(
					new CSVLineBuilder().add(Time.writeTime(time, timeFormat)).addAll(cells(taskProfiles, bin))));
		}

		if (this.matsimServices.getConfig().controler().isCreateGraphs()) {
			DefaultTableXYDataset xyDataset = createXYDataset(timeDiscretizer, taskProfiles);
			generateImage(xyDataset, TimeProfileCharts.ChartType.Line);
			generateImage(xyDataset, TimeProfileCharts.ChartType.StackedArea);
		}
	}

	private Stream<String> cells(Map<String, double[]> profiles, int idx) {
		return profiles.values().stream().map(values -> values[idx] + "");
	}

	private DefaultTableXYDataset createXYDataset(TimeDiscretizer timeDiscretizer, Map<String, double[]> profiles) {
		List<XYSeries> seriesList = new ArrayList<>(profiles.size());
		profiles.forEach((name, profile) -> {
			XYSeries series = new XYSeries(name, true, false);
			timeDiscretizer.forEach((bin, time) -> series.add(((double)time) / 3600, profile[bin]));
			seriesList.add(series);
		});

		DefaultTableXYDataset dataset = new DefaultTableXYDataset();
		Lists.reverse(seriesList).forEach(dataset::addSeries);
		return dataset;
	}

	private void generateImage(DefaultTableXYDataset xyDataset, TimeProfileCharts.ChartType chartType) {
		JFreeChart chart = TimeProfileCharts.chartProfile(xyDataset, chartType);
		String runID = matsimServices.getConfig().controler().getRunId();
		if (runID != null) {
			chart.setTitle(runID + " " + chart.getTitle().getText());
		}
		makeStayTaskSeriesGrey(chart.getXYPlot());
		String imageFile = filename(OUTPUT_FILE + "_" + chartType.name());
		ChartSaveUtils.saveAsPNG(chart, imageFile, 1500, 1000);
	}

	private void makeStayTaskSeriesGrey(XYPlot plot) {
		var seriesPaints = EntryStream.of(taskTypePaints).mapKeys(Task.TaskType::name).toMap();
		XYDataset dataset = plot.getDataset(0);
		for (int i = 0; i < dataset.getSeriesCount(); i++) {
			Paint paint = seriesPaints.get((String)dataset.getSeriesKey(i));
			if (paint != null) {
				plot.getRenderer().setSeriesPaint(i, paint);
			}
		}
	}

	private String filename(String prefix) {
		return matsimServices.getControlerIO()
				.getIterationFilename(matsimServices.getIterationNumber(), prefix + "_" + mode);
	}
}
