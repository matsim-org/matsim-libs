/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package playground.agarwalamit.analysis;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;

/**
 * Created by amit on 11.06.17.
 */


public class FileMerger {

    private static final Logger LOG = Logger.getLogger(FileMerger.class);

    public FileMerger(final String headerFileInitializer) {
        LOG.info("Header file will be written only once at start.");
        this.headerFileInitializer = headerFileInitializer;
    }

    public FileMerger(){
        LOG.warn("Assuming there is no header file. Merging everthing in the input file.");
    }

    private boolean isHeaderAdded = false;
    private BufferedWriter writer;
    private String headerFileInitializer ;

    public void mergeTo(final String outFile) {
        writer = IOUtils.getBufferedWriter(outFile);
    }

    public void finish () {
        try {
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException("Data is not written/read. Reason : " + e);
        }
    }

    public void readAndMerge(final String inputFile){
        readAndMerge (IOUtils.getBufferedReader(inputFile));
    }

    public void readAndMerge (final BufferedReader reader) {
        try {
            String line = reader.readLine();
            while (line!= null) {
                if (isHeader(line)) {
                    if (! isHeaderAdded ) {
                        writer.write(line);
                        writer.newLine();
                        isHeaderAdded = true;
                    }
                } else {
                    writer.write(line);
                    writer.newLine();
                }
                line = reader.readLine();
            }
        } catch (IOException e) {
            throw new RuntimeException("Data is not written/read. Reason : " + e);
        }
    }

    private boolean isHeader(final String line) {
        if (this.headerFileInitializer == null) return false;
        else return line.startsWith(this.headerFileInitializer);
    }
}
