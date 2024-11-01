package org.matsim.simwrapper.dashboard;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.application.analysis.population.TripAnalysis;
import org.matsim.application.options.CsvOptions;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.Header;
import org.matsim.simwrapper.Layout;
import org.matsim.simwrapper.viz.*;
import tech.tablesaw.plotly.components.Axis;
import tech.tablesaw.plotly.components.Line;
import tech.tablesaw.plotly.traces.BarTrace;
import tech.tablesaw.plotly.traces.ScatterTrace;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Shows trip information, optionally against reference data.
 */
public class TripDashboard implements Dashboard {

	private static final Logger log = LogManager.getLogger(TripDashboard.class);

	@Nullable
	private final String modeShareRefCsv;
	@Nullable
	private final String modeShareDistRefCsv;
	@Nullable
	private final String modeUsersRefCsv;

	@Nullable
	private String groupedRefCsv;
	@Nullable
	private String[] categories;
	@Nullable
	private String distanceRefCsv;

	private String[] args;

	private boolean choiceEvaluation;

	/**
	 * Default trip dashboard constructor.
	 */
	public TripDashboard() {
		this(null, null, null);
	}

	/**
	 * Create a dashboard containing reference data. If any of the reference data is not available it can also be null
	 * Data format needs to be the same as produced by the analysis. Please refer to the dashboard output.
	 * All given argument must be resources in the classpath.
	 *
	 * @param modeShareRefCsv     resource containing the mode share per distance group and mode, summing to a total of one
	 * @param modeShareDistRefCsv resource with mode share, where each group sums to 1.
	 * @param modeUsersRefCsv     resource with mode users data
	 */
	public TripDashboard(@Nullable String modeShareRefCsv, @Nullable String modeShareDistRefCsv, @Nullable String modeUsersRefCsv) {
		this.modeShareRefCsv = modeShareRefCsv;
		this.modeShareDistRefCsv = modeShareDistRefCsv;
		this.modeUsersRefCsv = modeUsersRefCsv;
		args = new String[0];
	}

	private static String[] detectCategories(String groupedRefCsv) {
		try {
			Character c = CsvOptions.detectDelimiter(groupedRefCsv);
			try (BufferedReader reader = IOUtils.getBufferedReader(groupedRefCsv)) {
				String header = reader.readLine();
				return Arrays.stream(header.split(String.valueOf(c)))
					.filter(s -> !s.equals("main_mode") && !s.equals("share") && !s.equals("dist_group"))
					.toArray(String[]::new);
			}

		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * This enables detailed analysis of the distance distribution.
	 */
	public TripDashboard withDistanceDistribution(String modeShareDistRefCsv) {
		this.distanceRefCsv = modeShareDistRefCsv;
		return this;
	}

	/**
	 * Set grouped reference data. Will enable additional tab with analysis for subgroups of the population.
	 *
	 * @param groupedRefCsv resource containing the grouped reference data
	 * @param categories    categories to show on dashboard, if empty all categories will be used
	 */
	public TripDashboard withGroupedRefData(String groupedRefCsv, String... categories) {
		this.groupedRefCsv = groupedRefCsv;
		if (categories.length == 0) {
			categories = detectCategories(groupedRefCsv);
			log.info("Detected categories from reference data: {}", Arrays.toString(categories));
		}
		this.categories = categories;
		return this;
	}

	/**
	 * Enable choice evaluation tab. This only produces valid data if choice reference data was set in the population.
	 *
	 * @see TripAnalysis#ATTR_REF_MODES
	 */
	public TripDashboard withChoiceEvaluation(boolean enable) {
		this.choiceEvaluation = enable;
		return this;
	}

	/**
	 * Set argument that will be passed to the analysis script. See {@link TripAnalysis}.
	 */
	public TripDashboard setAnalysisArgs(String... args) {
		this.args = args;
		return this;
	}

	@Override
	public void configure(Header header, Layout layout) {

		header.title = "Trips";
		header.description = "General information about modal share and trip distributions.";

		String[] args = new String[this.groupedRefCsv == null ? this.args.length : this.args.length + 2];
		System.arraycopy(this.args, 0, args, 0, this.args.length);

		// Add ref data to the argument if set
		if (groupedRefCsv != null) {
			args[this.args.length] = "--input-ref-data";
			args[this.args.length + 1] = groupedRefCsv;
		}

		// A tab will only be present if one of the other tabs is used as well
		String tab = (groupedRefCsv != null || choiceEvaluation) ? header.title : null;

		Layout.Row first = layout.row("first", tab);
		first.el(Plotly.class, (viz, data) -> {
			viz.title = "Modal split";

			viz.layout = tech.tablesaw.plotly.components.Layout.builder()
				.barMode(tech.tablesaw.plotly.components.Layout.BarMode.STACK)
				.build();

			Plotly.DataSet ds = viz.addDataset(data.compute(TripAnalysis.class, "mode_share.csv", args))
				.constant("source", "Simulated")
				.aggregate(List.of("main_mode"), "share", Plotly.AggrFunc.SUM);

			if (modeShareRefCsv != null) {
				viz.addDataset(data.resource(modeShareRefCsv))
					.constant("source", "Reference")
					.aggregate(List.of("main_mode"), "share", Plotly.AggrFunc.SUM);

				viz.mergeDatasets = true;
			}

			viz.addTrace(BarTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT).orientation(BarTrace.Orientation.HORIZONTAL).build(),
				ds.mapping()
					.name("main_mode")
					.y("source")
					.x("share")
			);
		});

		first.el(Plotly.class, (viz, data) -> {

			viz.title = "Trip distance distribution";
			viz.colorRamp = ColorScheme.Viridis;

			viz.addTrace(BarTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT).name("Simulated").build(),
				viz.addDataset(data.compute(TripAnalysis.class, "mode_share.csv", args))
					.aggregate(List.of("dist_group"), "share", Plotly.AggrFunc.SUM)
					.mapping()
					.x("dist_group")
					.y("share")
			);

			if (modeShareRefCsv != null) {
				viz.addTrace(BarTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT).name("Reference").build(),
					viz.addDataset(data.resource(modeShareRefCsv))
						.aggregate(List.of("dist_group"), "share", Plotly.AggrFunc.SUM)
						.mapping()
						.x("dist_group")
						.y("share")
				);
			}
		});

