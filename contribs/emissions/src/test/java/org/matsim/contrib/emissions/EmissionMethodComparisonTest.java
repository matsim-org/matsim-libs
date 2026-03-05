package org.matsim.contrib.emissions;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;

import java.io.IOException;
import java.util.*;

public class EmissionMethodComparisonTest {

	private final static String HBEFA_4_1_PATH = "/Users/aleksander/Documents/VSP/PHEMTest/hbefa/";
	private final static String HBEFA_HOT_AVG = HBEFA_4_1_PATH + "EFA_HOT_Vehcat_2020_Average.csv";
	private final static String HBEFA_COLD_AVG = HBEFA_4_1_PATH + "EFA_ColdStart_Vehcat_2020_Average.csv";
	private final static String HBEFA_HOT_DET = HBEFA_4_1_PATH + "EFA_HOT_Subsegm_detailed_Car_Aleks_filtered.csv";
	private final static String HBEFA_COLD_DET = HBEFA_4_1_PATH + "EFA_ColdStart_Concept_2020_detailed_perTechAverage.csv";

	private final static String HBEFA_HOT_GRAD = HBEFA_4_1_PATH + "EFA_HOT_Concept_Aleks_Average_V1.1.csv";

	private static EmissionModulePack modulePackDetailed;
	private static EmissionModulePack modulePackGradient;

	private static EmissionsConfigGroup getEmissionsConfigGroup(EmissionsConfigGroup.EmissionsComputationMethod method, boolean gradientFile) {
		EmissionsConfigGroup ecg = new EmissionsConfigGroup();
		ecg.setHbefaVehicleDescriptionSource( EmissionsConfigGroup.HbefaVehicleDescriptionSource.usingVehicleTypeId );
		ecg.setEmissionsComputationMethod( method );
		ecg.setDetailedVsAverageLookupBehavior( EmissionsConfigGroup.DetailedVsAverageLookupBehavior.onlyTryDetailedElseAbort );
		ecg.setHbefaTableConsistencyCheckingLevel(EmissionsConfigGroup.HbefaTableConsistencyCheckingLevel.none);
		ecg.setDuplicateSubsegments( EmissionsConfigGroup.DuplicateSubsegments.useFirstDuplicate );
		ecg.setAverageWarmEmissionFactorsFile(HBEFA_HOT_AVG);
		ecg.setAverageColdEmissionFactorsFile(HBEFA_COLD_AVG);
		ecg.setDetailedColdEmissionFactorsFile(HBEFA_COLD_DET);

		if (gradientFile) {
			ecg.setDetailedWarmEmissionFactorsFile(HBEFA_HOT_GRAD);
		} else {
			ecg.setDetailedWarmEmissionFactorsFile(HBEFA_HOT_DET);
		}

		return ecg;
	}

	private static Tuple<HbefaVehicleCategory, HbefaVehicleAttributes> getVehicleAttributesTuple(String technology, String emConcept) {
		HbefaVehicleAttributes vehicleAttributes = new HbefaVehicleAttributes();
		vehicleAttributes.setHbefaTechnology(technology);
		vehicleAttributes.setHbefaEmConcept(emConcept);
		vehicleAttributes.setHbefaSizeClass("average");
		Tuple<HbefaVehicleCategory, HbefaVehicleAttributes> vehHbefaInfo = new Tuple<>(
			HbefaVehicleCategory.PASSENGER_CAR,
			vehicleAttributes);
		return vehHbefaInfo;
	}

	private static class EmissionModulePack {
		private final Map<EmissionsConfigGroup.EmissionsComputationMethod, EmissionModule> emissionMethod2emissionModule;

		EmissionModulePack(boolean gradientFile) {
			List<EmissionsConfigGroup> emissionsConfigGroups = new ArrayList<>();
			List<Config> configs = new ArrayList<>();
			List<Scenario> scenarios = new ArrayList<>();
			List<EventsManager> eventsManagers = new ArrayList<>();
			emissionMethod2emissionModule = new HashMap<>();

			for(var method : EmissionsConfigGroup.EmissionsComputationMethod.values()){
				emissionsConfigGroups.add(getEmissionsConfigGroup(method, gradientFile));
				configs.add(ConfigUtils.createConfig(emissionsConfigGroups.getLast()));
				scenarios.add(ScenarioUtils.loadScenario(configs.getLast()));
				eventsManagers.add(EventsUtils.createEventsManager(configs.getLast()));
				emissionMethod2emissionModule.put(method, new EmissionModule(scenarios.getLast(), eventsManagers.getLast()));
			}
		}

		public EmissionModule getModule(EmissionsConfigGroup.EmissionsComputationMethod method){
			return emissionMethod2emissionModule.get(method);
		}
	}

	@BeforeAll
	public static void prepare(){
		modulePackDetailed = new EmissionModulePack(false);
		modulePackGradient = new EmissionModulePack(true);
	}

