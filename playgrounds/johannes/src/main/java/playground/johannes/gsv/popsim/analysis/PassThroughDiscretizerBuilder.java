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

package playground.johannes.gsv.popsim.analysis;

import org.matsim.contrib.common.stats.Discretizer;

/**
 * @author johannes
 */
public class PassThroughDiscretizerBuilder implements DiscretizerBuilder {

    private final Discretizer discretizer;

    private final String name;

    public PassThroughDiscretizerBuilder(Discretizer discretizer, String name) {
        this.discretizer = discretizer;
        this.name = name;
    }

    @Override
    public Discretizer build(double[] values) {
        return discretizer;
    }

    @Override
    public String getName() {
        return name;
    }
}
