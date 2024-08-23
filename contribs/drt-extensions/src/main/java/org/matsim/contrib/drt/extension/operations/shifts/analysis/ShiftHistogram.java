package org.matsim.contrib.drt.extension.operations.shifts.analysis;

import org.matsim.contrib.drt.extension.operations.shifts.events.*;
import org.matsim.core.config.Config;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UncheckedIOException;

/**
 * @author nkuehnel / MOIA
 */
public class ShiftHistogram implements DrtShiftStartedEventHandler, DrtShiftEndedEventHandler,
        DrtShiftBreakStartedEventHandler, DrtShiftBreakEndedEventHandler {

    private final String mode;

    public static final int DEFAULT_END_TIME = 30 * 3600;
    public static final int DEFAULT_BIN_SIZE = 300;

    private int iteration = 0;
    private final int binSize;
    private final int nofBins;
    private DataFrame data = null;


    public ShiftHistogram(String mode, Config config) {
        super();
        this.mode = mode;
        this.binSize = DEFAULT_BIN_SIZE;
        this.nofBins = ((int) config.qsim().getEndTime().orElse(DEFAULT_END_TIME)) / this.binSize + 1;
        reset(0);
    }

    /**
     * Creates a new LegHistogram with the specified binSize and the specified number of bins.
     *
     * @param binSize The size of a time bin in seconds.
     * @param nofBins The number of time bins for this analysis.
     */
    public ShiftHistogram(String mode, final int binSize, final int nofBins) {
        super();
        this.mode = mode;
        this.binSize = binSize;
        this.nofBins = nofBins;
        reset(0);
    }

    /* Implementation of EventHandler-Interfaces */

    @Override
    public void handleEvent(final DrtShiftStartedEvent event) {
        if (event.getMode().equals(mode)) {
            int index = getBinIndex(event.getTime());
            DataFrame dataFrame = getData();
            dataFrame.countsStart[index]++;

        }
    }

    @Override
    public void handleEvent(final DrtShiftEndedEvent event) {
        if (event.getMode().equals(mode)) {
            int index = getBinIndex(event.getTime());
            DataFrame dataFrame = getData();
            dataFrame.countsEnd[index]++;

        }
    }

    @Override
    public void handleEvent(DrtShiftBreakStartedEvent event) {
        if (event.getMode().equals(mode)) {
            int index = getBinIndex(event.getTime());
            DataFrame dataFrame = getData();
            dataFrame.countsBreaksStart[index]++;
        }
    }

    @Override
    public void handleEvent(DrtShiftBreakEndedEvent event) {
        if (event.getMode().equals(mode)) {
            int index = getBinIndex(event.getTime());
            DataFrame dataFrame = getData();
            dataFrame.countsBreaksEnd[index]++;
        }
    }


    @Override
    public void reset(final int iter) {
        this.iteration = iter;
        this.data = null;
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
        stream.print("time\ttime\tshift_start\tshift_end\tshift_active\tshift_break_start\tshift_break_end\tshift_break_active");
        stream.print("\n");
        int active = 0;
        int activeBreaks = 0;
        DataFrame allModesData = getData();
        for (int i = 0; i < allModesData.countsStart.length; i++) {
            // data about all modes
            active = active + allModesData.countsStart[i] - allModesData.countsEnd[i];
            activeBreaks = activeBreaks + allModesData.countsBreaksStart[i] - allModesData.countsBreaksEnd[i];
            stream.print(Time.writeTime(i * this.binSize) + "\t" + i * this.binSize);
            stream.print("\t" + allModesData.countsStart[i] + "\t" + allModesData.countsEnd[i] + "\t" + active);
            stream.print("\t" + allModesData.countsBreaksStart[i] + "\t" + allModesData.countsBreaksEnd[i] + "\t" + activeBreaks);
            // new line
            stream.print("\n");
        }
    }

    /**
     * @return number of shift starts per time-bin
     */
    public int[] getShiftStarts() {
        return this.data.countsStart;
    }

    /**
     * @return number of all shift ends per time-bin
     */
    public int[] getArrivals() {
        return this.data.countsEnd;
    }


    int getIteration() {
        return this.iteration;
    }


    private int getBinIndex(final double time) {
        int bin = (int) (time / this.binSize);
        return Math.min(bin, this.nofBins);
    }

    DataFrame getData() {
        DataFrame dataFrame = this.data;
        if (dataFrame == null) {
            dataFrame = new DataFrame(this.binSize, this.nofBins + 1); // +1 for all times out of our range
            this.data = dataFrame;
        }
        return dataFrame;
    }


    static class DataFrame {
        final int[] countsStart;
        final int[] countsEnd;

        final int[] countsBreaksStart;
        final int[] countsBreaksEnd;

        final int binSize;

        public DataFrame(final int binSize, final int nofBins) {
            this.countsStart = new int[nofBins];
            this.countsEnd = new int[nofBins];
            this.countsBreaksStart = new int[nofBins];
            this.countsBreaksEnd = new int[nofBins];
            this.binSize = binSize;
        }
    }
}
