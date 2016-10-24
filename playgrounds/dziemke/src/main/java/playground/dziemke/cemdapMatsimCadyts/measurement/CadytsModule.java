/* *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.dziemke.cemdapMatsimCadyts.measurement;

import cadyts.calibrators.analytical.AnalyticalCalibrator;
import com.google.inject.Binder;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.cadyts.car.PlansTranslatorBasedOnEvents;
import org.matsim.contrib.cadyts.general.CadytsBuilderImpl;
import org.matsim.contrib.cadyts.general.CadytsConfigGroup;
import org.matsim.contrib.cadyts.general.LookUpItemFromId;
import org.matsim.contrib.cadyts.general.PlansTranslator;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.counts.Counts;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.Set;
import java.util.stream.Collectors;

public class CadytsModule extends AbstractModule {

    @Override
    public void install() {
        Multibinder<MeasurementLoader<Link>> measurementLoaderBinder = Multibinder.newSetBinder((Binder) binder(), new TypeLiteral<MeasurementLoader<Link>>(){});
        bind(AnalyticalCalibrator.class).toProvider(CalibratorProvider.class).in(Singleton.class);
        bind(PlansTranslatorBasedOnEvents.class).in(Singleton.class);
        bind(PlansTranslator.class).to(PlansTranslatorBasedOnEvents.class).in(Singleton.class);
        addControlerListenerBinding().to(CadytsControlerListener.class);
        addEventHandlerBinding().to(PlansTranslatorBasedOnEvents.class);
    }

    static class CalibratorProvider implements Provider<AnalyticalCalibrator<Link>> {

        @Inject Scenario scenario;
        @Inject Set<MeasurementLoader<Link>> measurementLoaders;

        @Override
        public AnalyticalCalibrator<Link> get() {
            CadytsConfigGroup cadytsConfig = ConfigUtils.addOrGetModule(scenario.getConfig(), CadytsConfigGroup.GROUP_NAME, CadytsConfigGroup.class);
            LookUpItemFromId<Link> linkLookUp = new LookUpItemFromId<Link>() {
                @Override
                public Link getItem(Id id) {
                    return scenario.getNetwork().getLinks().get(id);
                }
            };
            Counts<Link> calibrationCounts = (Counts<Link>) scenario.getScenarioElement("calibrationCounts");
            cadytsConfig.setCalibratedItems(calibrationCounts.getCounts().keySet().stream().map(Id::toString).collect(Collectors.toSet()));
            AnalyticalCalibrator<Link> linkAnalyticalCalibrator = new CadytsBuilderImpl().buildCalibratorAndAddMeasurements(scenario.getConfig(), calibrationCounts, linkLookUp, Link.class);
            for (MeasurementLoader<Link> measurementLoader : measurementLoaders) {
                measurementLoader.load(linkAnalyticalCalibrator);
            }
            return linkAnalyticalCalibrator;
        }
    }
}