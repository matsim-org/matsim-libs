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

package playground.agarwalamit.opdyts.analysis;

import java.io.BufferedReader;
import java.io.IOException;
import org.matsim.core.utils.io.IOUtils;

/**
 * A class to extract the currentBestDevisionVariable and currentBestObjectiveFunction.
 *
 * Created by amit on 30.05.17.
 */


public class OpdytsLogReader {

    private static final String inputFile = "";
    private static final String outputFile = "";

    private enum OpdytsLogLabel {  }

    public void readFile(final String inputFile){

        try(BufferedReader reader = IOUtils.getBufferedReader(inputFile)) {

            String line = reader.readLine();

            boolean isHeaderLine = true;

            while(line!= null) {
                if (isHeaderLine) {
                    isHeaderLine = false;



                } else {

                }


                line = reader.readLine();
            }
        } catch (IOException e) {
            throw new RuntimeException("Data is not read/written. Reason "+e);
        }



    }



}





