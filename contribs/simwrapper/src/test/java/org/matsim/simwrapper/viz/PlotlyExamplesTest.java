package org.matsim.simwrapper.viz;

import com.fasterxml.jackson.databind.ObjectWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.testcases.MatsimTestUtils;
import tech.tablesaw.api.IntColumn;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;
import tech.tablesaw.plotly.api.Heatmap;
import tech.tablesaw.plotly.api.TimeSeriesPlot;
import tech.tablesaw.plotly.components.Figure;
import tech.tablesaw.plotly.components.Layout;
import tech.tablesaw.plotly.traces.HistogramTrace;
import tech.tablesaw.plotly.traces.PieTrace;
import tech.tablesaw.plotly.traces.Scatter3DTrace;

import java.io.File;
import java.io.IOException;

import static org.matsim.simwrapper.viz.PlotlyTest.createWriter;
import static tech.tablesaw.plotly.traces.HistogramTrace.HistFunc.COUNT;

/**
 * Examples taken from <a href="https://github.com/jtablesaw/tablesaw/tree/master/jsplot/src/test/java/tech/tablesaw/examples">here</a>
 */
public class PlotlyExamplesTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	private ObjectWriter writer;

	@BeforeEach
	public void setUp() throws Exception {
		writer = createWriter();
	}

	@Test
	void pie() throws IOException {

		String[] modes = {"car", "bike", "pt", "ride", "walk"};
		double[] shares = {0.2, 0.15, 0.25, 0.1, 0.3};

		Figure figure = new Figure(PieTrace.builder(modes, shares).build());

		Plotly plot = new Plotly(figure);

		writer.writeValue(new File(utils.getOutputDirectory(), "plotly-pie.yaml"), plot);
	}


	@Test
	void timeSeries() throws IOException {
		Table bush = Table.read().csv(new File(utils.getClassInputDirectory(), "bush.csv"));

		Figure figure = TimeSeriesPlot.create("George W. Bush approval ratings", bush, "date", "approval", "who");

		Plotly plot = new Plotly(figure);

		writer.writeValue(new File(utils.getOutputDirectory(), "plotly-timeseries.yaml"), plot);
	}

	@Test
	void hist() throws IOException {

		Table test = Table.create(
				StringColumn.create("type").append("apples").append("apples").append("apples").append("oranges").append("bananas"),
				IntColumn.create("num").append(5).append(10).append(3).append(10).append(5));

		Layout layout1 = Layout.builder().title("Histogram COUNT Test (team batting averages)").build();
		HistogramTrace trace = HistogramTrace.
				builder(test.stringColumn("type"), test.intColumn("num"))
				.histFunc(COUNT)
				.build();

		Plotly plot = new Plotly(layout1, trace);

		writer.writeValue(new File(utils.getOutputDirectory(), "plotly-hist.yaml"), plot);
	}

	@Test
	void heatmap() throws IOException {

		Table table = Table.read().csv(new File(utils.getClassInputDirectory(), "bush.csv"));

		StringColumn yearsMonth = table.dateColumn("date").yearMonth();
		String name = "Year and month";
		yearsMonth.setName(name);
		table.addColumns(yearsMonth);

		Figure heatmap = Heatmap.create("Polls conducted by year and month", table, name, "who");

		Plotly plot = new Plotly(heatmap);

		writer.writeValue(new File(utils.getOutputDirectory(), "plotly-heatmap.yaml"), plot);

	}

	@Test
	void scatter() throws IOException {

		final double[] x = {1, 2, 3, 4, 5, 6};
		final double[] y = {0, 1, 6, 14, 25, 39};
		final double[] z = {-23, 11, -2, -7, 0.324, -11};
		final String[] labels = {"apple", "bike", "car", "dog", "elephant", "fox"};

		Scatter3DTrace trace = Scatter3DTrace.builder(x, y, z).text(labels).build();

		Plotly plot = new Plotly(null, trace);
		writer.writeValue(new File(utils.getOutputDirectory(), "plotly-scatter3d.yaml"), plot);

	}
}
