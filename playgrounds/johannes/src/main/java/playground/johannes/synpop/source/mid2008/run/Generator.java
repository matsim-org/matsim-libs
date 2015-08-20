/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.johannes.synpop.source.mid2008.run;

import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import playground.johannes.gsv.synPop.io.XMLWriter;
import playground.johannes.synpop.data.PlainFactory;
import playground.johannes.synpop.data.PlainPerson;
import playground.johannes.synpop.source.mid2008.generator.FileReader;
import playground.johannes.synpop.source.mid2008.generator.PersonAgeHandler;
import playground.johannes.synpop.source.mid2008.generator.PersonCarAvailHandler;

import java.io.IOException;
import java.util.Set;

/**
 * @author johannes
 */
public class Generator {

    private static final Logger logger = Logger.getLogger(Generator.class);

    public static final String MODULE_NAME = "mid2008Generator";

    public static final String PERSONS_FILE = "personsFile";

    public static final String TRIPS_FILE = "tripsFile";

    public static final String JOURNEYS_FILE = "journeysFile";

    public static final String OUTPUT_DIR = "output";

    public static void main(String args[]) throws IOException {
        Config config = new Config();
        ConfigUtils.loadConfig(config, args[0]);

        String personsFile = config.getParam(MODULE_NAME, PERSONS_FILE);
        String tripsFile = config.getParam(MODULE_NAME, TRIPS_FILE);
        String journeysFile = config.getParam(MODULE_NAME, JOURNEYS_FILE);
        String outDir = config.getParam(MODULE_NAME, OUTPUT_DIR);

        FileReader fileReader = new FileReader(new PlainFactory());

        fileReader.addPersonAttributeHandler(new PersonAgeHandler());
        fileReader.addPersonAttributeHandler(new PersonCarAvailHandler());

        logger.info("Generating persons...");
        Set<PlainPerson> persons = (Set<PlainPerson>)fileReader.read(personsFile, tripsFile, journeysFile);
        logger.info(String.format("Generated %s persons.", persons.size()));

        logger.info("Writing persons...");
        XMLWriter writer = new XMLWriter();
        writer.write(String.format("%s/mid2008.xml", outDir), persons);
        logger.info("Done.");
    }
}
