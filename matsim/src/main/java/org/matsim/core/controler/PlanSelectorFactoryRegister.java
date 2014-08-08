/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * PlanSelectorFactoryRegister.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
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
 */

package org.matsim.core.controler;

import org.matsim.core.replanning.selectors.PlanSelectorFactory;

import java.util.HashMap;
import java.util.Map;

public class PlanSelectorFactoryRegister {

    private Map<String, PlanSelectorFactory> factoryMap = new HashMap<String, PlanSelectorFactory>();

    public PlanSelectorFactory getInstance(String selectorType) {
        if (!factoryMap.containsKey(selectorType)) {
            throw new IllegalArgumentException("Plan strategy " + selectorType
                    + " doesn't exist.");
        }
        return factoryMap.get(selectorType);
    }

    public void register(String string, PlanSelectorFactory selectorFactory) {
        factoryMap.put(string, selectorFactory);
    }

}
