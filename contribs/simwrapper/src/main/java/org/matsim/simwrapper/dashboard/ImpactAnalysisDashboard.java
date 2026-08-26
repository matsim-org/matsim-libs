package org.matsim.simwrapper.dashboard;

import org.matsim.application.analysis.impact.ImpactAnalysis;
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
		this.modes = modes;
		this.referenceRunDirectory = referenceRunDirectory;
	}

	@Override
	public void configure(Header header, Layout layout, SimWrapperConfigGroup configGroup) {

		// An explicitly supplied path wins. Otherwise the common SimWrapper setting makes the dashboard comparative.
		String effectiveReference = referenceRunDirectory != null && !referenceRunDirectory.isBlank()
			? referenceRunDirectory : configGroup.getBaseCase();
		boolean comparison = effectiveReference != null && !effectiveReference.isBlank();
		header.title = "Wirkungsanalyse";
		header.description = comparison
			? "Absolute Wirkungen des Szenarios und Veraenderungen gegenueber dem Bezugsfall."
			: "Absolute verkehrliche, physikalische und umweltbezogene Wirkungen des Szenarios.";

		String[] args = analysisArgs(effectiveReference);

		layout.row("traffic", "Verkehrliche und physikalische Wirkungen")
			.el(Table.class, (viz, data) -> {
				viz.title = comparison
					? "Szenario und Bezugsfall"
					: "Absolute Szenariowirkungen";
				viz.description = comparison
					? "Die Differenz ist als Szenario minus Bezugsfall definiert."
					: "Tageswerte werden mit der konfigurierten Stichprobengroesse auf die Gesamtbevoelkerung hochgerechnet.";
				viz.style = "topsheet";
				viz.dataset = data.compute(ImpactAnalysis.class, "impact.csv", args);
				viz.enableFilter = true;
				viz.showAllRows = true;
				viz.width = 1d;
				viz.height = 9d;
				viz.alignment = new String[]{"left", "left", "left", "left", "left", "left", "right", "right",
					"right", "right", "left", "left"};
			});

		layout.row("scope")
			.el(TextBlock.class, (viz, data) -> {
				viz.backgroundColor = "white";
				viz.content = comparison
					? """
						## Einordnung

						Dieser erste Analyseschritt zeigt absolute physikalische Wirkungen und deren Veraenderung. Monetarisierte Nutzen, Verkehrssicherheit, Barwerte und NKV werden in den naechsten Ausbaustufen ergaenzt.
						"""
					: """
						## Einordnung

						Ohne Bezugsfall zeigt das Dashboard absolute Szenariowirkungen. Es werden keine Nutzen und kein Nutzen-Kosten-Verhaeltnis ausgewiesen. Monetarisierte Wirkungen und Verkehrssicherheit werden in den naechsten Ausbaustufen ergaenzt.
						""";
			});
	}

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

		return args.toArray(new String[0]);
	}
}
