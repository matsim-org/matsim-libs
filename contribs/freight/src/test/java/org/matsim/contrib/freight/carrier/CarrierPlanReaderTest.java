/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * ${file_name}
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) ${year} by the members listed in the COPYING,        *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 *
 * ${filecomment}
 * ${package_declaration}
 *
 * ${typecomment}
 * ${type_declaration}
 */

package org.matsim.contrib.freight.carrier;

import org.junit.Test;
import sun.jvm.hotspot.utilities.Assert;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by IntelliJ IDEA.
 * User: zilske
 * Date: 10/31/11
 * Time: 11:46 AM
 * To change this template use File | Settings | File Templates.
 */
public class CarrierPlanReaderTest {

    @Test
    public void carrierPlanReaderDoesSomething() {
        Carriers carriers = new Carriers();
        CarrierPlanReader carrierPlanReader = new CarrierPlanReader(carriers);
        carrierPlanReader.read("/Users/zilske/Documents/IdeaProjects/playgrounds/sschroeder/input/carrierPlans.xml");
        junit.framework.Assert.assertEquals(2, carriers.getCarriers().size());
    }

}
