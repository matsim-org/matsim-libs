/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.johannes.studies.matrix2014.data;

import com.graphhopper.GraphHopper;
import com.graphhopper.reader.osm.GraphHopperOSM;
import com.graphhopper.routing.util.CarFlagEncoder;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.routing.util.FlagEncoder;
import org.apache.log4j.Logger;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.common.gis.CRSUtils;
import org.matsim.contrib.common.util.XORShiftRandom;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.MatsimFacilitiesReader;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.MathTransform;
import playground.johannes.synpop.data.*;
import playground.johannes.synpop.data.io.PopulationIO;
import playground.johannes.synpop.gis.FacilityData;
import playground.johannes.synpop.processing.EpisodeTask;
import playground.johannes.synpop.processing.RouteLegGH;
import playground.johannes.synpop.processing.TaskRunner;
import playground.johannes.synpop.util.Executor;

import java.util.Set;

/**
 * @author johannes
 */
public class RouteLegsGH {

    private static final Logger logger = Logger.getLogger(RouteLegsGH.class);

    private static final String MODULE_NAME = "routeLegs";

    private static final String FACILITIES_FILE = "facilitiesFile";

    private static final String PERSONS_FILE = "personsInFile";

    private static final String PERSONS_OUT_FILE = "personsOutFile";

    private static final String OSM_FILE = "osmFile";

    private static final String GH_DIR = "ghDirectory";

    private static final String USE_CH = "useCH";

    public static void main(String args[]) throws FactoryException {
        Config config = ConfigUtils.createConfig();
        ConfigUtils.loadConfig(config, args[0]);
        ConfigGroup group = config.getModules().get(MODULE_NAME);
        Scenario scenario = ScenarioUtils.createScenario(config);
        /*
        Load facilities.
         */
        MatsimFacilitiesReader facilitiesReader = new MatsimFacilitiesReader(scenario);
        facilitiesReader.readFile(group.getValue(FACILITIES_FILE));
        FacilityData facilityData = new FacilityData(scenario.getActivityFacilities(), null, new XORShiftRandom());
        /*
        Load persons.
         */
        logger.info("Loading persons...");
        Set<Person> persons = PopulationIO.loadFromXML(group.getValue(PERSONS_FILE), new PlainFactory());
        /*
        Preprocess...
         */
        logger.info("Prerocess routing...");
//        int nThreads = Executor.getFreePoolSize();
        int nThreads = 4;

        FlagEncoder encoder = new CarFlagEncoder();
        EncodingManager em = new EncodingManager(encoder);

        GraphHopper hopper = new GraphHopperOSM().forDesktop();
        hopper.setDataReaderFile(group.getParams().get(OSM_FILE));
        hopper.setGraphHopperLocation(group.getParams().get(GH_DIR));
        hopper.setEncodingManager(em);
        boolean useCh = Boolean.parseBoolean(group.getParams().get(USE_CH));
        hopper.setCHEnabled(useCh);
        hopper.importOrLoad();
        /*
        Run...
         */
        MathTransform transform = CRS.findMathTransform(CRSUtils.getCRS(31467), DefaultGeographicCRS.WGS84);
        logger.info("Route legs...");
        RouteEpisode task = new RouteEpisode(new RouteLegGH(hopper, encoder, facilityData, transform, useCh));
        TaskRunner.run(task, persons, nThreads, true);
        /*
        Validate...
         */
        int failed = 0;
        int total = 0;
        for(Person p : persons) {
            for(Episode e : p.getEpisodes()) {
                for(Segment l : e.getLegs()) {
                    total++;
                    if(l.getAttribute(CommonKeys.LEG_ROUTE) == null) failed++;
                }
            }
        }

        logger.info(String.format("Calculated %s routes.", total));
        if(failed > 0) logger.warn(String.format("Failed to calculate %s routes.", failed));
        /*
        Write out.
         */
        logger.info("Write persons...");
        PopulationIO.writeToXML(group.getValue(PERSONS_OUT_FILE), persons);

        logger.info("Done.");
        Executor.shutdown();
    }

    private static class RouteEpisode implements EpisodeTask {

        private RouteLegGH routeLeg;

        public RouteEpisode(RouteLegGH routeLeg) {
            this.routeLeg = routeLeg;
        }

        @Override
        public void apply(Episode episode) {
            for(Segment leg : episode.getLegs()) {
                routeLeg.apply(leg);
            }
        }
    }
}
