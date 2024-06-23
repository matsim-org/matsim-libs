/*
 * Copyright (C) 2022 MOIA GmbH - All Rights Reserved
 *
 * You may use, distribute and modify this code under the terms
 * of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 */

package org.matsim.contrib.drt.extension.operations.shifts.analysis.efficiency;

import com.google.inject.Provider;
import com.google.inject.TypeLiteral;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShiftsSpecification;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.core.controler.MatsimServices;

/**
 * @author nkuehnel / MOIA
 */
public class DrtShiftEfficiencyModeModule extends AbstractDvrpModeModule {

    private final DrtConfigGroup drtConfigGroup;

    public DrtShiftEfficiencyModeModule(DrtConfigGroup drtConfigGroup) {
        super(drtConfigGroup.getMode());
        this.drtConfigGroup = drtConfigGroup;
    }

    @Override
    public void install() {
        bindModal(ShiftEfficiencyTracker.class).toProvider(modalProvider(getter ->
                new ShiftEfficiencyTracker(getMode()))).asEagerSingleton();
        addEventHandlerBinding().to(modalKey(ShiftEfficiencyTracker.class));
		bindModal(ShiftEfficiencyAnalysisControlerListener.class).toProvider(modalProvider(getter ->
						new ShiftEfficiencyAnalysisControlerListener(drtConfigGroup,
								getter.getModal(ShiftEfficiencyTracker.class),
								getter.getModal(new TypeLiteral<Provider<DrtShiftsSpecification>>(){}),
								getter.get(MatsimServices.class))
		));
        addControlerListenerBinding().to(modalKey(ShiftEfficiencyAnalysisControlerListener.class));
    }
}
