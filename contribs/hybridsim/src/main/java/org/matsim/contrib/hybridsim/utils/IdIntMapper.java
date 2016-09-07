package org.matsim.contrib.hybridsim.utils;
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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by laemmel on 15/08/16.
 */
public class IdIntMapper {

    private final Map<Id<Link>, Integer> linkIdInt = new HashMap<>();
    private final Map<Integer, Id<Link>> intLinkId = new HashMap<>();
    private final Map<Id<Node>, Integer> nodeIdInt = new HashMap<>();
    private final Map<Integer, Id<Node>> intNodeId = new HashMap<>();
    private final Map<Id<Person>, Integer> personIdInt = new HashMap<>();
    private final Map<Integer, Id<Person>> intPersonId = new HashMap<>();
    private int nextId = 0;

    public int getIntLink(Id<Link> id) {
        Integer ret = linkIdInt.get(id);
        if (ret == null) {
            ret = nextId++;
            linkIdInt.put(id, ret);
            intLinkId.put(ret, id);
        }
        return ret;
    }

    public Id<Link> getLinkId(int id) {
        return intLinkId.get(id);
    }

    public int getIntNode(Id<Node> id) {
        Integer ret = nodeIdInt.get(id);
        if (ret == null) {
            ret = nextId++;
            nodeIdInt.put(id, ret);
            intNodeId.put(ret, id);
        }
        return ret;
    }

    public Id<Node> getNodeId(int id) {
        return intNodeId.get(id);
    }

    public int getIntPerson(Id<Person> id) {
        Integer ret = personIdInt.get(id);
        if (ret == null) {
            ret = nextId++;
            personIdInt.put(id, ret);
            intPersonId.put(ret, id);
        }
        return ret;
    }

    public Id<Person> getPersonId(int id) {
        return intPersonId.get(id);
    }
}
