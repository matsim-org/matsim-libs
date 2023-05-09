package org.matsim.contrib.ev.stats;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.tuple.Pair;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.common.timeprofile.ProfileWriter;
import org.matsim.contrib.ev.infrastructure.Charger;

import java.awt.*;
import java.util.Comparator;
import java.util.Map;

import one.util.streamex.EntryStream;

public class ChargerPowerTimeProfileView implements ProfileWriter.ProfileView {
	private final ChargerPowerTimeProfileCalculator calculator;
	private final Comparator<Id<Charger>> comparator;

	private final Map<String, Paint> seriesPaints;

	public ChargerPowerTimeProfileView(ChargerPowerTimeProfileCalculator calculator, Comparator<Id<Charger>> comparator,
									   Map<Id<Charger>, Paint> chargerPaint) {
		this.calculator = calculator;
		this.comparator = comparator;
		seriesPaints = EntryStream.of(chargerPaint).mapKeys(Id<Charger>::toString).toMap();

	}
	@Override
	public int[] times() {
		return calculator.getTimeDiscretizer().getTimes();
	}

	@Override
	public ImmutableMap<String, double[]> profiles() {
		return calculator.getChargerProfiles()
				.entrySet()
				.stream()
				.sorted(Map.Entry.comparingByKey(comparator))
				.map(e -> Pair.of(e.getKey().toString(), e.getValue()))
				.collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	@Override
	public Map<String, Paint> seriesPaints() {
		return seriesPaints;
	}


}
