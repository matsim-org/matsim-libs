package org.matsim.simwrapper.dashboard;

import org.matsim.application.analysis.impact.ImpactAnalysis;
import org.matsim.application.analysis.impact.ImpactDashboardTables;
import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.Header;
import org.matsim.simwrapper.Layout;
import org.matsim.simwrapper.SimWrapperConfigGroup;
import org.matsim.simwrapper.viz.Table;
import org.matsim.simwrapper.viz.TextBlock;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Dashboard for absolute physical impacts and, optionally, a comparison with a reference run.
 */
public class ImpactAnalysisDashboard implements Dashboard {

	private final Collection<String> modes;
	private final String referenceRunDirectory;
	private final double marginalUtilityOfMoney;

	/**
	 * Creates an absolute impact dashboard containing all modes found in the run.
	 */
	public ImpactAnalysisDashboard() {
		this(null, (String) null);
	}

	/**
	 * Creates an absolute impact dashboard for selected modes.
	 */
	public ImpactAnalysisDashboard(Collection<String> modes) {
		this(modes, (String) null);
	}

	/**
	 * Creates a comparative impact dashboard containing all modes found in both runs.
	 */
	public ImpactAnalysisDashboard(Path referenceRunDirectory) {
		this(null, referenceRunDirectory);
	}

	public ImpactAnalysisDashboard(Collection<String> modes, Path referenceRunDirectory) {
		this(modes, referenceRunDirectory == null ? null : referenceRunDirectory.toString());
	}

	public ImpactAnalysisDashboard(Collection<String> modes, String referenceRunDirectory) {
		this(modes, referenceRunDirectory, 1.0);
	}

	public ImpactAnalysisDashboard(Collection<String> modes, String referenceRunDirectory, double marginalUtilityOfMoney) {
		this.modes = modes;
		this.referenceRunDirectory = referenceRunDirectory;
		this.marginalUtilityOfMoney = marginalUtilityOfMoney;
	}

	@Override
	public void configure(Header header, Layout layout, SimWrapperConfigGroup configGroup) {

		boolean comparison = referenceRunDirectory != null && !referenceRunDirectory.isBlank();
		header.title = "Impact Analysis";
		header.description = comparison
			? "Absolute impacts of the policy case and changes relative to the base case."
			: "Absolute traffic, physical and environmental impacts of the scenario.";

		String[] args = analysisArgs(referenceRunDirectory);

		layout.row("impact-overview").el(Table.class, (viz, data) -> {
			data.compute(ImpactAnalysis.class, "impact.csv", args);
			viz.title = "Traffic, Physical and Environmental Effects";
			viz.dataset = data.compute(ImpactDashboardTables.class, "impact_overview.csv");
			viz.style = "topsheet"; viz.enableFilter = false; viz.hideHeader = false; viz.showAllRows = true;
			viz.height = 15d;
			viz.alignment = new String[]{"left", "left", "left", "right", "left", "right", "left"};
		});

	}

	private void displayScoreTable(Layout layout, String[] args) {
		layout.row("score").el(Table.class, (viz, data) -> {
			data.compute(ImpactAnalysis.class, "impact.csv", args);
			viz.title = "Score";
			viz.dataset = data.compute(ImpactDashboardTables.class, "impact_scores.csv");
			viz.style = "topsheet"; viz.enableFilter = false; viz.hideHeader = false; viz.showAllRows = true;
			viz.alignment = new String[]{"left", "right", "right", "left"};
		});
	}

	private void modeTables(Layout layout, String[] args, String mode) {
		String label = mode.substring(0, 1).toUpperCase() + mode.substring(1);
		layout.row(mode).el(Table.class, (viz, data) -> {
			data.compute(ImpactAnalysis.class, "impact.csv", args);
			viz.title = "Traffic / Physical Effects (" + label + ")";
			viz.dataset = data.compute(ImpactDashboardTables.class, "impact_general_" + mode + ".csv");
			viz.style = "topsheet"; viz.enableFilter = false; viz.hideHeader = false; viz.showAllRows = true;
			viz.width = 0.5d; viz.height = 5d; viz.alignment = new String[]{"left", "right", "right", "left"};
		}).el(Table.class, (viz, data) -> {
			data.compute(ImpactAnalysis.class, "impact.csv", args);
			viz.title = "Exhaust Emissions (" + label + ")";
			viz.dataset = data.compute(ImpactDashboardTables.class, "impact_emissions_" + mode + ".csv");
			viz.style = "topsheet"; viz.enableFilter = false; viz.hideHeader = false; viz.showAllRows = true;
			viz.width = 0.5d; viz.height = 5d; viz.alignment = new String[]{"left", "right", "right", "left"};
		});
	}

	private void trafficSection(Layout layout, String[] args, String section, List<ImpactView> views) {
		layout.row(section.toLowerCase() + "-header").el(TextBlock.class, (viz, data) -> {
			viz.title = section;
			viz.content = "## " + section;
		});
		for (ImpactView view : views) {
			String id = (section + "-" + view.metric()).toLowerCase().replace(' ', '-');
			displayCaseTables(layout, args, id, view.metric(), view.file(), 4d);
		}
	}

	private void displayCaseTables(Layout layout, String[] args, String id, String title, String file, double height) {
		Layout.Row row = layout.row(id);
		for (String kind : List.of("base", "policy", "difference")) {
			String label = switch (kind) { case "base" -> "Base"; case "policy" -> "Policy"; default -> "Differenz"; };
			row.el(Table.class, (viz, data) -> {
				data.compute(ImpactAnalysis.class, "impact.csv", args);
				viz.title = title + " – " + label;
				viz.dataset = data.compute(ImpactDashboardTables.class, file + "_" + kind + ".csv");
				viz.height = height;
				viz.style = "topsheet";
				viz.enableFilter = false;
				viz.hideHeader = false;
				viz.showAllRows = true;
				viz.alignment = file.contains("person_") || file.contains("freight_")
					? new String[]{"left", "right", "right"}
					: file.contains("emissions")
					? new String[]{"left", "left", "right", "right"}
					: new String[]{"left", "left", "right"};
			});
		}
	}

	private record ImpactView(String metric, String file) { }

	private String[] analysisArgs(String effectiveReference) {

		List<String> args = new ArrayList<>();
		if (modes != null && !modes.isEmpty()) {
			args.add("--modes");
			args.add(String.join(",", modes));
		}

		if (effectiveReference != null && !effectiveReference.isBlank()) {
			args.add("--reference-run-directory");
			args.add(effectiveReference);
		}
		args.add("--marginal-utility-of-money");
		args.add(Double.toString(marginalUtilityOfMoney));

		return args.toArray(new String[0]);
	}
}
