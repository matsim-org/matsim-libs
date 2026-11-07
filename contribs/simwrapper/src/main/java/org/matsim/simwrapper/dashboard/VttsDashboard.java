package org.matsim.simwrapper.dashboard;

import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.Header;
import org.matsim.simwrapper.Layout;
import org.matsim.simwrapper.SimWrapperConfigGroup;
import org.matsim.simwrapper.viz.Plotly;
import org.matsim.simwrapper.viz.Table;
import org.matsim.simwrapper.viz.TextBlock;
import org.matsim.simwrapper.viz.Tile;
import tech.tablesaw.plotly.components.Axis;
import tech.tablesaw.plotly.traces.HistogramTrace;

/** Displays single-run VTTS CSVs produced with the scenario's own scoring setup. */
public final class VttsDashboard implements Dashboard {

	public static final String DEFAULT_DIRECTORY = "analysis/vtts";
	private final String directory;
	private final String populationDescription;

	public VttsDashboard() {
		this(DEFAULT_DIRECTORY);
	}

	/** Path relative to the run output; all CSVs follow the schema documented in the VSP VTTS README. */
	public VttsDashboard(String directory) {
		this(directory, "Population selection is defined by the scenario's VTTS module.");
	}

	/** The producing module supplies its population rule, including any scenario-specific filter. */
	public VttsDashboard(String directory, String populationDescription) {
		if (directory == null || directory.isBlank())
			throw new IllegalArgumentException("A VTTS data directory is required");
		if (populationDescription == null || populationDescription.isBlank())
			throw new IllegalArgumentException("A description of the included population is required");
		this.directory = directory;
		this.populationDescription = populationDescription;
	}

	@Override
	public void configure(Header header, Layout layout, SimWrapperConfigGroup configGroup) {
		// These files are produced during the run by the VSP module. data.output() references
		// existing files without registering a command that would reconstruct the scoring setup.
		header.title = "Value of travel time savings";
		header.description = "Activity and person values from the run's scoring setup. " + populationDescription;
		// This dashboard is explicitly enabled by the scenario. Leave triggerPattern unset:
		// SimWrapper matches it against filenames in the current folder, not nested CSV paths.
		layout.row("overview", "Per person").el(Tile.class, (viz, data) -> {
			viz.dataset = data.output(directory, "vtts_overview.csv");
			viz.height = 0.1;
		});
		histogram(layout.row("vtts", "Per person"), "VTTS", "mean_vtts_money_per_hour", "Money/hour", false);
		histogram(layout.row("muse", "Per person"), "Marginal utility of starting earlier (MUSE)", "mean_muse_score_per_hour", "Score/hour", false);
		histogram(layout.row("vse", "Per person"), "Value of starting earlier (VSE)", "mean_vse_money_per_hour", "Money/hour", false);
		layout.row("statistics", "Per person").el(Table.class, (viz, data) -> {
			viz.title = "Summary statistics";
			viz.dataset = data.output(directory, "vtts_summary.csv");
			viz.showAllRows = true;
		}).el(Table.class, (viz, data) -> {
			viz.title = "Person coverage";
			viz.dataset = data.output(directory, "vtts_coverage.csv");
			viz.showAllRows = true;
		});
		layout.row("method", "Per person").el(TextBlock.class, (viz, data) -> {
			viz.title = "Interpretation";
			viz.content = "Per person plots contain one mean per person. Per trip plots contain one value per trip, "
				+ "so persons with more trips contribute more observations there. Missing values are omitted separately for each metric. "
				+ "MUSE is the activity score gain from starting "
				+ "one second earlier with the end time fixed. VSE divides MUSE by the person's marginal utility of money. "
				+ "VTTS also includes one second less traveling in the realized trip's analysis main mode. PT values "
				+ "concern in-vehicle time, not waiting or access time. Money/hour uses the run's monetary unit. "
				+ "Counts describe the simulated sample without expansion. Summary rows distinguish person and trip "
				+ "weighting. Person/trip exports contain coverage statuses. Custom scorers must support the KN activity calculation "
				+ "and supply compatible linear travel/money conversion parameters.";
		});
		// Reuse the overview, statistics and interpretation; only the plotted observations change.
		layout.tab("Per trip").add("overview");
		histogram(layout.row("trip_vtts", "Per trip"), "VTTS", "vtts_money_per_hour", "Money/hour", true);
		histogram(layout.row("trip_muse", "Per trip"), "Marginal utility of starting earlier (MUSE)", "muse_score_per_hour", "Score/hour", true);
		histogram(layout.row("trip_vse", "Per trip"), "Value of starting earlier (VSE)", "vse_money_per_hour", "Money/hour", true);
		layout.tab("Per trip").add("statistics").add("method");
	}

	private void histogram(Layout.Row row, String title, String column, String unit, boolean perTrip) {
		row.el(Plotly.class, (viz, data) -> {
			viz.title = title;
			viz.description = perTrip ? "Distribution of trip values; each trip has equal weight"
				: "Distribution of person means; each person has equal weight";
			viz.layout = tech.tablesaw.plotly.components.Layout.builder()
				.xAxis(Axis.builder().title(unit).build())
				.yAxis(Axis.builder().title(perTrip ? "Trips" : "Persons").build()).build();
			var dataset = viz.addDataset(data.output(directory, perTrip ? "vtts_trips.csv.gz" : "vtts_persons.csv.gz"))
				// Missing conversion results are not zero-valued observations. Keep genuine zero
				// and negative values; omit only empty/null cells in the metric being plotted.
				.filterNotIn(column, "", null);
			viz.addTrace(HistogramTrace.builder(Plotly.INPUT).build(), dataset.mapping().x(column));
		});
	}
}
