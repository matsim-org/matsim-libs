/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.pieter.events;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import org.matsim.core.utils.io.IOUtils;

public class EventsMergeSort {
	private final String outputEventsPath;
	private final String inputEventsPath;
	private ArrayList<String> inputFiles;
    private int counter = 0;

	public EventsMergeSort(String outputEventsPath, String inputEventsPath) {
		super();
		this.outputEventsPath = outputEventsPath;
		this.inputEventsPath = inputEventsPath;
		getInputFileList();
	}

	public void run() throws IOException {
        ArrayList<String> intermediaryFiles = new ArrayList<>();
		int numberOfFiles = inputFiles.size();
		if (numberOfFiles % 2 != 0){
			intermediaryFiles.add(inputFiles.get(numberOfFiles - 1));
			inputFiles.remove(inputFiles.get(numberOfFiles - 1));			
		}
		for (int i = 1; i < numberOfFiles; i += 2) {
			String fileName = null;
			if (numberOfFiles > 2) {
				fileName = "mergesortfile_" + counter++ + ".xml.gz";
				intermediaryFiles.add(fileName);
				fileName = inputEventsPath + "/" + fileName;
			} else
				fileName = outputEventsPath + "/OUT_events.xml.gz";
			System.out.printf("Merging files %s and %s into %s \n", inputFiles.get(i - 1), inputFiles.get(i), fileName);
			BufferedReader reader1 = IOUtils.getBufferedReader(inputEventsPath + "/" + inputFiles.get(i - 1));
			BufferedReader reader2 = IOUtils.getBufferedReader(inputEventsPath + "/" + inputFiles.get(i));
			BufferedWriter writer = IOUtils.getBufferedWriter(fileName);

			writer.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
			writer.write("<events version=\"1.0\">\n");
			reader1.readLine();
			reader1.readLine();
			reader2.readLine();
			reader2.readLine();
			boolean doneReading = false;
			String line1 = reader1.readLine();
			String line2 = reader2.readLine();
			while (!doneReading) {
				if (line1 == null || line1.startsWith("</")) {
					// fill it from the second reader
					boolean finishedReadingRest = false;
					while (!finishedReadingRest) {

						if (line2 != null) {
							writer.write(line2 + "\n");

						} else {

							finishedReadingRest = true;
							doneReading = true;
						}
						line2 = reader2.readLine();

					}
				} else if (line2 == null || line2.startsWith("</")) {
					// fill it from the second reader
					boolean finishedReadingRest = false;
					while (!finishedReadingRest) {

						if (line1 != null) {

							writer.write(line1 + "\n");
						} else {
							finishedReadingRest = true;
							doneReading = true;
						}
						line1 = reader1.readLine();
					}
				} else if (Double.parseDouble(line1.split("\"")[1]) >= Double.parseDouble(line2.split("\"")[1])) {
					writer.write(line2 + "\n");
					line2 = reader2.readLine();
				} else {
					writer.write(line1 + "\n");
					line1 = reader1.readLine();
				}
			}
			reader1.close();
			reader2.close();
			writer.close();
		}
		if (intermediaryFiles.size() > 1) {
			for(String fileName:inputFiles){
				if(fileName.startsWith("merge")){
					File file = new File(inputEventsPath + "/"+fileName); 
					file.delete();
				}
			}
			inputFiles = intermediaryFiles;
			run();
		}
	}

	void getInputFileList() {
		File f = new File(inputEventsPath);
		inputFiles = new ArrayList<>(Arrays.asList(f.list()));
		ArrayList<String> removeFiles = new ArrayList<>();
		for (String fileName : inputFiles) {
			if (!(fileName.endsWith("xml") || fileName.endsWith("xml.gz")) || fileName.startsWith("merge")|| fileName.startsWith("OUT")) {
				removeFiles.add(fileName);

			}
		}
		for (String file : removeFiles) {
			inputFiles.remove(file);
		}
	}

	public static void main(String[] args) throws IOException {
		EventsMergeSort eventsMergeSort = new EventsMergeSort(args[0], args[1]);
		eventsMergeSort.run();
	}
}
