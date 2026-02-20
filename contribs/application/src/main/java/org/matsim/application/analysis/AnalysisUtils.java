package org.matsim.application.analysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AnalysisUtils {

	public static List<String> createGroupLabels(List<Long> distGroups) {
		List<String> labels = new ArrayList<>();
		for (int i = 0; i < distGroups.size() - 1; i++) {
			labels.add(String.format("%d - %d", distGroups.get(i), distGroups.get(i + 1)));
		}
		labels.add(distGroups.getLast() + "+");
		distGroups.add(Long.MAX_VALUE);
		return labels;
	}

	public static String getLabelForValue(long attributeValue, List<Long> attributeGroups, List<String> attributeLabels) {

		int idx = Collections.binarySearch(attributeGroups, attributeValue);

		if (idx >= 0)
			return attributeLabels.get(idx);

		int ins = -(idx + 1);
		return attributeLabels.get(ins - 1);
	}
}
