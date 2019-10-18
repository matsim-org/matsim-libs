/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

package playground.vsp.cadyts.multiModeCadyts;

import java.util.Map;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import org.matsim.api.core.v01.Id;
import org.matsim.core.controler.AbstractModule;
import org.matsim.counts.Counts;

/**
 * Created by amit on 24.02.18.
 */

public class MultiModalCountsCadytsModule extends AbstractModule {

    private final Map<Id<ModalCountsLinkIdentifier>, ModalCountsLinkIdentifier> modalLinkContainer;
    private final Counts<ModalCountsLinkIdentifier> modalLinkCounts;

    public MultiModalCountsCadytsModule(Counts<ModalCountsLinkIdentifier> modalLinkCounts,
                                     Map<Id<ModalCountsLinkIdentifier>, ModalCountsLinkIdentifier> modalLinkContainer){
        this.modalLinkContainer = modalLinkContainer;
        this.modalLinkCounts = modalLinkCounts;
    }

    @Override
    public void install() {
        bind(Key.get(new TypeLiteral<Counts<ModalCountsLinkIdentifier>>(){}, Names.named("calibration"))).toInstance(modalLinkCounts);
        bind(Key.get(new TypeLiteral<Map<Id<ModalCountsLinkIdentifier>,ModalCountsLinkIdentifier>>(){})).toInstance(modalLinkContainer);

        bind(ModalCountsCadytsContext.class).asEagerSingleton();
        addControlerListenerBinding().to(ModalCountsCadytsContext.class);

        addControlerListenerBinding().to(MultiModeCountsControlerListener.class);
    }
}