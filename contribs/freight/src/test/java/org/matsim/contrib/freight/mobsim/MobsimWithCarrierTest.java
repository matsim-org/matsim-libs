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

package org.matsim.contrib.freight.mobsim;

import org.matsim.contrib.freight.controler.RunMobSimWithCarrier;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.testcases.MatsimTestCase;

/**
 * Created by IntelliJ IDEA.
 * User: zilske
 * Date: 11/9/11
 * Time: 4:16 PM
 * To change this template use File | Settings | File Templates.
 */
public class MobsimWithCarrierTest extends MatsimTestCase {

    private String NETWORK_FILENAME;

    public void testMobsimWithCarrier() {
        NETWORK_FILENAME = getInputDirectory() + "grid.xml";
		Config config = new Config();
		config.addCoreModules();
		config.global().setCoordinateSystem("EPSG:32632");
		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(0);
        config.network().setInputFile(NETWORK_FILENAME);
		config.addQSimConfigGroup(new QSimConfigGroup());
		Controler controler = new Controler(config);
		controler.setCreateGraphs(false);
		controler.addControlerListener(new RunMobSimWithCarrier(getInputDirectory() + "carrierPlans.xml"));
		controler.setOverwriteFiles(true);
		controler.run();
    }

}
