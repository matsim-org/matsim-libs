/* *********************************************************************** *
 * project: org.matsim.*
 * DgXmlConverter
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.dgrether.utils;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.MatsimFileTypeGuesser;
import org.matsim.lanes.MatsimLaneDefinitionsWriter;
import org.matsim.lanes.basic.BasicLaneDefinitions;
import org.matsim.lanes.basic.BasicLaneDefinitionsImpl;
import org.matsim.signalsystems.MatsimSignalSystemConfigurationsReader;
import org.matsim.signalsystems.MatsimSignalSystemConfigurationsWriter;
import org.matsim.signalsystems.MatsimSignalSystemsReader;
import org.matsim.signalsystems.MatsimSignalSystemsWriter;
import org.matsim.signalsystems.SignalSystemConfigurationsWriter11;
import org.matsim.signalsystems.basic.BasicSignalSystems;
import org.matsim.signalsystems.basic.BasicSignalSystemsImpl;
import org.matsim.signalsystems.config.BasicSignalSystemConfigurations;
import org.matsim.signalsystems.config.BasicSignalSystemConfigurationsImpl;

import playground.dgrether.DgPaths;


/**
 * @author dgrether
 *
 */
public class DgXmlConverter {
	
	private static final Logger log = Logger.getLogger(DgXmlConverter.class);
	
//	private String baseDir = "test/input/org/matsim/signalsystems";
	private String baseDir = DgPaths.STUDIESDG + "lsaZurich/";
	
	
	public DgXmlConverter() throws IOException{
		File baseDirectory = new File(baseDir);
		if (!baseDirectory.isDirectory()){
			throw new IllegalArgumentException("not a directory");
		}
		File[] files = baseDirectory.listFiles();
		List<File> searchFiles = new LinkedList<File>();
		this.addAllArrayElements(files, searchFiles);
		File file;
		while (!searchFiles.isEmpty()){
			file = searchFiles.get(0);
//			log.debug("processing file: " + file.getAbsolutePath());
			if (file.isDirectory()){
				this.addAllArrayElements(file.listFiles(), searchFiles);
			}
			else{
				if (file.getName().endsWith(".xml") || file.getName().endsWith(".xml.gz")){
					log.info("found xml file: " + file.getAbsolutePath());
					log.info("file name: " + file.getName());
					log.info("parent name: " + file.getParentFile().getAbsolutePath());
					this.convertXml(file);
				}
			}
			searchFiles.remove(0);
		}
	}
	
	private void convertXml(File file) throws IOException {
		MatsimFileTypeGuesser guesser = new MatsimFileTypeGuesser(file.getAbsolutePath());
		String filename;
		if (MatsimSignalSystemsReader.SIGNALSYSTEMS10.compareTo(guesser.getSystemId()) == 0){
			BasicLaneDefinitions lanes = new BasicLaneDefinitionsImpl();
			BasicSignalSystems sigSys = new BasicSignalSystemsImpl();
			MatsimSignalSystemsReader reader = new MatsimSignalSystemsReader(lanes, sigSys);
			reader.readFile(file.getAbsolutePath());
			MatsimLaneDefinitionsWriter lanewriter = new MatsimLaneDefinitionsWriter(lanes);
			filename = file.getParentFile().getAbsolutePath() + "/testLaneDefinitions_v1.1.xml";
			lanewriter.writeFile(filename);
			log.info("new file: " + filename);
			MatsimSignalSystemsWriter writer = new MatsimSignalSystemsWriter(sigSys);
			filename = file.getParentFile().getAbsolutePath() + "/testSignalSystems_v1.1.xml";
			writer.writeFile(filename);
			log.info("new file: " + filename);
		}
		else if (MatsimSignalSystemConfigurationsReader.SIGNALSYSTEMSCONFIG10.compareTo(guesser.getSystemId()) == 0){
			BasicSignalSystemConfigurations configs = new BasicSignalSystemConfigurationsImpl();
			MatsimSignalSystemConfigurationsReader reader = new MatsimSignalSystemConfigurationsReader(configs);
			reader.readFile(file.getAbsolutePath());
			MatsimSignalSystemConfigurationsWriter writer = new MatsimSignalSystemConfigurationsWriter(new SignalSystemConfigurationsWriter11(configs));
			filename = file.getParentFile().getAbsolutePath() + "/testSignalSystemConfigurations_v1.1.xml";
			writer.writeFile(filename);
			log.info("new file: " + filename);
		}
	}

	private void addAllArrayElements(Object[] array, List list){
		for (int i = 0; i < array.length; i++){
			list.add(array[i]);
		}
	}
	
	
	
	public static void main(String[] args){
		try {
			new DgXmlConverter();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
