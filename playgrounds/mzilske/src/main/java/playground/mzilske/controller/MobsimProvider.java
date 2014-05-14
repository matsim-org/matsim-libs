/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * MobsimProvider.java
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

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.SimulationConfigGroup;
import org.matsim.core.controler.MobsimFactoryRegister;
import org.matsim.core.controler.MobsimRegistrar;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.mobsim.external.ExternalMobsim;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.framework.ObservableMobsim;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;
import org.matsim.vis.snapshotwriters.SnapshotWriter;
import org.matsim.vis.snapshotwriters.SnapshotWriterFactory;
import org.matsim.vis.snapshotwriters.SnapshotWriterManager;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;
import java.util.Map;

class MobsimProvider {

    @Inject Config config;
    @Inject Scenario scenario;
    @Inject @Named("fullyLoadedEventsManager") EventsManager events;
    @Inject OutputDirectoryHierarchy controlerIO;
    @Inject Map<String, MobsimFactory> mobsimFactories;
    @Inject List<MobsimListener> mobsimListeners;
    @Inject List<SnapshotWriterFactory> snapshotWriterFactories;

    public Mobsim create(int iteration) {
        if (config.getModule(SimulationConfigGroup.GROUP_NAME) != null && ((SimulationConfigGroup) this.config.getModule(SimulationConfigGroup.GROUP_NAME)).getExternalExe() != null ) {
            ExternalMobsim simulation = new ExternalMobsim(scenario, this.events);
            simulation.setControlerIO(controlerIO);
            simulation.setIterationNumber(iteration);
            return simulation;
        } else {
            MobsimRegistrar mobsimRegistrar = new MobsimRegistrar();
            MobsimFactoryRegister mobsimFactoryRegister = mobsimRegistrar.getFactoryRegister();
            for (Map.Entry<String, MobsimFactory> entry : mobsimFactories.entrySet()) {
                mobsimFactoryRegister.register(entry.getKey(), entry.getValue());
            }
            MobsimFactory mobsimFactory = mobsimFactoryRegister.getInstance(config.controler().getMobsim());
            Mobsim sim = mobsimFactory.createMobsim(scenario, events);
            if (sim instanceof ObservableMobsim) {
                for (MobsimListener l : mobsimListeners) {
                    ((ObservableMobsim) sim).addQueueSimulationListeners(l);
                }
                if (config.controler().getWriteSnapshotsInterval() != 0 && iteration % config.controler().getWriteSnapshotsInterval() == 0) {
                    SnapshotWriterManager manager = new SnapshotWriterManager(config);
                    for (SnapshotWriterFactory snapshotWriterFactory : snapshotWriterFactories) {
                        String baseFileName = snapshotWriterFactory.getPreferredBaseFilename();
                        String fileName = controlerIO.getIterationFilename(iteration, baseFileName);
                        SnapshotWriter snapshotWriter = snapshotWriterFactory.createSnapshotWriter(fileName, scenario);
                        manager.addSnapshotWriter(snapshotWriter);
                    }
                    ((ObservableMobsim) sim).addQueueSimulationListeners(manager);
                }
            }
            return sim;
        }
    }

}
