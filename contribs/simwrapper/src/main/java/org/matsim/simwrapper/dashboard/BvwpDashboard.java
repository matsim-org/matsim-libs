package org.matsim.simwrapper.dashboard;

import org.matsim.application.analysis.impact.BvwpAnalysis;
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
import java.util.Set;

/**
 * Dashboard showing BVWP-style central traffic effects and cost-benefit tables.
 */
public class BvwpDashboard implements Dashboard {

	private final Collection<String> personTrafficModes;
	private final Collection<String> freightTrafficModes;
	private final String referenceRunDirectory;

	public BvwpDashboard() {
		this(Set.of("car"), Set.of("freight", "truck"), (String) null);
	}

	public BvwpDashboard(Collection<String> personTrafficModes, Collection<String> freightTrafficModes) {
		this(personTrafficModes, freightTrafficModes, (String) null);
	}

	public BvwpDashboard(Collection<String> personTrafficModes, Collection<String> freightTrafficModes, Path referenceRunDirectory) {
		this(personTrafficModes, freightTrafficModes, referenceRunDirectory == null ? null : referenceRunDirectory.toString());
	}

	public BvwpDashboard(Collection<String> personTrafficModes, Collection<String> freightTrafficModes, String referenceRunDirectory) {
		this.personTrafficModes = personTrafficModes;
		this.freightTrafficModes = freightTrafficModes;
		this.referenceRunDirectory = referenceRunDirectory;
	}

	@Override
	public void configure(Header header, Layout layout, SimWrapperConfigGroup configGroup) {

		header.title = "BVWP";
		header.description = "BVWP-style analysis of central traffic effects and cost-benefit positions.";

		String[] args = analysisArgs();

		layout.row("central", "Zentrale verkehrliche / physikalische Wirkungen")
			.el(Table.class, (viz, data) -> {
				viz.title = "Zentrale verkehrliche / physikalische Wirkungen";
				viz.style = "topsheet";
				viz.dataset = data.compute(BvwpAnalysis.class, "bvwp_central_traffic_effects.csv", args);
				viz.enableFilter = false;
				viz.showAllRows = true;
				viz.width = 1d;
				viz.height = 8d;
				viz.alignment = new String[]{"left", "left", "left", "right", "right", "right", "left"};
			});

		layout.row("emissions", "Veraenderung der Abgasemissionen (PV+GV)")
			.el(Table.class, (viz, data) -> {
				viz.title = "Veraenderung der Abgasemissionen (PV+GV)";
				viz.style = "topsheet";
				viz.dataset = data.compute(BvwpAnalysis.class, "bvwp_emissions.csv", args);
				viz.enableFilter = false;
				viz.showAllRows = true;
				viz.width = 1d;
				viz.height = 4d;
				viz.alignment = new String[]{"left", "left", "right", "right", "right", "left", "left"};
			});

		layout.row("nka", "Nutzen-Kosten-Analyse (Modul A)")
			.el(Table.class, (viz, data) -> {
				viz.title = "Nutzen-Kosten-Analyse (Modul A)";
				viz.style = "topsheet";
				viz.dataset = data.compute(BvwpAnalysis.class, "bvwp_cost_benefit_analysis.csv", args);
				viz.enableFilter = false;
				viz.showAllRows = true;
				viz.width = 1d;
				viz.height = 8d;
				viz.alignment = new String[]{"left", "left", "right", "right", "left"};
			});

		layout.row("costs", "Kosten und Nutzen-Kosten-Verhaeltnis")
			.el(Table.class, (viz, data) -> {
				viz.title = "Kosten";
				viz.style = "topsheet";
				viz.dataset = data.compute(BvwpAnalysis.class, "bvwp_costs.csv", args);
				viz.enableFilter = false;
				viz.showAllRows = true;
				viz.width = 0.5d;
				viz.height = 4d;
				viz.alignment = new String[]{"left", "right", "right", "left"};
			})
			.el(Table.class, (viz, data) -> {
				viz.title = "Nutzen-Kosten-Verhaeltnis";
				viz.style = "topsheet";
				viz.dataset = data.compute(BvwpAnalysis.class, "bvwp_summary.csv", args);
				viz.enableFilter = false;
				viz.showAllRows = true;
				viz.width = 0.5d;
				viz.height = 4d;
				viz.alignment = new String[]{"left", "right", "left", "left"};
			});

		layout.row("notes")
			.el(TextBlock.class, (viz, data) -> {
				viz.backgroundColor = "white";
				viz.content = """
					## Notes

					This dashboard mirrors the BVWP table structure. Rows marked as placeholders require additional BVWP-specific inputs, for example a reference run, project link set, fuel consumption factors, safety model, noise model, reliability model, cost rates, and investment costs.

					Computed traffic effects use `legs.csv`, the configured MATSim sample size, and the dashboard mode groups. Emission differences are filled only when `emissions_per_network_mode.csv` exists for both the plan and reference run.
					""";
			});
	}

	private String[] analysisArgs() {

		List<String> args = new ArrayList<>();

		args.add("--person-traffic-modes");
		args.add(String.join(",", personTrafficModes));
		args.add("--freight-traffic-modes");
		args.add(String.join(",", freightTrafficModes));

		if (referenceRunDirectory != null && !referenceRunDirectory.isBlank()) {
			args.add("--reference-run-directory");
			args.add(referenceRunDirectory);
		}

		return args.toArray(new String[0]);
	}
}
