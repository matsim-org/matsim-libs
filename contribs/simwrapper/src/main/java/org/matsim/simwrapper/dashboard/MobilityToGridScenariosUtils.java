package org.matsim.simwrapper.dashboard;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.emissions.HbefaVehicleCategory;
import org.matsim.vehicles.EngineInformation;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import java.util.Map;

/**
 * Utils class for plugging together different policies in the same scenario.
 */
public final class MobilityToGridScenariosUtils {
	public static final String AVERAGE = "average";
	public static final String RICH = "rich";

	private static final Logger log = LogManager.getLogger(MobilityToGridScenariosUtils.class);

	private MobilityToGridScenariosUtils() {}

	public static void addEngineInformationToVehicleTypes(Scenario scenario, String carFuelType, HbefaVehicleCategory carVehicleCategory, Map<String, HbefaVehicleCategory> commercialVehicleCategories) {
		for (VehicleType type : scenario.getVehicles().getVehicleTypes().values()) {
			EngineInformation engineInformation = type.getEngineInformation();

//			set engine information if none are present
			if (engineInformation.getAttributes().isEmpty()) {
				switch (type.getId().toString()) {
//						all other vehicle types (which are not listed here) already have engine information assigned in the input vehicle types file
//						berlin-v6.4.vehicleTypes.xml in same dir as config.
//						for hbefa 4.1 (which we are using here) diesel, petrol etc. is saved as "HbefaTechnology"
					case TransportMode.car -> {
						VehicleUtils.setHbefaVehicleCategory(engineInformation, carVehicleCategory.toString());
						VehicleUtils.setHbefaTechnology(engineInformation, carFuelType);
						VehicleUtils.setHbefaSizeClass(engineInformation, AVERAGE);
//							based on Kraftfahrzeugbestand germany 1.1.2025 ~60% petrol and 28% diesel, so we take petrol here.
//							source: https://www.kba.de/DE/Presse/Pressemitteilungen/Fahrzeugbestand/2025/pm10_fz_bestand_pm_komplett.html
						VehicleUtils.setHbefaEmissionsConcept(engineInformation, AVERAGE);
					}
					case TransportMode.ride -> {
//							ignore ride, the mode is routed on network, but then teleported
						VehicleUtils.setHbefaVehicleCategory(engineInformation, HbefaVehicleCategory.NON_HBEFA_VEHICLE.toString());
						VehicleUtils.setHbefaTechnology(engineInformation, AVERAGE);
						VehicleUtils.setHbefaSizeClass(engineInformation, AVERAGE);
						VehicleUtils.setHbefaEmissionsConcept(engineInformation, AVERAGE);
					}
					case TransportMode.bike -> {
//							ignore bikes
						VehicleUtils.setHbefaVehicleCategory(engineInformation, HbefaVehicleCategory.NON_HBEFA_VEHICLE.toString());
						VehicleUtils.setHbefaTechnology(engineInformation, AVERAGE);
						VehicleUtils.setHbefaSizeClass(engineInformation, AVERAGE);
						VehicleUtils.setHbefaEmissionsConcept(engineInformation, AVERAGE);
					}
					case "freight", TransportMode.truck -> {
						VehicleUtils.setHbefaVehicleCategory(engineInformation, HbefaVehicleCategory.NON_HBEFA_VEHICLE.toString());
						VehicleUtils.setHbefaTechnology(engineInformation, Hbefa41Technology.DIESEL.id);
						VehicleUtils.setHbefaSizeClass(engineInformation, AVERAGE);
						VehicleUtils.setHbefaEmissionsConcept(engineInformation, AVERAGE);
					}
					default -> throw new IllegalArgumentException("does not know how to handle vehicleType " + type.getId().toString());
				}
			} else {
//				for all veh types with engine info already present, we need to switch emissionConcept and technology.
//				The emission analysis relies on technology and not emission concept!!!
				if (!VehicleUtils.getHbefaEmissionsConcept(engineInformation).equals(AVERAGE) &&
					VehicleUtils.getHbefaTechnology(engineInformation).equals(AVERAGE)) {
					String isEmissionConceptButShouldBeTechnology = VehicleUtils.getHbefaEmissionsConcept(engineInformation);
					String isTechnologyButShouldBeEmissionConcept = VehicleUtils.getHbefaTechnology(engineInformation);

					VehicleUtils.setHbefaEmissionsConcept(engineInformation, isTechnologyButShouldBeEmissionConcept);
					VehicleUtils.setHbefaTechnology(engineInformation, isEmissionConceptButShouldBeTechnology);
				}
			}
		}

		//			this is necessary to switch off emissions for commercial vehicle types
		for (Map.Entry<String, HbefaVehicleCategory> entry : commercialVehicleCategories.entrySet()) {
			Id<VehicleType> vehTypeId = Id.create(entry.getKey(), VehicleType.class);

			if (scenario.getVehicles().getVehicleTypes().containsKey(vehTypeId)) {
				VehicleType vehicleType = scenario.getVehicles().getVehicleTypes().get(vehTypeId);
				VehicleUtils.setHbefaVehicleCategory(vehicleType.getEngineInformation(), entry.getValue().toString());
			}
		}
//			ignore all pt veh types
		scenario.getTransitVehicles()
			.getVehicleTypes()
			.values().forEach(type -> VehicleUtils.setHbefaVehicleCategory(type.getEngineInformation(), HbefaVehicleCategory.NON_HBEFA_VEHICLE.toString()));
	}

//	/**
//	 * configure qsim components for drt and sharing at the same time.
//	 * If done separately, simulation fails.
//	 */
//	public static QSimComponentsConfigurator drtAndSharingQSimComponentsConfigurator(SharingConfigGroup sharingConfig, MultiModeDrtConfigGroup multiModeDrtConfigGroup) {
//		return components -> {
//			components.addNamedComponent(DynActivityEngine.COMPONENT_NAME);
//			components.addNamedComponent(PreplanningEngineQSimModule.COMPONENT_NAME);
//
//			//activate additional named components
////			additionalNamedComponents.forEach(components::addNamedComponent);
//
//			List<String> dvrpModes = multiModeDrtConfigGroup.modes().toList();
//
//			//activate all DvrpMode components
//			MultiModals.requireAllModesUnique(dvrpModes);
//			for (String m : dvrpModes) {
//				components.addComponent(DvrpModes.mode(m));
//			}
//
//			for (SharingServiceConfigGroup serviceConfig : sharingConfig.getServices()) {
//				components.addComponent(SharingModes.mode(SharingUtils.getServiceMode(serviceConfig)));
//			}
//		};
//	}

	/**
	 * Enum for setting HBEFA 4.1 technology = fuel type for a vehicle type.
	 */
	public enum Hbefa41Technology {
		PETROL_4S("petrol (4S)"), DIESEL("diesel"), ELECTRICITY("electricity");

		public final String id;

		Hbefa41Technology(String id){
			this.id = id;
		}
	}
}
