/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package playground.agarwalamit.berlin.berlinBVG09;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import playground.agarwalamit.utils.PersonFilter;

/**
 * Created by amit on 11.06.17.
 */


public class BerlinPersonFilter implements PersonFilter {

    private static final Logger LOGGER = Logger.getLogger(BerlinPersonFilter.class);

    public enum BerlinUserGroup {
        carUsers_berlin, carUsers_brandenburger, carUsers_tourists, carUsers_airport,
        pt_tourists, pt_airport, pt_DB,
        commercial, freight }

    public static BerlinUserGroup getUserGroup(Id<Person> personId){
        if(personId.toString().startsWith("b")){ //these are Berliners
            return BerlinUserGroup.carUsers_berlin;
        } else if(personId.toString().startsWith("u")){ //these are Brandenburgers
            return BerlinUserGroup.carUsers_brandenburger;
        } else if(personId.toString().startsWith("tmiv")){// these are tourists car users
            return BerlinUserGroup.carUsers_tourists;
        } else if(personId.toString().startsWith("fhmiv")){// these are car users driving to/from airport
            return BerlinUserGroup.carUsers_airport;
        } else if(personId.toString().startsWith("toev")){// these are tourist transit users
            return BerlinUserGroup.pt_tourists;
        } else if(personId.toString().startsWith("fhoev")){// these are transit users driving to/from airport
            return BerlinUserGroup.pt_airport;
        } else if(personId.toString().startsWith("fernoev")){// these are DB transit users
            return BerlinUserGroup.pt_DB;
        } else if(personId.toString().startsWith("wv")){// this should be commercial transport -- vehicle type unclear; more likely a PASSENGER_CAR; TODO: CHK!
            return BerlinUserGroup.commercial;
        } else if(personId.toString().startsWith("lkw")){// these are HGVs
            return BerlinUserGroup.freight;
        } else {
             throw new RuntimeException("Dont know the user group of person "+ personId);
        }
    }

    @Override
    public String getUserGroupAsStringFromPersonId(Id<Person> personId) {
        return getUserGroup(personId).toString();
    }

    @Override
    public List<String> getUserGroupsAsStrings() {
        return Arrays.stream(BerlinUserGroup.values()).map(Enum::toString).collect(Collectors.toList());
    }
}
