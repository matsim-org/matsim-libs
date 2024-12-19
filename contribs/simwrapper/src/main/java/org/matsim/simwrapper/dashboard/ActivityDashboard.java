package org.matsim.simwrapper.dashboard;

import org.apache.commons.lang3.StringUtils;
import org.matsim.application.analysis.activity.ActivityCountAnalysis;
import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.Header;
import org.matsim.simwrapper.Layout;
import org.matsim.simwrapper.viz.ColorScheme;
import org.matsim.simwrapper.viz.MapPlot;
import org.matsim.simwrapper.viz.TextBlock;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Dashboard to show activity related statistics aggregated by type and location.
 * <p>
 * Note that {@link #addActivityType(String, List, List, boolean, String)} needs to be called for each activity type.
 * There is no default configuration.
 */
public class ActivityDashboard implements Dashboard {

	private static final String ID_COLUMN = "id";
	private static final String REF_JOIN = "id";

	private final String shpFile;
	private final Map<String, String> activityMapping = new LinkedHashMap<>();
	private final Map<String, String> refCsvs = new LinkedHashMap<>();
	private final Set<String> countMultipleOccurrencesSet = new HashSet<>();
	private List<Indicator> indicators = new ArrayList<>();

	public ActivityDashboard(String shpFile) {
		this.shpFile = Objects.requireNonNull(shpFile, "Shapefile can not be null!");
	}

	/**
	 * Convenience method to add an activity type with default configuration.
	 */
	public ActivityDashboard addActivityType(String name, List<String> activities, List<Indicator> indicators) {
		return addActivityType(name, activities, indicators, true, null);
	}

	/**
	 * Add an activity type to the dashboard.
	 *
	 * @param name                     name to show in the dashboard
	 * @param activities               List of activity names to include in this type
	 * @param indicators               List of indicators to show
	 * @param countMultipleOccurrences Whether multiple occurrences of the same activity for one person should be counted.
	 *                                 Can be used to count home or workplaces only once.
	 * @param refCsv                   Reference CSV file to compare the activities to. Can be null.
	 */
	public ActivityDashboard addActivityType(String name, List<String> activities, List<Indicator> indicators,
											 boolean countMultipleOccurrences, @Nullable String refCsv) {
		activityMapping.put(name, String.join(",", activities));
		refCsvs.put(name, refCsv);

		if (countMultipleOccurrences) {
			countMultipleOccurrencesSet.add(name);
		}

		this.indicators = indicators;
		return this;
	}

	@Override
	public void configure(Header header, Layout layout) {

		header.title = "Activities";
		header.description = "Displays the activities by type and location.";

		List<String> args = new ArrayList<>(List.of("--id-column", ID_COLUMN, "--shp", shpFile));
		args.add("--activity-mapping");
		args.add(activityMapping.entrySet().stream()
			.map(e -> "%s=%s".formatted(e.getKey(), e.getValue()))
			.collect(Collectors.joining(";")));

		args.add("--single-occurrence");
		if (!countMultipleOccurrencesSet.isEmpty()) {
			args.add(String.join(";", countMultipleOccurrencesSet));
		}


		for (Map.Entry<String, String> activity : activityMapping.entrySet()) {

			String activityName = StringUtils.capitalize(activity.getKey());

			layout.row("category_header_" + activity.getKey())
				.el(TextBlock.class, (viz, data) -> {
					viz.content = "## **" + activityName + "**";
					viz.backgroundColor = "transparent";
				});

			for (Indicator ind : Indicator.values()) {

				if (indicators.contains(ind)) {

					Layout.Row row = layout.row(activity.getKey() + "_" + ind.name)
						.el(MapPlot.class, (viz, data) -> {
							viz.title = "Simulated %s Activities (%s)".formatted(activityName, ind.displayName);
							viz.height = 8.;
							String shp = data.resource(shpFile);
							viz.setShape(shp, ID_COLUMN);
							viz.addDataset("transit-trips", data.computeWithPlaceholder(ActivityCountAnalysis.class, "activities_%s_per_region.csv", activity.getKey(), args.toArray(new String[0])));
							viz.display.fill.columnName = ind.name;
							viz.display.fill.dataset = "transit-trips";
							viz.display.fill.join = REF_JOIN;
							if (ind == Indicator.RELATIVE_DENSITY) {
								viz.display.fill.setColorRamp(ColorScheme.RdBu, 11, false, "0.2, 0.25, 0.33, 0.5, 0.67, 1.5, 2.0, 3.0, 4.0, 5.0");
							}
						});

					if (refCsvs.get(activity.getKey()) != null) {
						row.el(MapPlot.class, (viz, data) -> {

							viz.title = "Reference %s Activities (%s)".formatted(activityName, ind.displayName);
							viz.height = 8.;

							String shp = data.resource(shpFile);
							viz.setShape(shp, ID_COLUMN);

							viz.addDataset("transit-trips", data.resource(refCsvs.get(activity.getKey())));

							viz.display.fill.dataset = "transit-trips";
							viz.display.fill.join = REF_JOIN;

							if (ind == Indicator.RELATIVE_DENSITY) {
								viz.display.fill.columnName = "relative_density";
								viz.display.fill.setColorRamp(ColorScheme.RdBu, 11, false, "0.2, 0.25, 0.33, 0.5, 0.67, 1.5, 2.0, 3.0, 4.0, 5.0");
							} else if (ind == Indicator.DENSITY) {
								viz.display.fill.columnName = "density";
							} else {
								viz.display.fill.columnName = "count";
							}
						});
					}
				}
			}
		}
	}

	/**
	 * Metric to show in the dashboard.
	 */
	public enum Indicator {
		COUNTS("count", "Counts"),
		DENSITY("density", "Density"),
		RELATIVE_DENSITY("relative_density", "Relative Density");

		private final String name;
		private final String displayName;

		Indicator(String name, String displayName) {
			this.name = name;
			this.displayName = displayName;
		}
	}
}


























