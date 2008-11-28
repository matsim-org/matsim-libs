/* *********************************************************************** *
 * project: org.matsim.*
 * StreamReaderA.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.utils.vis.netvis.streaming;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.interfaces.basic.v01.BasicNet;
import org.matsim.utils.vis.netvis.config.IndexationConfig;
import org.matsim.utils.vis.netvis.config.TemporalConfig;

/**
 * Provides basic functionality for buffered reading of network states, most
 * likely from a file.
 * 
 * @author gunnar
 */
public abstract class StreamReaderA implements SimStateReaderI {

    // -------------------- MEMBER VARIABLES --------------------

    protected final StreamConfig streamConfig;

    protected TemporalConfig temporalConfig;

    protected IndexationConfig indexConfig;

    protected int bufferStartTime_s;

    private int bufferIndex;

    protected StateI[] buffer;

    // -------------------- CONSTRUCTION --------------------

    /**
     * The complete constructor. After construction, a call to
     * <code>open()</code> is required before writing.
     * 
     * @param network
     *            a network the stream to be read belongs to
     * @param filePrefix
     *            the read files' prefix
     * @param fileSuffix
     *            the read files' suffix
     */
    protected StreamReaderA(BasicNet network, String filePrefix,
            String fileSuffix) {
        this.streamConfig = new StreamConfig(network, filePrefix, fileSuffix, false);
    }

    // -------------------- INTERFACE DEFINITION --------------------

    protected abstract StateI newState();

    // -------------------- GETTERS, MOSTLY FOR CONVENIENCE --------------------

    public int getCurrentTime_s() {
        return bufferStartTime_s + bufferIndex * timeStepLength_s();
    }

    public BasicNet getNetwork() {
        return streamConfig.getNetwork();
    }

    public String getFilePrefix() {
        return streamConfig.getFilePrefix();
    }

    public IndexationConfig getIndexConfig() {
        return indexConfig;
    }

    public TemporalConfig getTemporalConfig() {
        return temporalConfig;
    }

    public int startTime_s() {
        return temporalConfig.getStartTime_s();
    }

    public int bufferSize() {
        return temporalConfig.getBufferSize();
    }

    public int timeStepLength_s() {
        return temporalConfig.getTimeStepLength_s();
    }

    public int endTime_s() {
        return temporalConfig.getEndTime_s();
    }

    public boolean represents(int time_s) {
        return (time_s / timeStepLength_s()) * timeStepLength_s() == getCurrentTime_s();
    }

    // -------------------- READER IMPLEMENTATION --------------------

    /**
     * Has to be called before anything is read. Conducts initialization actions
     * that are not possible during construction, since subclasses might not yet
     * have been fully initialized.
     */
    public void open() throws IOException {
        temporalConfig = streamConfig.newTemporalConfig();
        indexConfig = streamConfig.newIndexationConfig();

        bufferStartTime_s = startTime_s();

        /*
         * NEW 2oct06
         * 
         * As defined by the DumpTimer class, files are only written if they are
         * integer multiples of timeStepLength_s, which also is assumed by this
         * reader.
         */
        if (bufferStartTime_s % timeStepLength_s() != 0)
            Logger.getLogger(StreamReaderA.class).warn(
                    "bufferStartTime_s=" + bufferStartTime_s
                            + " is no integer multiple of timeStepLength_s="
                            + timeStepLength_s());

        bufferIndex = 0;

        buffer = new StateI[bufferSize()];
        for (int i = 0; i < buffer.length; i++)
            buffer[i] = newState();

        loadBuffer();
    }

    protected void loadBuffer() throws IOException {

        // CHECK

        if (buffer == null)
            throw new NullPointerException(
                    "Buffer is null, has reader been opened?");

        // CONTINUE

        String fileName = streamConfig.getStreamFileName(bufferStartTime_s);

        final FileInputStream fis = new FileInputStream(fileName);
        final DataInputStream inputStream = new DataInputStream(new BufferedInputStream(fis));

        int i = 0;
        while (i < bufferSize() && bufferStartTime_s + i * timeStepLength_s() <= endTime_s())
            buffer[i++].readMyself(inputStream);

        inputStream.close();
    }

    /**
     * Loads the very first buffer block and sets the network to the very first
     * element in this block.
     */
    public void toStart() throws IOException {
        bufferIndex = 0;
        if (bufferStartTime_s != startTime_s()) {
            bufferStartTime_s = startTime_s();
            loadBuffer();
        }
        buffer[bufferIndex].setState();
    }

    /**
     * Moves the buffer pointer one step back in time (if possible) and applies
     * the according state to the network.
     */
    public void toPrevTimeStep() throws IOException {
        if (getCurrentTime_s() > startTime_s()) {
            if (bufferIndex == 0) {
                bufferStartTime_s -= bufferSize() * timeStepLength_s();
                bufferIndex = bufferSize() - 1;
                loadBuffer();
            } else
                bufferIndex--;
        }
        buffer[bufferIndex].setState();
    }

    /**
     * Moves the buffer pointer one step forwards in time (if possible) and
     * applies the according state to the network.
     */
    public void toNextTimeStep() throws IOException {
        if (getCurrentTime_s() < endTime_s()) {
            if (bufferIndex == bufferSize() - 1) {
                bufferIndex = 0;
                bufferStartTime_s += bufferSize() * timeStepLength_s();
                loadBuffer();
            } else
                bufferIndex++;
        }
        buffer[bufferIndex].setState();
    }

    /**
     * Loads the network state according to <code>newTime_s</code> together
     * with the according buffer segment. If the passed time step is out of
     * bound or does not lie within the temporal grid, it is accordingly
     * corrected. Use <code>getCurrentTime_s</code> to check the new time
     * step.
     * 
     * @param newTime_s
     *            time of the state to be loaded
     */
    public void toTimeStep(int newTime_s) throws IOException {

        // CHECK

        if (newTime_s < startTime_s()) {
            Logger.getLogger(StreamReaderA.class).warn(
                    "new time " + newTime_s + " < start time " + startTime_s());
            newTime_s = startTime_s();
        }
        if (newTime_s > endTime_s()) {
        	Logger.getLogger(StreamReaderA.class).warn(
                    "new time " + newTime_s + " > end time " + endTime_s());
            newTime_s = endTime_s();
        }

        // CONTINUE

        final int oldBufferStartTime_s = bufferStartTime_s;
        final int bufferCounter = (newTime_s - startTime_s())
                / (bufferSize() * timeStepLength_s());
        bufferStartTime_s = startTime_s() + bufferCounter * bufferSize()
                * timeStepLength_s();
        bufferIndex = (newTime_s - bufferStartTime_s) / timeStepLength_s();

        if (bufferStartTime_s != oldBufferStartTime_s)
            loadBuffer();

        // (re)set state anyway since system might have been modified
        buffer[bufferIndex].setState();
    }

    /**
     * Loads the last state together with the according buffer segment.
     */
    public void toEnd() throws IOException {
        toTimeStep(endTime_s());
    }

    /**
     * Does nothing.
     */
    public void close() {
    }

}
