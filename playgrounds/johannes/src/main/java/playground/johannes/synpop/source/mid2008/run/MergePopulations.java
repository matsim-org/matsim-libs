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

import playground.johannes.synpop.data.Factory;
import playground.johannes.synpop.data.Person;
import playground.johannes.synpop.data.PlainFactory;
import playground.johannes.synpop.data.io.PopulationIO;

import java.util.HashSet;
import java.util.Set;

/**
 * @author johannes
 */
public class MergePopulations {

    public static void main(String args[]) {
        Factory factory = new PlainFactory();
        Set<? extends Person> tripPersons = PopulationIO.loadFromXML(args[0], factory);
        Set<? extends Person> journeyPersons = PopulationIO.loadFromXML(args[1], factory);

        Set<Person> mergedPersons = new HashSet<>();
        mergedPersons.addAll(tripPersons);
        mergedPersons.addAll(journeyPersons);

        PopulationIO.writeToXML(args[2], mergedPersons);
    }
}
