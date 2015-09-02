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

import playground.johannes.synpop.data.io.XMLWriter;
import playground.johannes.synpop.data.Person;
import playground.johannes.synpop.data.PlainFactory;
import playground.johannes.synpop.data.io.PopulationIO;
import playground.johannes.synpop.processing.TaskRunner;
import playground.johannes.synpop.source.mid2008.MiDKeys;
import playground.johannes.synpop.source.mid2008.processing.SortLegsTask;

import java.util.Set;

/**
 * @author johannes
 */
public class Validator {

    public static final void main(String args[]) {
        Set<? extends Person> persons = PopulationIO.loadFromXML(args[0], new PlainFactory());

        TaskRunner.run(new SortLegsTask(MiDKeys.LEG_INDEX, new SortLegsTask.IntComparator()), persons);

        PopulationIO.writeToXML(args[1], persons);
    }
}
