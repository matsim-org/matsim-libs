/* *********************************************************************** *
 * project: org.matsim.*
 * StreamConfig.java
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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.matsim.interfaces.basic.v01.BasicNetwork;
import org.matsim.utils.misc.Time;
import org.matsim.utils.vis.netvis.config.ConfigModuleI;
import org.matsim.utils.vis.netvis.config.GeneralConfig;
import org.matsim.utils.vis.netvis.config.IndexationConfig;
import org.matsim.utils.vis.netvis.config.TemporalConfig;

/**
 * Most of this is package private so the streaming cannot be disturbed by
 * changes in this class. Package private getters of this class are available
 * via StreamReaderA's and StreamWriterA's public getters.
 *
 * @author gunnar
 *
 */
public class StreamConfig {

    // -------------------- CLASS VARIABLES --------------------

    private static final String CONFIGFILE_IDENT = "CONFIG";

    private static final String NEWLINE = "\n";

    // -------------------- MEMBER VARIABLES --------------------

    private final BasicNetwork network;

    private final String filePrefix;

    private final String fileSuffix;

    // -------------------- CONSTRUCTION --------------------

    StreamConfig(BasicNetwork network, String filePrefix, String fileSuffix,
            boolean compress) {
        this.network = network;
        this.filePrefix = filePrefix;
        this.fileSuffix = fileSuffix;
    }

    // -------------------- (PACKAGE) PRIVATE GETTERS --------------------

    BasicNetwork getNetwork() {
        return network;
    }

    public String getFilePrefix() {
        return filePrefix;
    }

    public static String getConfigFileName(String filePrefix, String fileSuffix) {
        return filePrefix + CONFIGFILE_IDENT + "." + fileSuffix;
    }

    private String getConfigFileName() {
        return getConfigFileName(filePrefix, fileSuffix);
    }

    public String getStreamFileName(int fileStartTime_s) {
        return filePrefix + Time.writeTime(fileStartTime_s, '-') + "." + fileSuffix;
    }

    // -------------------- FILE READING --------------------

    protected TemporalConfig newTemporalConfig() {
        return new TemporalConfig(getConfigFileName());
    }

    protected IndexationConfig newIndexationConfig() {
        return new IndexationConfig(network, getConfigFileName());
    }

    // -------------------- FILE CREATION --------------------

    void createFile(GeneralConfig generalConfig, TemporalConfig temporalConfig,
            IndexationConfig indexConfig, List<ConfigModuleI> furtherConfigs,
            Class streamWriterClass) {

        System.out.println("Writing configuration file.");

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(
                    getConfigFileName()));

            writer.write("<?xml version='1.0' encoding='UTF-8'?>");
            writer.write(NEWLINE);
            writer.write(NEWLINE);

            writer.write("<config>");
            writer.write(NEWLINE);
            writer.write(NEWLINE);

            /*
             * STREAMING
             */
            writer.write("\t<!--");
            writer.write(NEWLINE);
            writer.write(NEWLINE);
            writer.write("\tAuto generated configuration file.");
            writer.write(NEWLINE);
            writer.write(NEWLINE);
            writer.write("\tUsed StreamWriter implementation: ");
            writer.write(NEWLINE);
            writer.write("\t  " + streamWriterClass.getName());
            writer.write(NEWLINE);
            writer.write(NEWLINE);
            writer.write("\t -->");
            writer.write(NEWLINE);
            writer.write(NEWLINE);

            /*
             * GENERAL
             */
            writer.write(generalConfig.asXmlSegment(4));
            writer.write(NEWLINE);

            /*
             * TEMPORAL
             */
            writer.write(temporalConfig.asXmlSegment(4));
            writer.write(NEWLINE);

            /*
             * FURTHER CONFIGS
             */
            if (furtherConfigs != null)
                for (ConfigModuleI module : furtherConfigs) {
                    writer.write(module.asXmlSegment(4));
                    writer.write(NEWLINE);
                }

            /*
             * INDEXATION
             */
            writer.write(indexConfig.asXmlSegment(4));
            writer.write(NEWLINE);

            writer.write("</config>");
            writer.write(NEWLINE);

            writer.flush();
            writer.close();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
