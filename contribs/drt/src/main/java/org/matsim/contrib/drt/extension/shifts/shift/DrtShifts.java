package org.matsim.contrib.drt.extension.shifts.shift;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.internal.MatsimToplevelContainer;

import java.util.Map;

/**
 * @author nkuehnel, fzwick
 */
public interface DrtShifts extends MatsimToplevelContainer {

    @Override
    DrtShiftFactory getFactory();

    Map<Id<DrtShift>, ? extends DrtShift> getShifts();

    void addShift(DrtShift shift);

    DrtShift removeShift(Id<DrtShift> shift);

}
