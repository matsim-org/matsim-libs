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

public class ActivityDashboard implements Dashboard {

	private static final String ID_COLUMN = "id";
	private static final String REF_JOIN = "id";

	private final String shpFile;
	private final Map<String, String> activityMapping = new LinkedHashMap<>();
	private final Map<String, String> refCsvs = new LinkedHashMap<>();
	private final Set<String> countMultipleOccurrencesSet = new HashSet<>();
	private final String refColumn = "count";
	private List<Indicator> indicators = new ArrayList<>();

	public ActivityDashboard(String shpFile) {
		this.shpFile = Objects.requireNonNull(shpFile, "Shapefile can not be null!");
	}

	public ActivityDashboard addActivityType(String name, List<String> activities, List<Indicator> indicators) {
		return addActivityType(name, activities, indicators, true, null);
	}

	public ActivityDashboard addActivityType(String name, List<String> activities, List<Indicator> indicators, boolean countMultipleOccurrences, @Nullable String refCsv) {
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

		header.title = "Activity Analysis";
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

			layout.row("category_header_" + activity.getKey())
				.el(TextBlock.class, (viz, data) -> {
					viz.content = "## **" + StringUtils.capitalize(activity.getKey()) + "**";
					viz.backgroundColor = "transparent";
				});

			for (Indicator ind : Indicator.values()) {

				if (indicators.contains(ind)) {

					Layout.Row row = layout.row(activity.getKey() + "_" + ind.name)
						.el(MapPlot.class, (viz, data) -> {
							viz.title = "MATSim " + activity.getKey() + " Count (" + ind.name + ")";
							viz.height = 8.;
							String shp = data.resource(shpFile);
							viz.setShape(shp, ID_COLUMN);
							viz.addDataset("transit-trips", data.computeWithPlaceholder(ActivityCountAnalysis.class, "activities_%s_per_region.csv", activity.getKey(), args.toArray(new String[0])));
							viz.display.fill.columnName = ind.name;
							viz.display.fill.dataset = "transit-trips";
							viz.display.fill.join = REF_JOIN;
							if (ind == Indicator.RELATIVE_DENSITY) {
								viz.display.fill.setColorRamp(ColorScheme.RdBu, 12, false, "-80,-75,-67,-50,-33,50,100,200,300,400,500");
							} else if (ind == Indicator.COUNTS) {
								viz.display.fill.normalize = "transit-trips:area";
							}
						});

					if (refCsvs.get(activity.getKey()) != null) {
						row.el(MapPlot.class, (viz, data) -> {

							viz.title = "Reference " + activity.getKey() + " Count (" + ind.name + ")";
							viz.description = "Polygon Reference Description";
							viz.height = 8.;

							String shp = data.resource(shpFile);
							viz.setShape(shp, ID_COLUMN);

							viz.addDataset("transit-trips", data.resource(refCsvs.get(activity.getKey())));

							viz.display.fill.dataset = "transit-trips";
							viz.display.fill.join = REF_JOIN;

							if (ind == Indicator.RELATIVE_DENSITY) {
								viz.display.fill.columnName = "relative_density";
								viz.display.fill.setColorRamp(ColorScheme.RdBu, 12, false, "-80,-75,-67,-50,-33,50,100,200,300,400,500");
							} else if (ind == Indicator.DENSITY) {
								viz.display.fill.columnName = "density";
							} else {
								viz.display.fill.columnName = "count";
								viz.display.fill.normalize = "transit-trips:area";
							}
						});
					}
				}
			}
		}
	}

	public enum Indicator {
		COUNTS("count"),
		DENSITY("density"),
		RELATIVE_DENSITY("relative_density");

		private final String name;

		Indicator(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}
}


























