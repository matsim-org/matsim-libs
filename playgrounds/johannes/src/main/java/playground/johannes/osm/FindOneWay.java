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

package playground.johannes.osm;

import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Stack;

/**
 * @author johannes
 */
public class FindOneWay extends MatsimXmlParser {

    private static final String WAY_NODE = "way";

    private static final String ID_ATTR = "id";

    private static final String TAG_NODE = "tag";

    private static final String KEY_ATTR = "k";

    private static final String VALUE_ATTR = "v";

    private static final String ONE_WAY_VALUE = "oneway";

    private String wayId;

    private String oneWayValue;

    private BufferedWriter writer;

    public static void main(String args[]) throws IOException {
        String osmFile = args[0];
        String outFile = args[1];

        FindOneWay fow = new FindOneWay();
        fow.writer = new BufferedWriter(new FileWriter(outFile));
        fow.writer.write("id;oneway");
        fow.writer.newLine();

        fow.setValidating(false);
        fow.readFile(osmFile);

        fow.writer.close();
    }

    @Override
    public void startTag(String name, Attributes atts, Stack<String> context) {
        if(WAY_NODE.equalsIgnoreCase(name)) {
            wayId = atts.getValue(ID_ATTR);
        } else if(TAG_NODE.equalsIgnoreCase(name)) {
            if(ONE_WAY_VALUE.equalsIgnoreCase(atts.getValue(KEY_ATTR))) {
                oneWayValue = atts.getValue(VALUE_ATTR);
            }
        }
    }

    @Override
    public void endTag(String name, String content, Stack<String> context) {
        if(WAY_NODE.equalsIgnoreCase(name)) {
            try {
                writer.write(wayId);
                writer.write(";");
                if(oneWayValue != null) writer.write(oneWayValue);
                writer.newLine();
//                writer.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }


            oneWayValue = null;
            wayId = null;
        }
    }
}
