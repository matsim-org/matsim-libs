/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package pl.poznan.put.vrp.dynamic.data.network.impl;

import java.util.*;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import pl.poznan.put.vrp.dynamic.data.network.*;

import com.google.common.collect.Iterators;


public class GrowingVrpGraph
    implements VrpGraph
{
    private final ArcFactory arcFactory;
    private final HashMap<Id, Map<Id, Arc>> arcs;// fromLink.id->toLink.id->Arc


    public GrowingVrpGraph(ArcFactory arcFactory)
    {
        this.arcFactory = arcFactory;
        arcs = new HashMap<Id, Map<Id, Arc>>();
    }


    @Override
    public Arc getArc(Link fromLink, Link toLink)
    {
        Id fromId = fromLink.getId();
        Id toId = toLink.getId();

        Map<Id, Arc> fromLinkOutgoingArcs = arcs.get(fromId);

        Arc arc;

        if (fromLinkOutgoingArcs == null) {
            fromLinkOutgoingArcs = new HashMap<Id, Arc>();
            arcs.put(fromId, fromLinkOutgoingArcs);
            arc = null;
        }
        else {
            arc = fromLinkOutgoingArcs.get(toId);
        }

        if (arc == null) {
            arc = arcFactory.createArc(fromLink, toLink);
            fromLinkOutgoingArcs.put(toLink.getId(), arc);
        }

        return arc;
    }


    @Override
    public Iterator<Arc> arcIterator()
    {
        return new GrowingVrpGraphArcIterator();
    }


    private class GrowingVrpGraphArcIterator
        implements Iterator<Arc>
    {
        private Iterator<Map<Id, Arc>> outerIter;
        private Iterator<Arc> innerIter;
        private Arc nextArc;


        public GrowingVrpGraphArcIterator()
        {
            outerIter = arcs.values().iterator();
            innerIter = Iterators.emptyIterator();
            updateNextArc();
        }


        @Override
        public Arc next()
        {
            if (nextArc == null) {
                throw new NoSuchElementException();
            }

            Arc currentArc = nextArc;
            updateNextArc();
            return currentArc;
        }


        private void updateNextArc()
        {
            if (innerIter.hasNext()) {
                nextArc = innerIter.next();
                return;
            }

            while (outerIter.hasNext()) {
                Map<Id, Arc> arcRow = outerIter.next();

                if (arcRow != null) {
                    innerIter = arcRow.values().iterator();
                    nextArc = innerIter.next(); // always at least one entry inside a row
                    return;
                }
            }

            nextArc = null;
        }


        @Override
        public boolean hasNext()
        {
            return nextArc != null;
        }


        @Override
        public void remove()
        {
            throw new UnsupportedOperationException();
        }
    }
}
