package org.matsim.simwrapper.dashboard;

import org.matsim.application.analysis.traffic.traveltime.TravelTimeComparison;
import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.Header;
import org.matsim.simwrapper.Layout;
import org.matsim.simwrapper.viz.Plotly;
import tech.tablesaw.plotly.components.Axis;
import tech.tablesaw.plotly.components.Line;
import tech.tablesaw.plotly.traces.ScatterTrace;

/**
 * Compares travel time from simulation with reference data.
 */
public class TravelTimeComparisonDashboard implements Dashboard {

	private final String refData;

	/**
	 * Constructor, which needs the path to the reference files produces by {@link org.matsim.application.analysis.traffic.traveltime.SampleValidationRoutes}.
	 */
	public TravelTimeComparisonDashboard(String refData) {
		this.refData = refData;
	}

	@Override
	public double priority() {
		return -1;
	}

	@Override
	public void configure(Header header, Layout layout) {

		header.title = "Travel time";
		header.description = "Comparison of simulated travel times vs. results from routing services.";

		layout.row("first")
			.el(Plotly.class, (viz, data) -> {

				viz.title = "Travel time comparison";
				viz.description = "by hour";
				viz.fixedRatio = true;
				viz.height = 8d;

				viz.interactive = Plotly.Interactive.slider;
				viz.layout = tech.tablesaw.plotly.components.Layout.builder()
					.xAxis(Axis.builder().title("Observed historical mean speed [km/h]").build())
					.yAxis(Axis.builder().title("Simulated mean speed [km/h]").build())
					.build();

				Plotly.DataSet ds = viz.addDataset(data.compute(TravelTimeComparison.class, "travel_time_comparison_by_route.csv", "--input-ref", refData));

				viz.addTrace(ScatterTrace.builder(Plotly.INPUT, Plotly.INPUT).build(), ds.mapping()
					.x("mean")
					.y("simulated")
					.text("from_node")
					.name("hour")
				);

			}).el(Plotly.class, ((viz, data) -> {

				viz.title = "Avg. Speed";
				viz.description = "by hour";

				Plotly.DataSet ds = viz.addDataset(data.compute(TravelTimeComparison.class, "travel_time_comparison_by_hour.csv", "--input-ref", refData));

				viz.layout = tech.tablesaw.plotly.components.Layout.builder()
					.xAxis(Axis.builder().title("Hour").build())
					.yAxis(Axis.builder().title("Speed [km/h]").build())
					.build();

				viz.addTrace(ScatterTrace.builder(Plotly.INPUT, Plotly.INPUT)
					.name("Mean")
					.mode(ScatterTrace.Mode.LINE)
					.line(Line.builder().dash(Line.Dash.LONG_DASH_DOT).build()).build(), ds.mapping()
					.x("hour").y("mean")
				);

				viz.addTrace(ScatterTrace.builder(Plotly.INPUT, Plotly.INPUT)
					.name("Min")
					.mode(ScatterTrace.Mode.LINE)
					.line(Line.builder().dash(Line.Dash.DASH).build()).build(), ds.mapping()
					.x("hour").y("min")
				);

				viz.addTrace(ScatterTrace.builder(Plotly.INPUT, Plotly.INPUT)
					.name("Max")
					.mode(ScatterTrace.Mode.LINE)
					.line(Line.builder().dash(Line.Dash.DASH).build()).build(), ds.mapping()
					.x("hour").y("max")
				);

				viz.addTrace(ScatterTrace.builder(Plotly.INPUT, Plotly.INPUT)
					.name("Simulated")
					.mode(ScatterTrace.Mode.LINE)
					.line(Line.builder().dash(Line.Dash.SOLID).build()).build(), ds.mapping()
					.x("hour").y("simulated")
				);

			})).el(Plotly.class, ((viz, data) -> {

				viz.title = "Error and bias";
				viz.description = "by hour";

				Plotly.DataSet ds = viz.addDataset(data.compute(TravelTimeComparison.class, "travel_time_comparison_by_hour.csv", "--input-ref", refData));

				viz.layout = tech.tablesaw.plotly.components.Layout.builder()
					.xAxis(Axis.builder().title("Hour").build())
					.yAxis(Axis.builder().title("Speed [km/h]").build())
					.build();

				viz.addTrace(ScatterTrace.builder(Plotly.INPUT, Plotly.INPUT)
					.name("Mean abs. error")
					.mode(ScatterTrace.Mode.LINE)
					.line(Line.builder().dash(Line.Dash.SOLID).build()).build(), ds.mapping()
					.x("hour").y("abs_error")
				);

				viz.addTrace(ScatterTrace.builder(Plotly.INPUT, Plotly.INPUT)
					.name("Ref. std.")
					.mode(ScatterTrace.Mode.LINE)
					.line(Line.builder().dash(Line.Dash.LONG_DASH_DOT).build()).build(), ds.mapping()
					.x("hour").y("std")
				);

				viz.addTrace(ScatterTrace.builder(Plotly.INPUT, Plotly.INPUT)
					.name("Bias")
					.mode(ScatterTrace.Mode.LINE)
					.line(Line.builder().dash(Line.Dash.SOLID).build()).build(), ds.mapping()
					.x("hour").y("bias")
				);

			}));
	}
}
