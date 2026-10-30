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

	public VttsDashboard() {
		this(DEFAULT_DIRECTORY);
	}

	/** Path relative to the run output; all CSVs follow the schema documented in the VSP VTTS README. */
	public VttsDashboard(String directory) {
		if (directory == null || directory.isBlank())
			throw new IllegalArgumentException("A VTTS data directory is required");
		this.directory = directory;
	}

	@Override
	public void configure(Header header, Layout layout, SimWrapperConfigGroup configGroup) {
		// These files are produced during the run by the VSP module. data.output() references
		// existing files without registering a command that would reconstruct the scoring setup.
		header.title = "Value of travel time savings";
		header.description = "Activity and person values from the run's scoring setup.";
		header.triggerPattern = directory + "/vtts_persons.csv.gz";
		layout.row("overview").el(Tile.class, (viz, data) -> {
			viz.dataset = data.output(directory, "vtts_overview.csv");
			viz.height = 0.1;
		});
		histogram(layout, "vtts", "VTTS", "mean_vtts_money_per_hour", "Money/hour");
		histogram(layout, "muse", "Marginal utility of starting earlier (MUSE)", "mean_muse_score_per_hour", "Score/hour");
		histogram(layout, "vse", "Value of starting earlier (VSE)", "mean_vse_money_per_hour", "Money/hour");
		layout.row("statistics").el(Table.class, (viz, data) -> {
			viz.title = "Summary statistics";
			viz.dataset = data.output(directory, "vtts_summary.csv");
			viz.showAllRows = true;
		}).el(Table.class, (viz, data) -> {
			viz.title = "Person coverage";
			viz.dataset = data.output(directory, "vtts_coverage.csv");
			viz.showAllRows = true;
		});
		layout.row("method").el(TextBlock.class, (viz, data) -> {
			viz.title = "Interpretation";
			viz.content = "Each histogram contains one mean per person. MUSE is the activity score gain from starting "
				+ "one second earlier with the end time fixed. VSE divides MUSE by the person's marginal utility of money. "
				+ "VTTS also includes one second less traveling in the realized trip's analysis main mode. PT values "
				+ "concern in-vehicle time, not waiting or access time. Money/hour uses the run's monetary unit. "
				+ "Counts describe the simulated sample without expansion. Summary rows distinguish person and destination "
				+ "weighting. Person/trip exports contain coverage statuses. Custom scorers must support the KN activity calculation "
				+ "and supply compatible linear travel/money conversion parameters.";
		});
	}

	private void histogram(Layout layout, String row, String title, String column, String unit) {
		layout.row(row).el(Plotly.class, (viz, data) -> {
			// Use the person export: each person contributes one destination mean to the histogram.
			viz.title = title;
			viz.description = "Distribution of person means";
			viz.layout = tech.tablesaw.plotly.components.Layout.builder()
				.xAxis(Axis.builder().title(unit).build())
				.yAxis(Axis.builder().title("Persons").build()).build();
			var dataset = viz.addDataset(data.output(directory, "vtts_persons.csv.gz"));
			viz.addTrace(HistogramTrace.builder(Plotly.INPUT).build(), dataset.mapping().x(column));
		});
	}
}
