/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * SightingsImpl.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * *********************************************************************** 
 */

package playground.mzilske.cdr;


import org.matsim.api.core.v01.Id;
import playground.mzilske.d4d.Sighting;

import java.util.List;
import java.util.Map;

public class SightingsImpl implements Sightings {

    private Map<Id, List<Sighting>> sightings;

    public SightingsImpl(Map<Id, List<Sighting>> sightings) {
        this.sightings = sightings;
    }

    @Override
    public Map<Id, List<Sighting>> getSightingsPerPerson() {
        return sightings;
    }

}
