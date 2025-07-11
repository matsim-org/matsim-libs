package org.matsim.contrib.emissions;


import com.google.common.collect.Iterables;
import jakarta.annotation.Nullable;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.utils.collections.ArrayMap;

import java.util.*;

public class HbefaConsistencyChecker {
	/**
	 * Tests the read-in hbefa-tables and makes sure that: <br>
	 * 	(1) Average table contain only average entries in VehicleAttributes. <br>
	 * 	(2) Technology-column contains a valid HBEFA V4.1 technology key. <br>
	 * 	(3) EmissionConcept contains a valid HBEFA V4.1 emConcept leading key Id (given by vehCat). <br>
	 * 	(4) EmissionConcept and SizeClass contains "average" as fallback (used in technology-average-lookup, skipped if not needed by lookup-behavior). <br>
	 * 	(5) EmissionConcept leading key Id mapps correctly to the given vehCat. <br>
	 *
	 * 	Background of this consistency check is an error in the hbefa database, which causes the column names to be named wrong. This checker works
	 * 	with HBEFA version V4.1. Future versions may not work with this setup.
	 *
	 * @throws IllegalArgumentException with explanation, if one of the check listed above failed.
	 */
	static void checkConsistency(EmissionsConfigGroup.DetailedVsAverageLookupBehavior lookupBehavior,
								 @Nullable Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> hbefaAvgWarm,
								 @Nullable Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> hbefaDetWarm,
								 @Nullable Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> hbefaAvgCold,
								 @Nullable Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> hbefaDetCold) throws IllegalArgumentException {

		// First, prepare the allowed combinations

		// List of all possible technologies:
		List<String> allowedTechnologies = List.of(
			"average",
			HbefaTechnology.BIFUEL_CNG_PETROL.id,
			HbefaTechnology.BIFUEL_LPG_PETROL.id,
			HbefaTechnology.ELECTRICITY.id,
			HbefaTechnology.FLEX_FUEL_E85.id,
			HbefaTechnology.FUEL_CELL.id,
			HbefaTechnology.LCV.id,
			HbefaTechnology.PETROL_2S.id,
			HbefaTechnology.PETROL_4S.id,
			HbefaTechnology.PLUG_IN_HYBRID_DIESEL_ELECTRIC.id,
			HbefaTechnology.PLUG_IN_HYBRID_PETROL_ELECTRIC.id);

		// Each emConceptKey has one or multiple identifier(s), which depends on the vehCat to which the emConcept is mapped
		List<String> allowedEmConceptLeadingIds = new ArrayList<>();
		allowedEmConceptLeadingIds.add("average");
		allowedEmConceptLeadingIds.addAll(HbefaVehicleCategory.PASSENGER_CAR.ids);
		allowedEmConceptLeadingIds.addAll(HbefaVehicleCategory.LIGHT_COMMERCIAL_VEHICLE.ids);
		allowedEmConceptLeadingIds.addAll(HbefaVehicleCategory.HEAVY_GOODS_VEHICLE.ids);
		allowedEmConceptLeadingIds.addAll(HbefaVehicleCategory.URBAN_BUS.ids);
		allowedEmConceptLeadingIds.addAll(HbefaVehicleCategory.COACH.ids);
		allowedEmConceptLeadingIds.addAll(HbefaVehicleCategory.MOTORCYCLE.ids);

		Map<HbefaVehicleCategory, List<String>> vehCat2emConceptLeadingIds = new ArrayMap<>();
		vehCat2emConceptLeadingIds.put(HbefaVehicleCategory.PASSENGER_CAR, HbefaVehicleCategory.PASSENGER_CAR.ids);
		vehCat2emConceptLeadingIds.put(HbefaVehicleCategory.LIGHT_COMMERCIAL_VEHICLE, HbefaVehicleCategory.LIGHT_COMMERCIAL_VEHICLE.ids);
		vehCat2emConceptLeadingIds.put(HbefaVehicleCategory.HEAVY_GOODS_VEHICLE, HbefaVehicleCategory.HEAVY_GOODS_VEHICLE.ids);
		vehCat2emConceptLeadingIds.put(HbefaVehicleCategory.COACH, HbefaVehicleCategory.COACH.ids);
		vehCat2emConceptLeadingIds.put(HbefaVehicleCategory.URBAN_BUS, HbefaVehicleCategory.URBAN_BUS.ids);
		vehCat2emConceptLeadingIds.put(HbefaVehicleCategory.MOTORCYCLE, HbefaVehicleCategory.MOTORCYCLE.ids);

		if(hbefaAvgWarm != null){
			Set<String> technologiesAvg = new HashSet<>();
			Set<String> sizeClassesAvg = new HashSet<>();
			Set<String> emConceptsAvg = new HashSet<>();

			// Test for the average warm tables
			for (var key : hbefaAvgWarm.keySet()){
				technologiesAvg.add(key.getVehicleAttributes().getHbefaTechnology());
				sizeClassesAvg.add(key.getVehicleAttributes().getHbefaSizeClass());
				emConceptsAvg.add(key.getVehicleAttributes().getHbefaEmConcept());
			}

			// Test (1)
			if(technologiesAvg.size() != 1)
				throw new IllegalArgumentException("average warm table contains " + technologiesAvg.size() + " entries in technology-column. It should contain only \"average\". Technologies in table:" + technologiesAvg);
			if(!technologiesAvg.contains("average"))
				throw new IllegalArgumentException("average warm table contains " + technologiesAvg.iterator().next() + " key in technology-column. It should contain only \"average\"");

			if(sizeClassesAvg.size() != 1)
				throw new IllegalArgumentException("average cold table contains " + sizeClassesAvg.size() + " entries in size-class-column. It should contain only \"average\". Technologies in table:" + sizeClassesAvg);
			if(!sizeClassesAvg.contains("average"))
				throw new IllegalArgumentException("average cold table contains " + sizeClassesAvg.iterator().next() + " key in size-class-column. It should contain only \"average\"");

			if(emConceptsAvg.size() != 1)
				throw new IllegalArgumentException("average warm table contains " + emConceptsAvg.size() + " entries in emission-concept-column. It should contain only \"average\". Technologies in table:" + technologiesAvg);
			if(!emConceptsAvg.contains("average"))
				throw new IllegalArgumentException("average warm table contains " + emConceptsAvg.iterator().next() + " key in emission-concept-column. It should contain only \"average\"");

		}

		if(hbefaAvgCold != null){
			Set<String> technologiesAvg = new HashSet<>();
			Set<String> sizeClassesAvg = new HashSet<>();
			Set<String> emConceptsAvg = new HashSet<>();

			// Test for average cold tables
			// Test for the average warm tables
			for (var key : hbefaAvgCold.keySet()){
				technologiesAvg.add(key.getVehicleAttributes().getHbefaTechnology());
				sizeClassesAvg.add(key.getVehicleAttributes().getHbefaSizeClass());
				emConceptsAvg.add(key.getVehicleAttributes().getHbefaEmConcept());
			}

			// Test (1)
			if(technologiesAvg.size() != 1)
				throw new IllegalArgumentException("average cold table contains " + technologiesAvg.size() + " entries in technology-column. It should contain only \"average\". Technologies in table:" + technologiesAvg);
			if(!technologiesAvg.contains("average"))
				throw new IllegalArgumentException("average cold table contains " + technologiesAvg.iterator().next() + " key in technology-column. It should contain only \"average\"");

			if(sizeClassesAvg.size() != 1)
				throw new IllegalArgumentException("average cold table contains " + sizeClassesAvg.size() + " entries in size-class-column. It should contain only \"average\". Technologies in table:" + sizeClassesAvg);
			if(!sizeClassesAvg.contains("average"))
				throw new IllegalArgumentException("average cold table contains " + sizeClassesAvg.iterator().next() + " key in size-class-column. It should contain only \"average\"");

			if(emConceptsAvg.size() != 1)
				throw new IllegalArgumentException("average cold table contains " + emConceptsAvg.size() + " entries in emission-concept-column. It should contain only \"average\". Technologies in table:" + technologiesAvg);
			if(!emConceptsAvg.contains("average"))
				throw new IllegalArgumentException("average cold table contains " + emConceptsAvg.iterator().next() + " key in emission-concept-column. It should contain only \"average\"");

		}

		if(hbefaDetWarm != null){
			Set<String> technologiesDetailed = new HashSet<>();
			Set<String> sizeClassesDetailed = new HashSet<>();
			Set<String> emConceptsDetailed = new HashSet<>();
			Map<HbefaVehicleCategory, Set<String>> vehCat2emConceptsDetailed = new ArrayMap<>();

			// Test for the detailed warm tables
			for (var key : hbefaDetWarm.keySet()){
				technologiesDetailed.add(key.getVehicleAttributes().getHbefaTechnology());
				sizeClassesDetailed.add(key.getVehicleAttributes().getHbefaSizeClass());
				emConceptsDetailed.add(key.getVehicleAttributes().getHbefaEmConcept());
				vehCat2emConceptsDetailed.putIfAbsent(key.getVehicleCategory(), new HashSet<>());
			}

			// Test (2)
			if( technologiesDetailed.stream().noneMatch(allowedTechnologies::contains) ) {
				throw new CorruptedHbefaTableException(
					"Detailed warm table does not contain any technology key in the technology-column. Unless you specifically filtered them out, you are probably using a corrupted hbefa table! \n" +
					"The first 5 entries of technology-column are: " + Iterables.limit(technologiesDetailed, 5));
			}

			// Test (3)
			if( emConceptsDetailed.stream().noneMatch(s -> allowedEmConceptLeadingIds.stream().anyMatch(s::startsWith)) ){
				throw new CorruptedHbefaTableException(
					"Detailed warm table does not contain any emConcept key in the emission-concept-column. Unless you specifically filtered them out, you are probably using a corrupted hbefa table! \n" +
					"The first 5 entries of emission-concept-column are: " + Iterables.limit(emConceptsDetailed, 5));
			}

			// Test (4)
			if( lookupBehavior != EmissionsConfigGroup.DetailedVsAverageLookupBehavior.onlyTryDetailedElseAbort &&
				lookupBehavior != EmissionsConfigGroup.DetailedVsAverageLookupBehavior.directlyTryAverageTable ){
				if( !emConceptsDetailed.contains("average"))
					throw new IllegalArgumentException("""
					Emission-concept-column of warm detailed table does not contain average as key.\s
					This may cause problems with the lookup-behaviors "tryDetailedThenTechnologyAverageElseAbort" and\s
					"tryDetailedThenTechnologyAverageThenAverageTable". If you use one of these behaviors, make sure that an average entry exists!\s
					If you want to proceed without average values, you can deactivate the ConsistencyCheck with EmissionsConfigGroup.setHbefaConsistencyChecker()\s""");

				if( !sizeClassesDetailed.contains("average"))
					throw new IllegalArgumentException("""
					size-class-column of warm detailed table does not contain average as key.\s
					This may cause problems with the lookup-behaviors "tryDetailedThenTechnologyAverageElseAbort" and\s
					"tryDetailedThenTechnologyAverageThenAverageTable". If you use one of these behaviors, make sure that an average entry exists!\s
					If you want to proceed without average values, you can deactivate the ConsistencyCheck with EmissionsConfigGroup.setHbefaConsistencyChecker()\s""");
			}

			// Test (5)
			if ( !vehCat2emConceptsDetailed.entrySet().stream().allMatch(
				e -> vehCat2emConceptLeadingIds.get(e.getKey()).stream().anyMatch(
					ids -> e.getValue().stream().allMatch(ids::contains))) ){
				throw new IllegalArgumentException("""
					Emission-concept-column of warm detailed table has a emConcept, which's leading vehCat id does not match with the vehCat of the entry.\s
					Explanation: Each emConept starts with an acronym of the vehCat it is mapped to: passenger car -> "PC", urban bus -> "UBus".\s
					In the given table, there is at least one emConcept, which has a different leading Id. You should investigate this before continuing.""");
			}
		}

		if( hbefaDetCold != null ){
			Set<String> technologiesDetailed = new HashSet<>();
			Set<String> sizeClassesDetailed = new HashSet<>();
			Set<String> emConceptsDetailed = new HashSet<>();
			Map<HbefaVehicleCategory, Set<String>> vehCat2emConceptsDetailed = new ArrayMap<>();


			// Test for the detailed cold tables
			for (var key : hbefaDetCold.keySet()){
				technologiesDetailed.add(key.getVehicleAttributes().getHbefaTechnology());
				sizeClassesDetailed.add(key.getVehicleAttributes().getHbefaSizeClass());
				emConceptsDetailed.add(key.getVehicleAttributes().getHbefaEmConcept());
				vehCat2emConceptsDetailed.putIfAbsent(key.getVehicleCategory(), new HashSet<>());
			}

			// Test (2)
			if( technologiesDetailed.stream().noneMatch(allowedTechnologies::contains)) {
				throw new CorruptedHbefaTableException(
					"Detailed cold table does not contain any technology key in the technology-column. Unless you specifically filtered them out, you are probably using a corrupted hbefa table! \n" +
					"The first 5 entries of technology-column are: " + Iterables.limit(technologiesDetailed, 5));
			}

			// Test (3)
			if(emConceptsDetailed.stream().noneMatch(s -> allowedEmConceptLeadingIds.stream().anyMatch(s::startsWith))) {
				throw new CorruptedHbefaTableException(
					"Detailed cold table does not contain any emConcept key in the emission-concept-column. Unless you specifically put them in, you are probably using a corrupted hbefa table! \n" +
					"The first 5 entries of emission-concept-column are: " + Iterables.limit(emConceptsDetailed, 5));
			}

			// Test (4)
			if( lookupBehavior != EmissionsConfigGroup.DetailedVsAverageLookupBehavior.onlyTryDetailedElseAbort &&
				lookupBehavior != EmissionsConfigGroup.DetailedVsAverageLookupBehavior.directlyTryAverageTable) {
				if ( !emConceptsDetailed.contains("average"))
					throw new IllegalArgumentException("""
						Emission-concept-column of warm detailed table does not contain average as key.\s
						This may cause problems with the lookup-behaviors "tryDetailedThenTechnologyAverageElseAbort" and\s
						"tryDetailedThenTechnologyAverageThenAverageTable". If you use one of these behaviors, make sure that an average entry exists!\s
						If you want to proceed without average values, you can deactivate the ConsistencyCheck with EmissionsConfigGroup.setHbefaConsistencyChecker()""");

				if ( !sizeClassesDetailed.contains("average"))
					throw new IllegalArgumentException("""
						size-class-column of warm detailed table does not contain average as key.\s
						This may cause problems with the lookup-behaviors "tryDetailedThenTechnologyAverageElseAbort" and\s
						"tryDetailedThenTechnologyAverageThenAverageTable". If you use one of these behaviors, make sure that an average entry exists!\s
						If you want to proceed without average values, you can deactivate the ConsistencyCheck with EmissionsConfigGroup.setHbefaConsistencyChecker()""");
			}

			// Test (5)
			if ( !vehCat2emConceptsDetailed.entrySet().stream().allMatch(
				e -> vehCat2emConceptLeadingIds.get(e.getKey()).stream().anyMatch(
					ids -> e.getValue().stream().allMatch(ids::contains))) ){
				throw new IllegalArgumentException("""
					Emission-concept-column of cold detailed table has a emConcept, which's leading vehCat id does not match with the vehCat of the entry.\s
					Explanation: Each emConept starts with an acronym of the vehCat it is mapped to: passenger car -> "PC", urban bus -> "UBus".\s
					In the given table, there is at least one emConcept, which has a different leading Id. You should investigate this before continuing.""");
			}
		}
	}

	static class CorruptedHbefaTableException extends IllegalArgumentException{
		public CorruptedHbefaTableException(String msg){
			super(
				 msg + "\n" +
				"When exporting hbefa-tables out of the Microsoft Access database, it can happen that technology-column and emission-concept-column are switched. There are the following possible fixes:\n" +
				"\t(1) VSP provides encrypted default tables which were checked for this problem. If you just need a general emission setup, you can use these." +
				"\t(2) In case that you are using custom exported tables, you can fix the hbefa table by switching technology-class and emission-class in the header-entry. This may change your MATSim emission results.\n" +
				"\t(3) Disabling this Checker with EmissionsConfigGroup.setHbefaConsistencyChecker() and setting the EmissionConfigGroup lookup bahviour to \"directlyTryAverageTable\" (with EmissionsConfigGroup.setDetailedVsAverageLookupBehavior()). This will keep the current MATSim Behavior but results will be unprecise.\n" +
				"If neither of the 3 fixes worked, ask Aleks or KMT.");
		}
	}
}
