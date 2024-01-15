package org.matsim.contrib.ev.stats;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.tuple.Pair;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.common.timeprofile.ProfileWriter;

import java.awt.*;
import java.util.Map;

final class ChargerPowerTimeProfileView implements ProfileWriter.ProfileView {
	private final ChargerPowerTimeProfileCalculator calculator;

	ChargerPowerTimeProfileView(ChargerPowerTimeProfileCalculator calculator) {
		this.calculator = calculator;

	}
	@Override
	public double[] times() {
		return calculator.getTimeDiscretizer().getTimes();
	}

	@Override
	public ImmutableMap<String, double[]> profiles() {
		return calculator.getChargerProfiles()
				.entrySet()
				.stream()
				.sorted(Map.Entry.comparingByKey(Id::compareTo))
				.map(e -> Pair.of(e.getKey().toString(), e.getValue()))
				.collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	@Override
	public Map<String, Paint> seriesPaints() {
		return Map.of();
	}


}
