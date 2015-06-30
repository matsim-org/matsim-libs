/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * DownloadExampleData.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2015 by the members listed in the COPYING, *
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

package roadclassification;

import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.IOUtils;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;

/**
 * @author michaz
 * 
 * @deprecated Don't assume that an osm network is used.
 */
public class DownloadExampleData {

    public static final String SIOUX_FALLS = "output/sioux-falls.osm";

    public static final CoordinateTransformation COORDINATE_TRANSFORMATION = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, "EPSG:3359");

    public static void main(String[] args) throws IOException {
        new DownloadExampleData().run();
    }

    private void run() throws IOException {
        InputStream query = getClass().getResourceAsStream("query-sioux-falls.xml");
        URL osm = new URL("http://www.overpass-api.de/api/interpreter");
        URLConnection connection = osm.openConnection();
        connection.setDoInput(true);
        connection.setDoOutput(true);

        DataOutputStream printout = new DataOutputStream(connection.getOutputStream());
        IOUtils.copyStream(query, printout);
        printout.close();

        new File("output").mkdirs();
        InputStream inputStream = connection.getInputStream();
        FileOutputStream toStream = new FileOutputStream(SIOUX_FALLS);
        IOUtils.copyStream(inputStream, toStream);
        inputStream.close();
        toStream.close();
    }

}
