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

package playground.johannes.studies.matrix2014.analysis.run;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.contrib.common.util.XORShiftRandom;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import playground.johannes.gsv.synPop.mid.Route2GeoDistance;
import playground.johannes.gsv.synPop.sim3.ReplaceActTypes;
import playground.johannes.studies.matrix2014.analysis.SetSeason;
import playground.johannes.studies.matrix2014.matrix.postprocess.SeasonTask;
import playground.johannes.studies.matrix2014.sim.ValidatePersonWeight;
import playground.johannes.studies.matrix2014.sim.run.RefPopulationBuilder;
import playground.johannes.synpop.analysis.*;
import playground.johannes.synpop.data.*;
import playground.johannes.synpop.data.io.PopulationIO;
import playground.johannes.synpop.gis.DataPool;
import playground.johannes.synpop.gis.FacilityDataLoader;
import playground.johannes.synpop.gis.ZoneDataLoader;
import playground.johannes.synpop.processing.GuessMissingActTypes;
import playground.johannes.synpop.processing.TaskRunner;
import playground.johannes.synpop.processing.ValidateMissingAttribute;
import playground.johannes.synpop.util.Executor;

import java.util.Collection;
import java.util.Random;
import java.util.Set;

/**
 * @author johannes
 */
public class PopulationAnalyzer {

    private static final String MODULE_NAME = "synPopSim";

    private static final Logger logger = Logger.getLogger(PopulationAnalyzer.class);

    public static void main(String args[]) {
        Logger.getRootLogger().setLevel(Level.TRACE);

        final Config config = new Config();
        ConfigUtils.loadConfig(config, args[0]);

        ConfigGroup configGroup = config.getModule(MODULE_NAME);
        /*
        Load parameters...
         */
        Random random = new XORShiftRandom(Long.parseLong(config.getParam("global", "randomSeed")));
        FileIOContext ioContext = new FileIOContext(configGroup.getValue("output"));
        /*
        Load GIS data...
         */
        DataPool dataPool = new DataPool();
        dataPool.register(new FacilityDataLoader(configGroup.getValue("facilities"), random), FacilityDataLoader.KEY);
        dataPool.register(new ZoneDataLoader(configGroup), ZoneDataLoader.KEY);

//        ValidateFacilities.validate(dataPool, "nuts3");

//        ZoneCollection lau2Zones = ((ZoneData) dataPool.get(ZoneDataLoader.KEY)).getLayer("lau2");
//        new ZoneSetLAU2Class().apply(lau2Zones);
        /*
        Load population...
         */
        logger.info("Loading persons...");
        Set<Person> persons = PopulationIO.loadFromXML(config.findParam(MODULE_NAME, "popInputFile"), new
                PlainFactory());
        logger.info(String.format("Loaded %s persons.", persons.size()));

        logger.info("Validating persons...");
        TaskRunner.validatePersons(new ValidateMissingAttribute(CommonKeys.PERSON_WEIGHT), persons);
        TaskRunner.validatePersons(new ValidatePersonWeight(), persons);

        Predicate<Segment> carPredicate = new LegAttributePredicate(CommonKeys.LEG_MODE, CommonValues.LEG_MODE_CAR);

        TaskRunner.run(new RefPopulationBuilder.SetVacationsPurpose(), persons);
        TaskRunner.run(new RefPopulationBuilder.ReplaceHomePurpose(), persons);
        TaskRunner.run(new RefPopulationBuilder.NullifyPurpose(ActivityTypes.HOME), persons);
        TaskRunner.run(new RefPopulationBuilder.NullifyPurpose(ActivityTypes.MISC), persons);
        TaskRunner.run(new RefPopulationBuilder.ReplaceLegPurposes(), persons);
        TaskRunner.run(new RefPopulationBuilder.GuessMissingPurposes(persons, carPredicate, random), persons);

        TaskRunner.run(new ReplaceActTypes(), persons);
        new GuessMissingActTypes(random).apply(persons);
        TaskRunner.run(new Route2GeoDistance(new playground.johannes.studies.matrix2014.sim.Simulator.Route2GeoDistFunction()), persons);

        /*
        Build analyzer...
         */

        AnalyzerTaskComposite<Collection<? extends Person>> tasks = new AnalyzerTaskComposite<>();

        TaskRunner.run(new SetSeason(), persons);

        tasks.addComponent(new SeasonTask(ioContext));
        tasks.addComponent(new DayTask(ioContext));

        AnalyzerTaskRunner.run(persons, tasks, ioContext);

        Executor.shutdown();
        logger.info("Done.");
    }
}
