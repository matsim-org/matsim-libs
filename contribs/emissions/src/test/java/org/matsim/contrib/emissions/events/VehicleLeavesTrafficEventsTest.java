package org.matsim.contrib.emissions.events;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.contrib.emissions.VspHbefaRoadTypeMapping;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Injector;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.utils.eventsfilecomparison.EventsFileComparator;

/**
 *
 * Calculate offline emissions to test rare cases of vehicle
 * enter and leave traffic events occurring on the same links.
 *
 * This class tests the (proper) handling of these cases as a result of
 * the VehicleLeavesTrafficEvent method in the WarmEmissionHandler.
 *
 * @author Ruan J. Gräbe
 */
public class VehicleLeavesTrafficEventsTest {
    private static final String hbefaColdFile = IOUtils.extendUrl( ExamplesUtils.getTestScenarioURL( "emissions-sampleScenario" ), "sample_41_EFA_ColdStart_vehcat_2020average.csv" ).toString();
    private static final String hbefaWarmFile = IOUtils.extendUrl( ExamplesUtils.getTestScenarioURL( "emissions-sampleScenario" ), "sample_41_EFA_HOT_vehcat_2020average.csv" ).toString();
    private static final String eventsFile =  "test/input/org/matsim/contrib/emissions/events/VehicleLeavesTrafficEventTest/smallBerlinSample.output_events.xml.gz";
    private static final String vehiclesFile = "test/input/org/matsim/contrib/emissions/events/VehicleLeavesTrafficEventTest/smallBerlinSample_emissionVehicles.xml";
    private static final String networkFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5-network.xml.gz";
    private static final String outputDirectory = "test/output/org/matsim/contrib/emissions/events/VehicleLeavesTrafficEventTest/";
    private static final String inputDirectory = "test/input/org/matsim/contrib/emissions/events/VehicleLeavesTrafficEventTest/";
    private static final String emissionEventsFileName = "smallBerlinSample.emissions.events.offline.xml.gz";

    // =======================================================================================================

    @Test
    public final void testRareEventsFromBerlinScenario (){
        Config config = ConfigUtils.createConfig();
        config.setContext(IOUtils.getFileUrl("./"));

        config.controler().setOutputDirectory( outputDirectory );
        config.vehicles().setVehiclesFile( vehiclesFile );
        config.network().setInputFile( networkFile );

        EmissionsConfigGroup emissionsConfig = ConfigUtils.addOrGetModule( config, EmissionsConfigGroup.class );

        emissionsConfig.setAverageColdEmissionFactorsFile( hbefaColdFile );
        emissionsConfig.setAverageWarmEmissionFactorsFile( hbefaWarmFile );
        emissionsConfig.setDetailedVsAverageLookupBehavior( EmissionsConfigGroup.DetailedVsAverageLookupBehavior.directlyTryAverageTable );
        emissionsConfig.setNonScenarioVehicles( EmissionsConfigGroup.NonScenarioVehicles.ignore );

        Scenario scenario = ScenarioUtils.loadScenario( config ) ;
        new VspHbefaRoadTypeMapping().addHbefaMappings(scenario.getNetwork());

        EventsManager eventsManager = EventsUtils.createEventsManager();

        AbstractModule module = new AbstractModule(){
            @Override
            public void install(){
                bind( Scenario.class ).toInstance( scenario );
                bind( EventsManager.class ).toInstance( eventsManager );
                bind( EmissionModule.class ) ;
            }
        };

        com.google.inject.Injector injector = Injector.createInjector( config, module );
        injector.getInstance(EmissionModule.class);

        try {
            final EventWriterXML eventWriterXML = new EventWriterXML( config.controler().getOutputDirectory() + emissionEventsFileName );
            eventsManager.addHandler( eventWriterXML );
            new MatsimEventsReader(eventsManager).readFile( eventsFile );
            eventWriterXML.closeFile();
        } catch ( Exception e ) {
            throw new RuntimeException( "Failing because there is no proper handling for the case where" +
                    " a vehicle leaves traffic WITHOUT entering the link (no link enter time). A zero" +
                    " emissions event should occur for these instances because there is no significant travel" +
                    " on the link" );
        }
        // If the try-block executes, this ensures emission events occur as expected:
        String expected = inputDirectory + emissionEventsFileName;
        String actual = outputDirectory + emissionEventsFileName;
        EventsFileComparator.Result result = EventsUtils.compareEventsFiles( expected, actual );
        Assert.assertEquals( EventsFileComparator.Result.FILES_ARE_EQUAL, result);
    }

}