	@TestFactory
	Collection<DynamicTest> speedCurveFactory(){
		List<Tuple<String, Double>> roadTypes = List.of(
			new Tuple<>("URB/Local/60", 60.),
			new Tuple<>("URB/Local/50", 50.),
			new Tuple<>("RUR/MW/100", 100.),
			new Tuple<>("RUR/MW/130", 130.),
			new Tuple<>("RUR/MW/>130", 140.)
		);

		List<Tuple<HbefaVehicleCategory, HbefaVehicleAttributes>> vehHbefaInfo = List.of(
			getVehicleAttributesTuple("petrol (4S)", "PC P Euro-4"),
			getVehicleAttributesTuple("petrol (4S)", "PC P Euro-6"),
			getVehicleAttributesTuple("diesel", "PC D Euro-4"),
			getVehicleAttributesTuple("diesel", "PC D Euro-6")
		);

		return roadTypes.stream().flatMap(r ->
			vehHbefaInfo.stream().map(v ->
				DynamicTest.dynamicTest(r + "; " + v, () -> speedCurves(r.getFirst(), r.getSecond(), v))
		)).toList();
	}

	@TestFactory
	Collection<DynamicTest> gradientCurveFactory(){
		List<Tuple<String, Double>> roadTypes = List.of(
			new Tuple<>("URB/Local/60", 60.),
			new Tuple<>("URB/Local/50", 50.),
			new Tuple<>("RUR/MW/100", 100.),
			new Tuple<>("RUR/MW/130", 130.),
			new Tuple<>("RUR/MW/>130", 140.)
		);

		List<Tuple<HbefaVehicleCategory, HbefaVehicleAttributes>> vehHbefaInfo = List.of(
			getVehicleAttributesTuple("petrol (4S)", "average"),
			getVehicleAttributesTuple("diesel", "average")
		);

		return roadTypes.stream().flatMap(r ->
			vehHbefaInfo.stream().map(v ->
				DynamicTest.dynamicTest(r + "; " + v, () -> gradientCurves(r.getFirst(), r.getSecond(), v))
			)).toList();
	}

	void speedCurves(String roadType, double freeVelocity, Tuple<HbefaVehicleCategory, HbefaVehicleAttributes> vehHbefaInfo) throws IOException {
		String filename = roadType.replace("/", "_") + "_" + vehHbefaInfo.getSecond().getHbefaEmConcept().replace(" ", "_");
		CSVPrinter writer = new CSVPrinter(IOUtils.getBufferedWriter("/Users/aleksander/Documents/VSP/PHEMTest/Pretoria/PAPER/InterpolationCurves/" + filename + ".csv"), CSVFormat.DEFAULT);

		writer.print("vel");

		for (var method : EmissionsConfigGroup.EmissionsComputationMethod.values()) {
			for (var component : List.of("CO", "CO2", "NOx")){
				writer.print(component + "_" + method);
			}
		}

		writer.println();

		for(double v = 0.05; v < freeVelocity; v += 0.05){
			writer.print(v);
			for (var method : EmissionsConfigGroup.EmissionsComputationMethod.values()) {
				var values = modulePackDetailed.getModule(method).getWarmEmissionAnalysisModule().calculateWarmEmissions(
					3600 / v,
					roadType,
					freeVelocity,
					1000,
					vehHbefaInfo
				);

				writer.print(values.get(Pollutant.CO));
				writer.print(values.get(Pollutant.CO2_TOTAL));
				writer.print(values.get(Pollutant.NOx));
			}
			writer.println();
		}

		writer.flush();
		writer.close();
	}

	void gradientCurves(String roadType, double freeVelocity, Tuple<HbefaVehicleCategory, HbefaVehicleAttributes> vehHbefaInfo) throws IOException {
		String filename = roadType.replace("/", "_") + "_" + vehHbefaInfo.getSecond().getHbefaEmConcept().replace(" ", "_") + "_G";
		CSVPrinter writer = new CSVPrinter(IOUtils.getBufferedWriter("/Users/aleksander/Documents/VSP/PHEMTest/Pretoria/PAPER/InterpolationCurves/" + filename + ".csv"), CSVFormat.DEFAULT);

		writer.print("grad");

		for (var method : EmissionsConfigGroup.EmissionsComputationMethod.values()) {
			for (var component : List.of("CO", "CO2", "NOx")){
				writer.print(component + "_" + method);
			}
		}

		writer.println();

		for(double g = -6; g <= 6+1e-6; g += 0.05){
			writer.print(g);
			for (var method : EmissionsConfigGroup.EmissionsComputationMethod.values()) {
				var values = modulePackGradient.getModule(method).getWarmEmissionAnalysisModule().calculateWarmEmissions(
					3600 / freeVelocity,
					roadType,
					freeVelocity,
					1000,
					1000 * (g/100),
					vehHbefaInfo
				);

				writer.print(values.get(Pollutant.CO));
				writer.print(values.get(Pollutant.CO2_TOTAL));
				writer.print(values.get(Pollutant.NOx));
			}
			writer.println();
		}

		writer.flush();
		writer.close();
	}

}
