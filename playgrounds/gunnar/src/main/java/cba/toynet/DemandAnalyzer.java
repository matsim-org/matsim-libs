package cba.toynet;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class DemandAnalyzer {

	private final Map<TourSequence.Type, Double> tourSeqType2cnt = new LinkedHashMap<>();

	private final Map<TourSequence.Type, Double> tourSeqType2sampersUtl = new LinkedHashMap<>();

	private final Map<TourSequence.Type, Double> tourSeqType2matsimUtl = new LinkedHashMap<>();

	private int personCnt = 0;

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

	void registerChoice(final PlanForResampling planForResampling) {
		final TourSequence tourSequence = planForResampling.getTourSequence();
		assert tourSequence != null;
		this.personCnt++;
		this.add(tourSequence.type, 1.0, this.tourSeqType2cnt);
		this.add(tourSequence.type, planForResampling.getSampersOnlyScore() + planForResampling.getSampersEpsilonRealization()
				+ planForResampling.getSampersTimeScore(), this.tourSeqType2sampersUtl);
		this.add(tourSequence.type, planForResampling.getSampersOnlyScore() + planForResampling.getSampersEpsilonRealization()
				+ planForResampling.getMATSimTimeScore(), this.tourSeqType2matsimUtl);
	}

	private double null2zero(Double val) {
		return ((val == null) ? 0.0 : val);
	}

	@Override
	public String toString() {
		final StringBuffer result = new StringBuffer();
		result.append("PATTERN\tCOUNT\tFREQ.\tSAMPERS(total)\tMATSim(total)\n");
		for (TourSequence.Type type : TourSequence.Type.values()) {
			final double cnt = this.null2zero(this.tourSeqType2cnt.get(type));
			result.append(type);
			result.append("\t");
			result.append(cnt);
			result.append("\t");
			result.append(100.0 * cnt / this.personCnt);
			result.append("\t");
			result.append(this.null2zero(this.tourSeqType2sampersUtl.get(type)) / cnt);
			result.append("\t");
			result.append(this.null2zero(this.tourSeqType2matsimUtl.get(type)) / cnt);
			result.append("\n");
		}
		return result.toString();
	}
}
