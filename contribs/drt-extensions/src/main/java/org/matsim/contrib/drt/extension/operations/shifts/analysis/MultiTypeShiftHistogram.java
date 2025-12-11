package org.matsim.contrib.drt.extension.operations.shifts.analysis;

import org.matsim.contrib.drt.extension.operations.shifts.events.*;
import org.matsim.core.config.Config;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class MultiTypeShiftHistogram
        implements DrtShiftStartedEventHandler,
        DrtShiftEndedEventHandler,
        DrtShiftBreakStartedEventHandler,
        DrtShiftBreakEndedEventHandler {

    private final String mode;
    private final Config config;
    private final ShiftHistogram combined;
    private final Map<String, ShiftHistogram> byType = new HashMap<>();

    public MultiTypeShiftHistogram(String mode, Config config) {
        this.mode    = mode;
        this.config = config;
        this.combined = new ShiftHistogram(mode, config);
    }

    private ShiftHistogram getFor(String type) {
        return byType.computeIfAbsent(type, t -> new ShiftHistogram(mode, config));
    }

    private void dispatch(AbstractShiftEvent event,
                          Consumer<ShiftHistogram> op) {
        op.accept(combined);
        // and, if the event has a type, to that histogram
        event.getShiftType().ifPresent(type -> op.accept(getFor(type)));
    }

    @Override
    public void handleEvent(DrtShiftStartedEvent e) {
        if (!e.getMode().equals(mode)) {
            return;
        }
        dispatch(e, h -> h.handleEvent(e));
    }

    @Override
    public void handleEvent(DrtShiftBreakEndedEvent e) {
        if (!e.getMode().equals(mode)) {
            return;
        }
        dispatch(e, h -> h.handleEvent(e));
    }

    @Override
    public void handleEvent(DrtShiftBreakStartedEvent e) {
        if (!e.getMode().equals(mode)) {
            return;
        }
        dispatch(e, h -> h.handleEvent(e));
    }

    @Override
    public void handleEvent(DrtShiftEndedEvent e) {
        if (!e.getMode().equals(mode)) {
            return;
        }
        dispatch(e, h -> h.handleEvent(e));
    }


    @Override
    public void reset(int iter) {
        combined.reset(iter);
        byType.clear();
    }

    /**
     * @return number of shift starts per type and time-bin
     */
    public Map<String, int[]> getShiftStarts() {
        Map<String, int[]> result = new HashMap<>();
        for (Map.Entry<String,ShiftHistogram> e : byType.entrySet()) {
            result.put(e.getKey(), (e.getValue().getShiftStarts()));
        }
        return result;
    }

    /**
     * Applies the given function to a shift histogram for a given type. The type may be null
     * @param function
     */
    public void applyToAll(BiFunction<String, ShiftHistogram, Void> function) {
        function.apply(null, combined);
        for (Map.Entry<String, ShiftHistogram> e : byType.entrySet()) {
            function.apply(e.getKey(), e.getValue());
        }
    }
}

