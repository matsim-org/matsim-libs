/* *********************************************************************** *
 * project: org.matsim.*
 * CountsToCSV.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.telaviv.counts;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsReaderMatsimV1;
import org.matsim.counts.Volume;

import playground.telaviv.config.TelAvivConfig;

public class CountsToCSV {

	private final String separator = ",";
	private final Charset charset = Charset.forName("UTF-8");
	
	public static void main(String[] args) throws IOException {
		new CountsToCSV(TelAvivConfig.basePath + "/counts/counts_updated.xml", TelAvivConfig.basePath + "/counts/counts_updated.csv");
		new CountsToCSV(TelAvivConfig.basePath + "/counts/counts_tlv_model_counted.xml", TelAvivConfig.basePath + "/counts/counts_tlv_model_counted.csv");
	}
	
	public CountsToCSV(String countsInFile, String countsOutFile) throws IOException {
		Counts <Link>counts = new Counts();
		new CountsReaderMatsimV1(counts).parse(countsInFile);
		
		BufferedWriter writer = IOUtils.getBufferedWriter(countsOutFile, charset);
		writeHeader(writer);
		for (Count count : counts.getCounts().values()) {
			writeRow(writer, count);
		}
		writer.flush();
		writer.close();
	}
	
	private void writeHeader(BufferedWriter writer) throws IOException {
		writer.write("loc_id");
		writer.write(separator);
		writer.write("cs_id");
		for (int i = 1; i < 25; i++) {
			writer.write(separator);
			writer.write("hour_" + i);
		}
		writer.newLine();
	}
	
	private void writeRow(BufferedWriter writer, Count count) throws IOException {
		writer.write(count.getLocId().toString());
		writer.write(separator);
		writer.write(count.getCsId());
		for (int i = 1; i < 25; i++) {
			writer.write(separator);
			Volume volume = count.getVolume(i);
			if (volume == null) writer.write("-1.0");
			else writer.write(String.valueOf(volume.getValue()));
		}
		writer.newLine();
	}
}
