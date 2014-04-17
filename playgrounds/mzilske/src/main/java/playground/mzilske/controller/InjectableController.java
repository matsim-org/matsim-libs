/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * Controller.java
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
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.AbstractController;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.pt.PtConstants;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

class InjectableController extends AbstractController implements Controller {

    private Config config;
    @Inject @Named("coreControlerListeners") List<ControlerListener> coreControlerListeners;
    @Inject @Named("defaultControlerListeners") List<ControlerListener> controlerListeners;
    @Inject Set<ControlerListener> pluginControlerListeners;
    @Inject MobsimProvider mobsimProvider;
    @Inject Controler.TerminationCriterion terminationCriterion;
    @Inject @Named("prepareForSim") Provider<Runnable> prepareForSim;

    @Inject
    InjectableController(Config config) {
        super();
        this.config = config;
        setupOutputDirectory(config.controler().getOutputDirectory(), config.controler().getRunId(), true);
    }

    public void run() {
        if (config.scenario().isUseTransit()) {
            setupTransitSimulation();
        }
        run(config);
    }

    private void setupTransitSimulation() {
        Set<ControlerConfigGroup.EventsFileFormat> formats = EnumSet.copyOf(this.config.controler().getEventsFileFormats());
        formats.add(ControlerConfigGroup.EventsFileFormat.xml);
        this.config.controler().setEventsFileFormats(formats);

        PlanCalcScoreConfigGroup.ActivityParams transitActivityParams = new PlanCalcScoreConfigGroup.ActivityParams(PtConstants.TRANSIT_ACTIVITY_TYPE);
        transitActivityParams.setTypicalDuration(120.0);
        transitActivityParams.setOpeningTime(0.) ;
        transitActivityParams.setClosingTime(0.) ;

        this.config.planCalcScore().addActivityParams(transitActivityParams);
    }

    @Override
    protected void loadCoreListeners() {
        for (ControlerListener controlerListener : coreControlerListeners) {
            addCoreControlerListener(controlerListener);
        }
        for (ControlerListener controlerListener : controlerListeners) {
            addControlerListener(controlerListener);
        }
        for (ControlerListener controlerListener : pluginControlerListeners) {
            addControlerListener(controlerListener);
        }
    }

    @Override
    protected void prepareForSim() {
        prepareForSim.get().run();
    }

    @Override
    protected void runMobSim(int iteration) {
        Mobsim sim = mobsimProvider.create(iteration);
        sim.run();
    }

    @Override
    protected boolean continueIterations(int iteration) {
        return terminationCriterion.continueIterations(iteration);
    }

}
