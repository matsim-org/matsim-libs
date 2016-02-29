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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.zip.GZIPOutputStream;

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

	private static Area area;
	private static String outputDirectory;
	private static long timeIntervalSec;

	private final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss"); 
	private final boolean downloadZipFile = true;
	private final boolean downloadXMLFile = false;

	public static void main(String[] args) throws XMLStreamException, IOException {
		IncidentDataDownload incidentDownload = new IncidentDataDownload();
		
		if (args.length > 0) {
			
			outputDirectory = args[0];		
			log.info("Output directory: "+ outputDirectory);
			
			String areaString = args[1];
			if (areaString.equalsIgnoreCase(Area.germany.toString())) {
				area = Area.germany;
			} else if (areaString.equalsIgnoreCase(Area.berlin.toString())) {
				area = Area.berlin;
			} else {
				throw new RuntimeException("Unknown area. Aborting...");
			}
			log.info("Area: " + area);
			
			timeIntervalSec = Long.valueOf(args[2]);
			log.info("Time interval: " + timeIntervalSec);
			
		} else {
			
//			outputDirectory = "../../../shared-svn/studies/ihab/incidents/germany-test/";
//			area = Area.germany;
			
			outputDirectory = "../../../shared-svn/studies/ihab/incidents/berlinXXX/";
			area = Area.berlin;

			timeIntervalSec = 0;

		}
				
		if (timeIntervalSec > 0) {
			// run in certain time intervals
			Timer t = new Timer();
			t.scheduleAtFixedRate(incidentDownload, 0, timeIntervalSec * 1000);	
			
		} else {
			// run it once
			incidentDownload.run();
		}
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
		
		new File(outputDirectory).mkdirs();
		
		log.info("URL: " + urlString);

		URL url = null;
		try {
			url = new URL(urlString);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
				
		String outputFile = "incidentData_" + area.toString() + "_" + System.currentTimeMillis() + "_" + formatter.format(new Date()) + ".xml";
		String outputPathAndFile = outputDirectory + outputFile;	
		
		if (downloadXMLFile) {
			try {
				FileUtils.copyURLToFile(url, new File(outputPathAndFile));
				
			} catch (IOException e) {
				e.printStackTrace();
			}
			log.info("URL content copied to file " + outputPathAndFile);
		}
		
		if (downloadZipFile) {
			
			try {
				URLConnection conn = url.openConnection();
				InputStream in = conn.getInputStream();
				
				GZIPOutputStream zipOut = new GZIPOutputStream(new FileOutputStream(outputPathAndFile + ".gz"));
				
				byte[] b = new byte[1024];
		        int count;
		        while ((count = in.read(b)) >= 0) {
		            zipOut.write(b, 0, count);
		        }
		        in.close();

		        zipOut.finish();
		        zipOut.close();
	    	
	    	} catch(IOException ex){
	    	   ex.printStackTrace();
	    	}
			
			log.info("URL content written to zip file " + outputPathAndFile + ".gz");

		}
	}
}
