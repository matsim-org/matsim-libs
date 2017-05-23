/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package playground.agarwalamit.emissions;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.contrib.emissions.events.EmissionEventsReader;
import org.matsim.contrib.emissions.events.WarmEmissionEvent;
import org.matsim.contrib.emissions.events.WarmEmissionEventHandler;
import org.matsim.contrib.emissions.types.HbefaVehicleCategory;
import org.matsim.contrib.emissions.utils.EmissionSpecificationMarker;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutilityFactory;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;
import playground.vsp.airPollution.flatEmissions.EmissionCostModule;
import playground.vsp.airPollution.flatEmissions.InternalizeEmissionsControlerListener;

/**
 *
 * A test to check:
 * <li>
 *     online : based on the switch isWritingEmissionsEvents  in {@link EmissionsConfigGroup},
 *     include/exclude emission events in normal events
 * </li>
 *
 *  <li>
 *     offline : well, if emission events are not required, simply don't write them in a new events file.
 *     Otherwise, events will be written to the specified events file.
 * </li>
 *
 * Created by amit on 22/03/2017.
 */

@RunWith(Parameterized.class)
public class EmissionEventsTest {

    @Rule
    public MatsimTestUtils helper = new MatsimTestUtils();

    private final boolean isWritingEmissionsEvents;

    @Parameterized.Parameters(name = "{index}: isWritingEmissionsEvents == {0}")
    public static List<Object> considerCO2 () {
        Object[] isIgnoringEmissionsFromEventsFile = new Object [] { true , false };
        return Arrays.asList(isIgnoringEmissionsFromEventsFile);
    }

    public EmissionEventsTest (final boolean isIgnoringEmissionsFromEventsFile) {
        this.isWritingEmissionsEvents = isIgnoringEmissionsFromEventsFile;
    }

    @Test
    public void eventsOfflineTest(){
        String inputEventsFile = helper.getClassInputDirectory()+"/0.events.xml.gz";
        new File(helper.getOutputDirectory()+"/ignoreingEmissionFromEventsFile="+this.isWritingEmissionsEvents).mkdir();
        String outputEventsFile = helper.getOutputDirectory()+"/ignoreingEmissionFromEventsFile="+this.isWritingEmissionsEvents +"/outputEvents.xml.gz";

        // generate emissions

        EquilTestSetUp equilTestSetUp = new EquilTestSetUp();
        Scenario sc = equilTestSetUp.createConfigAndReturnScenario();
        equilTestSetUp.createNetwork(sc);

        String carPersonId = "567417.1#12424";
        String bikePersonId = "567417.1#12425"; // no emissions

        Vehicles vehs = sc.getVehicles();

        VehicleType car = vehs.getFactory().createVehicleType(Id.create(TransportMode.car,VehicleType.class));
        car.setMaximumVelocity(100.0/3.6);
        car.setPcuEquivalents(1.0);
        car.setDescription(EmissionSpecificationMarker.BEGIN_EMISSIONS.toString()
                + HbefaVehicleCategory.PASSENGER_CAR.toString().concat(";petrol (4S);>=2L;PC-P-Euro-0")
                + EmissionSpecificationMarker.END_EMISSIONS.toString() );
        // TODO "&gt;" is an escape character for ">" in xml (http://stackoverflow.com/a/1091953/1359166); need to be very careful with them.
        // thus, reading from vehicles file and directly passing to vehicles container is not the same.
        vehs.addVehicleType(car);

        Vehicle carVeh = vehs.getFactory().createVehicle(Id.createVehicleId(carPersonId),car);
        vehs.addVehicle(carVeh);

        VehicleType bike = vehs.getFactory().createVehicleType(Id.create("bicycle",VehicleType.class));
        bike.setMaximumVelocity(20./3.6);
        bike.setPcuEquivalents(0.25);
        bike.setDescription(EmissionSpecificationMarker.BEGIN_EMISSIONS.toString()+
                HbefaVehicleCategory.ZERO_EMISSION_VEHICLE.toString().concat(";;;")+
                EmissionSpecificationMarker.END_EMISSIONS.toString() );
        vehs.addVehicleType(bike);

        {
            Vehicle bikeVeh = vehs.getFactory().createVehicle(Id.createVehicleId(bikePersonId),bike);
            vehs.addVehicle(bikeVeh);
        }
        {
            Vehicle bikeVeh = vehs.getFactory().createVehicle(Id.createVehicleId(bikePersonId+"_bicycle"),bike);
            vehs.addVehicle(bikeVeh);
        }

        emissionSettings(sc, this.isWritingEmissionsEvents);

        EventsManager emissionEventsManager = EventsUtils.createEventsManager();
        EmissionModule emissionModule = new EmissionModule(sc, emissionEventsManager);

        EventWriterXML emissionEventWriter;

        if ( this.isWritingEmissionsEvents) { // i.e., ignoring emission events,
            emissionEventWriter = new EventWriterXML(outputEventsFile);
            emissionModule.getEmissionEventsManager().addHandler(emissionEventWriter);
        } else {
            return;
        }

        MatsimEventsReader matsimEventsReader = new MatsimEventsReader(emissionEventsManager);
        matsimEventsReader.readFile(inputEventsFile);

        if ( this.isWritingEmissionsEvents) {
            emissionEventWriter.closeFile();
        }

        // check
        EventsManager eventsManager = EventsUtils.createEventsManager();
        EmissionEventsReader reader = new EmissionEventsReader(eventsManager);
        List<WarmEmissionEvent> warmEvents = new ArrayList<>();
        eventsManager.addHandler(new WarmEmissionEventHandler() {
            @Override
            public void handleEvent(WarmEmissionEvent event) {
                warmEvents.add(event);
            }
            @Override
            public void reset(int iteration) {
            }
        });
        reader.readFile(outputEventsFile);

        if ( ! isWritingEmissionsEvents && warmEvents.size()!=0 ) throw new RuntimeException("There should NOT be any warm emission events in "+ outputEventsFile + " file.");
        else if ( isWritingEmissionsEvents && warmEvents.size()==0) throw new RuntimeException("There should be some warm emission events in "+ outputEventsFile + " file.");
    }

