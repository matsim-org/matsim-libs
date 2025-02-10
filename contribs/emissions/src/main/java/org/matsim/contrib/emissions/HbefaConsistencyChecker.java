package org.matsim.contrib.emissions;


import com.google.common.collect.Iterables;

import javax.annotation.Nullable;
import java.util.*;

public class HbefaConsistencyChecker {

	/**
	 * Tests the read-in hbefa-maps and makes sure that:
	 * 	(1) Technology-column contains petrol or diesel
	 * 	(2) EmissionConcept contains neither petrol nor diesel
	 * 	(3) EmissionConcept and SizeClass contains "average" as fallback (used in technology-average-lookup)
	 * 	(4) Average table contain only average entries in VehicleAttributes
	 *
	 * 	Background of this consistency check is an error in the hbefa database, which causes the column names to be named wrong.
	 *
	 * @throws IllegalArgumentException with explanation, if one of the check listed above failed.
	 */
	static void checkConsistency(@Nullable Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> hbefaAvgWarm,
								 @Nullable Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> hbefaDetWarm,
								 @Nullable Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> hbefaAvgCold,
								 @Nullable Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> hbefaDetCold) throws IllegalArgumentException {

		Set<String> technologies = new HashSet<>();
		Set<String> sizeClass = new HashSet<>();
		Set<String> emConcepts = new HashSet<>();

		if(hbefaAvgWarm != null){
			// Test for the average warm tables
			for (var key : hbefaAvgWarm.keySet()){
				technologies.add(key.getVehicleAttributes().getHbefaTechnology());
				sizeClass.add(key.getVehicleAttributes().getHbefaSizeClass());
				emConcepts.add(key.getVehicleAttributes().getHbefaEmConcept());
			}

			// Test (4)
			if(technologies.size() != 1)
				throw new IllegalArgumentException("average warm table contains " + technologies.size() + " entries in technology-column. It should contain only \"average\". Technologies in table:" + technologies);
			if(!technologies.contains("average"))
				throw new IllegalArgumentException("average warm table contains " + technologies.iterator().next() + " key in technology-column. It should contain only \"average\"");

			if(sizeClass.size() != 1)
				throw new IllegalArgumentException("average cold table contains " + sizeClass.size() + " entries in size-class-column. It should contain only \"average\". Technologies in table:" + sizeClass);
			if(!sizeClass.contains("average"))
				throw new IllegalArgumentException("average cold table contains " + sizeClass.iterator().next() + " key in size-class-column. It should contain only \"average\"");

			if(emConcepts.size() != 1)
				throw new IllegalArgumentException("average warm table contains " + emConcepts.size() + " entries in emission-concept-column. It should contain only \"average\". Technologies in table:" + technologies);
			if(!emConcepts.contains("average"))
				throw new IllegalArgumentException("average warm table contains " + emConcepts.iterator().next() + " key in emission-concept-column. It should contain only \"average\"");

			technologies.clear();
			sizeClass.clear();
			emConcepts.clear();
		}

		if(hbefaAvgCold != null){
			// Test for average cold tables
			// Test for the average warm tables
			for (var key : hbefaAvgCold.keySet()){
				technologies.add(key.getVehicleAttributes().getHbefaTechnology());
				sizeClass.add(key.getVehicleAttributes().getHbefaSizeClass());
				emConcepts.add(key.getVehicleAttributes().getHbefaEmConcept());
			}

			// Test (4)
			if(technologies.size() != 1)
				throw new IllegalArgumentException("average cold table contains " + technologies.size() + " entries in technology-column. It should contain only \"average\". Technologies in table:" + technologies);
			if(!technologies.contains("average"))
				throw new IllegalArgumentException("average cold table contains " + technologies.iterator().next() + " key in technology-column. It should contain only \"average\"");

			if(sizeClass.size() != 1)
				throw new IllegalArgumentException("average cold table contains " + sizeClass.size() + " entries in size-class-column. It should contain only \"average\". Technologies in table:" + sizeClass);
			if(!sizeClass.contains("average"))
				throw new IllegalArgumentException("average cold table contains " + sizeClass.iterator().next() + " key in size-class-column. It should contain only \"average\"");

			if(emConcepts.size() != 1)
				throw new IllegalArgumentException("average cold table contains " + emConcepts.size() + " entries in emission-concept-column. It should contain only \"average\". Technologies in table:" + technologies);
			if(!emConcepts.contains("average"))
				throw new IllegalArgumentException("average cold table contains " + emConcepts.iterator().next() + " key in emission-concept-column. It should contain only \"average\"");

			technologies.clear();
			emConcepts.clear();
		}

		if(hbefaDetWarm != null){
			// Test for the detailed warm tables
			for (var key : hbefaDetWarm.keySet()){
				technologies.add(key.getVehicleAttributes().getHbefaTechnology());
				emConcepts.add(key.getVehicleAttributes().getHbefaEmConcept());
			}

			// Test (1)
			if( technologies.stream().filter(s -> s.matches(".*diesel.*")).toList().isEmpty() &&
				technologies.stream().filter(s -> s.matches(".*petrol.*")).toList().isEmpty())
				throw new CorruptedHbefaTableException(
					"Detailed warm table does not contains neither diesel nor petrol key in the technology-column. Unless you specifically filtered them out, you are probably using a corrupted hbefa table! \n"+
					"The first 5 entries of technology-column are: " + Iterables.limit(technologies, 5));

			if( !emConcepts.stream().filter(s -> s.matches(".*diesel.*")).toList().isEmpty() ||
				!emConcepts.stream().filter(s -> s.matches(".*petrol.*")).toList().isEmpty())
				throw new CorruptedHbefaTableException("Detailed warm table contains diesel or petrol key in the emission-concept-column. Unless you specifically put them in, you are probably using a corrupted hbefa table! \n"+
					"The first 5 entries of emission-concept-column are: " + Iterables.limit(emConcepts, 5));

			// Test (3)
			if( !emConcepts.contains("average") )
				throw new IllegalArgumentException("Emission-concept-column of warm detailed table does not contain average as key. " +
					"This may cause problems with the lookup-behaviors \"tryDetailedThenTechnologyAverageElseAbort\" and " +
					"\"tryDetailedThenTechnologyAverageThenAverageTable\". If you use one of these behaviors, make sure that an average entry exists! " +
					"If you want to proceed without average values, you can deactivate the ConsistencyCheck with EmissionsConfigGroup.setHbefaConsistencyChecker() ");

			if( !sizeClass.contains("average") )
				throw new IllegalArgumentException("Emission-concept-column of warm detailed table does not contain average as key. " +
					"This may cause problems with the lookup-behaviors \"tryDetailedThenTechnologyAverageElseAbort\" and " +
					"\"tryDetailedThenTechnologyAverageThenAverageTable\". If you use one of these behaviors, make sure that an average entry exists! " +
					"If you want to proceed without average values, you can deactivate the ConsistencyCheck with EmissionsConfigGroup.setHbefaConsistencyChecker() ");

			technologies.clear();
			emConcepts.clear();

		}

		if(hbefaDetCold != null){
			// Test for the detailed cold tables
			for (var key : hbefaDetCold.keySet()){
				technologies.add(key.getVehicleAttributes().getHbefaTechnology());
				emConcepts.add(key.getVehicleAttributes().getHbefaEmConcept());
			}

			// Test (1)
			if( technologies.stream().filter(s -> s.matches(".*diesel.*")).toList().isEmpty() &&
				technologies.stream().filter(s -> s.matches(".*petrol.*")).toList().isEmpty())
				throw new CorruptedHbefaTableException("Detailed cold table does not contains neither diesel nor petrol key in the technology-column. Unless you specifically filtered them out, you are probably using a corrupted hbefa table!");

			if( !emConcepts.stream().filter(s -> s.matches(".*diesel.*")).toList().isEmpty() ||
				!emConcepts.stream().filter(s -> s.matches(".*petrol.*")).toList().isEmpty())
				throw new CorruptedHbefaTableException("Detailed cold table contains diesel or petrol key in the emission-concept-column. Unless you specifically put them in, you are probably using a corrupted hbefa table!");

			// Test (3)
			if( !emConcepts.contains("average") )
				throw new IllegalArgumentException("Emission-concept-column of warm detailed table does not contain average as key. " +
					"This may cause problems with the lookup-behaviors \"tryDetailedThenTechnologyAverageElseAbort\" and " +
					"\"tryDetailedThenTechnologyAverageThenAverageTable\". If you use one of these behaviors, make sure that an average entry exists! " +
					"If you want to proceed without average values, you can deactivate the ConsistencyCheck with EmissionsConfigGroup.setHbefaConsistencyChecker() ");

			if( !sizeClass.contains("average") )
				throw new IllegalArgumentException("Emission-concept-column of warm detailed table does not contain average as key. " +
					"This may cause problems with the lookup-behaviors \"tryDetailedThenTechnologyAverageElseAbort\" and " +
					"\"tryDetailedThenTechnologyAverageThenAverageTable\". If you use one of these behaviors, make sure that an average entry exists! " +
					"If you want to proceed without average values, you can deactivate the ConsistencyCheck with EmissionsConfigGroup.setHbefaConsistencyChecker() ");

			technologies.clear();
			emConcepts.clear();
		}
	}

	static class CorruptedHbefaTableException extends IllegalArgumentException{
		public CorruptedHbefaTableException(String msg){
			super(
				 msg + "\n" +
				"When exporting hbefa-tables out of the Microsoft Access database, it can happen that technology-column and emission-concept-column are switched. There are the following possible fixes:\n" +
				"\t(1) Fixing the hbefa table by switching technology-class and emission-class in the header-entry. This may change your MATSim emission results.\n" +
				"\t(2) Disabling this Checker (with EmissionsConfigGroup.setHbefaConsistencyChecker()) and setting the EmissionConfigGroup lookup bahviour to \"directlyTryAverageTable\" (with EmissionsConfigGroup.setDetailedVsAverageLookupBehavior()). This will keep the current MATSim Behavior but results will be unprecise.\n" +
				"If neither of the 2 fixes worked, ask Jakub or KMT.");
		}
	}
}
