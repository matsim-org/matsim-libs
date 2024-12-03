package org.matsim.smallScaleCommercialTrafficGeneration.data;

import org.apache.commons.math3.distribution.EnumeratedDistribution;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.util.Pair;
import org.matsim.smallScaleCommercialTrafficGeneration.GenerateSmallScaleCommercialTrafficDemand;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.matsim.smallScaleCommercialTrafficGeneration.GenerateSmallScaleCommercialTrafficDemand.makeServiceDurationPerCategoryKey;

public class DefaultTourSpecificationsByUsingKID2002 implements CommercialTourSpecifications {

	@Override
	public Map<GenerateSmallScaleCommercialTrafficDemand.ServiceDurationPerCategoryKey, EnumeratedDistribution<GenerateSmallScaleCommercialTrafficDemand.DurationsBounds>> createStopDurationDistributionPerCategory(
		RandomGenerator rng) {
		Map<GenerateSmallScaleCommercialTrafficDemand.ServiceDurationPerCategoryKey, EnumeratedDistribution<GenerateSmallScaleCommercialTrafficDemand.DurationsBounds>> stopDurationProbabilityDistribution = new HashMap<>();

		List<Pair<GenerateSmallScaleCommercialTrafficDemand.DurationsBounds, Double>> thisStopDurationProbabilityDistribution = new ArrayList<>();
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(0, 30), 0.098));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(30, 60), 0.17));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(60, 90), 0.127));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(90, 120), 0.11));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(120, 180), 0.17));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(180, 240), 0.076));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(240, 300), 0.057));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(300, 360), 0.01));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(360, 420), 0.026));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(420, 480), 0.045));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(480, 540), 0.064));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(540, 600), 0.034));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(600, 720), 0.012));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(720, 840), 0.002));
		stopDurationProbabilityDistribution.put(makeServiceDurationPerCategoryKey("Employee Primary Sector", null,
				GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficType.commercialPersonTraffic.toString()),
			new EnumeratedDistribution<>(rng, thisStopDurationProbabilityDistribution));
		thisStopDurationProbabilityDistribution.clear();

		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(0, 30), 0.054));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(30, 60), 0.164));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(60, 90), 0.153));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(90, 120), 0.087));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(120, 180), 0.12));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(180, 240), 0.055));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(240, 300), 0.044));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(300, 360), 0.02));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(360, 420), 0.025));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(420, 480), 0.069));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(480, 540), 0.132));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(540, 600), 0.058));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(600, 720), 0.016));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(720, 840), 0.002));
		stopDurationProbabilityDistribution.put(makeServiceDurationPerCategoryKey("Employee Construction", null,
				GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficType.commercialPersonTraffic.toString()),
			new EnumeratedDistribution<>(rng, thisStopDurationProbabilityDistribution));
		thisStopDurationProbabilityDistribution.clear();

		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(0, 30), 0.13));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(30, 60), 0.324));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(60, 90), 0.178));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(90, 120), 0.108));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(120, 180), 0.097));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(180, 240), 0.034));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(240, 300), 0.02));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(300, 360), 0.018));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(360, 420), 0.02));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(420, 480), 0.027));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(480, 540), 0.029));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(540, 600), 0.008));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(600, 720), 0.006));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(720, 840), 0.001));
		stopDurationProbabilityDistribution.put(makeServiceDurationPerCategoryKey("Employee Secondary Sector Rest", null,
				GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficType.commercialPersonTraffic.toString()),
			new EnumeratedDistribution<>(rng, thisStopDurationProbabilityDistribution));
		thisStopDurationProbabilityDistribution.clear();

		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(0, 30), 0.178));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(30, 60), 0.301));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(60, 90), 0.192));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(90, 120), 0.104));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(120, 180), 0.092));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(180, 240), 0.043));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(240, 300), 0.013));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(300, 360), 0.017));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(360, 420), 0.011));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(420, 480), 0.016));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(480, 540), 0.016));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(540, 600), 0.007));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(600, 720), 0.007));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(720, 840), 0.001));
		stopDurationProbabilityDistribution.put(makeServiceDurationPerCategoryKey("Employee Retail", null,
				GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficType.commercialPersonTraffic.toString()),
			new EnumeratedDistribution<>(rng, thisStopDurationProbabilityDistribution));
		thisStopDurationProbabilityDistribution.clear();

		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(0, 30), 0.144));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(30, 60), 0.372));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(60, 90), 0.203));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(90, 120), 0.069));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(120, 180), 0.112));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(180, 240), 0.038));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(240, 300), 0.011));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(300, 360), 0.011));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(360, 420), 0.011));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(420, 480), 0.012));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(480, 540), 0.007));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(540, 600), 0.005));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(600, 720), 0.005));
		stopDurationProbabilityDistribution.put(makeServiceDurationPerCategoryKey("Employee Traffic/Parcels", null,
				GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficType.commercialPersonTraffic.toString()),
			new EnumeratedDistribution<>(rng, thisStopDurationProbabilityDistribution));
		thisStopDurationProbabilityDistribution.clear();

		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(0, 30), 0.196));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(30, 60), 0.292));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(60, 90), 0.19));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(90, 120), 0.101));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(120, 180), 0.105));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(180, 240), 0.034));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(240, 300), 0.017));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(300, 360), 0.009));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(360, 420), 0.013));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(420, 480), 0.019));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(480, 540), 0.014));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(540, 600), 0.006));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(600, 720), 0.004));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(720, 840), 0.001));
		stopDurationProbabilityDistribution.put(makeServiceDurationPerCategoryKey("Employee Tertiary Sector Rest", null,
				GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficType.commercialPersonTraffic.toString()),
			new EnumeratedDistribution<>(rng, thisStopDurationProbabilityDistribution));
		thisStopDurationProbabilityDistribution.clear();

		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(0, 10), 0.038));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(10, 20), 0.049));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(20, 30), 0.052));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(30, 40), 0.094));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(40, 50), 0.125));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(50, 60), 0.094));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(60, 75), 0.167));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(75, 90), 0.094));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(90, 120), 0.113));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(120, 150), 0.056));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(150, 180), 0.04));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(180, 240), 0.024));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(240, 300), 0.009));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(300, 420), 0.016));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(420, 540), 0.026));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(540, 660), 0.002));
		stopDurationProbabilityDistribution.put(makeServiceDurationPerCategoryKey("Employee Primary Sector", "vehTyp1",
				GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficType.goodsTraffic.toString()),
			new EnumeratedDistribution<>(rng, thisStopDurationProbabilityDistribution));
		thisStopDurationProbabilityDistribution.clear();

		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(0, 10), 0.025));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(10, 20), 0.025));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(20, 30), 0.05));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(30, 40), 0.043));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(40, 50), 0.112));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(50, 60), 0.168));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(60, 75), 0.149));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(75, 90), 0.081));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(90, 120), 0.168));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(120, 150), 0.068));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(180, 240), 0.068));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(420, 540), 0.025));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(540, 660), 0.019));
		stopDurationProbabilityDistribution.put(makeServiceDurationPerCategoryKey("Employee Primary Sector", "vehTyp2",
				GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficType.goodsTraffic.toString()),
			new EnumeratedDistribution<>(rng, thisStopDurationProbabilityDistribution));
		thisStopDurationProbabilityDistribution.clear();

		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(0, 10), 0.036));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(10, 20), 0.098));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(20, 30), 0.036));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(30, 40), 0.016));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(40, 50), 0.042));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(50, 60), 0.124));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(60, 75), 0.085));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(75, 90), 0.144));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(90, 120), 0.105));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(120, 150), 0.052));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(150, 180), 0.072));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(180, 240), 0.052));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(240, 300), 0.023));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(300, 420), 0.033));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(420, 540), 0.062));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(540, 660), 0.016));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(660, 780), 0.003));
		stopDurationProbabilityDistribution.put(makeServiceDurationPerCategoryKey("Employee Primary Sector", "vehTyp3",
				GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficType.goodsTraffic.toString()),
			new EnumeratedDistribution<>(rng, thisStopDurationProbabilityDistribution));
		thisStopDurationProbabilityDistribution.clear();

		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(0, 10), 0.071));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(20, 30), 0.143));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(30, 40), 0.429));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(60, 75), 0.179));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(90, 120), 0.107));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(300, 420), 0.071));
		stopDurationProbabilityDistribution.put(makeServiceDurationPerCategoryKey("Employee Primary Sector", "vehTyp4",
				GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficType.goodsTraffic.toString()),
			new EnumeratedDistribution<>(rng, thisStopDurationProbabilityDistribution));
		thisStopDurationProbabilityDistribution.clear();

		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(10, 20), 0.026));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(30, 40), 0.395));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(50, 60), 0.158));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(60, 75), 0.132));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(75, 90), 0.026));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(90, 120), 0.105));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(120, 150), 0.079));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(150, 180), 0.026));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(540, 660), 0.053));
		stopDurationProbabilityDistribution.put(makeServiceDurationPerCategoryKey("Employee Primary Sector", "vehTyp5",
				GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficType.goodsTraffic.toString()),
			new EnumeratedDistribution<>(rng, thisStopDurationProbabilityDistribution));
		thisStopDurationProbabilityDistribution.clear();

		thisStopDurationProbabilityDistribution = new ArrayList<>();
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(0, 10), 0.014));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(10, 20), 0.033));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(20, 30), 0.064));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(30, 40), 0.109));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(40, 50), 0.088));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(50, 60), 0.095));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(60, 75), 0.112));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(75, 90), 0.105));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(90, 120), 0.114));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(120, 150), 0.053));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(150, 180), 0.088));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(180, 240), 0.038));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(240, 300), 0.012));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(300, 420), 0.01));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(420, 540), 0.051));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(540, 660), 0.015));
		stopDurationProbabilityDistribution.put(makeServiceDurationPerCategoryKey("Employee Construction", "vehTyp1",
				GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficType.goodsTraffic.toString()),
			new EnumeratedDistribution<>(rng, thisStopDurationProbabilityDistribution));
		thisStopDurationProbabilityDistribution.clear();

		thisStopDurationProbabilityDistribution = new ArrayList<>();
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(0, 10), 0.02));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(10, 20), 0.027));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(20, 30), 0.061));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(30, 40), 0.045));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(40, 50), 0.068));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(50, 60), 0.083));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(60, 75), 0.112));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(75, 90), 0.114));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(90, 120), 0.146));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(120, 150), 0.058));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(150, 180), 0.114));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(180, 240), 0.036));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(240, 300), 0.022));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(300, 420), 0.007));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(420, 540), 0.065));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(540, 660), 0.023));
		stopDurationProbabilityDistribution.put(makeServiceDurationPerCategoryKey("Employee Construction", "vehTyp2",
				GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficType.goodsTraffic.toString()),
			new EnumeratedDistribution<>(rng, thisStopDurationProbabilityDistribution));
		thisStopDurationProbabilityDistribution.clear();

		thisStopDurationProbabilityDistribution = new ArrayList<>();
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(0, 10), 0.009));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(10, 20), 0.04));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(20, 30), 0.074));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(30, 40), 0.09));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(40, 50), 0.086));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(50, 60), 0.069));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(60, 75), 0.113));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(75, 90), 0.135));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(90, 120), 0.071));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(120, 150), 0.008));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(150, 180), 0.044));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(180, 240), 0.041));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(240, 300), 0.03));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(300, 420), 0.021));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(420, 540), 0.075));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(540, 660), 0.022));
		stopDurationProbabilityDistribution.put(makeServiceDurationPerCategoryKey("Employee Construction", "vehTyp3",
				GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficType.goodsTraffic.toString()),
			new EnumeratedDistribution<>(rng, thisStopDurationProbabilityDistribution));
		thisStopDurationProbabilityDistribution.clear();

		thisStopDurationProbabilityDistribution = new ArrayList<>();
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(0, 10), 0.036));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(10, 20), 0.055));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(20, 30), 0.018));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(30, 40), 0.236));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(40, 50), 0.073));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(50, 60), 0.018));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(75, 90), 0.164));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(120, 150), 0.091));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(150, 180), 0.109));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(180, 240), 0.055));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(240, 300), 0.018));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(300, 420), 0.055));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(420, 540), 0.055));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(540, 660), 0.018));
		stopDurationProbabilityDistribution.put(makeServiceDurationPerCategoryKey("Employee Construction", "vehTyp4",
				GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficType.goodsTraffic.toString()),
			new EnumeratedDistribution<>(rng, thisStopDurationProbabilityDistribution));
		thisStopDurationProbabilityDistribution.clear();

		thisStopDurationProbabilityDistribution = new ArrayList<>();
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(0, 10), 0.014));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(10, 20), 0.163));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(20, 30), 0.21));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(30, 40), 0.165));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(40, 50), 0.125));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(50, 60), 0.095));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(60, 75), 0.101));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(75, 90), 0.04));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(90, 120), 0.03));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(120, 150), 0.024));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(150, 180), 0.006));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(180, 240), 0.008));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(240, 300), 0.002));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(300, 420), 0.004));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(420, 540), 0.008));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(540, 660), 0.004));
		stopDurationProbabilityDistribution.put(makeServiceDurationPerCategoryKey("Employee Construction", "vehTyp5",
				GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficType.goodsTraffic.toString()),
			new EnumeratedDistribution<>(rng, thisStopDurationProbabilityDistribution));
		thisStopDurationProbabilityDistribution.clear();

		thisStopDurationProbabilityDistribution = new ArrayList<>();
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(0, 10), 0.072));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(10, 20), 0.093));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(20, 30), 0.123));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(30, 40), 0.113));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(40, 50), 0.137));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(50, 60), 0.081));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(60, 75), 0.102));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(75, 90), 0.087));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(90, 120), 0.079));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(120, 150), 0.032));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(150, 180), 0.021));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(180, 240), 0.018));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(240, 300), 0.016));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(300, 420), 0.009));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(420, 540), 0.014));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(540, 780), 0.002));
		stopDurationProbabilityDistribution.put(makeServiceDurationPerCategoryKey("Employee Secondary Sector Rest", "vehTyp1",
				GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficType.goodsTraffic.toString()),
			new EnumeratedDistribution<>(rng, thisStopDurationProbabilityDistribution));
		thisStopDurationProbabilityDistribution.clear();

		thisStopDurationProbabilityDistribution = new ArrayList<>();
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(0, 10), 0.062));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(10, 20), 0.14));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(20, 30), 0.093));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(30, 40), 0.115));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(40, 50), 0.133));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(50, 60), 0.102));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(60, 75), 0.098));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(75, 90), 0.071));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(90, 120), 0.067));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(120, 150), 0.038));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(150, 180), 0.027));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(180, 240), 0.011));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(240, 300), 0.009));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(300, 420), 0.024));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(420, 540), 0.011));
		stopDurationProbabilityDistribution.put(makeServiceDurationPerCategoryKey("Employee Secondary Sector Rest", "vehTyp2",
				GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficType.goodsTraffic.toString()),
			new EnumeratedDistribution<>(rng, thisStopDurationProbabilityDistribution));
		thisStopDurationProbabilityDistribution.clear();

		thisStopDurationProbabilityDistribution = new ArrayList<>();
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(0, 10), 0.051));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(10, 20), 0.214));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(20, 30), 0.146));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(30, 40), 0.129));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(40, 50), 0.10));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(50, 60), 0.072));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(60, 75), 0.083));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(75, 90), 0.063));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(90, 120), 0.054));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(120, 150), 0.02));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(150, 180), 0.016));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(180, 240), 0.022));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(240, 300), 0.008));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(300, 420), 0.007));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(420, 540), 0.011));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(540, 900), 0.003));
		stopDurationProbabilityDistribution.put(makeServiceDurationPerCategoryKey("Employee Secondary Sector Rest", "vehTyp3",
				GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficType.goodsTraffic.toString()),
			new EnumeratedDistribution<>(rng, thisStopDurationProbabilityDistribution));
		thisStopDurationProbabilityDistribution.clear();

		thisStopDurationProbabilityDistribution = new ArrayList<>();
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(10, 20), 0.163));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(20, 30), 0.224));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(30, 40), 0.153));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(40, 50), 0.061));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(50, 60), 0.173));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(60, 75), 0.082));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(75, 90), 0.122));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(240, 300), 0.01));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(420, 540), 0.01));
		stopDurationProbabilityDistribution.put(makeServiceDurationPerCategoryKey("Employee Secondary Sector Rest", "vehTyp4",
				GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficType.goodsTraffic.toString()),
			new EnumeratedDistribution<>(rng, thisStopDurationProbabilityDistribution));
		thisStopDurationProbabilityDistribution.clear();

		thisStopDurationProbabilityDistribution = new ArrayList<>();
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(0, 10), 0.003));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(10, 20), 0.195));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(20, 30), 0.225));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(30, 40), 0.16));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(40, 50), 0.143));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(50, 60), 0.089));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(60, 75), 0.075));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(75, 90), 0.031));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(90, 120), 0.048));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(120, 150), 0.01));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(180, 240), 0.003));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(240, 300), 0.007));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(300, 660), 0.009));
		stopDurationProbabilityDistribution.put(makeServiceDurationPerCategoryKey("Employee Secondary Sector Rest", "vehTyp5",
				GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficType.goodsTraffic.toString()),
			new EnumeratedDistribution<>(rng, thisStopDurationProbabilityDistribution));
		thisStopDurationProbabilityDistribution.clear();

		thisStopDurationProbabilityDistribution = new ArrayList<>();
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(0, 10), 0.057));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(10, 20), 0.108));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(20, 30), 0.093));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(30, 40), 0.133));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(40, 50), 0.133));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(50, 60), 0.11));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(60, 75), 0.102));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(75, 90), 0.064));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(90, 120), 0.104));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(120, 150), 0.049));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(150, 180), 0.015));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(180, 240), 0.015));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(240, 300), 0.003));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(300, 420), 0.005));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(420, 540), 0.006));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(540, 660), 0.003));
		stopDurationProbabilityDistribution.put(makeServiceDurationPerCategoryKey("Employee Retail", "vehTyp1",
				GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficType.goodsTraffic.toString()),
			new EnumeratedDistribution<>(rng, thisStopDurationProbabilityDistribution));
		thisStopDurationProbabilityDistribution.clear();

		thisStopDurationProbabilityDistribution = new ArrayList<>();
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(0, 10), 0.084));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(10, 20), 0.119));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(20, 30), 0.183));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(30, 40), 0.076));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(40, 50), 0.085));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(50, 60), 0.101));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(60, 75), 0.124));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(75, 90), 0.069));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(90, 120), 0.057));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(120, 150), 0.041));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(150, 180), 0.002));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(180, 240), 0.025));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(240, 300), 0.011));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(300, 420), 0.007));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(420, 540), 0.007));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(540, 660), 0.004));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(780, 900), 0.002));
		stopDurationProbabilityDistribution.put(makeServiceDurationPerCategoryKey("Employee Retail", "vehTyp2",
				GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficType.goodsTraffic.toString()),
			new EnumeratedDistribution<>(rng, thisStopDurationProbabilityDistribution));
		thisStopDurationProbabilityDistribution.clear();

		thisStopDurationProbabilityDistribution = new ArrayList<>();
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(0, 10), 0.103));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(10, 20), 0.23));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(20, 30), 0.193));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(30, 40), 0.08));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(40, 50), 0.065));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(50, 60), 0.071));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(60, 75), 0.072));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(75, 90), 0.044));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(90, 120), 0.054));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(120, 150), 0.035));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(150, 180), 0.009));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(180, 240), 0.013));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(240, 300), 0.014));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(300, 420), 0.01));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(420, 540), 0.003));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(540, 660), 0.003));
		stopDurationProbabilityDistribution.put(makeServiceDurationPerCategoryKey("Employee Retail", "vehTyp3",
				GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficType.goodsTraffic.toString()),
			new EnumeratedDistribution<>(rng, thisStopDurationProbabilityDistribution));
		thisStopDurationProbabilityDistribution.clear();

		thisStopDurationProbabilityDistribution = new ArrayList<>();
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(0, 10), 0.094));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(10, 20), 0.179));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(20, 30), 0.094));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(40, 50), 0.245));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(50, 60), 0.123));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(60, 75), 0.075));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(75, 90), 0.094));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(90, 120), 0.038));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(150, 180), 0.019));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(180, 240), 0.009));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(240, 300), 0.009));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(420, 540), 0.019));
		stopDurationProbabilityDistribution.put(makeServiceDurationPerCategoryKey("Employee Retail", "vehTyp4",
				GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficType.goodsTraffic.toString()),
			new EnumeratedDistribution<>(rng, thisStopDurationProbabilityDistribution));
		thisStopDurationProbabilityDistribution.clear();

		thisStopDurationProbabilityDistribution = new ArrayList<>();
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(0, 10), 0.066));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(10, 20), 0.063));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(20, 30), 0.142));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(30, 40), 0.165));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(40, 50), 0.135));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(50, 60), 0.102));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(60, 75), 0.122));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(75, 90), 0.033));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(90, 120), 0.086));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(120, 150), 0.043));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(150, 180), 0.023));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(180, 240), 0.017));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(240, 300), 0.003));
		stopDurationProbabilityDistribution.put(makeServiceDurationPerCategoryKey("Employee Retail", "vehTyp5",
				GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficType.goodsTraffic.toString()),
			new EnumeratedDistribution<>(rng, thisStopDurationProbabilityDistribution));
		thisStopDurationProbabilityDistribution.clear();

		thisStopDurationProbabilityDistribution = new ArrayList<>();
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(0, 10), 0.159));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(10, 20), 0.173));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(20, 30), 0.173));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(30, 40), 0.088));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(40, 50), 0.115));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(50, 60), 0.071));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(60, 75), 0.024));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(75, 90), 0.051));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(90, 120), 0.041));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(120, 150), 0.02));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(150, 180), 0.031));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(180, 240), 0.024));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(240, 300), 0.017));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(300, 420), 0.007));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(420, 540), 0.007));
		stopDurationProbabilityDistribution.put(makeServiceDurationPerCategoryKey("Employee Traffic/Parcels", "vehTyp1",
				GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficType.goodsTraffic.toString()),
			new EnumeratedDistribution<>(rng, thisStopDurationProbabilityDistribution));
		thisStopDurationProbabilityDistribution.clear();

		thisStopDurationProbabilityDistribution = new ArrayList<>();
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(0, 10), 0.292));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(10, 20), 0.135));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(20, 30), 0.062));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(30, 40), 0.197));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(40, 50), 0.051));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(50, 60), 0.079));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(60, 75), 0.022));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(75, 90), 0.045));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(90, 120), 0.056));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(120, 150), 0.034));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(180, 240), 0.006));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(300, 420), 0.022));
		stopDurationProbabilityDistribution.put(makeServiceDurationPerCategoryKey("Employee Traffic/Parcels", "vehTyp2",
				GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficType.goodsTraffic.toString()),
			new EnumeratedDistribution<>(rng, thisStopDurationProbabilityDistribution));
		thisStopDurationProbabilityDistribution.clear();

		thisStopDurationProbabilityDistribution = new ArrayList<>();
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(0, 10), 0.092));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(10, 20), 0.111));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(20, 30), 0.224));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(30, 40), 0.173));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(40, 50), 0.09));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(50, 60), 0.103));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(60, 75), 0.045));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(75, 90), 0.028));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(90, 120), 0.056));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(120, 150), 0.017));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(150, 180), 0.019));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(180, 240), 0.025));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(240, 300), 0.006));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(300, 420), 0.007));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(420, 540), 0.006));
		stopDurationProbabilityDistribution.put(makeServiceDurationPerCategoryKey("Employee Traffic/Parcels", "vehTyp3",
				GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficType.goodsTraffic.toString()),
			new EnumeratedDistribution<>(rng, thisStopDurationProbabilityDistribution));
		thisStopDurationProbabilityDistribution.clear();

		thisStopDurationProbabilityDistribution = new ArrayList<>();
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(0, 10), 0.146));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(10, 20), 0.098));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(20, 30), 0.146));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(30, 40), 0.195));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(40, 50), 0.268));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(50, 60), 0.012));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(60, 75), 0.024));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(75, 90), 0.024));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(90, 120), 0.024));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(120, 150), 0.037));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(180, 240), 0.012));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(300, 420), 0.012));
		stopDurationProbabilityDistribution.put(makeServiceDurationPerCategoryKey("Employee Traffic/Parcels", "vehTyp4",
				GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficType.goodsTraffic.toString()),
			new EnumeratedDistribution<>(rng, thisStopDurationProbabilityDistribution));
		thisStopDurationProbabilityDistribution.clear();

		thisStopDurationProbabilityDistribution = new ArrayList<>();
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(0, 10), 0.026));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(10, 20), 0.042));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(20, 30), 0.062));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(30, 40), 0.121));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(40, 50), 0.133));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(50, 60), 0.144));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(60, 75), 0.144));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(75, 90), 0.104));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(90, 120), 0.121));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(120, 150), 0.046));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(150, 180), 0.011));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(180, 240), 0.026));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(240, 300), 0.007));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(300, 420), 0.005));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(420, 900), 0.008));
		stopDurationProbabilityDistribution.put(makeServiceDurationPerCategoryKey("Employee Traffic/Parcels", "vehTyp5",
				GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficType.goodsTraffic.toString()),
			new EnumeratedDistribution<>(rng, thisStopDurationProbabilityDistribution));
		thisStopDurationProbabilityDistribution.clear();

		thisStopDurationProbabilityDistribution = new ArrayList<>();
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(0, 10), 0.061));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(10, 20), 0.093));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(20, 30), 0.101));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(30, 40), 0.125));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(40, 50), 0.125));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(50, 60), 0.101));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(60, 75), 0.124));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(75, 90), 0.08));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(90, 120), 0.093));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(120, 150), 0.046));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(150, 180), 0.013));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(180, 240), 0.017));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(240, 300), 0.011));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(300, 420), 0.004));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(420, 540), 0.005));
		stopDurationProbabilityDistribution.put(makeServiceDurationPerCategoryKey("Employee Tertiary Sector Rest", "vehTyp1",
				GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficType.goodsTraffic.toString()),
			new EnumeratedDistribution<>(rng, thisStopDurationProbabilityDistribution));
		thisStopDurationProbabilityDistribution.clear();

		thisStopDurationProbabilityDistribution = new ArrayList<>();
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(0, 10), 0.081));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(10, 20), 0.101));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(20, 30), 0.101));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(30, 40), 0.109));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(40, 50), 0.124));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(50, 60), 0.065));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(60, 75), 0.109));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(75, 90), 0.124));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(90, 120), 0.097));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(120, 150), 0.032));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(150, 180), 0.022));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(180, 240), 0.017));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(240, 300), 0.003));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(300, 420), 0.007));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(420, 540), 0.008));
		stopDurationProbabilityDistribution.put(makeServiceDurationPerCategoryKey("Employee Tertiary Sector Rest", "vehTyp2",
				GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficType.goodsTraffic.toString()),
			new EnumeratedDistribution<>(rng, thisStopDurationProbabilityDistribution));
		thisStopDurationProbabilityDistribution.clear();

		thisStopDurationProbabilityDistribution = new ArrayList<>();
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(0, 10), 0.052));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(10, 20), 0.114));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(20, 30), 0.155));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(30, 40), 0.111));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(40, 50), 0.151));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(50, 60), 0.112));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(60, 75), 0.125));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(75, 90), 0.043));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(90, 120), 0.051));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(120, 150), 0.026));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(150, 180), 0.014));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(180, 240), 0.016));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(240, 300), 0.011));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(300, 420), 0.007));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(420, 540), 0.009));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(660, 780), 0.003));
		stopDurationProbabilityDistribution.put(makeServiceDurationPerCategoryKey("Employee Tertiary Sector Rest", "vehTyp3",
				GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficType.goodsTraffic.toString()),
			new EnumeratedDistribution<>(rng, thisStopDurationProbabilityDistribution));
		thisStopDurationProbabilityDistribution.clear();

		thisStopDurationProbabilityDistribution = new ArrayList<>();
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(20, 30), 0.02));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(30, 40), 0.082));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(40, 50), 0.102));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(50, 60), 0.449));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(60, 75), 0.061));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(90, 120), 0.163));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(120, 150), 0.102));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(180, 240), 0.02));
		stopDurationProbabilityDistribution.put(makeServiceDurationPerCategoryKey("Employee Tertiary Sector Rest", "vehTyp4",
				GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficType.goodsTraffic.toString()),
			new EnumeratedDistribution<>(rng, thisStopDurationProbabilityDistribution));
		thisStopDurationProbabilityDistribution.clear();

		thisStopDurationProbabilityDistribution = new ArrayList<>();
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(0, 10), 0.02));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(10, 20), 0.02));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(20, 30), 0.151));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(30, 40), 0.296));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(40, 50), 0.156));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(50, 60), 0.065));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(60, 75), 0.121));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(75, 90), 0.05));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(90, 120), 0.075));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(150, 180), 0.01));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(180, 240), 0.015));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(240, 300), 0.01));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(300, 420), 0.005));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(420, 540), 0.005));
		stopDurationProbabilityDistribution.put(makeServiceDurationPerCategoryKey("Employee Tertiary Sector Rest", "vehTyp5",
				GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficType.goodsTraffic.toString()),
			new EnumeratedDistribution<>(rng, thisStopDurationProbabilityDistribution));
		thisStopDurationProbabilityDistribution.clear();

		// because no data für private persons; use average numbers of all employee categories
		thisStopDurationProbabilityDistribution = new ArrayList<>();
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(0, 10), 0.056));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(10, 20), 0.084));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(20, 30), 0.095));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(30, 40), 0.118));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(40, 50), 0.12));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(50, 60), 0.096));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(60, 75), 0.112));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(75, 90), 0.083));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(90, 120), 0.095));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(120, 150), 0.045));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(150, 180), 0.033));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(180, 240), 0.022));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(240, 300), 0.011));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(300, 420), 0.007));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(420, 540), 0.018));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(540, 660), 0.004));
		stopDurationProbabilityDistribution.put(makeServiceDurationPerCategoryKey("Inhabitants", "vehTyp1",
				GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficType.goodsTraffic.toString()),
			new EnumeratedDistribution<>(rng, thisStopDurationProbabilityDistribution));
		thisStopDurationProbabilityDistribution.clear();

		thisStopDurationProbabilityDistribution = new ArrayList<>();
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(0, 10), 0.077));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(10, 20), 0.093));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(20, 30), 0.103));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(30, 40), 0.092));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(40, 50), 0.098));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(50, 60), 0.091));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(60, 75), 0.108));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(75, 90), 0.092));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(90, 120), 0.095));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(120, 150), 0.043));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(150, 180), 0.035));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(180, 240), 0.024));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(240, 300), 0.01));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(300, 420), 0.011));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(420, 540), 0.021));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(540, 660), 0.007));
		stopDurationProbabilityDistribution.put(makeServiceDurationPerCategoryKey("Inhabitants", "vehTyp2",
				GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficType.goodsTraffic.toString()),
			new EnumeratedDistribution<>(rng, thisStopDurationProbabilityDistribution));
		thisStopDurationProbabilityDistribution.clear();

		thisStopDurationProbabilityDistribution = new ArrayList<>();
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(0, 10), 0.06));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(10, 20), 0.141));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(20, 30), 0.152));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(30, 40), 0.107));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(40, 50), 0.094));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(50, 60), 0.087));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(60, 75), 0.089));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(75, 90), 0.067));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(90, 120), 0.06));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(120, 150), 0.037));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(150, 180), 0.023));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(180, 240), 0.025));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(240, 300), 0.015));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(300, 420), 0.012));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(420, 540), 0.024));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(540, 660), 0.006));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(660, 780), 0.001));
		stopDurationProbabilityDistribution.put(makeServiceDurationPerCategoryKey("Inhabitants", "vehTyp3",
				GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficType.goodsTraffic.toString()),
			new EnumeratedDistribution<>(rng, thisStopDurationProbabilityDistribution));
		thisStopDurationProbabilityDistribution.clear();

		thisStopDurationProbabilityDistribution = new ArrayList<>();
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(0, 10), 0.062));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(10, 20), 0.11));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(20, 30), 0.12));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(30, 40), 0.144));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(40, 50), 0.151));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(50, 60), 0.129));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(60, 75), 0.062));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(75, 90), 0.079));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(90, 120), 0.041));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(120, 150), 0.031));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(150, 180), 0.019));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(180, 240), 0.014));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(240, 300), 0.007));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(300, 420), 0.014));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(420, 540), 0.014));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(540, 660), 0.002));
		stopDurationProbabilityDistribution.put(makeServiceDurationPerCategoryKey("Inhabitants", "vehTyp4",
				GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficType.goodsTraffic.toString()),
			new EnumeratedDistribution<>(rng, thisStopDurationProbabilityDistribution));
		thisStopDurationProbabilityDistribution.clear();

		thisStopDurationProbabilityDistribution = new ArrayList<>();
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(0, 10), 0.024));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(10, 20), 0.099));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(20, 30), 0.147));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(30, 40), 0.17));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(40, 50), 0.133));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(50, 60), 0.108));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(60, 75), 0.116));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(75, 90), 0.058));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(90, 120), 0.075));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(120, 150), 0.03));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(150, 180), 0.01));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(180, 240), 0.014));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(240, 300), 0.005));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(300, 420), 0.004));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(420, 660), 0.007));
		thisStopDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(660, 900), 0.002));
		stopDurationProbabilityDistribution.put(makeServiceDurationPerCategoryKey("Inhabitants", "vehTyp5",
				GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficType.goodsTraffic.toString()),
			new EnumeratedDistribution<>(rng, thisStopDurationProbabilityDistribution));
		thisStopDurationProbabilityDistribution.clear();

		return stopDurationProbabilityDistribution;
	}

	@Override
	public Map<String, EnumeratedDistribution<GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration>> createTourDistribution(
		RandomGenerator rng) {
		Map<String, EnumeratedDistribution<GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration>> tourDistribution = new HashMap<>();
		List<Pair<GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration, Double>> tourDurationProbabilityDistribution = new ArrayList<>();

		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(0, 4, 0.0, 30.0), 0.0005917893035900173));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(0, 4, 30.0, 60.0), 0.00021859484237437887));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(0, 4, 90.0, 120.0), 0.00037490287407786324));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(0, 4, 120.0, 180.0), 0.0004337321926125666));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(0, 4, 180.0, 240.0), 0.0005834182239827621));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(0, 4, 240.0, 300.0), 0.0005116938323661723));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(0, 4, 300.0, 360.0), 0.0005027065159573272));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(0, 4, 360.0, 420.0), 0.0006719740164147071));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(0, 4, 420.0, 480.0), 0.00022375027665644004));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(0, 4, 480.0, 540.0), 0.00022103749529549306));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(0, 4, 540.0, 600.0), 0.00022119440831885122));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(0, 4, 600.0, 660.0), 0.0002732185104003396));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(0, 4, 660.0, 720.0), 7.287567629774946e-05));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(0, 4, 720.0, 780.0), 0.0005090670761685264));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(0, 4, 780.0, 840.0), 0.0002169454122557984));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(0, 4, 840.0, 1080.0), 0.0016947794402011696));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(4, 5, 0.0, 30.0), 0.00033050926084770643));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(4, 5, 30.0, 60.0), 0.0004963985976117265));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(4, 5, 60.0, 90.0), 0.0009458837608304906));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(4, 5, 90.0, 120.0), 0.0006507941771038976));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(4, 5, 120.0, 180.0), 0.0002949035696660126));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(4, 5, 180.0, 240.0), 0.0005812406149568905));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(4, 5, 240.0, 300.0), 0.00072666224822023));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(4, 5, 300.0, 360.0), 0.0006017750128936798));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(4, 5, 360.0, 420.0), 0.0007696491628020603));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(4, 5, 420.0, 480.0), 0.0006951014583380694));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(4, 5, 480.0, 540.0), 0.0006675367479652174));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(4, 5, 540.0, 600.0), 0.0009951412624367468));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(4, 5, 600.0, 660.0), 0.0006193958232902363));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(4, 5, 660.0, 720.0), 0.0005496335422364244));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(4, 5, 720.0, 780.0), 0.000963763774344583));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(4, 5, 780.0, 840.0), 0.001585152586657775));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(4, 5, 840.0, 1080.0), 0.0022779973751500433));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(5, 6, 0.0, 30.0), 0.003678291745870938));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(5, 6, 30.0, 60.0), 0.0037749680865755936));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(5, 6, 60.0, 90.0), 0.0021464058981758467));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(5, 6, 90.0, 120.0), 0.0010105726369455444));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(5, 6, 120.0, 180.0), 0.0017166729332290624));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(5, 6, 180.0, 240.0), 0.001218657902054598));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(5, 6, 240.0, 300.0), 0.0019212859349972463));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(5, 6, 300.0, 360.0), 0.0018498349748915703));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(5, 6, 360.0, 420.0), 0.0020820722844894844));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(5, 6, 420.0, 480.0), 0.0033255032578691536));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(5, 6, 480.0, 540.0), 0.004499580798913233));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(5, 6, 540.0, 600.0), 0.004508722079694882));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(5, 6, 600.0, 660.0), 0.009460453046374911));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(5, 6, 660.0, 720.0), 0.008632039128635343));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(5, 6, 720.0, 780.0), 0.005173130409039029));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(5, 6, 780.0, 840.0), 0.0021287189901771954));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(5, 6, 840.0, 1080.0), 0.002735246591728173));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(6, 7, 0.0, 30.0), 0.015534599731489868));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(6, 7, 30.0, 60.0), 0.009424737666749776));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(6, 7, 60.0, 90.0), 0.003979757502241877));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(6, 7, 90.0, 120.0), 0.0026219034509082214));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(6, 7, 120.0, 180.0), 0.004373894821911171));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(6, 7, 180.0, 240.0), 0.005349695968407728));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(6, 7, 240.0, 300.0), 0.008398668008895199));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(6, 7, 300.0, 360.0), 0.013017576110359298));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(6, 7, 360.0, 420.0), 0.013178466937493282));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(6, 7, 420.0, 480.0), 0.015799261066253244));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(6, 7, 480.0, 540.0), 0.031932993774084484));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(6, 7, 540.0, 600.0), 0.056976770375347194));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(6, 7, 600.0, 660.0), 0.03411514635058722));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(6, 7, 660.0, 720.0), 0.010952547256934878));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(6, 7, 720.0, 780.0), 0.005071677294689363));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(6, 7, 780.0, 840.0), 0.002758017802376135));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(6, 7, 840.0, 1080.0), 0.003182481371327368));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(7, 8, 0.0, 30.0), 0.018010507239762663));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(7, 8, 30.0, 60.0), 0.009246211080247332));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(7, 8, 60.0, 90.0), 0.006297103845359016));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(7, 8, 90.0, 120.0), 0.003415561088528113));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(7, 8, 120.0, 180.0), 0.010918022744746231));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(7, 8, 180.0, 240.0), 0.011371721163141522));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(7, 8, 240.0, 300.0), 0.01861910064916215));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(7, 8, 300.0, 360.0), 0.015443374909900384));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(7, 8, 360.0, 420.0), 0.020470726990450452));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(7, 8, 420.0, 480.0), 0.030727618880727087));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(7, 8, 480.0, 540.0), 0.07364088624635841));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(7, 8, 540.0, 600.0), 0.04082061588575034));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(7, 8, 600.0, 660.0), 0.012935881167590665));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(7, 8, 660.0, 720.0), 0.005469250367916343));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(7, 8, 720.0, 780.0), 0.0030030673084490513));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(7, 8, 780.0, 840.0), 0.0011042643367551329));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(7, 8, 840.0, 1080.0), 0.0011327583672022575));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(8, 9, 0.0, 30.0), 0.015589932735904798));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(8, 9, 30.0, 60.0), 0.007157798082590814));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(8, 9, 60.0, 90.0), 0.006563655710107534));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(8, 9, 90.0, 120.0), 0.004888423230467872));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(8, 9, 120.0, 180.0), 0.01261126944262904));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(8, 9, 180.0, 240.0), 0.013275311108363174));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(8, 9, 240.0, 300.0), 0.011059737216827653));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(8, 9, 300.0, 360.0), 0.00980644443311104));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(8, 9, 360.0, 420.0), 0.013476523854959467));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(8, 9, 420.0, 480.0), 0.01766932338862498));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(8, 9, 480.0, 540.0), 0.013855266610087914));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(8, 9, 540.0, 600.0), 0.006090238569895901));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(8, 9, 600.0, 660.0), 0.00326688741194661));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(8, 9, 660.0, 720.0), 0.0009742217966822537));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(8, 9, 720.0, 780.0), 0.0008462163162537791));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(8, 9, 780.0, 840.0), 0.0009357453082055104));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(8, 9, 840.0, 1080.0), 0.0006867783494497427));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(9, 10, 0.0, 30.0), 0.011836581569331607));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(9, 10, 30.0, 60.0), 0.0060475163532472224));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(9, 10, 60.0, 90.0), 0.006091033719221284));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(9, 10, 90.0, 120.0), 0.004870323217391879));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(9, 10, 120.0, 180.0), 0.009852214102720915));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(9, 10, 180.0, 240.0), 0.006649077724867284));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(9, 10, 240.0, 300.0), 0.006549809619698136));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(9, 10, 300.0, 360.0), 0.00743649188225418));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(9, 10, 360.0, 420.0), 0.008370330719772223));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(9, 10, 420.0, 480.0), 0.006055410372169952));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(9, 10, 480.0, 540.0), 0.003221026290023441));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(9, 10, 540.0, 600.0), 0.00270804359225063));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(9, 10, 600.0, 660.0), 0.0011328763880567346));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(9, 10, 660.0, 720.0), 0.0005295062815147344));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(9, 10, 720.0, 780.0), 0.0005244739409173669));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(9, 10, 780.0, 840.0), 0.00022261373811852168));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(9, 10, 840.0, 1080.0), 0.0002976820307410009));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(10, 11, 0.0, 30.0), 0.0072347359578799255));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(10, 11, 30.0, 60.0), 0.005528762818372258));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(10, 11, 60.0, 90.0), 0.004301874597910846));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(10, 11, 90.0, 120.0), 0.002706271535768685));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(10, 11, 120.0, 180.0), 0.004461225555303183));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(10, 11, 180.0, 240.0), 0.003289266637558867));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(10, 11, 240.0, 300.0), 0.004773112389257731));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(10, 11, 300.0, 360.0), 0.004153307715767419));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(10, 11, 360.0, 420.0), 0.0023002274828502435));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(10, 11, 420.0, 480.0), 0.002295722460734858));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(10, 11, 480.0, 540.0), 0.0008008191218782178));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(10, 11, 540.0, 600.0), 0.0005302938593833011));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(10, 11, 600.0, 660.0), 0.00012017333498779025));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(10, 11, 660.0, 720.0), 0.00029497120761336085));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(10, 11, 720.0, 780.0), 7.442207741095891e-05));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(10, 11, 780.0, 840.0), 7.491510042413546e-05));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(11, 12, 0.0, 30.0), 0.005979044848708125));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(11, 12, 30.0, 60.0), 0.0030727725862362003));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(11, 12, 60.0, 90.0), 0.0018328582061095421));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(11, 12, 90.0, 120.0), 0.0015730248216810105));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(11, 12, 120.0, 180.0), 0.0025909176745678485));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(11, 12, 180.0, 240.0), 0.0023584284876344117));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(11, 12, 240.0, 300.0), 0.002888683132930499));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(11, 12, 300.0, 360.0), 0.0026723295114103734));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(11, 12, 360.0, 420.0), 0.001368034507711622));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(11, 12, 420.0, 480.0), 0.001322142609646873));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(11, 12, 480.0, 540.0), 0.00014896322977011863));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(11, 12, 540.0, 600.0), 0.00036793050573151096));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(11, 12, 600.0, 660.0), 0.0003024749417379503));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(11, 12, 660.0, 720.0), 7.263766179594998e-05));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(11, 12, 720.0, 780.0), 7.737798495114381e-05));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(11, 12, 840.0, 1080.0), 7.360037219024495e-05));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(12, 13, 0.0, 30.0), 0.005442934607459622));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(12, 13, 30.0, 60.0), 0.0023099603288455053));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(12, 13, 60.0, 90.0), 0.0015476125810207045));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(12, 13, 90.0, 120.0), 0.0015690710859882222));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(12, 13, 120.0, 180.0), 0.003155552178314994));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(12, 13, 180.0, 240.0), 0.0024715148201473933));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(12, 13, 240.0, 300.0), 0.00214638868043489));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(12, 13, 300.0, 360.0), 0.0017134793037846727));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(12, 13, 360.0, 420.0), 0.0009684921868733149));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(12, 13, 420.0, 480.0), 0.0005519992558366529));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(12, 13, 480.0, 540.0), 0.0004441672064981391));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(12, 13, 540.0, 600.0), 0.00022332686365997108));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(12, 13, 600.0, 660.0), 0.00023780343565208111));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(12, 13, 660.0, 720.0), 0.00014898555439278127));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(13, 14, 0.0, 30.0), 0.0065652971880044205));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(13, 14, 30.0, 60.0), 0.0033645458423904226));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(13, 14, 60.0, 90.0), 0.002247264924524252));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(13, 14, 90.0, 120.0), 0.0021755851670695867));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(13, 14, 120.0, 180.0), 0.00292250684836152));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(13, 14, 180.0, 240.0), 0.0029939610328467135));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(13, 14, 240.0, 300.0), 0.0013771262994841458));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(13, 14, 300.0, 360.0), 0.0005929387919824101));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(13, 14, 360.0, 420.0), 0.0007299574379337656));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(13, 14, 420.0, 480.0), 0.00015161310680499916));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(13, 14, 480.0, 540.0), 0.00022326623210165028));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(13, 14, 540.0, 600.0), 0.00021908720500178134));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(14, 15, 0.0, 30.0), 0.004700575755513116));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(14, 15, 30.0, 60.0), 0.002876930233578738));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(14, 15, 60.0, 90.0), 0.0012326059557891803));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(14, 15, 90.0, 120.0), 0.001688513011030605));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(14, 15, 120.0, 180.0), 0.0024148215923521744));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(14, 15, 180.0, 240.0), 0.0009664823712470381));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(14, 15, 240.0, 300.0), 0.0008158516384741175));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(14, 15, 300.0, 360.0), 0.0005326476409500361));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(14, 15, 360.0, 420.0), 0.00037447250704764534));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(14, 15, 420.0, 480.0), 7.278074100962308e-05));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(14, 15, 480.0, 540.0), 0.00015460621875651884));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(14, 15, 540.0, 600.0), 0.00022625636961834557));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(14, 15, 840.0, 1080.0), 7.369704340227916e-05));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(15, 16, 0.0, 30.0), 0.005421542133242069));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(15, 16, 30.0, 60.0), 0.0028543297205245563));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(15, 16, 60.0, 90.0), 0.001320449445343739));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(15, 16, 90.0, 120.0), 0.0011372744623221703));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(15, 16, 120.0, 180.0), 0.0011175546229352943));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(15, 16, 180.0, 240.0), 0.0005212091408906178));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(15, 16, 240.0, 300.0), 0.00025063117439263165));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(15, 16, 300.0, 360.0), 0.0002906557976189996));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(15, 16, 360.0, 420.0), 6.934683987097806e-05));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(15, 16, 420.0, 480.0), 7.198332684426051e-05));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(16, 17, 0.0, 30.0), 0.005997678933359281));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(16, 17, 30.0, 60.0), 0.0014450238860978966));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(16, 17, 60.0, 90.0), 0.0008909835110546583));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(16, 17, 90.0, 120.0), 0.0008692603958852261));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(16, 17, 120.0, 180.0), 0.0004645626068627116));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(16, 17, 180.0, 240.0), 0.0005161866418057845));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(16, 17, 240.0, 300.0), 0.00047492492382272117));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(16, 17, 300.0, 360.0), 7.348989097075777e-05));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(16, 17, 360.0, 420.0), 0.0003000342936128893));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(17, 18, 0.0, 30.0), 0.004621906661329853));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(17, 18, 30.0, 60.0), 0.0015152391398060199));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(17, 18, 60.0, 90.0), 0.0006769045119123614));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(17, 18, 90.0, 120.0), 0.00044820275277284946));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(17, 18, 120.0, 180.0), 0.0007140653752077821));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(17, 18, 180.0, 240.0), 0.0001502672132808765));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(17, 18, 240.0, 300.0), 0.0003842231300012746));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(17, 18, 300.0, 360.0), 0.00021634404805889257));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(18, 19, 0.0, 30.0), 0.0034023082743939916));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(18, 19, 30.0, 60.0), 0.0006251774232962365));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(18, 19, 60.0, 90.0), 0.00022163965781205308));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(18, 19, 90.0, 120.0), 7.360037219024495e-05));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(18, 19, 120.0, 180.0), 0.00045934601255169126));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(18, 19, 180.0, 240.0), 7.511874968194916e-05));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(18, 19, 240.0, 300.0), 0.0001486019187134722));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(18, 19, 300.0, 360.0), 7.505084488366769e-05));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(18, 19, 420.0, 480.0), 7.594714627228585e-05));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(19, 24, 0.0, 30.0), 0.005137034953520923));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(19, 24, 30.0, 60.0), 0.0010774703023578233));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(19, 24, 60.0, 90.0), 0.00048539418673270443));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(19, 24, 90.0, 120.0), 0.0002988049182984063));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(19, 24, 120.0, 180.0), 0.00032644209078127245));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(19, 24, 180.0, 240.0), 0.0005357497395368892));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(19, 24, 240.0, 300.0), 0.0002944914928100358));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(19, 24, 300.0, 360.0), 0.00022851651374757815));

		tourDistribution.put(GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficType.commercialPersonTraffic.toString(),
			new EnumeratedDistribution<>(rng, tourDurationProbabilityDistribution));
		tourDurationProbabilityDistribution.clear();

		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(0, 4, 0.0, 30.0), 0.0002666800577200411));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(0, 4, 30.0, 60.0), 0.0006395055678719748));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(0, 4, 60.0, 90.0), 0.0007110769046958423));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(0, 4, 90.0, 120.0), 0.0006665961628449491));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(0, 4, 120.0, 180.0), 0.0023195866923785575));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(0, 4, 180.0, 240.0), 0.00261751319938476));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(0, 4, 240.0, 300.0), 0.0021430032453503087));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(0, 4, 300.0, 360.0), 0.0029303876579925905));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(0, 4, 360.0, 420.0), 0.00283576618143643));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(0, 4, 420.0, 480.0), 0.0027188265347502893));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(0, 4, 480.0, 540.0), 0.002597768116531099));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(0, 4, 540.0, 600.0), 0.002659151494701916));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(0, 4, 600.0, 660.0), 0.0021738406044924437));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(0, 4, 660.0, 720.0), 0.0021949848461843176));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(0, 4, 720.0, 780.0), 0.0021801193011023083));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(0, 4, 780.0, 840.0), 0.001746033717539671));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(0, 4, 840.0, 1080.0), 0.00350888397405923));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(4, 5, 0.0, 30.0), 0.0006845643884312735));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(4, 5, 30.0, 60.0), 0.0004003126952082357));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(4, 5, 60.0, 90.0), 0.0008155012585632697));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(4, 5, 90.0, 120.0), 0.0010930534970200114));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(4, 5, 120.0, 180.0), 0.0011760353713952051));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(4, 5, 180.0, 240.0), 0.0019364061980548415));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(4, 5, 240.0, 300.0), 0.002953452881036028));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(4, 5, 300.0, 360.0), 0.002589370165068672));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(4, 5, 360.0, 420.0), 0.0025604405819583055));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(4, 5, 420.0, 480.0), 0.0034319041631081476));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(4, 5, 480.0, 540.0), 0.0033480025727905907));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(4, 5, 540.0, 600.0), 0.002175717502193024));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(4, 5, 600.0, 660.0), 0.0028036478238686957));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(4, 5, 660.0, 720.0), 0.0028759635193342887));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(4, 5, 720.0, 780.0), 0.0017584406503249872));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(4, 5, 780.0, 840.0), 0.0016742001219093045));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(4, 5, 840.0, 1080.0), 0.0020658205220468245));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(5, 6, 0.0, 30.0), 0.0017247403950228777));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(5, 6, 30.0, 60.0), 0.003090998236080484));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(5, 6, 60.0, 90.0), 0.0015209554995803177));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(5, 6, 90.0, 120.0), 0.0016533392810110293));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(5, 6, 120.0, 180.0), 0.003732306124403562));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(5, 6, 180.0, 240.0), 0.004106247357091271));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(5, 6, 240.0, 300.0), 0.003188442431357427));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(5, 6, 300.0, 360.0), 0.005929370570550301));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(5, 6, 360.0, 420.0), 0.005992695595693005));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(5, 6, 420.0, 480.0), 0.006390572360276255));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(5, 6, 480.0, 540.0), 0.00993732232424166));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(5, 6, 540.0, 600.0), 0.007917613781985494));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(5, 6, 600.0, 660.0), 0.00753055040114282));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(5, 6, 660.0, 720.0), 0.004839531706746983));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(5, 6, 720.0, 780.0), 0.003571294178536547));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(5, 6, 780.0, 840.0), 0.0022261075091276465));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(5, 6, 840.0, 1080.0), 0.0020123396391017526));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(6, 7, 0.0, 30.0), 0.00553085745500388));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(6, 7, 30.0, 60.0), 0.005164301035284355));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(6, 7, 60.0, 90.0), 0.0034287284279468384));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(6, 7, 90.0, 120.0), 0.003359657704287739));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(6, 7, 120.0, 180.0), 0.005963896679549981));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(6, 7, 180.0, 240.0), 0.006376396116305889));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(6, 7, 240.0, 300.0), 0.011553162434249647));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(6, 7, 300.0, 360.0), 0.01216390369869719));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(6, 7, 360.0, 420.0), 0.015303642980241483));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(6, 7, 420.0, 480.0), 0.01894502604909179));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(6, 7, 480.0, 540.0), 0.026995818384739457));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(6, 7, 540.0, 600.0), 0.03735238580259259));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(6, 7, 600.0, 660.0), 0.02007351137947408));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(6, 7, 660.0, 720.0), 0.007579189226621267));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(6, 7, 720.0, 780.0), 0.003806896198418994));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(6, 7, 780.0, 840.0), 0.0020371212990837376));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(6, 7, 840.0, 1080.0), 0.00246729057836831));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(7, 8, 0.0, 30.0), 0.007834929725170775));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(7, 8, 30.0, 60.0), 0.007875284751511802));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(7, 8, 60.0, 90.0), 0.0056369706407995695));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(7, 8, 90.0, 120.0), 0.007252792818630801));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(7, 8, 120.0, 180.0), 0.011595289158181222));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(7, 8, 180.0, 240.0), 0.01584695155572567));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(7, 8, 240.0, 300.0), 0.019385993489144607));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(7, 8, 300.0, 360.0), 0.01804569113072999));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(7, 8, 360.0, 420.0), 0.020338168968415053));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(7, 8, 420.0, 480.0), 0.03244941203821404));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(7, 8, 480.0, 540.0), 0.046986423884473));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(7, 8, 540.0, 600.0), 0.026127574804977814));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(7, 8, 600.0, 660.0), 0.006859707180170414));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(7, 8, 660.0, 720.0), 0.004053368732850601));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(7, 8, 720.0, 780.0), 0.0017728320836715625));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(7, 8, 780.0, 840.0), 0.0008117046283836942));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(7, 8, 840.0, 1080.0), 0.0014889766393137468));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(8, 9, 0.0, 30.0), 0.008702611915372131));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(8, 9, 30.0, 60.0), 0.009703391735884857));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(8, 9, 60.0, 90.0), 0.00833249802530372));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(8, 9, 90.0, 120.0), 0.008160824294542027));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(8, 9, 120.0, 180.0), 0.014522058792957903));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(8, 9, 180.0, 240.0), 0.019189639247661674));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(8, 9, 240.0, 300.0), 0.022628081955363144));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(8, 9, 300.0, 360.0), 0.018168175275565253));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(8, 9, 360.0, 420.0), 0.01830766579908246));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(8, 9, 420.0, 480.0), 0.022414786327228577));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(8, 9, 480.0, 540.0), 0.015454698179801149));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(8, 9, 540.0, 600.0), 0.00743339793333549));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(8, 9, 600.0, 660.0), 0.0028959167218627997));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(8, 9, 660.0, 720.0), 0.0011608823477359163));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(8, 9, 720.0, 780.0), 0.0006126324367099846));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(8, 9, 780.0, 840.0), 0.0007090395380022889));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(8, 9, 840.0, 1080.0), 0.0009650931773638335));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(9, 10, 0.0, 30.0), 0.010532384705529854));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(9, 10, 30.0, 60.0), 0.010106787618396446));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(9, 10, 60.0, 90.0), 0.007305519187631069));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(9, 10, 90.0, 120.0), 0.0065298278976416635));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(9, 10, 120.0, 180.0), 0.012991661099288086));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(9, 10, 180.0, 240.0), 0.011082392048301831));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(9, 10, 240.0, 300.0), 0.013735041027849332));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(9, 10, 300.0, 360.0), 0.012921165569106639));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(9, 10, 360.0, 420.0), 0.010187951930469277));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(9, 10, 420.0, 480.0), 0.0070071162811467125));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(9, 10, 480.0, 540.0), 0.003478434072337058));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(9, 10, 540.0, 600.0), 0.002487434148850001));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(9, 10, 600.0, 660.0), 0.0007617139935295275));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(9, 10, 660.0, 720.0), 0.0004794259473854554));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(9, 10, 720.0, 780.0), 0.00011828408353297643));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(9, 10, 780.0, 840.0), 0.0009221448817170415));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(10, 11, 0.0, 30.0), 0.0053803765038808364));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(10, 11, 30.0, 60.0), 0.00748440387556175));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(10, 11, 60.0, 90.0), 0.003817044622559703));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(10, 11, 90.0, 120.0), 0.0042559767658946045));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(10, 11, 120.0, 180.0), 0.004633517730561146));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(10, 11, 180.0, 240.0), 0.0040156278424527785));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(10, 11, 240.0, 300.0), 0.004097425621422603));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(10, 11, 300.0, 360.0), 0.00534407493573042));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(10, 11, 360.0, 420.0), 0.002849425985304954));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(10, 11, 420.0, 480.0), 0.0024443772372422234));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(10, 11, 480.0, 540.0), 0.0011258612568464076));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(10, 11, 540.0, 600.0), 0.0005966047093584399));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(10, 11, 600.0, 660.0), 0.0005779388889435179));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(10, 11, 660.0, 720.0), 0.0004527621290439082));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(10, 11, 720.0, 780.0), 0.00011727646428602624));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(10, 11, 780.0, 840.0), 0.00011130198744577025));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(11, 12, 0.0, 30.0), 0.0025301846046864363));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(11, 12, 30.0, 60.0), 0.002932856090944951));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(11, 12, 60.0, 90.0), 0.0015297442159744696));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(11, 12, 90.0, 120.0), 0.0016816440829740813));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(11, 12, 120.0, 180.0), 0.0023140070407952395));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(11, 12, 180.0, 240.0), 0.0013768767086426792));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(11, 12, 240.0, 300.0), 0.0019019317686819275));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(11, 12, 300.0, 360.0), 0.0015577691125463963));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(11, 12, 360.0, 420.0), 0.001499121306916632));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(11, 12, 420.0, 480.0), 0.0007361366421130972));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(11, 12, 480.0, 540.0), 0.0007423049940853575));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(11, 12, 540.0, 600.0), 0.00011130198744577025));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(11, 12, 660.0, 720.0), 0.00024243947114654707));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(11, 12, 720.0, 780.0), 0.000261579996858755));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(12, 13, 0.0, 30.0), 0.0021669594044717543));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(12, 13, 30.0, 60.0), 0.0033993161916113994));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(12, 13, 60.0, 90.0), 0.001870484877697732));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(12, 13, 90.0, 120.0), 0.0008448185262884799));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(12, 13, 120.0, 180.0), 0.002024573233571085));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(12, 13, 180.0, 240.0), 0.0021888099857994042));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(12, 13, 240.0, 300.0), 0.0021657834323017752));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(12, 13, 300.0, 360.0), 0.0010623089332746248));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(12, 13, 360.0, 420.0), 0.0006268095760401356));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(12, 13, 420.0, 480.0), 0.0005094532977538987));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(12, 13, 480.0, 540.0), 0.0004744090926784203));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(12, 13, 540.0, 600.0), 0.00016487328572417658));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(12, 13, 660.0, 720.0), 0.0001162996982120756));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(13, 14, 0.0, 30.0), 0.0033401411497772818));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(13, 14, 30.0, 60.0), 0.002492685695459365));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(13, 14, 60.0, 90.0), 0.0027064477589805068));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(13, 14, 90.0, 120.0), 0.0018052297053924354));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(13, 14, 120.0, 180.0), 0.0027984509294891498));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(13, 14, 180.0, 240.0), 0.0022758505657711914));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(13, 14, 240.0, 300.0), 0.0003535503655144059));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(13, 14, 300.0, 360.0), 0.0005890430396050117));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(13, 14, 360.0, 420.0), 0.0002319134363595028));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(13, 14, 420.0, 480.0), 0.00011617748025141993));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(13, 14, 480.0, 540.0), 0.0003690064941818713));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(13, 14, 540.0, 600.0), 0.0001650495071007077));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(13, 14, 600.0, 660.0), 0.00023113252306835525));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(13, 14, 840.0, 1080.0), 0.00017239206443126303));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(14, 15, 0.0, 30.0), 0.003543871129770451));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(14, 15, 30.0, 60.0), 0.0018407982276338393));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(14, 15, 60.0, 90.0), 0.0010649270862293423));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(14, 15, 90.0, 120.0), 0.0009538696044712171));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(14, 15, 120.0, 180.0), 0.0021318639289119572));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(14, 15, 180.0, 240.0), 0.0019740243143620277));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(14, 15, 240.0, 300.0), 0.0006157677659961421));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(14, 15, 300.0, 360.0), 0.0004035374922773149));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(14, 15, 360.0, 420.0), 0.00011607019237524387));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(14, 15, 420.0, 480.0), 0.0003938282727195195));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(14, 15, 480.0, 540.0), 0.00011130198744577025));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(14, 15, 600.0, 660.0), 0.00011942109323430472));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(15, 16, 0.0, 30.0), 0.00254340964132742));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(15, 16, 30.0, 60.0), 0.0017847751078888892));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(15, 16, 60.0, 90.0), 0.000841891386995212));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(15, 16, 90.0, 120.0), 0.0003543852337006742));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(15, 16, 120.0, 180.0), 0.0013974221085794884));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(15, 16, 180.0, 240.0), 0.0006229273683665316));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(15, 16, 240.0, 300.0), 0.00020579571489011056));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(15, 16, 300.0, 360.0), 0.0004809214516599411));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(15, 16, 360.0, 420.0), 0.00022514291890117063));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(15, 16, 420.0, 480.0), 0.00014748146383900364));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(15, 16, 720.0, 780.0), 0.00011605559293173729));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(16, 17, 0.0, 30.0), 0.0019634787835054656));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(16, 17, 30.0, 60.0), 0.000860670737476427));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(16, 17, 60.0, 90.0), 0.0003550148096943092));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(16, 17, 90.0, 120.0), 0.000855728546868917));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(16, 17, 120.0, 180.0), 0.0009283998993093458));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(16, 17, 180.0, 240.0), 0.00022795178106384156));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(16, 17, 240.0, 300.0), 0.00024119874825349313));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(16, 17, 420.0, 480.0), 0.00023429279224671318));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(16, 17, 480.0, 540.0), 0.00011727269965059726));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(16, 17, 660.0, 720.0), 0.00011130198744577025));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(17, 18, 0.0, 30.0), 0.0017099830161073832));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(17, 18, 30.0, 60.0), 0.0006015092064895483));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(17, 18, 60.0, 90.0), 0.00011819436012345105));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(17, 18, 90.0, 120.0), 0.0002279569151752547));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(17, 18, 120.0, 180.0), 0.0006440525787748041));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(17, 18, 180.0, 240.0), 0.0003142746964600832));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(17, 18, 300.0, 360.0), 0.00022788575876606104));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(17, 18, 360.0, 420.0), 0.0004761806298753505));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(17, 18, 480.0, 540.0), 0.00011727269965059726));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(18, 19, 0.0, 30.0), 0.0020011795184968267));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(18, 19, 30.0, 60.0), 0.00023620950461199452));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(18, 19, 60.0, 90.0), 0.00011935825257957617));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(18, 19, 90.0, 120.0), 0.00011130198744577025));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(18, 19, 120.0, 180.0), 0.00012222981614916706));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(18, 19, 180.0, 240.0), 0.0002377005397786721));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(18, 19, 240.0, 300.0), 0.00026373526728965034));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(18, 19, 300.0, 360.0), 0.000256086036315955));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(18, 19, 360.0, 420.0), 0.00011394287938236544));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(19, 24, 0.0, 30.0), 0.0021116872169622083));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(19, 24, 30.0, 60.0), 0.0003681765715703113));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(19, 24, 60.0, 90.0), 0.0004137833254678062));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(19, 24, 90.0, 120.0), 0.00025108497234833097));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(19, 24, 120.0, 180.0), 0.0007576827338029722));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(19, 24, 180.0, 240.0), 0.0005180490039062906));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(19, 24, 240.0, 300.0), 0.0004944106124208977));
		tourDurationProbabilityDistribution.add(
			Pair.create(new GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration(19, 24, 300.0, 360.0), 0.0002278857587658224));

		tourDistribution.put(GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficType.goodsTraffic.toString(),
			new EnumeratedDistribution<>(rng, tourDurationProbabilityDistribution));
		return tourDistribution;
	}

	@Override
	public EnumeratedDistribution<GenerateSmallScaleCommercialTrafficDemand.DurationsBounds> createTourStartTimeDistribution(
		String smallScaleCommercialTrafficType, RandomGenerator rng) {
		List<Pair<GenerateSmallScaleCommercialTrafficDemand.DurationsBounds, Double>> tourStartProbabilityDistribution = new ArrayList<>();
		if (smallScaleCommercialTrafficType.equals(GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficType.commercialPersonTraffic.toString())) {
			tourStartProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(0, 1), 0.002));
			tourStartProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(1, 2), 0.001));
			tourStartProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(2, 3), 0.001));
			tourStartProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(3, 4), 0.002));
			tourStartProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(4, 5), 0.008));
			tourStartProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(5, 6), 0.031));
			tourStartProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(6, 7), 0.144));
			tourStartProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(7, 8), 0.335));
			tourStartProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(8, 9), 0.182));
			tourStartProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(9, 10), 0.108));
			tourStartProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(10, 11), 0.057));
			tourStartProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(11, 12), 0.032));
			tourStartProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(12, 13), 0.021));
			tourStartProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(13, 14), 0.021));
			tourStartProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(14, 15), 0.019));
			tourStartProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(15, 16), 0.012));
			tourStartProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(16, 17), 0.009));
			tourStartProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(17, 18), 0.006));
			tourStartProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(18, 19), 0.004));
			tourStartProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(19, 20), 0.003));
			tourStartProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(20, 21), 0.001));
			tourStartProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(22, 23), 0.001));
		} else if (smallScaleCommercialTrafficType.equals(GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficType.goodsTraffic.toString())) {
			tourStartProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(0, 1), 0.008));
			tourStartProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(1, 2), 0.003));
			tourStartProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(2, 3), 0.008));
			tourStartProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(3, 4), 0.012));
			tourStartProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(4, 5), 0.028));
			tourStartProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(5, 6), 0.052));
			tourStartProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(6, 7), 0.115));
			tourStartProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(7, 8), 0.222));
			tourStartProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(8, 9), 0.197));
			tourStartProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(9, 10), 0.14));
			tourStartProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(10, 11), 0.076));
			tourStartProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(11, 12), 0.035));
			tourStartProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(12, 13), 0.022));
			tourStartProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(13, 14), 0.022));
			tourStartProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(14, 15), 0.021));
			tourStartProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(15, 16), 0.014));
			tourStartProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(16, 17), 0.008));
			tourStartProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(17, 18), 0.005));
			tourStartProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(18, 19), 0.004));
			tourStartProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(19, 20), 0.002));
			tourStartProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(20, 21), 0.001));
			tourStartProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(21, 22), 0.001));
			tourStartProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(22, 23), 0.002));
			tourStartProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(23, 24), 0.001));
		}
		return new EnumeratedDistribution<>(rng, tourStartProbabilityDistribution);
	}

	@Override
	public EnumeratedDistribution<GenerateSmallScaleCommercialTrafficDemand.DurationsBounds> createTourDurationTimeDistribution(
		String smallScaleCommercialTrafficType, RandomGenerator rng) {
		List<Pair<GenerateSmallScaleCommercialTrafficDemand.DurationsBounds, Double>> tourDurationProbabilityDistribution = new ArrayList<>();
		if (smallScaleCommercialTrafficType.equals(GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficType.commercialPersonTraffic.toString())) {
			tourDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(0, 1), 0.14));
			tourDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(1, 2), 0.066));
			tourDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(2, 3), 0.056));
			tourDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(3, 4), 0.052));
			tourDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(4, 5), 0.061));
			tourDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(5, 6), 0.063));
			tourDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(6, 7), 0.07));
			tourDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(7, 8), 0.086));
			tourDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(8, 9), 0.14));
			tourDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(9, 10), 0.122));
			tourDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(10, 11), 0.068));
			tourDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(11, 12), 0.031));
			tourDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(12, 13), 0.018));
			tourDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(13, 14), 0.01));
			tourDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(14, 15), 0.006));
			tourDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(15, 16), 0.003));
			tourDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(16, 17), 0.002));
			tourDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(17, 18), 0.001));
		} else if (smallScaleCommercialTrafficType.equals(GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficType.goodsTraffic.toString())) {
			tourDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(0, 1), 0.096));
			tourDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(1, 2), 0.074));
			tourDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(2, 3), 0.065));
			tourDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(3, 4), 0.071));
			tourDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(4, 5), 0.086));
			tourDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(5, 6), 0.084));
			tourDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(6, 7), 0.084));
			tourDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(7, 8), 0.101));
			tourDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(8, 9), 0.118));
			tourDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(9, 10), 0.092));
			tourDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(10, 11), 0.048));
			tourDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(11, 12), 0.027));
			tourDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(12, 13), 0.015));
			tourDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(13, 14), 0.011));
			tourDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(14, 15), 0.006));
			tourDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(15, 16), 0.004));
			tourDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(16, 17), 0.002));
			tourDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(17, 18), 0.001));
			tourDurationProbabilityDistribution.add(Pair.create(new GenerateSmallScaleCommercialTrafficDemand.DurationsBounds(18, 19), 0.001));
		}
		return new EnumeratedDistribution<>(rng, tourDurationProbabilityDistribution);
	}
}