    @Test
    public void eventsOnlineTest(){
        EquilTestSetUp equilTestSetUp = new EquilTestSetUp();
        Scenario sc = equilTestSetUp.createConfigAndReturnScenario();
        equilTestSetUp.createNetwork(sc);

        String carPersonId = "567417.1#12424";

        Vehicles vehs = sc.getVehicles();
        VehicleType car = vehs.getFactory().createVehicleType(Id.create(TransportMode.car,VehicleType.class));
        car.setMaximumVelocity(100.0/3.6);
        car.setPcuEquivalents(1.0);
        car.setDescription(EmissionSpecificationMarker.BEGIN_EMISSIONS.toString()
                + HbefaVehicleCategory.PASSENGER_CAR.toString().concat(";petrol (4S);>=2L;PC-P-Euro-0")
                + EmissionSpecificationMarker.END_EMISSIONS.toString() );
        // TODO "&gt;" is an escape character for ">" in xml (http://stackoverflow.com/a/1091953/1359166); need to be very careful with them.
        // thus, reading from vehicles file and directly passing to vehicles container is not the same.
        vehs.addVehicleType(car);

        Vehicle carVeh = vehs.getFactory().createVehicle(Id.createVehicleId(carPersonId),car);
        vehs.addVehicle(carVeh);

        sc.getConfig().qsim().setUsePersonIdForMissingVehicleId(true);
        sc.getConfig().plansCalcRoute().getOrCreateModeRoutingParams(TransportMode.pt).setTeleportedModeFreespeedFactor(1.5);

        equilTestSetUp.createActiveAgents(sc, carPersonId, TransportMode.car, 6.0 * 3600.);
        emissionSettings(sc, this.isWritingEmissionsEvents);

        Controler controler = new Controler(sc);

        String outputDirectory = helper.getOutputDirectory();
        sc.getConfig().controler().setOutputDirectory(outputDirectory);

        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                bind(EmissionModule.class).asEagerSingleton();
                bind(EmissionCostModule.class).asEagerSingleton();

                addControlerListenerBinding().to(InternalizeEmissionsControlerListener.class);

                bindCarTravelDisutilityFactory().toInstance(new EmissionModalTravelDisutilityCalculatorFactory(new RandomizingTimeDistanceTravelDisutilityFactory("car", sc.getConfig().planCalcScore())));
            }
        });
        controler.run();

        int lastItr = controler.getConfig().controler().getLastIteration();
        String eventsFile = outputDirectory + "/ITERS/it."+lastItr+"/"+lastItr+".events.xml.gz";

        EventsManager eventsManager = EventsUtils.createEventsManager();
        EmissionEventsReader reader = new EmissionEventsReader(eventsManager);
        List<WarmEmissionEvent> warmEvents = new ArrayList<>();
        eventsManager.addHandler(new WarmEmissionEventHandler() {
            @Override
            public void handleEvent(WarmEmissionEvent event) {
                warmEvents.add(event);
            }
            @Override
            public void reset(int iteration) {
            }
        });
        reader.readFile(eventsFile);

        if ( !isWritingEmissionsEvents && warmEvents.size()!=0 ) throw new RuntimeException("There should NOT be any warm emission events in "+ eventsFile + " file.");
        else if (isWritingEmissionsEvents && warmEvents.size()==0) throw new RuntimeException("There should be some warm emission events in "+ eventsFile + " file.");
    }

    private void emissionSettings(final Scenario scenario, final boolean isIgnoringEmissionsFromEventsFile){
        String inputFilesDir = "../benjamin/test/input/playground/benjamin/internalization/";

        Config config = scenario.getConfig();
        EmissionsConfigGroup ecg = new EmissionsConfigGroup() ;
        ecg.setEmissionRoadTypeMappingFile(inputFilesDir + "/roadTypeMapping.txt");

        scenario.getConfig().vehicles().setVehiclesFile(inputFilesDir + "/equil_emissionVehicles_1pct.xml.gz");

        ecg.setAverageWarmEmissionFactorsFile(inputFilesDir + "/EFA_HOT_vehcat_2005average.txt");
        ecg.setAverageColdEmissionFactorsFile(inputFilesDir + "/EFA_ColdStart_vehcat_2005average.txt");

        ecg.setDetailedWarmEmissionFactorsFile(inputFilesDir + "/EFA_HOT_SubSegm_2005detailed.txt");
        ecg.setDetailedColdEmissionFactorsFile(inputFilesDir + "/EFA_ColdStart_SubSegm_2005detailed.txt");

        ecg.setUsingDetailedEmissionCalculation(true);
        ecg.setEmissionEfficiencyFactor(1.0);
        ecg.setConsideringCO2Costs(true);
        ecg.setEmissionCostMultiplicationFactor(1.0);

        ecg.setIgnoringEmissionsFromEventsFile(isIgnoringEmissionsFromEventsFile);
        config.addModule(ecg);
    }
}
