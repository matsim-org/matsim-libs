package org.matsim.smallScaleCommercialTrafficGeneration;

import org.apache.commons.math3.distribution.EnumeratedDistribution;
import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.util.Pair;
import org.matsim.facilities.ActivityFacility;
import org.matsim.smallScaleCommercialTrafficGeneration.GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficType;
import org.matsim.smallScaleCommercialTrafficGeneration.SmallScaleCommercialTrafficUtils.StructuralAttribute;
import org.matsim.smallScaleCommercialTrafficGeneration.data.GetGenerationRates;

import java.util.*;

public class DefaultVehicleSelection implements VehicleSelection{
	private final EnumMap<SmallScaleCommercialTrafficType,
		Map<Integer, Map<StructuralAttribute, Double>>> generationRatesStartByType = new EnumMap<>(SmallScaleCommercialTrafficType.class);
	private final EnumMap<SmallScaleCommercialTrafficType,
		Map<Integer, Map<StructuralAttribute, Double>>> generationRatesStopByType  = new EnumMap<>(SmallScaleCommercialTrafficType.class);
	private final EnumMap<SmallScaleCommercialTrafficType,
		Map<String, Map<StructuralAttribute, Double>>> commitmentRatesStartByType = new EnumMap<>(SmallScaleCommercialTrafficType.class);
	private final EnumMap<SmallScaleCommercialTrafficType,
		Map<String, Map<StructuralAttribute, Double>>> commitmentRatesStopByType  = new EnumMap<>(SmallScaleCommercialTrafficType.class);

	@Override
	public List<StructuralAttribute> getAllCategories() {
		return List.of(
			StructuralAttribute.EMPLOYEE_PRIMARY,
			StructuralAttribute.EMPLOYEE_CONSTRUCTION,
			StructuralAttribute.EMPLOYEE_SECONDARY,
			StructuralAttribute.EMPLOYEE_RETAIL,
			StructuralAttribute.EMPLOYEE_TRAFFIC,
			StructuralAttribute.EMPLOYEE_TERTIARY,
			StructuralAttribute.INHABITANTS
		);
	}