		layout.row("second", tab)
			.el(Table.class, (viz, data) -> {
				viz.title = "Mode Statistics";
				viz.description = "by main mode, over whole trip (including access & egress)";
				viz.dataset = data.compute(TripAnalysis.class, "trip_stats.csv", args);
				viz.showAllRows = true;
			})
			.el(Plotly.class, (viz, data) -> {

				viz.title = "Modal distance distribution";

				viz.layout = tech.tablesaw.plotly.components.Layout.builder()
					.xAxis(Axis.builder().title("Distance group").build())
					.yAxis(Axis.builder().title("Share").build())
					.barMode(tech.tablesaw.plotly.components.Layout.BarMode.STACK)
					.build();

				Plotly.DataSet sim = viz.addDataset(data.compute(TripAnalysis.class, "mode_share_per_dist.csv"))
					.constant("source", "Sim");

				viz.addTrace(BarTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT).build(),
					sim.mapping()
						.name("main_mode")
						.x("dist_group")
						.y("share")
				);

				if (modeShareDistRefCsv != null) {

					Plotly.DataSet ref = viz.addDataset(data.resource(modeShareDistRefCsv))
						.constant("source", "Ref");

					viz.multiIndex = Map.of("dist_group", "source");
					viz.mergeDatasets = true;
				}

			});

		layout.row("third", tab)
			.el(Table.class, (viz, data) -> {
				viz.title = "Population statistics";
				viz.description = "over simulated persons (not scaled by sample size)";
				viz.showAllRows = true;
				viz.dataset = data.compute(TripAnalysis.class, "population_trip_stats.csv");
			})
			.el(Plotly.class, (viz, data) -> {

				viz.title = "Mode usage";
				viz.description = "Share of persons using a main mode at least once per day";
				viz.width = 2d;

				Plotly.DataSet ds = viz.addDataset(data.compute(TripAnalysis.class, "mode_users.csv"));
				viz.addTrace(BarTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT).build(), ds.mapping()
					.x("main_mode")
					.y("user")
					.name("main_mode")
				);

				if (modeUsersRefCsv != null) {
					ds.constant("source", "sim");

					viz.addDataset(data.resource(modeUsersRefCsv))
						.constant("source", "ref");

					viz.multiIndex = Map.of("main_mode", "source");
					viz.mergeDatasets = true;
				}

			}).el(Sankey.class, (viz, data) -> {
				viz.title = "Mode shift";
				viz.width = 1.5d;
				viz.description = "by main mode. Compares initial input with output after the last iteration";
				viz.csv = data.compute(TripAnalysis.class, "mode_shift.csv", args);
			});

		createDistancePlot(layout, args, tab);

		layout.row("departures", tab).el(Plotly.class, (viz, data) -> {

			viz.title = "Departures";
			viz.description = "by hour and purpose";
			viz.layout = tech.tablesaw.plotly.components.Layout.builder()
				.xAxis(Axis.builder().title("Hour").build())
				.yAxis(Axis.builder().title("Share").build())
				.barMode(tech.tablesaw.plotly.components.Layout.BarMode.STACK)
				.build();

			viz.addTrace(BarTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT).build(),
				viz.addDataset(data.compute(TripAnalysis.class, "trip_purposes_by_hour.csv")).mapping()
					.name("purpose", ColorScheme.Spectral)
					.x("h")
					.y("departure")
			);

		});

		layout.row("arrivals", tab).el(Plotly.class, (viz, data) -> {

			viz.title = "Arrivals";
			viz.description = "by hour and purpose";
			viz.layout = tech.tablesaw.plotly.components.Layout.builder()
				.xAxis(Axis.builder().title("Hour").build())
				.yAxis(Axis.builder().title("Share").build())
				.barMode(tech.tablesaw.plotly.components.Layout.BarMode.STACK)
				.build();

			viz.addTrace(BarTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT).build(),
				viz.addDataset(data.compute(TripAnalysis.class, "trip_purposes_by_hour.csv")).mapping()
					.name("purpose", ColorScheme.Spectral)
					.x("h")
					.y("arrival")
			);

		});

		if (groupedRefCsv != null) {
			createGroupedTab(layout, args);
		}

		if (choiceEvaluation) {
			createChoiceTab(layout, args);
		}

	}

	private void createDistancePlot(Layout layout, String[] args, String tab) {

		layout.row("dist-dist", tab).el(Plotly.class, (viz, data) -> {

			viz.title = "Detailed distance distribution";
			viz.description = "by mode.";
			viz.layout = tech.tablesaw.plotly.components.Layout.builder()
				.xAxis(Axis.builder().title("Distance [m]").build())
				.yAxis(Axis.builder().title("Share").build())
				.showLegend(false)
				.build();

			viz.colorRamp = ColorScheme.Viridis;
			viz.interactive = Plotly.Interactive.dropdown;

			Plotly.DataSet ds = viz.addDataset(data.compute(TripAnalysis.class, "mode_share_distance_distribution.csv", args))
				.pivot(List.of("dist"), "main_mode", "share")
				.constant("source", "Sim");

			viz.addTrace(ScatterTrace.builder(Plotly.INPUT, Plotly.INPUT)
					.mode(ScatterTrace.Mode.LINE)
					.build(),
				ds.mapping()
					.name("main_mode")
					.x("dist")
					.y("share")
			);

			if (distanceRefCsv != null) {
				viz.description += " Dashed line represents the reference data.";

				Plotly.DataSet ref = viz.addDataset(data.resource(distanceRefCsv))
					.pivot(List.of("dist"), "main_mode", "share")
					.constant("source", "Ref");

				viz.addTrace(ScatterTrace.builder(Plotly.INPUT, Plotly.INPUT)
						.mode(ScatterTrace.Mode.LINE)
						.line(Line.builder().dash(Line.Dash.DASH).color("black").build())
						.build(),
					ref.mapping()
						.name("main_mode")
						.text("source")
						.x("dist")
						.y("share")
				);
			}

		});

	}

	private void createChoiceTab(Layout layout, String[] args) {

		layout.row("choice-intro", "Mode Choice").el(TextBlock.class, (viz, data) -> {
			viz.backgroundColor = "transparent";
			viz.content = """
				### **Information**
				Note that these metrics are based on a single run and may have limited interpretability. For a more robust evaluation, consider running multiple simulations with different seeds and use metrics that consider probabilities as well.
				(log-likelihood, Brier score, etc.)
				For policy cases, these metrics do not have any meaning. They are solely for the base-case.

				- Precision is the fraction of instances correctly classified as belonging to a specific class out of all instances the model predicted to belong to that class.
				- Recall is the fraction of instances in a class that the model correctly classified out of all instances in that class.
				- The macro-average computes the metric independently for each class and then take the average (hence treating all classes equally).
				- The micro-averages of Precision, Recall and F1 score are identical to the accuracy.
				- Cohen's Kappa is a measure of agreement between two raters that corrects for chance agreement. 1.0 indicates perfect agreement, 0.0 or less indicates agreement by chance.
				""";
		});

		layout.row("choice", "Mode Choice").el(Table.class, (viz, data) -> {
			viz.title = "Choice Evaluation";
			viz.description = "Metrics for mode choice.";
			viz.showAllRows = true;
			viz.dataset = data.compute(TripAnalysis.class, "mode_choice_evaluation.csv", args);
		});

		layout.row("choice", "Mode Choice").el(Table.class, (viz, data) -> {
			viz.title = "Choice Evaluation per Mode";
			viz.description = "Metrics for choices per mode.";
			viz.showAllRows = true;
			viz.dataset = data.compute(TripAnalysis.class, "mode_choice_evaluation_per_mode.csv", args);
		});

		layout.row("choice-plots", "Mode Choice").el(Heatmap.class, (viz, data) -> {
			viz.title = "Confusion Matrix";
			viz.description = "Share of (mis)classified modes.";
			viz.xAxisTitle = "Predicted";
			viz.yAxisTitle = "True";
			viz.y = "True/Pred";
			viz.flipAxes = false;
			viz.showLabels = true;
			viz.dataset = data.compute(TripAnalysis.class, "mode_confusion_matrix.csv", args);
		});

		layout.row("choice-plots", "Mode Choice").el(Plotly.class, (viz, data) -> {
			viz.title = "Mode Prediction Error";
			viz.description = "Plot showing the number of (mis)classified modes.";

			viz.layout = tech.tablesaw.plotly.components.Layout.builder()
				.xAxis(Axis.builder().title("True mode").build())
				.yAxis(Axis.builder().title("Predicted mode count").build())
				.barMode(tech.tablesaw.plotly.components.Layout.BarMode.STACK)
				.build();

			Plotly.DataMapping ds = viz.addDataset(data.compute(TripAnalysis.class, "mode_prediction_error.csv", args))
				.mapping()
				.x("true_mode")
				.y("count")
				.name("predicted_mode");

			viz.addTrace(BarTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT).build(), ds);
		});
	}

	private void createGroupedTab(Layout layout, String[] args) {

		for (String cat : Objects.requireNonNull(categories, "Categories not set")) {

			layout.row("category_header_" + cat, "By Groups")

				.el(TextBlock.class, (viz, data) -> {
					viz.content = "## **" + StringUtils.capitalize(cat) + "**";
					viz.backgroundColor = "transparent";
				});

			layout.row("category_1_" + cat, "By Groups")
				.el(Plotly.class, (viz, data) -> {

					viz.title = "Mode share";
					viz.description = "by " + cat;
					viz.height = 6d;
					viz.layout = tech.tablesaw.plotly.components.Layout.builder()
						.barMode(tech.tablesaw.plotly.components.Layout.BarMode.STACK)
						.build();

					Plotly.DataMapping ds = viz.addDataset(data.computeWithPlaceholder(TripAnalysis.class, "mode_share_per_%s.csv", cat))
						.pivot(List.of("main_mode", "dist_group", cat), "source", "share")
						.aggregate(List.of("main_mode", "source", cat), "share", Plotly.AggrFunc.SUM)
						.rename("sim_share", "Sim")
						.rename("ref_share", "Ref")
						.mapping()
						.facetCol(cat)
						.name("main_mode")
						.x("source")
						.y("share");


					viz.addTrace(BarTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT)
						.orientation(BarTrace.Orientation.VERTICAL)
						.build(), ds);

				});

			layout.row("category_2_" + cat, "By Groups")

				.el(Plotly.class, (viz, data) -> {
					viz.title = "Modal distance distribution";
					viz.description = "by " + cat;
					viz.layout = tech.tablesaw.plotly.components.Layout.builder()
						.xAxis(Axis.builder().title("Distance group").build())
						.yAxis(Axis.builder().title("Share").build())
						.barMode(tech.tablesaw.plotly.components.Layout.BarMode.STACK)
						.build();

					viz.interactive = Plotly.Interactive.dropdown;

					Plotly.DataMapping ds = viz.addDataset(data.computeWithPlaceholder(TripAnalysis.class, "mode_share_per_%s.csv", cat))
						.pivot(List.of("main_mode", "dist_group", cat), "source", "share")
						.normalize(List.of("dist_group", "source", cat), "share")
						.rename("sim_share", "Sim")
						.rename("ref_share", "Ref")
						.mapping()
						.name("main_mode")
						.facetCol(cat)
						.x("dist_group")
						.y("share");

					viz.multiIndex = Map.of("dist_group", "source");

					viz.addTrace(BarTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT)
						.orientation(BarTrace.Orientation.VERTICAL)
						.build(), ds);

				});

		}
	}

}
