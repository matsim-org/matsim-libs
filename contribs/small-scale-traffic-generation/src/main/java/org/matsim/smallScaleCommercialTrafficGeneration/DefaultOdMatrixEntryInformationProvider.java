package org.matsim.smallScaleCommercialTrafficGeneration;

import org.apache.commons.math3.distribution.EnumeratedDistribution;
import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.util.Pair;
import org.matsim.smallScaleCommercialTrafficGeneration.GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficSegment;
import org.matsim.smallScaleCommercialTrafficGeneration.SmallScaleCommercialTrafficUtils.ZoneAttribute;
import org.matsim.smallScaleCommercialTrafficGeneration.data.GetGenerationRates;

import java.util.*;

public class DefaultOdMatrixEntryInformationProvider implements OdMatrixEntryInformationProvider{
	private final VehicleTypeSelection vehicleTypeSelection;
	private final EnumMap<SmallScaleCommercialTrafficSegment,
		Map<Integer, Map<ZoneAttribute, Double>>> generationRatesStartByType = new EnumMap<>( SmallScaleCommercialTrafficSegment.class);
	private final EnumMap<SmallScaleCommercialTrafficSegment,
		Map<Integer, Map<ZoneAttribute, Double>>> generationRatesStopByType  = new EnumMap<>( SmallScaleCommercialTrafficSegment.class);
	private final EnumMap<SmallScaleCommercialTrafficSegment,
		Map<String, Map<ZoneAttribute, Double>>> commitmentRatesStartByType = new EnumMap<>( SmallScaleCommercialTrafficSegment.class);
	private final EnumMap<SmallScaleCommercialTrafficSegment,
		Map<String, Map<ZoneAttribute, Double>>> commitmentRatesStopByType  = new EnumMap<>( SmallScaleCommercialTrafficSegment.class);

	public DefaultOdMatrixEntryInformationProvider() {
		this(new DefaultVehicleTypeSelection());
	}

	public DefaultOdMatrixEntryInformationProvider(VehicleTypeSelection vehicleTypeSelection) {
		this.vehicleTypeSelection = Objects.requireNonNull(vehicleTypeSelection);
	}

	@Override
	public List<ZoneAttribute> getAllCategories() {
		return List.of(
			ZoneAttribute.EMPLOYEE_PRIMARY,
			ZoneAttribute.EMPLOYEE_CONSTRUCTION,
			ZoneAttribute.EMPLOYEE_SECONDARY,
			ZoneAttribute.EMPLOYEE_RETAIL,
			ZoneAttribute.EMPLOYEE_TRAFFIC,
			ZoneAttribute.EMPLOYEE_TERTIARY,
			ZoneAttribute.INHABITANTS
		);
	}

	@Override
	public OdMatrixEntryInformation getOdMatrixEntryInformation(int purpose, String modeORvehType, SmallScaleCommercialTrafficSegment smallScaleCommercialTrafficSegment ) {

			generationRatesStartByType.computeIfAbsent( smallScaleCommercialTrafficSegment,
				t -> GetGenerationRates.setGenerationRates(t, "start"));
			generationRatesStopByType.computeIfAbsent( smallScaleCommercialTrafficSegment,
				t -> GetGenerationRates.setGenerationRates(t, "stop"));
			commitmentRatesStartByType.computeIfAbsent( smallScaleCommercialTrafficSegment,
				t -> GetGenerationRates.setCommitmentRates(t, "start"));
			commitmentRatesStopByType.computeIfAbsent( smallScaleCommercialTrafficSegment,
				t -> GetGenerationRates.setCommitmentRates(t, "stop"));

		OdMatrixEntryInformation information = new OdMatrixEntryInformation();

		if ( smallScaleCommercialTrafficSegment.equals( SmallScaleCommercialTrafficSegment.commercialPersonTraffic )) {
			//generate start category distribution based on generation rates
			List<Pair<ZoneAttribute, Double>> pairsStarting = new ArrayList<>();
			generationRatesStartByType.get( smallScaleCommercialTrafficSegment ).get(purpose ).forEach(( key, value) -> {
				if (key.equals( ZoneAttribute.EMPLOYEE ))
					return;
				pairsStarting.add(Pair.create(key, value));
			} );
			information.startCategoryDistribution = new EnumeratedDistribution<>(new MersenneTwister(4711), pairsStarting);

			//generate stop category distribution based on generation rates
			List<Pair<ZoneAttribute, Double>> pairsStopping = new ArrayList<>();
			generationRatesStopByType.get( smallScaleCommercialTrafficSegment ).get(purpose ).forEach(( key, value) -> {
				if (key.equals( ZoneAttribute.EMPLOYEE ))
					return;
				pairsStopping.add(Pair.create(key, value));
			} );
			information.stopCategoryDistribution = new EnumeratedDistribution<>(new MersenneTwister(4711), pairsStopping);
		}
		else {
			//generate start category distribution based on generation rates
			List<Pair<ZoneAttribute, Double>> pairsStarting = new ArrayList<>();
			generationRatesStartByType.get( smallScaleCommercialTrafficSegment ).get(purpose ).forEach(( key, value) -> {
				if (key.equals( ZoneAttribute.EMPLOYEE ) || commitmentRatesStartByType.get( smallScaleCommercialTrafficSegment )
				                                                                      .get(purpose + "_" + modeORvehType.substring(modeORvehType.length() - 1))
				                                                                      .get(key) == null)
					return;
				Double commitmentFactor = commitmentRatesStartByType.get( smallScaleCommercialTrafficSegment )
					.get(purpose + "_" + modeORvehType.substring(modeORvehType.length() - 1))
					.get(key);

				pairsStarting.add(Pair.create(key, value * commitmentFactor));
			} );
			information.startCategoryDistribution = new EnumeratedDistribution<>(new MersenneTwister(4711), pairsStarting);

			//generate stop category distribution based on generation rates
			List<Pair<ZoneAttribute, Double>> pairsStopping = new ArrayList<>();
			generationRatesStopByType.get( smallScaleCommercialTrafficSegment ).get(purpose ).forEach(( key, value) -> {
				if (key.equals( ZoneAttribute.EMPLOYEE ) || commitmentRatesStopByType.get( smallScaleCommercialTrafficSegment )
				                                                                     .get(purpose + "_" + modeORvehType.substring(modeORvehType.length() - 1))
				                                                                     .get(key) == null)
					return;
				Double commitmentFactor = commitmentRatesStopByType.get( smallScaleCommercialTrafficSegment )
					.get(purpose + "_" + modeORvehType.substring(modeORvehType.length() - 1))
					.get(key);
				pairsStopping.add(Pair.create(key, value * commitmentFactor));
			} );
			information.stopCategoryDistribution = new EnumeratedDistribution<>(new MersenneTwister(4711), pairsStopping);
		}

		VehicleTypeSelection.VehicleTypeInformation vehicleTypeInformation = vehicleTypeSelection.getVehicleTypeInformation(
			purpose, modeORvehType, smallScaleCommercialTrafficSegment );
		if (vehicleTypeInformation != null) {
			information.possibleVehicleTypes = vehicleTypeInformation.possibleVehicleTypes();
			information.occupancyRate = vehicleTypeInformation.occupancyRate();
		}
		return information;
	}
}
