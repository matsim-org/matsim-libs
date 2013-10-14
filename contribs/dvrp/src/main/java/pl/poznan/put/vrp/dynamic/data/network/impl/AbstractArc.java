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

import pl.poznan.put.vrp.dynamic.data.network.*;

public abstract class AbstractArc
    implements Arc
{
    protected final Vertex fromVertex;
    protected final Vertex toVertex;


    public AbstractArc(Vertex fromVertex, Vertex toVertex)
    {
        this.fromVertex = fromVertex;
        this.toVertex = toVertex;
    }


    @Override
    public Vertex getFromVertex()
    {
        return fromVertex;
    }


    @Override
    public Vertex getToVertex()
    {
        return toVertex;
    }
}
