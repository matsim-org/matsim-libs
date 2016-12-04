package cba.trianglenet;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class DemandAnalyzer {

	private final Map<List<String>, Double> actSeq2cnt = new LinkedHashMap<>();

	private final Map<List<String>, Double> actSeq2matsimScore = new LinkedHashMap<>();

	private final Map<List<String>, Double> actSeq2sampersUtility = new LinkedHashMap<>();

	private final Map<String, Double> mode2cnt = new LinkedHashMap<>();

	private final Map<String, Double> mode2matsimScore = new LinkedHashMap<>();

	private final Map<String, Double> mode2sampersUtility = new LinkedHashMap<>();

	private int personCnt = 0;

	private int legCnt = 0;

	DemandAnalyzer() {
	}

	private <K> void add(final K key, final double addend, final Map<K, Double> key2cnt) {
		final Double oldCnt = key2cnt.get(key);
		if (oldCnt == null) {
			key2cnt.put(key, addend);
		} else {
			key2cnt.put(key, oldCnt + addend);
		}

	}

	void registerChoice(final Plan plan, final double sampersUtility) {

		this.personCnt++;

		final List<String> actTypes = new ArrayList<>();
		for (int i = 0; i < plan.getPlanElements().size(); i += 2) {
			final Activity act = (Activity) (plan.getPlanElements().get(i));
			actTypes.add(act.getType());
		}
		this.add(actTypes, 1.0, this.actSeq2cnt);
		this.add(actTypes, plan.getScore(), this.actSeq2matsimScore);
		this.add(actTypes, sampersUtility, this.actSeq2sampersUtility);

		for (int i = 1; i < plan.getPlanElements().size(); i += 2) {
			final Leg leg = (Leg) (plan.getPlanElements().get(i));
			this.add(leg.getMode(), 1.0, this.mode2cnt);
			this.add(leg.getMode(), plan.getScore(), this.mode2matsimScore);
			this.add(leg.getMode(), sampersUtility, this.mode2sampersUtility);
			this.legCnt++;
		}
	}

	@Override
	public String toString() {
		final StringBuffer result = new StringBuffer();

		result.append("ACTIVITY PATTERNS\n");
		for (Map.Entry<?, Double> entry : this.actSeq2cnt.entrySet()) {
			result.append(entry);
			result.append("\t[");
			result.append(100.0 * (double) entry.getValue() / this.personCnt);
			result.append("%]\t");
			result.append("avg.score = " + this.actSeq2matsimScore.get(entry.getKey()) / entry.getValue()
					+ ", avg.utility = " + this.actSeq2sampersUtility.get(entry.getKey()) / entry.getValue() + "\n");
		}

		result.append("MODES\n");
		for (Map.Entry<?, Double> entry : this.mode2cnt.entrySet()) {
			result.append(entry);
			result.append("\t[");
			result.append(100.0 * (double) entry.getValue() / this.legCnt);
			result.append("%]\t");
			result.append("avg.score = " + this.mode2matsimScore.get(entry.getKey()) / entry.getValue()
					+ ", avg.utility = " + this.mode2sampersUtility.get(entry.getKey()) / entry.getValue() + "\n");
		}

		return result.toString();
	}

}
