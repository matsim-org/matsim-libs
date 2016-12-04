package cba.trianglenet;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class ExperiencedScoreAnalyzer {

	private final Map<Id<Person>, List<Double>> personId2scoreList = new LinkedHashMap<>();

	ExperiencedScoreAnalyzer() {
	}

	void add(final Id<Person> personId, final double score) {
		List<Double> list = this.personId2scoreList.get(personId);
		if (list == null) {
			list = new ArrayList<>();
			this.personId2scoreList.put(personId, list);
		}
		list.add(score);
	}

	private double avg2ndHalf(final List<Double> list) {
		// final int startIndex = list.size() / 2;
		final int startIndex = list.size() - 1;
		double sum = 0;
		for (int i = startIndex; i < list.size(); i++) {
			sum += list.get(i);
		}
		return (sum / (list.size() - startIndex));
	}

	@Override
	public String toString() {
		final StringBuffer result = new StringBuffer();
		for (Map.Entry<Id<Person>, List<Double>> entry : this.personId2scoreList.entrySet()) {
			result.append(entry.getKey());
			result.append("\t");
			result.append(this.avg2ndHalf(entry.getValue()));
			result.append("\t");
			result.append(entry.getValue());
			result.append("\n");
		}
		return result.toString();
	}
}
