/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * MatricesModule.java
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

package playground.mzilske.matrices;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import org.matsim.core.controler.listener.ControlerListener;
import playground.mzilske.cdr.LinkIsZone;
import playground.mzilske.cdr.Sightings;
import playground.mzilske.cdr.SightingsImpl;
import playground.mzilske.cdr.ZoneTracker;

public class MatricesModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(Sightings.class).to(SightingsImpl.class);
        bind(ZoneTracker.LinkToZoneResolver.class).to(LinkIsZone.class);
        Multibinder<ControlerListener> controlerListenerBinder = Multibinder.newSetBinder(binder(), ControlerListener.class);
        controlerListenerBinder.addBinding().toProvider(MatrixDemandControlerListener.class);
        controlerListenerBinder.addBinding().to(MatricesUpdater.class);
    }
}
