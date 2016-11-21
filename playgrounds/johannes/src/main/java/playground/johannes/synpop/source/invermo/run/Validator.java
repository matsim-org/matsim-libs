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

package playground.johannes.synpop.source.invermo.run;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import playground.johannes.studies.matrix2014.sim.ValidatePersonWeight;
import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.data.Person;
import playground.johannes.synpop.data.PlainFactory;
import playground.johannes.synpop.data.io.PopulationIO;
import playground.johannes.synpop.processing.TaskRunner;
import playground.johannes.synpop.processing.ValidateMissingAttribute;
import playground.johannes.synpop.processing.ValidateNoPlans;
import playground.johannes.synpop.source.invermo.processing.CalcGeoDistance;
import playground.johannes.synpop.source.invermo.processing.GeocodeLocationsTask;
import playground.johannes.synpop.source.invermo.processing.ValidateNoLegs;

import java.util.Set;

/**
 * @author johannes
 */
public class Validator {

    private static final Logger logger = Logger.getLogger(Validator.class);

    public static void main(String args[]) {
        String inFile = "/Users/johannes/gsv/germany-scenario/invermo/pop2/pop.xml";
        String outFile = "/Users/johannes/gsv/germany-scenario/invermo/pop2/pop.validated.xml";

        Set<Person> persons = PopulationIO.loadFromXML(inFile, new PlainFactory());

        TaskRunner.validatePersons(new ValidateMissingAttribute(CommonKeys.PERSON_WEIGHT), persons);
        TaskRunner.validatePersons(new ValidatePersonWeight(), persons);
        TaskRunner.validatePersons(new ValidateNoLegs(), persons);
        TaskRunner.validatePersons(new ValidateNoPlans(), persons);

        logger.setLevel(Level.INFO);
        GeocodeLocationsTask geoTask = new GeocodeLocationsTask("localhost", 3128);
        geoTask.setCacheFile("/Users/johannes/gsv/germany-scenario/invermo/txt-utf8/geocache.txt");
        TaskRunner.run(geoTask, persons);
        geoTask.writeCache();
        logger.setLevel(Level.ALL);

        TaskRunner.run(new CalcGeoDistance(), persons);

        logger.info(String.format("Writing %s persons...", persons.size()));
        PopulationIO.writeToXML(outFile, persons);

    }
}
