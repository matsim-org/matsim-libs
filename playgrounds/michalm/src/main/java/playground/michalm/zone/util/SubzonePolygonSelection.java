/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.michalm.zone.util;

import org.matsim.api.core.v01.Id;

import pl.poznan.put.util.random.WeightedRandomSelection;

import com.google.common.collect.*;
import com.vividsolutions.jts.geom.Polygon;


public class SubzonePolygonSelection<T>
{
    // zoneId x T -> random selection of polygons
    private final Table<Id, T, WeightedRandomSelection<Polygon>> selectionTable;


    public SubzonePolygonSelection(Iterable<? extends Id> zoneIds, Iterable<? extends T> attributes)
    {
        selectionTable = ArrayTable.create(zoneIds, attributes);
    }


    public SubzonePolygonSelection()
    {
        selectionTable = HashBasedTable.create();
    }


    public void add(Id zoneId, T attribute, Polygon polygon, double weight)
    {
        WeightedRandomSelection<Polygon> selection = selectionTable.get(zoneId, attribute);

        if (selection == null) {
            selection = new WeightedRandomSelection<Polygon>();
            selectionTable.put(zoneId, attribute, selection);
        }

        selection.add(polygon, weight);
    }


    public boolean contains(Id zoneId, T attribute)
    {
        WeightedRandomSelection<Polygon> selection = selectionTable.get(zoneId, attribute);
        return selection != null && selection.size() > 0;
    }


    public Polygon select(Id zoneId, T attribute)
    {
        WeightedRandomSelection<Polygon> selection = selectionTable.get(zoneId, attribute);
        return selection == null ? null : selection.select();
    }
}
