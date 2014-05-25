/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * MetaPopulationModule.java
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

package playground.mzilske.metapopulation;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import org.matsim.core.controler.listener.ControlerListener;

public class MetaPopulationModule extends AbstractModule {
    @Override
    protected void configure() {
        Multibinder<ControlerListener> controlerListenerBinder = Multibinder.newSetBinder(binder(), ControlerListener.class);
        controlerListenerBinder.addBinding().to(MetaPopulationReplanningControlerListener.class);
        controlerListenerBinder.addBinding().to(MetaPopulationScoringControlerListener.class);
        controlerListenerBinder.addBinding().toProvider(MetaPopulationStatsControlerListenerProvider.class);
    }
}
