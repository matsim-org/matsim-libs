package org.matsim.simwrapper.dashboard;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.application.analysis.impact.ImpactAnalysis;
import org.matsim.application.analysis.population.TripAnalysis;
import org.matsim.application.analysis.scenarioComparison.ScenarioComparisonAnalysis;
import org.matsim.simwrapper.*;
import org.matsim.simwrapper.viz.*;
import tech.tablesaw.plotly.components.Axis;
import tech.tablesaw.plotly.components.Line;
import tech.tablesaw.plotly.traces.BarTrace;
import tech.tablesaw.plotly.traces.ScatterTrace;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.IntStream;

public class ScenarioComparisonDashboard implements Dashboard {

	private Map<String, String> comparisonScenarios;
	private static final Logger log = LogManager.getLogger(ScenarioComparisonDashboard.class);
	private final ArrayList<ComparisonDashboards> comparisonDashboards;
	Map<ScenarioComparisonDashboard.ComparisonDashboards, Object> comparisonDashboardArgs;

	private String [] argsTripAnalysis;
	public enum AnalysisTypeArgs {
		COMMERCIAL,
		TRIP,
		BOTH
	}

	public enum ComparisonDashboards {
		Trips, ImpactAnalysis
	}

	/**
	 * Default scenario comparison dashboard constructor.
	 */
	// add logic for whcih types of comparison dashboard - right now, it's only trips, so don't need boolean tripsComparison
	public ScenarioComparisonDashboard(Map<String, String> comparisonScenarios, ArrayList<ComparisonDashboards> comparisonDashboards, Map<ScenarioComparisonDashboard.ComparisonDashboards, Object> comparisonDashboardArgs) {
//		this.tripsComparison  = tripsComparison;
		this.comparisonScenarios = comparisonScenarios;
		this.comparisonDashboards = comparisonDashboards;
		this.comparisonDashboardArgs = comparisonDashboardArgs;
	}


	// Below was added to accommodate changes to TripDashboard/TripAnalysis that broke the scenarioComparison.
	/**
	 * Reuses already registered {@link TripAnalysis} args if another dashboard configured the command first
	 * and appends missing commercial args. Registered args stay authoritative because the TripDashboard
	 * usually defines the more specific TripAnalysis setup. We also need to update the registered args
	 * in the current {@link Data} context, otherwise later compute calls would still see the old,
	 * incomplete command line and fail with conflicting args.
	 */
	private String[] resolveTripAnalysisArgs(Data data) {
		String[] registeredArgs = data.getArgs(TripAnalysis.class);
		String[] mergedArgs = DashboardUtils.mergeArgsPreferBase(
			registeredArgs.length > 0 ? registeredArgs : argsTripAnalysis,
			registeredArgs.length > 0 ? argsTripAnalysis : new String[0]
		);
		if (registeredArgs.length > 0 && !Arrays.equals(registeredArgs, mergedArgs)) {
			data.setArgs(TripAnalysis.class, mergedArgs);
			log.info("TripAnalysis was already registered with args {}. Appending missing CommercialTrafficDashboard args {} -> {}.",
				Arrays.toString(registeredArgs), Arrays.toString(argsTripAnalysis), Arrays.toString(mergedArgs));
		}
		return mergedArgs;
	}


