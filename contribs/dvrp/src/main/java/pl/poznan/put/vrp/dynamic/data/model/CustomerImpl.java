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

package pl.poznan.put.vrp.dynamic.data.model;

import pl.poznan.put.vrp.dynamic.data.network.Vertex;


/**
 * @author michalm
 */
public class CustomerImpl
    implements Customer
{
    private final int id;
    private final String name;
    private final Vertex vertex;


    public CustomerImpl(int id, String name, Vertex vertex)
    {
        this.id = id;
        this.name = name;
        this.vertex = vertex;
    }


    @Override
    public int getId()
    {
        return id;
    }


    @Override
    public String getName()
    {
        return name;
    }


    @Override
    public Vertex getVertex()
    {
        return vertex;
    }


    @Override
    public String toString()
    {
        return "Customer_" + id;
    }
}
