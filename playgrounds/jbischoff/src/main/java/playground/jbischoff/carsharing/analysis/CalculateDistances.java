/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.jbischoff.carsharing.analysis;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;

/**
 * @author  jbischoff
 *
 */
public class CalculateDistances {

	public static void main(String[] args) throws IOException {
		String file = "C:/Users/Joschka/Documents/shared-svn/documents/sli/2016/joschka/JRC_Nectar_Meeting_Sevilla_bigdata/data/rideslocs.csv";
		String outfile = "C:/Users/Joschka/Documents/shared-svn/documents/sli/2016/joschka/JRC_Nectar_Meeting_Sevilla_bigdata/data/distances.txt";
		String outfiled = "C:/Users/Joschka/Documents/shared-svn/documents/sli/2016/joschka/JRC_Nectar_Meeting_Sevilla_bigdata/data/distancedistribution.txt";
		TabularFileParserConfig config = new TabularFileParserConfig();
        config.setDelimiterTags(new String[] {";"});
        config.setFileName(file);
        config.setCommentTags(new String[] { "#" });
        final List<Double> distances = new ArrayList<>();
        final CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.WGS84_UTM33N);
        final int[] distrib = new int[21];
        final List<Integer> ii   = new ArrayList<>();
        new TabularFileParser().parse(config, new TabularFileHandler() {
			
			@Override
			public void startRow(String[] row) {
				try{
				Coord from = ct.transform( new Coord(Double.parseDouble(row[0]),Double.parseDouble(row[1])));
				Coord to= ct.transform( new Coord(Double.parseDouble(row[2]),Double.parseDouble(row[3])));
				double dist = CoordUtils.calcEuclideanDistance(from, to);
				distances.add(dist);
				int km = (int) (dist/1000);
//				System.out.println(dist +"\t"+km);
				if (km<20){
					distrib[km]++;
				}
				else {distrib[21]++;
					}
				}
				catch (Exception e) {System.out.println("oob");
				ii.add(0);

					}
				}
		});
		BufferedWriter bw = IOUtils.getBufferedWriter(outfile);
        for (double d : distances){
        	bw.write(Double.toString(d));
        	bw.newLine();
        }
        bw.flush();
        bw.close();
        BufferedWriter bw1 = IOUtils.getBufferedWriter(outfiled);
        for (int i = 0; i<distrib.length;i++){
        	bw1.write(i +"\t"+distrib[i]);
        	bw1.newLine();
        }
        bw1.flush();
        bw1.close();
        System.out.println(ii.size());
	}

}
