/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.ikaddoura.incidents;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

/**
 * @author ikaddoura 
 * 
 * This class requests incident data from HERE Maps.
 *
 */
public class IncidentDataDownload extends TimerTask {
	private static final Logger log = Logger.getLogger(IncidentDataDownload.class);

	private static enum Area { germany, berlin } ;
	
//	private final Area area = Area.berlin;
//	private final String outputDirectory = "../../../shared-svn/studies/ihab/incidents/berlin/";

	private final Area area = Area.germany;
	private final String outputDirectory = "../../../shared-svn/studies/ihab/incidents/germany/";

	private final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss"); 
	private final boolean zip = true;

	public static void main(String[] args) throws XMLStreamException, IOException {
		IncidentDataDownload incidentDownload = new IncidentDataDownload();
		
//		run in certain time intervals
		Timer t = new Timer();
		t.scheduleAtFixedRate(incidentDownload, 0, 60 * 1000);		
		incidentDownload.run();
		
		// run it once
//		incidentDownload.run();
	}

	@Override
	public void run() {
		
		String urlString;
		if (area == Area.berlin) {
			urlString = "http://traffic.cit.api.here.com/traffic/6.0/incidents.xml?app_id=iMCM7KBVFey9uI5uNEi4"
					+ "&app_code=xxx"
					+ "&bbox=52.1571,12.5903;52.7928,13.9856" // Greater Berlin Area
					+ "&status=active";
		} else if (area == Area.germany) {
			urlString = "http://traffic.cit.api.here.com/traffic/6.0/incidents.xml?app_id=iMCM7KBVFey9uI5uNEi4"
					+ "&app_code=xxx"
					+ "&bbox=47.06,5.32;55.19,15.47" // Germany
					+ "&status=active";
		} else {
			throw new RuntimeException("Undefined area. Aborting...");
		}
		
		log.info("URL: " + urlString);

		URL url = null;
		try {
			url = new URL(urlString);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		String outputFile = "incidentData_" + this.area.toString() + "_" + System.currentTimeMillis() + "_" + formatter.format(new Date()) + ".xml";
		String outputPathAndFile = outputDirectory + outputFile;

		try {
			FileUtils.copyURLToFile(url, new File(outputPathAndFile));
		} catch (IOException e) {
			e.printStackTrace();
		}
		log.info("URL content copied to file " + outputPathAndFile);
		
		if (zip) {
			
			byte[] buffer = new byte[1024];
	    	
	    	try{
	    		
	    		log.info("Zipping the file " + outputPathAndFile + "...");

	    		FileOutputStream fos = new FileOutputStream(outputPathAndFile + ".zip");
	    		ZipOutputStream zos = new ZipOutputStream(fos);
	    		ZipEntry ze = new ZipEntry(outputFile);
	    		zos.putNextEntry(ze);
	    		FileInputStream in = new FileInputStream(outputPathAndFile);
	   	   
	    		int len;
	    		while ((len = in.read(buffer)) > 0) {
	    			zos.write(buffer, 0, len);
	    		}

	    		in.close();
	    		zos.closeEntry();	           
	    		zos.close();
	          
	    		log.info("Zipping the file " + outputPathAndFile + "... Done.");
	    		
	    		if (new File(outputPathAndFile).delete()) {
	    			log.info("Original xml file deleted.");
	    		}
	    		
	    	} catch(IOException ex){
	    	   ex.printStackTrace();
	    	}
		}		
	}

}
