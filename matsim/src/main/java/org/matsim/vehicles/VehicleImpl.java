/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package org.matsim.vehicles;

import org.matsim.api.core.v01.Id;
import org.matsim.core.gbl.Gbl;
import org.matsim.utils.objectattributes.attributable.Attributes;
import org.matsim.utils.objectattributes.attributable.AttributesImpl;

final class VehicleImpl implements Vehicle {

    private VehicleType type;
    private Id<Vehicle> id;
    private Attributes attributes;

    VehicleImpl(Id<Vehicle> id, VehicleType type) {
        Gbl.assertNotNull(id);
        Gbl.assertNotNull(type);
        this.id = id;
        this.type = type;
        this.attributes = new AttributesImpl();
    }

    @Override
    public Id<Vehicle> getId() {
        return id;
    }

    @Override
    public VehicleType getType() {
        return this.type;
    }

    @Override
    public String toString() {
        return "[ID=" + id + " | type=" + type.toString() + "]";
    }


    @Override
    public Attributes getAttributes() {
        return this.attributes;
    }
}
