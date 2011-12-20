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

import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.population.algorithms.PlanAlgorithm;

/**
* Created by IntelliJ IDEA.
* User: zilske
* Date: 10/31/11
* Time: 12:41 PM
* To change this template use File | Settings | File Templates.
*/
public class SimpleCarrierAgentFactory implements CarrierAgentFactory {

    private PlanAlgorithm router;

    public void setRouter(PlanAlgorithm router){
        this.router = router;
    }

    @Override
    public CarrierAgent createAgent(CarrierAgentTracker tracker,Carrier carrier) {
        CarrierAgentImpl agent = new CarrierAgentImpl(tracker, carrier, router,  new CarrierDriverAgentFactoryImpl());
        return agent;
    }

}
