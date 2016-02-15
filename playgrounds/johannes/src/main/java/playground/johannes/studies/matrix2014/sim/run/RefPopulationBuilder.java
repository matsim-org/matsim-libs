/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,       *
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
package playground.johannes.studies.matrix2014.sim.run;

import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import playground.johannes.gsv.synPop.mid.Route2GeoDistance;
import playground.johannes.gsv.synPop.sim3.ReplaceActTypes;
import playground.johannes.studies.matrix2014.sim.ValidatePersonWeight;
import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.data.Person;
import playground.johannes.synpop.data.PlainFactory;
import playground.johannes.synpop.data.io.PopulationIO;
import playground.johannes.synpop.processing.GuessMissingActTypes;
import playground.johannes.synpop.processing.TaskRunner;
import playground.johannes.synpop.processing.ValidateMissingAttribute;

import java.util.Set;

/**
 * @author jillenberger
 */
public class RefPopulationBuilder {

    private static final Logger logger = Logger.getLogger(RefPopulationBuilder.class);

    public static Set<? extends Person> build(Simulator engine, Config config) {
        logger.info("Loading persons...");
        Set<Person> refPersons = PopulationIO.loadFromXML(config.findParam(engine.MODULE_NAME, "popInputFile"), new PlainFactory());
        logger.info(String.format("Loaded %s persons.", refPersons.size()));

        logger.info("Preparing reference simulation...");
        TaskRunner.validatePersons(new ValidateMissingAttribute(CommonKeys.PERSON_WEIGHT), refPersons);
        TaskRunner.validatePersons(new ValidatePersonWeight(), refPersons);

        TaskRunner.run(new ReplaceActTypes(), refPersons);
        new GuessMissingActTypes(engine.getRandom()).apply(refPersons);
        TaskRunner.run(new Route2GeoDistance(new playground.johannes.studies.matrix2014.sim.Simulator.Route2GeoDistFunction()), refPersons);

        return refPersons;
    }
}
