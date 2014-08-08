/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * IterationSummaryFileControlerListener.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package playground.mzilske.util;

import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;
import playground.mzilske.ant2014.StreamingOutput;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class IterationSummaryFileControlerListener implements StartupListener, IterationEndsListener, ShutdownListener {

    public static interface Writer {
        public StreamingOutput notifyStartup(StartupEvent event);
        public StreamingOutput notifyIterationEnds(IterationEndsEvent event);
    }

    private OutputDirectoryHierarchy controlerIO;
    private final Map<String, Writer> writers;
    private Map<String, PrintWriter> printWriters;

    public IterationSummaryFileControlerListener(OutputDirectoryHierarchy controlerIO, Map<String, Writer> writers) {
        this.controlerIO = controlerIO;
        this.writers = writers;
    }

    @Override
    public void notifyStartup(StartupEvent event) {
        this.printWriters = new HashMap<String, PrintWriter>();
        for (Map.Entry<String, Writer> entry : writers.entrySet()) {
            PrintWriter printWriter = new PrintWriter(IOUtils.getBufferedWriter(controlerIO.getOutputFilename(entry.getKey())));
            printWriters.put(entry.getKey(), printWriter);
            try {
                entry.getValue().notifyStartup(event).write(printWriter);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        for (Map.Entry<String, Writer> entry : writers.entrySet()) {
            try {
                PrintWriter pw = printWriters.get(entry.getKey());
                entry.getValue().notifyIterationEnds(event).write(pw);
                pw.flush();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    @Override
    public void notifyShutdown(ShutdownEvent event) {
        for (Map.Entry<String, PrintWriter> entry : printWriters.entrySet()) {
            entry.getValue().close();
        }
    }

}
