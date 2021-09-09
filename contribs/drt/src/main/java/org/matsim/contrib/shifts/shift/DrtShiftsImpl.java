package org.matsim.contrib.shifts.shift;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.core.utils.misc.Counter;

import java.util.Collections;
import java.util.Map;

/**
 * @author nkuehnel, fzwick
 */
public class DrtShiftsImpl implements DrtShifts {

    private final Counter counter = new Counter("[DrtShiftsImpl] added shift # ");

    private final Map<Id<DrtShift>, DrtShift> shifts = new IdMap<>(DrtShift.class);
    private final DrtShiftFactory builder = new DrtShiftFactoryImpl();

    @Override
    public DrtShiftFactory getFactory() {
        return builder;
    }

    @Override
    public Map<Id<DrtShift>, ? extends DrtShift> getShifts() {
        return Collections.unmodifiableMap(this.shifts);
    }

    @Override
    public void addShift(DrtShift shift) {
        if (this.shifts.containsKey(shift.getId())) {
            throw new IllegalArgumentException("Shift with id = " + shift.getId() + " already exists.");
        } else {
            this.shifts.put(shift.getId(), shift);
            this.counter.incCounter();
        }
    }

    @Override
    public DrtShift removeShift(Id<DrtShift> shift) {
        return this.shifts.remove(shift);
    }
}
