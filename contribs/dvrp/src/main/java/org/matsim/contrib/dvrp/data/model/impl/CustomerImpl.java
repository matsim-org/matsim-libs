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

package org.matsim.contrib.dvrp.data.model.impl;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.dvrp.data.model.Customer;


/**
 * @author michalm
 */
public class CustomerImpl
    implements Customer
{
    private final Id id;
    private final String name;


    public CustomerImpl(Id id, String name)
    {
        this.id = id;
        this.name = name;
    }


    @Override
    public Id getId()
    {
        return id;
    }


    @Override
    public String getName()
    {
        return name;
    }


    @Override
    public String toString()
    {
        return "Customer_" + id;
    }
}
