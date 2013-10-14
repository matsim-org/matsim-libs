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

import pl.poznan.put.vrp.dynamic.data.model.Localizable;
import pl.poznan.put.vrp.dynamic.data.network.Vertex;


public class VertexTimePair
    implements Localizable
{
    private final Vertex vertex;
    private final int time;


    public VertexTimePair(Vertex vertex, int time)
    {
        this.vertex = vertex;
        this.time = time;
    }


    @Override
    public Vertex getVertex()
    {
        return vertex;
    }


    public int getTime()
    {
        return time;
    }
}
