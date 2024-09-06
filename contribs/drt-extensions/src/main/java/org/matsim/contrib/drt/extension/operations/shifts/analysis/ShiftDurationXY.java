package org.matsim.contrib.drt.extension.operations.shifts.analysis;

import com.google.inject.Provider;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.drt.extension.operations.shifts.events.*;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShift;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShiftBreakSpecification;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShiftSpecification;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShiftsSpecification;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author nkuehnel / MOIA
 */
public class ShiftDurationXY implements DrtShiftStartedEventHandler, DrtShiftEndedEventHandler,
        DrtShiftBreakStartedEventHandler, DrtShiftBreakEndedEventHandler {

    private final Provider<DrtShiftsSpecification> shifts;
    private final Map<Id<DrtShift>, Double> shift2StartTime = new HashMap<>();
    private final Map<Id<DrtShift>, Double> shift2BreakStartTime = new HashMap<>();

    private final Map<Id<DrtShift>, Tuple<Double,Double>> shift2plannedVsActualDuration = new HashMap<>();
    private final Map<Id<DrtShift>, Tuple<Double,Double>> shift2plannedVsActualBreakDuration = new HashMap<>();

    private final String mode;

    public ShiftDurationXY(Provider<DrtShiftsSpecification> shifts, String mode) {
        super();
        this.shifts = shifts;
        this.mode = mode;
        reset(0);
    }

    /* Implementation of EventHandler-Interfaces */

    @Override
    public void handleEvent(final DrtShiftStartedEvent event) {
        if (event.getMode().equals(mode)) {
            shift2StartTime.put(event.getShiftId(), event.getTime());
        }
    }

    @Override
    public void handleEvent(DrtShiftBreakStartedEvent event) {
        if (event.getMode().equals(mode)) {
            shift2BreakStartTime.put(event.getShiftId(), event.getTime());
        }
    }

    @Override
    public void handleEvent(final DrtShiftEndedEvent event) {
        if (event.getMode().equals(mode)) {
            Map<Id<DrtShift>, DrtShiftSpecification> shiftSpecifications = shifts.get().getShiftSpecifications();
            if(shiftSpecifications.containsKey(event.getShiftId())) {
                final DrtShiftSpecification drtShift = shiftSpecifications.get(event.getShiftId());
                final Double start = shift2StartTime.get(event.getShiftId());
                double duration = event.getTime() - start;
                double plannedDuration = drtShift.getEndTime() - drtShift.getStartTime();
                shift2plannedVsActualDuration.put(event.getShiftId(), new Tuple<>(plannedDuration, duration));
            }
        }
    }

    @Override
    public void handleEvent(DrtShiftBreakEndedEvent event) {
        if (event.getMode().equals(mode)) {
            Map<Id<DrtShift>, DrtShiftSpecification> shiftSpecifications = shifts.get().getShiftSpecifications();
            if(shiftSpecifications.containsKey(event.getShiftId())) {
                final Double start = shift2BreakStartTime.get(event.getShiftId());
                double duration = event.getTime() - start;
                final DrtShiftBreakSpecification drtShift = shifts.get().getShiftSpecifications().get(event.getShiftId()).getBreak().orElseThrow();
                double plannedDuration = drtShift.getDuration();
                shift2plannedVsActualBreakDuration.put(event.getShiftId(), new Tuple<>(plannedDuration, duration));
            }
        }
    }

    @Override
    public void reset(final int iter) {
        this.shift2plannedVsActualDuration.clear();
		this.shift2plannedVsActualBreakDuration.clear();
        this.shift2StartTime.clear();
		this.shift2BreakStartTime.clear();
    }

    /**
     * Writes the gathered data tab-separated into a text file.
     *
     * @param filename The name of a file where to write the gathered data.
     */
    public void writeShiftDuration(final String filename) {
        try (OutputStream stream = IOUtils.getOutputStream(IOUtils.getFileUrl(filename), false)) {
            writeShiftDuration(new PrintStream(stream));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Writes the gathered data tab-separated into a text stream.
     *
     * @param stream The data stream where to write the gathered data.
     */
    public void writeShiftDuration(final PrintStream stream) {
        stream.print("shift\tplanned duration\tactual duration");
        stream.print("\n");
        for (Map.Entry<Id<DrtShift>, Tuple<Double, Double>> entry: shift2plannedVsActualDuration.entrySet()) {
            // data about all modes
            stream.print(entry.getKey() + "\t" + entry.getValue().getFirst() + "\t" + entry.getValue().getSecond());
            // new line
            stream.print("\n");
        }
    }

    /**
     * Writes the gathered data tab-separated into a text file.
     *
     * @param filename The name of a file where to write the gathered data.
     */
    public void writeBreakDuration(final String filename) {
        try (OutputStream stream = IOUtils.getOutputStream(IOUtils.getFileUrl(filename), false)) {
            writeBreakDuration(new PrintStream(stream));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Writes the gathered data tab-separated into a text stream.
     *
     * @param stream The data stream where to write the gathered data.
     */
    public void writeBreakDuration(final PrintStream stream) {
        stream.print("break of shift\tplanned duration\tactual duration");
        stream.print("\n");
        for (Map.Entry<Id<DrtShift>, Tuple<Double, Double>> entry: shift2plannedVsActualBreakDuration.entrySet()) {
            // data about all modes
            stream.print(entry.getKey() + "\t" + entry.getValue().getFirst() + "\t" + entry.getValue().getSecond());
            // new line
            stream.print("\n");
        }
    }

    public Map<Id<DrtShift>, Tuple<Double, Double>> getShift2plannedVsActualDuration() {
        return shift2plannedVsActualDuration;
    }

    public Map<Id<DrtShift>, Tuple<Double, Double>> getShift2plannedVsActualBreakDuration() {
        return shift2plannedVsActualBreakDuration;
    }
}
