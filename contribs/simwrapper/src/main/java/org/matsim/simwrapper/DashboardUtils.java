package org.matsim.simwrapper;

import org.matsim.simwrapper.viz.GridMap;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DashboardUtils {
	static final String DARK_BLUE = "#1175b3";
	static final String LIGHT_BLUE = "#95c7df";
	static final String ORANGE = "#f4a986";
	static final String RED = "#cc0c27";
	static final String SAND = "#dfb095";
	static final String YELLOW = "#dfdb95";

	public static void setGridMapStandards(GridMap viz, Data data, String crs) {
		viz.height = 12.0;
		viz.cellSize = 100;
		viz.opacity = 0.1;
		viz.maxHeight = 15;
		viz.projection = crs;
		viz.center = data.context().getCenter();
		viz.zoom = data.context().getMapZoomLevel();
		viz.setColorRamp(new double[]{30, 40, 50, 60, 70}, new String[]{DARK_BLUE, LIGHT_BLUE, YELLOW, SAND, ORANGE, RED});
	}

	/**
	 * Adds sampling information to the description based on the sample size in the SimWrapperConfig.
	 *
	 * @param description description for the plot
	 * @param data        the data object
	 * @param scaleBySampleSize if the value of a plot shows scaled values or not
	 * @return
	 */
	public static String adjustDescriptionBasedOnSampling(String description, Data data, boolean scaleBySampleSize) {

		if (scaleBySampleSize) {
			if (data.config().getSampleSize() != null && data.config().getSampleSize() < 1.0)
				description += " Values are scaled by sample size. The shown values are sampled to 100 % based on the simulated sample size of " + getSampleName(data.config().getSampleSize()) + ".";
			else if (data.config().getSampleSize() != null && data.config().getSampleSize() == 1.0)
				description += " Values are for the full population (sample size 100%).";
			else
				description += " Values are NOT scaled by sample size. Sample size not specified in the simWrapperConfig.";
		} else {
			if (data.config().getSampleSize() != null && data.config().getSampleSize() < 1.0)
				description += " Values are NOT scaled by sample size. The shown values are for the simulated sample size of " + getSampleName(data.config().getSampleSize()) + ".";
			else if (data.config().getSampleSize() != null && data.config().getSampleSize() == 1.0)
				description += " Values are for the full population (sample size 100%).";
			else
				description += " Values are NOT scaled by sample size. Sample size not specified in the simWrapperConfig.";
		}
		return description;
	}
	private static String getSampleName(double sampleSize){
		double percent = sampleSize * 100;
		DecimalFormat df;
		if (percent == Math.rint(percent)) {
			df = new DecimalFormat("0'%'");
		} else {
			df = new DecimalFormat("0.##'%'");
		}
		return df.format(percent);
	}

	/**
	 * Merge CLI-style args while keeping the base args authoritative for conflicting options.
	 * Missing options from {@code additionalArgs} are appended in their original order.
	 */
	public static String[] mergeArgsPreferBase(String[] baseArgs, String[] additionalArgs) {
		if (baseArgs == null || baseArgs.length == 0)
			return additionalArgs == null ? new String[0] : additionalArgs.clone();
		if (additionalArgs == null || additionalArgs.length == 0)
			return baseArgs.clone();

		Map<String, String> merged = toArgMap(baseArgs);
		Map<String, String> additional = toArgMap(additionalArgs);

		for (Map.Entry<String, String> entry : additional.entrySet()) {
			merged.putIfAbsent(entry.getKey(), entry.getValue());
		}

		return toArgArray(merged);
	}

	private static Map<String, String> toArgMap(String[] args) {
		Map<String, String> map = new LinkedHashMap<>();

		for (int i = 0; i < args.length; i++) {
			String key = args[i];
			String value = null;

			if (i + 1 < args.length && !args[i + 1].startsWith("--")) {
				value = args[++i];
			}

			map.put(key, value);
		}

		return map;
	}

	private static String[] toArgArray(Map<String, String> args) {
		List<String> result = new ArrayList<>();
		for (Map.Entry<String, String> entry : args.entrySet()) {
			result.add(entry.getKey());
			if (entry.getValue() != null) {
				result.add(entry.getValue());
			}
		}
		return result.toArray(String[]::new);
	}
}
