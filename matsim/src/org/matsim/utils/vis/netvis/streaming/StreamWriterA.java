/* *********************************************************************** *
 * project: org.matsim.*
 * StreamWriterA.java
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

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import org.apache.log4j.Logger;
import org.matsim.interfaces.basic.v01.BasicNetwork;
import org.matsim.utils.vis.netvis.config.ConfigModuleI;
import org.matsim.utils.vis.netvis.config.GeneralConfig;
import org.matsim.utils.vis.netvis.config.IndexationConfig;
import org.matsim.utils.vis.netvis.config.TemporalConfig;

/**
 * An abstract superclass that provides basic functionality for writing of
 * network states in files, where one file corresponds to one buffer block.
 *
 * @author gunnar
 *
 */
public abstract class StreamWriterA implements SimStateWriterI {

    // -------------------- MEMBER VARIABLES --------------------

    private final GeneralConfig generalConfig;

    private final IndexationConfig indexConfig;

    private final StreamConfig streamConfig;

    private final int timeStepLength_s;

    private final int bufferSize;

    private final DumpTimer dumpTimer;

    private DataOutputStream outputStream;

    private GZIPOutputStream gzipStream;

    private StateI buffer;

    private int fileStartTime_s;

    private int bufferIndex;

    // -------------------- CONSTRUCTION --------------------

    /**
     * Complete constructor containing all information for use of this writer
     * without further configuration.
     */
    protected StreamWriterA(BasicNetwork network, String networkFileName,
            IndexationConfig indexConfig, String filePrefix, String fileSuffix,
            int timeStepLength_s, int bufferSize) {

        this.streamConfig = new StreamConfig(network, filePrefix, fileSuffix, false);
        this.generalConfig = new GeneralConfig(true, networkFileName);
        this.indexConfig = indexConfig;
        this.timeStepLength_s = timeStepLength_s;
        this.bufferSize = bufferSize;

        this.dumpTimer = new DumpTimer(timeStepLength_s);
    }

    // -------------------- INTERFACE DEFINITION --------------------

    /**
     * Which type of network state information is to be written.
     *
     */
    protected abstract StateI newState();

    // -------------------- GETTERS --------------------

    public BasicNetwork getNetwork() {
        return streamConfig.getNetwork();
    }

    public GeneralConfig getGeneralConfig() {
        return generalConfig;
    }

    public StreamConfig getStreamConfig() {
        return streamConfig;
    }

    public IndexationConfig getIndexConfig() {
        return indexConfig;
    }

    public int getTimeStepLength_s() {
        return timeStepLength_s;
    }

    // --------------- ADDITIONAL FUNCTIONALITY ---------------

    private final List<ConfigModuleI> additionalConfigs = new ArrayList<ConfigModuleI>();

    protected void addConfig(ConfigModuleI config) {
        additionalConfigs.add(config);
    }

    // --------------- IMPLEMENTATION OF StateFileWriterI ---------------

    /**
     * Does nothing.
     */
    public void open() {
    }

    // public boolean due(int time_s) {
    // return dumpTimer.due(time_s);
    // }

    /**
     * Writes the current state to an appropriate file.
     */
    public boolean dump(int time_s) throws IOException {

        if (!dumpTimer.due(time_s))
            return false;

        if (!dumpTimer.getOpen()) {
            fileStartTime_s = time_s;
            bufferIndex = 0;
            buffer = newState();
        }

        dumpTimer.notifyDump(time_s);

        if (bufferIndex == 0) {
            String newName = streamConfig.getStreamFileName(fileStartTime_s);
            Logger.getLogger(StreamWriterA.class).info("opening file: " + newName);
            outputStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(newName)));
        }

        buffer.getState();
        buffer.writeMyself(outputStream);

        if (bufferIndex == bufferSize - 1) {
            closeFile();
            bufferIndex = 0;
            fileStartTime_s += timeStepLength_s * bufferSize;
        } else
            bufferIndex++;

        return true;
    }

    private void closeFile() throws IOException {
        if (outputStream != null) {
            System.out.println("closing file");
            outputStream.flush();
            if (gzipStream != null)
                gzipStream.finish(); // TODO prob. redundant
            outputStream.close();
        }
    }

    /**
     * Closes the underlying output stream and generates a configuration file.
     * Has to be called explicitly by the client after all data has been
     * written.
     */
    public void close() throws IOException {
        closeFile();

        if (dumpTimer.getOpen()) {
            TemporalConfig temporalConfig = new TemporalConfig(dumpTimer
                    .getFirstDumpTime_s(), dumpTimer.getLastDumpTime_s(),
                    bufferSize, timeStepLength_s);
            streamConfig.createFile(generalConfig, temporalConfig, indexConfig,
                    additionalConfigs, this.getClass());
        } else
        	System.out.println("dump() was never called - no files created");

        dumpTimer.close();
    }

}
