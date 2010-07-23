/* *********************************************************************** *
 * project: org.matsim.*
 * DgPlaygroundJobfileCreator
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.dgrether;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.matsim.core.utils.io.IOUtils;



/**
 * @author dgrether
 *
 */
public class DgPlaygroundJobfileCreator {
	
	private final static String[] template = {
			"#!/bin/bash --login",
			"#$ -l h_rt=259200", // 72 h runtime
			"#$ -N matsim-0.2.0 ",
			"#$ -o 1055.log",
			"#$ -j y",
			"#$ -m be",
			"#$ -M grether@vsp.tu-berlin.de",
			"#$ -cwd",
			"#$ -l cluster7",
			"# -l mem_free=16G use for min 16 G memory on cluster",
			"date",
			"hostname",
			"echo \"default java version:\"",
			"java -version",
			"echo \"using alternative java\"",
			"module add java-1.6",
			"java -version",
			"cd /net/ils/dgrether/"
	};
	
	private static final String command = "java -Djava.awt.headless=true";
	
	private static final String memory = "-Xmx15000M";
	
	private static final String jarDir = "./matsim/playgroundDgretherDaganzo6/";
	
	private static final String classPath = "-cp " + jarDir + "dgrether-0.2.0-SNAPSHOT.jar:" + jarDir + "dgrether-0.2.0-SNAPSHOT-jar-with-dependencies.jar";
	
//	private static final String mainClass = "org.matsim.run.Controler";
	private static final String mainClass = "playground.dgrether.daganzosignal.DaganzoRunner";
	
	public static void createJobfile(String filename, String configPath, String runId) {
		BufferedWriter writer = null;
		try {
			writer = IOUtils.getBufferedWriter(filename);
			for (int i = 0; i < template.length; i++){
				if (i == 3){
					writer.write("#$ -o " + runId + ".log");
				}
				else {
					writer.write(template[i]);
				}
				writer.newLine();
			}
			//write command
			writer.newLine();
			writer.write(command);
			writer.write(" ");
			writer.write(memory);
			writer.write(" ");
			writer.write(classPath);
			writer.write(" ");
			writer.write(mainClass);
			writer.write(" ");
			writer.write(configPath);

			writer.close(); 

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
