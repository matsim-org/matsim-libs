package cba;

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
		if (true || list == null) {
			list = new ArrayList<>();
			this.personId2scoreList.put(personId, list);
		}
		list.add(score);
	}
	
	private double avg(final List<Double> list) {
		double sum = 0;
		for (Double val : list) {
			sum += val;
		}
		return (sum / list.size());
	}
	
	@Override
	public String toString() {
		final StringBuffer result = new StringBuffer();
		for (Map.Entry<Id<Person>, List<Double>> entry : this.personId2scoreList.entrySet()) {
			result.append(entry.getKey());
			result.append("\t");
			result.append(this.avg(entry.getValue()));
			result.append("\t");
			result.append(entry.getValue());
			result.append("\n");
		}		
		return result.toString();
	}	
}
