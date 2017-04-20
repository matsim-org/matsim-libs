package playground.johannes.studies.matrix2014.matrix.io;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.FacilitiesReaderMatsimV1;
import playground.johannes.studies.matrix2014.gis.ActivityLocationLayer;
import playground.johannes.studies.matrix2014.matrix.DefaultMatrixBuilder;
import playground.johannes.synpop.data.Person;
import playground.johannes.synpop.data.PlainFactory;
import playground.johannes.synpop.data.io.PopulationIO;
import playground.johannes.synpop.gis.ZoneCollection;
import playground.johannes.synpop.gis.ZoneGeoJsonIO;
import playground.johannes.synpop.matrix.NumericMatrix;
import playground.johannes.synpop.matrix.NumericMatrixIO;
import playground.johannes.synpop.util.Executor;

import java.io.IOException;
import java.util.Set;

/**
 * Created by johannesillenberger on 11.04.17.
 */
public class Episodes2Matrix {

    private static final Logger logger = Logger.getLogger(Episodes2Matrix.class);

    private static final String MODULE_NAME = "episodes2matrix";

    private static final String ZONES_FILE_PARAM = "zoneFile";

    private static final String ZONE_KEY_PARAM = "zoneKey";

    private static final String FACILITY_FILE_PARAM = "facilityFile";

    private static final String PERSONS_FILE_PARAM = "personsFile";

    private static final String MATRIX_FILE_PARAM = "matrixFile";

    public static void main(String args[]) throws IOException {
        final Config config = new Config();
        ConfigUtils.loadConfig(config, args[0]);
        ConfigGroup group = config.getModules().get(MODULE_NAME);

        logger.info("Loading zones...");
        ZoneCollection zoneCollection = ZoneGeoJsonIO.readFromGeoJSON(
                group.getParams().get(ZONES_FILE_PARAM),
                group.getParams().get(ZONE_KEY_PARAM),
                null
        );

        logger.info("Loading facilities...");
        Scenario scenario = ScenarioUtils.createScenario(config);
        FacilitiesReaderMatsimV1 reader = new FacilitiesReaderMatsimV1(scenario);
        reader.readFile(group.getValue(FACILITY_FILE_PARAM));

        logger.info("Loading persons...");
        Set<? extends Person> persons = PopulationIO.loadFromXML(group.getParams().get(PERSONS_FILE_PARAM), new PlainFactory());

        logger.info("Building matrix...");
        DefaultMatrixBuilder builder = new DefaultMatrixBuilder(
                new ActivityLocationLayer(scenario.getActivityFacilities()),
                zoneCollection);
        NumericMatrix m = builder.build(persons);

        logger.info("Writing matrix...");
        NumericMatrixIO.write(m, group.getParams().get(MATRIX_FILE_PARAM));
        logger.info("Done.");

        Executor.shutdown();
    }
}
