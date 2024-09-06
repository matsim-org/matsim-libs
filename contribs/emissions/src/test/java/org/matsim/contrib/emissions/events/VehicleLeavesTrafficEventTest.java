package org.matsim.contrib.emissions.events;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
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
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.utils.eventsfilecomparison.ComparisonResult;

import java.net.URL;

/**
 *
 * Calculate offline emissions to test rare cases of "vehicle
 * enters traffic" and "vehicle leaves traffic" events occurring
 * on the same links.
 * <p>
 * This class tests the (proper) handling of these cases in
 * the VehicleLeavesTrafficEvent method from the WarmEmissionHandler.
 *
 * @author Ruan J. Gräbe
 */
public class VehicleLeavesTrafficEventTest {

	@RegisterExtension private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	final void testRareEventsFromBerlinScenario(){

		final String emissionEventsFileName = "smallBerlinSample.emissions.events.offline.xml.gz";
		final String resultingEvents = utils.getOutputDirectory() + emissionEventsFileName;

        Config config = utils.createConfigWithTestInputFilePathAsContext();
        config.vehicles().setVehiclesFile("smallBerlinSample_emissionVehicles.xml");
        config.network().setInputFile("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5-network.xml.gz");

		final URL testScenarioURL = ExamplesUtils.getTestScenarioURL("emissions-sampleScenario");
        EmissionsConfigGroup emissionsConfig = ConfigUtils.addOrGetModule( config, EmissionsConfigGroup.class );
        emissionsConfig.setAverageColdEmissionFactorsFile(IOUtils.extendUrl( testScenarioURL, "sample_41_EFA_ColdStart_vehcat_2020average.csv" ).toString());
        emissionsConfig.setAverageWarmEmissionFactorsFile(IOUtils.extendUrl( testScenarioURL, "sample_41_EFA_HOT_vehcat_2020average.csv" ).toString());
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
            final EventWriterXML eventWriterXML = new EventWriterXML( resultingEvents );
            eventsManager.addHandler( eventWriterXML );
            new MatsimEventsReader(eventsManager).readFile(utils.getClassInputDirectory() + "smallBerlinSample.output_events.xml.gz");
            eventWriterXML.closeFile();
        } catch ( Exception e ) {
			throw new RuntimeException(e) ;
        }
		final String expected = utils.getClassInputDirectory() + emissionEventsFileName;
		ComparisonResult result = EventsUtils.compareEventsFiles(expected, resultingEvents);
        Assertions.assertEquals( ComparisonResult.FILES_ARE_EQUAL, result);
    }

}