	@Override
	public OdMatrixEntryInformation getOdMatrixEntryInformation(int purpose, String modeORvehType, SmallScaleCommercialTrafficType smallScaleCommercialTrafficType) {

			generationRatesStartByType.computeIfAbsent(smallScaleCommercialTrafficType,
				t -> GetGenerationRates.setGenerationRates(t, "start"));
			generationRatesStopByType.computeIfAbsent(smallScaleCommercialTrafficType,
				t -> GetGenerationRates.setGenerationRates(t, "stop"));
			commitmentRatesStartByType.computeIfAbsent(smallScaleCommercialTrafficType,
				t -> GetGenerationRates.setCommitmentRates(t, "start"));
			commitmentRatesStopByType.computeIfAbsent(smallScaleCommercialTrafficType,
				t -> GetGenerationRates.setCommitmentRates(t, "stop"));

		VehicleSelection.OdMatrixEntryInformation information = new OdMatrixEntryInformation();

		if (smallScaleCommercialTrafficType.equals(SmallScaleCommercialTrafficType.commercialPersonTraffic)) {
			//generate start category distribution based on generation rates
			List<Pair<StructuralAttribute, Double>> pairsStarting = new ArrayList<>();
			generationRatesStartByType.get(smallScaleCommercialTrafficType).get(purpose).forEach((key, value) -> {
				if (key.equals(StructuralAttribute.EMPLOYEE))
					return;
				pairsStarting.add(Pair.create(key, value));
			});
			information.startCategoryDistribution = new EnumeratedDistribution<>(new MersenneTwister(4711), pairsStarting);

			//generate stop category distribution based on generation rates
			List<Pair<StructuralAttribute, Double>> pairsStopping = new ArrayList<>();
			generationRatesStopByType.get(smallScaleCommercialTrafficType).get(purpose).forEach((key, value) -> {
				if (key.equals(StructuralAttribute.EMPLOYEE))
					return;
				pairsStopping.add(Pair.create(key, value));
			});
			information.stopCategoryDistribution = new EnumeratedDistribution<>(new MersenneTwister(4711), pairsStopping);
		}
		else {
			//generate start category distribution based on generation rates
			List<Pair<StructuralAttribute, Double>> pairsStarting = new ArrayList<>();
			generationRatesStartByType.get(smallScaleCommercialTrafficType).get(purpose).forEach((key, value) -> {
				if (key.equals(StructuralAttribute.EMPLOYEE) || commitmentRatesStartByType.get(smallScaleCommercialTrafficType)
					.get(purpose + "_" + modeORvehType.substring(modeORvehType.length() - 1))
					.get(key) == null)
					return;
				Double commitmentFactor = commitmentRatesStartByType.get(smallScaleCommercialTrafficType)
					.get(purpose + "_" + modeORvehType.substring(modeORvehType.length() - 1))
					.get(key);

				pairsStarting.add(Pair.create(key, value * commitmentFactor));
			});
			information.startCategoryDistribution = new EnumeratedDistribution<>(new MersenneTwister(4711), pairsStarting);

			//generate stop category distribution based on generation rates
			List<Pair<StructuralAttribute, Double>> pairsStopping = new ArrayList<>();
			generationRatesStopByType.get(smallScaleCommercialTrafficType).get(purpose).forEach((key, value) -> {
				if (key.equals(StructuralAttribute.EMPLOYEE) || commitmentRatesStopByType.get(smallScaleCommercialTrafficType)
					.get(purpose + "_" + modeORvehType.substring(modeORvehType.length() - 1))
					.get(key) == null)
					return;
				Double commitmentFactor = commitmentRatesStopByType.get(smallScaleCommercialTrafficType)
					.get(purpose + "_" + modeORvehType.substring(modeORvehType.length() - 1))
					.get(key);
				pairsStopping.add(Pair.create(key, value * commitmentFactor));
			});
			information.stopCategoryDistribution = new EnumeratedDistribution<>(new MersenneTwister(4711), pairsStopping);
		}

		if (smallScaleCommercialTrafficType.equals(SmallScaleCommercialTrafficType.commercialPersonTraffic)) {
			if (purpose == 1) {
				information.possibleVehicleTypes = new String[]{"vwCaddy", "e_SpaceTourer"};
				information.occupancyRate = 1.5;
			} else if (purpose == 2) {
				information.possibleVehicleTypes = new String[]{"vwCaddy", "e_SpaceTourer"};
				information.occupancyRate = 1.6;
			} else if (purpose == 3) {
				information.possibleVehicleTypes = new String[]{"golf1.4", "c_zero"};
				information.occupancyRate = 1.2;
			} else if (purpose == 4) {
				information.possibleVehicleTypes = new String[]{"golf1.4", "c_zero"};
				information.occupancyRate = 1.2;
			} else if (purpose == 5) {
				information.possibleVehicleTypes = new String[]{"mercedes313", "e_SpaceTourer"};
				information.occupancyRate = 1.7;
			}
		} else if (smallScaleCommercialTrafficType.equals(SmallScaleCommercialTrafficType.goodsTraffic)) {
			information.occupancyRate = 1.;
			switch (modeORvehType) {
				case "vehTyp1" ->
					information.possibleVehicleTypes = new String[]{"vwCaddy", "e_SpaceTourer"}; // possible to add more types, see source
				case "vehTyp2" -> information.possibleVehicleTypes = new String[]{"mercedes313", "e_SpaceTourer"};
				case "vehTyp3", "vehTyp4" ->
					information.possibleVehicleTypes = new String[]{"light8t", "truck8t", "light8t_electro", "truck8t_electro"};
				case "vehTyp5" -> information.possibleVehicleTypes = new String[]{"medium18t", "medium18t_electro", "truck18t", "truck18t_electro"};
			}
		}
		return information;
	}
}