	@Override
	public void configure(Header header, Layout layout, SimWrapperConfigGroup configGroup) {

		header.title = "Scenario Comparison: Policy to Base Case";
		header.description = "Shows the differences in a variety of metrics between the policy and base case.";
		if (this.comparisonDashboards.contains(ComparisonDashboards.Trips)) {
			createTripsComparisonTab(header, layout, configGroup);
		}

		String impactAnalysisTab = "Impact Analysis Comparison";

		if (this.comparisonDashboards.contains(ComparisonDashboards.ImpactAnalysis))
		{

			Collection<String> modes = (Collection<String>) this.comparisonDashboardArgs.get(ComparisonDashboards.ImpactAnalysis);

			List<String> modeArgs = new ArrayList<>(List.of("--modes", String.join(",", modes)));

			String[] modeArgsArray = modeArgs.toArray(new String[0]);

			modes.forEach(mode -> {
				layout.row(mode, impactAnalysisTab)
					.el(Table.class, (viz, data) -> {
						viz.title = "Central Traffic / Physical Effects (" + mode.substring(0, 1).toUpperCase() + mode.substring(1) + ")";
						viz.style = "topsheet";
						viz.dataset = data.compute(ScenarioComparisonAnalysis.class, "general_impact_comparison.csv",
							"--input-comp-paths=" + String.join(",", this.comparisonScenarios.values()),
							"--input-comp-names=" + String.join(",", this.comparisonScenarios.keySet()),
							"--input-comp-args=" + String.join(",", ""));
						viz.enableFilter = false;
						viz.showAllRows = true;
						viz.width = 1d;
						viz.height = 5d;
//						viz.alignment = new String[]{"right", "right", "left"};
					})

					.el(Table.class, (viz, data) -> {
						viz.title = "Change In Exhaust Emissions (" + mode.substring(0, 1).toUpperCase() + mode.substring(1) + ")";
						viz.style = "topsheet";
						viz.dataset = data.compute(ScenarioComparisonAnalysis.class, "emissions_comparison.csv",
							"--input-comp-paths=" + String.join(",", this.comparisonScenarios.values()),
							"--input-comp-names=" + String.join(",", this.comparisonScenarios.keySet()),
							"--input-comp-args=" + String.join(",", ""));
						viz.enableFilter = false;
						viz.showAllRows = true;
						viz.width = 1d;
						viz.height = 5d;
//						viz.alignment = new String[]{"right", "right", "left"};
					});
			});

			layout.row("disclaimer", impactAnalysisTab)
				.el(TextBlock.class, (viz, data) -> {
					viz.backgroundColor = "white";
					viz.content = """
					# Disclaimer

					Die in dieser Analyse verwendeten Verkehrsdaten basieren auf einer Hochrechnung von Verkehrszahlen eines einzelnen Tages auf ein gesamtes Jahr. Die Grundlage dieser Hochrechnung stammt aus dem Bericht: \s

					**Grundsätzliche Überprüfung und Weiterentwicklung der Nutzen-Kosten-Analyse im Bewertungsverfahren der Bundesverkehrswegeplanung** \s
					FE-PROJEKTNR.: 960974/2011 \s
					Endbericht für das Bundesministerium für Verkehr und digitale Infrastruktur \s
					Essen, Berlin, München, 24. März 2015 \s

					Die spezifischen Hochrechnungsfaktoren wurden wie folgt angewendet: \s
					- Für PKW und alle anderen Verkehrsmittel: **Faktor 334** \s
					- Für LKW: **Faktor 302** \s

					Diese Faktoren basieren auf den Angaben des genannten Berichts (Seite 172) und wurden ebenfalls im Rahmen des Bundesverkehrswegeplans 2030 verwendet. \s

					# Disclaimer (English)

					The traffic data used in this analysis is based on an extrapolation of single-day traffic figures to an entire year. The basis for this extrapolation is derived from the report: \s

					**Fundamental Review and Further Development of the Cost-Benefit Analysis in the Assessment Procedure of Federal Transport Infrastructure Planning** \s
					FE-PROJECT NO.: 960974/2011 \s
					Final Report for the Federal Ministry of Transport and Digital Infrastructure \s
					Essen, Berlin, Munich, March 24, 2015 \s

					The specific extrapolation factors applied are as follows: \s
					- For passenger cars (PKW) and all other modes of transport: **Factor 334** \s
					- For trucks (LKW): **Factor 302** \s

					These factors are based on the data provided in the mentioned report (page 172) and were also used in the Federal Transport Infrastructure Plan 2030. \s
					""";
				});
		}
		}

	private void createTripsComparisonTab(Header header, Layout layout, SimWrapperConfigGroup configGroup) {

		String tab = "Trips Comparison";

		Layout.Row first = layout.row("first", tab);
		first.el(Plotly.class, (viz, data) -> {
			viz.title = "Mode Split";
//				viz.width = 2d;

			for (Map.Entry<String, String> compScenario : this.comparisonScenarios.entrySet()) {
				Path source = Path.of(compScenario.getValue() + "/analysis/population/mode_share.csv");
				Path alias = source.getParent().resolve("mode_share_" + "scenarioComp_" + compScenario.getKey() + ".csv");
				try {
					Files.copy(source, alias, StandardCopyOption.REPLACE_EXISTING);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
				viz.addTrace(BarTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT).name(compScenario.getKey()).build(),
					viz.addDataset(data.resource(alias.toString()))
						.aggregate(List.of("main_mode"), "share_total", Plotly.AggrFunc.SUM)
						.mapping()
						.x("main_mode")
						.y("share_total")
				);
			}

			viz.addTrace(BarTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT).name("sim").build(),
				viz.addDataset(data.compute(TripAnalysis.class, "mode_share.csv"))
					.aggregate(List.of("main_mode"), "share_total", Plotly.AggrFunc.SUM)
					.mapping()
					.x("main_mode")
					.y("share_total")
			);
		});

