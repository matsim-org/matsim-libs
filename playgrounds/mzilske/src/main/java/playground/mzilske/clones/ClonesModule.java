/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * ClonesModule.java
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

package playground.mzilske.clones;


import org.matsim.core.controler.AbstractModule;

public class ClonesModule extends AbstractModule {

    @Override
    public void install() {
        bindAsSingleton(CloneService.class, CloneServiceImpl.class);
        addControlerListenerBinding().toProvider(ClonesControlerListener.class);
        addControlerListenerBinding().toProvider(CloneHistogramControlerListener.class);
    }
}
