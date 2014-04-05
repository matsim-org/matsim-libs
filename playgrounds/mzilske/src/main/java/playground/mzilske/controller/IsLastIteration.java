/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * IsLastIteration.java
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

package playground.mzilske.controller;

import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;

import javax.inject.Inject;

class IsLastIteration implements Controler.TerminationCriterion {

    @Inject Config config;

    @Override
    public boolean continueIterations(int iteration) {
        return (iteration <= config.controler().getLastIteration());
    }

}
