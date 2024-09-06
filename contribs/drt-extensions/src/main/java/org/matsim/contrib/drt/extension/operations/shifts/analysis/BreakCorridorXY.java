package org.matsim.contrib.drt.extension.operations.shifts.analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.drt.extension.operations.shifts.events.DrtShiftBreakEndedEvent;
import org.matsim.contrib.drt.extension.operations.shifts.events.DrtShiftBreakEndedEventHandler;
import org.matsim.contrib.drt.extension.operations.shifts.events.DrtShiftBreakStartedEvent;
import org.matsim.contrib.drt.extension.operations.shifts.events.DrtShiftBreakStartedEventHandler;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShift;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShiftBreakSpecification;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShiftsSpecification;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;

import jakarta.inject.Provider;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author nkuehnel / MOIA
 */
public class BreakCorridorXY implements DrtShiftBreakStartedEventHandler, DrtShiftBreakEndedEventHandler {

    private final String mode;

    private final Provider<DrtShiftsSpecification> shifts;
    private final Map<Id<DrtShift>, Tuple<Double,Double>> shift2plannedVsActualBreakStart = new HashMap<>();
    private final Map<Id<DrtShift>, Tuple<Double,Double>> shift2plannedVsActualBreakEnd = new HashMap<>();

    public BreakCorridorXY(String mode, Provider<DrtShiftsSpecification> shifts) {
        super();
        this.mode = mode;
        this.shifts = shifts;
        reset(0);
    }

    /* Implementation of EventHandler-Interfaces */

    @Override
    public void handleEvent(DrtShiftBreakStartedEvent event) {
        if (event.getMode().equals(mode)) {
            final DrtShiftBreakSpecification drtShiftBreak = shifts.get().getShiftSpecifications().get(event.getShiftId()).getBreak().orElseThrow();
            final double earliestBreakStartTime = drtShiftBreak.getEarliestBreakStartTime();
            shift2plannedVsActualBreakStart.put(event.getShiftId(), new Tuple<>(earliestBreakStartTime, event.getTime()));
        }
    }

    @Override
    public void handleEvent(DrtShiftBreakEndedEvent event) {
        if (event.getMode().equals(mode)) {
            final DrtShiftBreakSpecification drtShiftBreak = shifts.get().getShiftSpecifications().get(event.getShiftId()).getBreak().orElseThrow();
            final double latestBreakEndTime = drtShiftBreak.getLatestBreakEndTime();
            shift2plannedVsActualBreakEnd.put(event.getShiftId(), new Tuple<>(latestBreakEndTime, event.getTime()));
        }
    }

    @Override
    public void reset(final int iter) {
		this.shift2plannedVsActualBreakStart.clear();
        this.shift2plannedVsActualBreakEnd.clear();
    }

    /**
     * Writes the gathered data tab-separated into a text file.
     *
     * @param filename The name of a file where to write the gathered data.
     */
    public void write(final String filename) {
        try (OutputStream stream = IOUtils.getOutputStream(IOUtils.getFileUrl(filename), false)) {
            write(new PrintStream(stream));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Writes the gathered data tab-separated into a text stream.
     *
     * @param stream The data stream where to write the gathered data.
     */
    public void write(final PrintStream stream) {
        stream.print("break of shift\tearliest break start\tlatest break end\tactual start\tactual end");
        stream.print("\n");
        for (Map.Entry<Id<DrtShift>, Tuple<Double, Double>> entry: shift2plannedVsActualBreakStart.entrySet()) {
            final Tuple<Double, Double> endTimeTuple = shift2plannedVsActualBreakEnd.get(entry.getKey());
            stream.print(entry.getKey()
                    + "\t"+ entry.getValue().getFirst()
                    + "\t" + entry.getValue().getSecond()
                    + "\t" + endTimeTuple.getFirst()
                    + "\t" + endTimeTuple.getSecond()
            );
            // new line
            stream.print("\n");
        }
    }

    public Map<Id<DrtShift>, Tuple<Double, Double>> getShift2plannedVsActualBreakStart() {
        return shift2plannedVsActualBreakStart;
    }

    public Map<Id<DrtShift>, Tuple<Double, Double>> getShift2plannedVsActualBreakEnd() {
        return shift2plannedVsActualBreakEnd;
    }
}