		first.el(Plotly.class, (viz, data) -> {

			viz.title = "Trip distance distribution";
			viz.colorRamp = ColorScheme.Viridis;

			for (Map.Entry<String, String> compScenario : this.comparisonScenarios.entrySet())
			{
				Path source = Path.of(compScenario.getValue() + "/analysis/population/mode_share.csv");
				Path alias = source.getParent().resolve("mode_share_" + "scenarioComp_" + compScenario.getKey() + ".csv");
				try {
					Files.copy(source, alias, StandardCopyOption.REPLACE_EXISTING);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
				viz.addTrace(BarTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT).name(compScenario.getKey()).build(),
					viz.addDataset(data.resource(alias.toString())).constant("source", compScenario.getKey())
						.aggregate(List.of("dist_group"), "share_total", Plotly.AggrFunc.SUM)
						.mapping()
						.x("dist_group")
						.y("share_total")
				);
				viz.multiIndex = Map.of("dist_group", "source");
			}

			viz.addTrace(BarTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT).name("sim").build(),
				viz.addDataset(data.compute(TripAnalysis.class, "mode_share.csv")).constant("source", "sim")
					.aggregate(List.of("dist_group"), "share_total", Plotly.AggrFunc.SUM)
					.mapping()
					.x("dist_group")
					.y("share_total")
			);
		});


		layout.row("second", tab)
			.el(Table.class, (viz, data) -> {
				viz.title = "Mode Statistics";
				viz.description = "by main mode, over whole trip (including access & egress)";
				viz.dataset = data.compute(ScenarioComparisonAnalysis.class, "trip_stats_comparison.csv",
					"--input-comp-paths=" + String.join(",", this.comparisonScenarios.values()),
					"--input-comp-names=" + String.join(",", this.comparisonScenarios.keySet()),
					"--input-comp-args=" + String.join(",", "")
					);
				// we can't find out how many columns are in the final datset, so setting this to high number as a hacky solution.
				viz.alignment = IntStream.range(0, 100)
					.mapToObj(i -> i == 0 ? "left" : "right")
					.toArray(String[]::new);
				viz.showAllRows = true;
			});

		createModeDistanceBarPlot(layout, tab);

		layout.row("third", tab)
			.el(Table.class, (viz, data) -> {
				viz.title = "Population statistics";
				viz.description = "over simulated persons (not scaled by sample size)";
				viz.dataset = data.compute(ScenarioComparisonAnalysis.class, "population_trip_stats_comparison.csv",
					"--input-comp-paths=" + String.join(",", this.comparisonScenarios.values()),
					"--input-comp-names=" + String.join(",", this.comparisonScenarios.keySet()),
					"--input-comp-args=" + String.join(",", ""));
				// we can't find out how many columns are in the final datset, so setting this to high number as a hacky solution.
				viz.alignment = IntStream.range(0, 100)
					.mapToObj(i -> i == 0 ? "left" : "right")
					.toArray(String[]::new);
				viz.showAllRows = true;
			})

			.el(Plotly.class, (viz, data) -> {

				viz.title = "Mode usage";
				viz.description = "Share of persons using a main mode at least once per day";
				viz.width = 2d;

				Plotly.DataSet ds = viz.addDataset(data.compute(TripAnalysis.class, "mode_users.csv"));

				for   (Map.Entry<String, String> compScenario : this.comparisonScenarios.entrySet())
				{

					Path source = Path.of(compScenario.getValue() + "/analysis/population/mode_users.csv");
					Path alias = source.getParent().resolve("mode_users_" + "scenarioComp_" + compScenario.getKey() + ".csv");
					try {
						Files.copy(source, alias, StandardCopyOption.REPLACE_EXISTING);
					} catch (IOException e) {
						throw new RuntimeException(e);
					}

					viz.addTrace(BarTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT).name(compScenario.getKey()).build(),
						viz.addDataset(data.resource(alias.toString())).mapping()
							.x("main_mode")
							.y("user")
					);
				}

				viz.addTrace(BarTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT).name("sim").build(), ds.mapping()
					.x("main_mode")
					.y("user")

				);
			});

		createDistancePlot(layout, tab);

	}

	//	priority is set to a lower number in order to force this class to be executed after population and emissions folders are already generated
	@Override
	public double priority() {
		return -2;
	}

	private void createDistancePlot(Layout layout, String tab) {

		layout.row("dist-dist", tab).el(Plotly.class, (viz, data) -> {

			viz.title = "Detailed distance distribution";
			viz.description = "by mode.";
			viz.layout = tech.tablesaw.plotly.components.Layout.builder()
				.xAxis(Axis.builder().title("Distance [m]").build())
				.yAxis(Axis.builder().title("share_total").build())
				.build();

			viz.colorRamp = ColorScheme.Viridis;
			viz.interactive = Plotly.Interactive.dropdown;

//			Plotly.DataSet ds = viz.addDataset(data.compute(TripAnalysis.class, "mode_share_distance_distribution.csv"))
			Plotly.DataSet ds = viz.addDataset(data.computeWithPlaceholder(TripAnalysis.class, "mode_share_distance_distribution_%s.csv", "total", resolveTripAnalysisArgs(data)))
				.pivot(List.of("dist"), "main_mode", "share_total")
				.constant("source", "Sim");

				for (Map.Entry<String, String> compScenario : this.comparisonScenarios.entrySet()) {

					Path source = Path.of(compScenario.getValue() + "/analysis/population/mode_share_distance_distribution_total.csv");
					Path alias = source.getParent().resolve("mode_share_distance_distribution_total_" + "scenarioComp_" + compScenario.getKey() + ".csv");
					try {
						Files.copy(source, alias, StandardCopyOption.REPLACE_EXISTING);
					} catch (IOException e) {
						throw new RuntimeException(e);
					}

					Plotly.DataSet ref = viz.addDataset(data.resource(alias.toString()))
						.pivot(List.of("dist"), "main_mode", "share_total")
						.constant("source", compScenario.getKey());

					viz.addTrace(ScatterTrace.builder(Plotly.INPUT, Plotly.INPUT).name(compScenario.getKey())
							.mode(ScatterTrace.Mode.LINE)
							.line(tech.tablesaw.plotly.components.Line.builder().dash(Line.Dash.DASH).build())
							.build(),
						ref.mapping().name("main_mode").x("dist").y("share_total")
					);
				}

			viz.addTrace(ScatterTrace.builder(Plotly.INPUT, Plotly.INPUT).name("sim")
				.showLegend(true)
				.mode(ScatterTrace.Mode.LINE).build(), ds.mapping().name("main_mode")
				.x("dist")
				.y("share_total")
			);
		});
	}


	private void createModeDistanceBarPlot(Layout layout, String tab) {

		layout.row("second", tab).el(Plotly.class, (viz, data) -> {

			viz.title = "Distance group";
			viz.description = "by mode.";
			viz.layout = tech.tablesaw.plotly.components.Layout.builder()
				.xAxis(Axis.builder().title("Distance Groups [m]").build())
				.yAxis(Axis.builder().title("share_total").build())
				.build();

			viz.colorRamp = ColorScheme.Viridis;
			viz.interactive = Plotly.Interactive.dropdown;

			Plotly.DataSet ds = viz.addDataset(data.compute(TripAnalysis.class, "mode_share_per_dist.csv"))
//				.pivot(List.of("dist_group"), "main_mode", "share")
				.constant("source", "Sim");

			for (Map.Entry<String, String> compScenario : this.comparisonScenarios.entrySet()) {

				Path source = Path.of(compScenario.getValue() + "/analysis/population/mode_share_per_dist.csv");
				Path alias = source.getParent().resolve("mode_share_per_dist_" + "scenarioComp_" + compScenario.getKey() + ".csv");
				try {
					Files.copy(source, alias, StandardCopyOption.REPLACE_EXISTING);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}

				Plotly.DataSet ref = viz.addDataset(data.resource(alias.toString()))
//					.pivot(List.of("dist_group"), "main_mode", "share")
					.constant("source", compScenario.getKey());

				viz.addTrace(BarTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT).name(compScenario.getKey()).build(),
					ref.mapping().name("main_mode").x("dist_group").y("share_total")
				);
			}

			viz.addTrace(BarTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT).name("sim")
				.showLegend(true).build(),
				ds.mapping().name("main_mode").x("dist_group").y("share_total")
			);
		});
	}
}
