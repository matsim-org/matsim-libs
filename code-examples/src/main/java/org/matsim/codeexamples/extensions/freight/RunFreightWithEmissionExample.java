/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.codeexamples.extensions.freight;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.contrib.emissions.HbefaVehicleCategory;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.contrib.freight.FreightConfigGroup;
import org.matsim.contrib.freight.carrier.CarrierPlanXmlWriterV2;
import org.matsim.contrib.freight.controler.CarrierModule;
import org.matsim.contrib.freight.controler.FreightUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import java.util.concurrent.ExecutionException;


/**
 * @see org.matsim.contrib.freight
 */
public class RunFreightWithEmissionExample {

	public static void main(String[] args) throws ExecutionException, InterruptedException{

		// ### config stuff: ###

		Config config = ConfigUtils.loadConfig( IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL( "freight-chessboard-9x9" ), "config.xml" ) );

		config.plans().setInputFile( null ); // remove passenger input

		//more general settings
		config.controler().setOutputDirectory("./output/freightWEmissions" );
		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

		config.controler().setLastIteration(0 );		// yyyyyy iterations currently do not work; needs to be fixed.

		//freight settings
		FreightConfigGroup freightConfigGroup = ConfigUtils.addOrGetModule( config, FreightConfigGroup.class ) ;
		freightConfigGroup.setCarriersFile( "singleCarrierWithoutRoutes.xml");
		freightConfigGroup.setCarriersVehicleTypesFile( "vehicleTypes.xml");

		//emission setting
		EmissionsConfigGroup emissionsConfigGroup = ConfigUtils.addOrGetModule(config, EmissionsConfigGroup.class);
//		emissionsConfigGroup.setDetailedWarmEmissionFactorsFile(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("emissions-sampleScenario"), "sample_41_EFA_HOT_HGV_2020detailed.csv").getFile());
//		emissionsConfigGroup.setDetailedColdEmissionFactorsFile(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("emissions-sampleScenario"), "coldTableExcept_LCV_PassCar_AllZero.csv").getFile());
//		emissionsConfigGroup.setAverageWarmEmissionFactorsFile(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("emissions-sampleScenario"), "sample_41_EFA_HOT_vehcat_2020average.txt").getFile()); //has only limited entries for HGV.... is here, because file is needed :(
//		emissionsConfigGroup.setAverageColdEmissionFactorsFile(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("emissions-sampleScenario"), "sample_41_EFA_ColdStart_vehcat_2020average.txt").getFile());
		emissionsConfigGroup.setDetailedWarmEmissionFactorsFile("../emissions-sampleScenario/sample_41_EFA_HOT_HGV_2020detailed.csv");
		emissionsConfigGroup.setDetailedColdEmissionFactorsFile("../emissions-sampleScenario/coldTableExcept_LCV_PassCar_AllZero.csv");
		emissionsConfigGroup.setAverageWarmEmissionFactorsFile("../emissions-sampleScenario/sample_41_EFA_HOT_vehcat_2020average.txt"); //has only limited entries for HGV.... is here, because file is needed :(
		emissionsConfigGroup.setAverageColdEmissionFactorsFile("../emissions-sampleScenario/sample_41_EFA_ColdStart_vehcat_2020average.txt");
		emissionsConfigGroup.setDetailedVsAverageLookupBehavior(EmissionsConfigGroup.DetailedVsAverageLookupBehavior.tryDetailedThenTechnologyAverageThenAverageTable);

		// load scenario (this is not loading the freight material):
		Scenario scenario = ScenarioUtils.loadScenario( config );

		//load carriers according to freight config
		FreightUtils.loadCarriersAccordingToFreightConfig( scenario );

		// how to set the capacity of the "light" vehicle type to "1":
//		FreightUtils.getCarrierVehicleTypes( scenario ).getVehicleTypes().get( Id.create("light", VehicleType.class ) ).getCapacity().setOther( 1 );

		//prepare network for emission calculation
		for (Link link : scenario.getNetwork().getLinks().values()) {
			if (link.getId().toString().startsWith("i(1,") || link.getId().toString().startsWith("i(9,") || link.getId().toString().startsWith("j(0,") || link.getId().toString().startsWith("j(9,")){
				//outer ring
				link.getAttributes().putAttribute("hbefa_road_type", "RUR/MW/130");
			} else {
				link.getAttributes().putAttribute("hbefa_road_type", "URB/Distr/30");
			}
		}

		//prepare vehicles for emission calculation
		{
			VehicleType lightVehType = FreightUtils.getCarrierVehicleTypes(scenario).getVehicleTypes().get(Id.create("light", VehicleType.class));
			VehicleUtils.setHbefaVehicleCategory(lightVehType.getEngineInformation(), HbefaVehicleCategory.HEAVY_GOODS_VEHICLE.toString());
			VehicleUtils.setHbefaTechnology(lightVehType.getEngineInformation(), "diesel");
			VehicleUtils.setHbefaSizeClass(lightVehType.getEngineInformation(), "RT ≤7,5t");
			VehicleUtils.setHbefaEmissionsConcept(lightVehType.getEngineInformation(), "HGV D Euro-VI");
		}
		{
			VehicleType heavyVehType = FreightUtils.getCarrierVehicleTypes(scenario).getVehicleTypes().get(Id.create("heavy", VehicleType.class));
			VehicleUtils.setHbefaVehicleCategory(heavyVehType.getEngineInformation(), HbefaVehicleCategory.HEAVY_GOODS_VEHICLE.toString());
			VehicleUtils.setHbefaTechnology(heavyVehType.getEngineInformation(), "diesel");
			VehicleUtils.setHbefaSizeClass(heavyVehType.getEngineInformation(), "RT >32t");
			VehicleUtils.setHbefaEmissionsConcept(heavyVehType.getEngineInformation(), "HGV D Euro-0");
		}

		// output before jsprit run (not necessary)
		new CarrierPlanXmlWriterV2(FreightUtils.getCarriers( scenario )).write( "output/jsprit_unplannedCarriers.xml" ) ;
		// (this will go into the standard "output" directory.  note that this may be removed if this is also used as the configured output dir.)

		//Solving the VRP (generate carrier's tour plans)
		FreightUtils.runJsprit( scenario );

		// output after jsprit run (not necessary)
		new CarrierPlanXmlWriterV2(FreightUtils.getCarriers( scenario )).write( "output/jsprit_plannedCarriers.xml" ) ;
		// (this will go into the standard "output" directory.  note that this may be removed if this is also used as the configured output dir.)

		//MATSim configuration:
		final Controler controler = new Controler( scenario ) ;
		controler.addOverridingModule(new CarrierModule() );

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(EmissionModule.class).asEagerSingleton();
			}
		});

		// otfvis (if you want to use):
//		OTFVisConfigGroup otfVisConfigGroup = ConfigUtils.addOrGetModule( config, OTFVisConfigGroup.class );
//		otfVisConfigGroup.setLinkWidth( 10 );
//		otfVisConfigGroup.setDrawNonMovingItems( false );
//		config.qsim().setTrafficDynamics( QSimConfigGroup.TrafficDynamics.kinematicWaves );
//		config.qsim().setSnapshotStyle( QSimConfigGroup.SnapshotStyle.kinematicWaves );
//		controler.addOverridingModule( new OTFVisLiveModule() );

//		start of the MATSim-Run:
		controler.run();

	}
}
