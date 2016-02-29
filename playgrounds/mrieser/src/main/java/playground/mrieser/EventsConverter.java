/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.mrieser;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.zip.DeflaterOutputStream;

import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.EventWriter;
import org.matsim.core.events.algorithms.EventWriterXML;

import net.jpountz.lz4.LZ4BlockInputStream;
import net.jpountz.lz4.LZ4BlockOutputStream;

/**
 * @author mrieser / Senozon AG
 */
public class EventsConverter {

	public static void main(String[] args) throws FileNotFoundException, IOException {
//		String input = "/data/senozon/gamma-mirror/runs/run0072/run_13/2014.25pct.13.output_events.xml.gz";
		String input = "/data/vis/michal/poznan-morning/40.events.xml.gz";
		
		String rawEvents = "events.xml";
		String gzipEvents = "events.xml.gz";
		String gzipEvents2 = "events2.xml.gz";
		String lz4Events1 = "events1.xml.lz4";
		String lz4Events2 = "events2.xml.lz4";
		String lz4gzipEvents = "events2.xml.lz4.gz";
		String lz4lz4Events = "events2.xml.lz4.lz4";
		String lz4DeflEvents = "events2.xml.lz4.defl";
		String deflEvents = "events2.xml.defl";

		long timeGz2Raw;
		{
			EventsManager events = new EventsManagerImpl();
			long start = System.currentTimeMillis();
			OutputStream output = new BufferedOutputStream(new FileOutputStream(rawEvents)); 
			EventWriter writer = new EventWriterXML(new PrintStream(output));
			events.addHandler(writer);
			new MatsimEventsReader(events).readFile(input);
			writer.closeFile();
			long end = System.currentTimeMillis();
			timeGz2Raw = end - start;
		}

		long timeGz2Gz;
		{
			EventsManager events = new EventsManagerImpl();
			long start = System.currentTimeMillis();
			OutputStream output = new GzipCompressorOutputStream(new BufferedOutputStream(new FileOutputStream(gzipEvents))); 
			EventWriter writer = new EventWriterXML(new PrintStream(output));
			events.addHandler(writer);
			new MatsimEventsReader(events).readFile(input);
			writer.closeFile();
			long end = System.currentTimeMillis();
			timeGz2Gz = end - start;
		}

		long timeGz2Lz4;
		{
			EventsManager events = new EventsManagerImpl();
			long start = System.currentTimeMillis();
			OutputStream output = new LZ4BlockOutputStream(new BufferedOutputStream(new FileOutputStream(lz4Events1)), 1024*1024); 
			EventWriter writer = new EventWriterXML(new PrintStream(output));
			events.addHandler(writer);
			new MatsimEventsReader(events).readFile(input);
			writer.closeFile();
			long end = System.currentTimeMillis();
			timeGz2Lz4= end - start;
		}
		
		long timeLz42Lz4;
		{
			EventsManager events = new EventsManagerImpl();
			long start = System.currentTimeMillis();
			OutputStream output = new LZ4BlockOutputStream(new BufferedOutputStream(new FileOutputStream(lz4Events2)), 1024*1024); 
			EventWriter writer = new EventWriterXML(new PrintStream(output));
			events.addHandler(writer);
			InputStream inStream = new LZ4BlockInputStream(new BufferedInputStream(new FileInputStream(lz4Events1)));
			new MatsimEventsReader(events).readStream(inStream);
			writer.closeFile();
			long end = System.currentTimeMillis();
			timeLz42Lz4= end - start;
		}
		
		long timeLz42Gz;
		{
			EventsManager events = new EventsManagerImpl();
			long start = System.currentTimeMillis();
			OutputStream output = new GzipCompressorOutputStream(new BufferedOutputStream(new FileOutputStream(lz4gzipEvents))); 
			EventWriter writer = new EventWriterXML(new PrintStream(output));
			events.addHandler(writer);
			InputStream inStream = new LZ4BlockInputStream(new BufferedInputStream(new FileInputStream(lz4Events1)));
			new MatsimEventsReader(events).readStream(inStream);
			writer.closeFile();
			long end = System.currentTimeMillis();
			timeLz42Gz= end - start;
		}
		
		long timeLz42Lz4Gz;
		{
			EventsManager events = new EventsManagerImpl();
			long start = System.currentTimeMillis();
			OutputStream output = new GzipCompressorOutputStream(new LZ4BlockOutputStream(new BufferedOutputStream(new FileOutputStream(gzipEvents2)), 1024*1024)); 
			EventWriter writer = new EventWriterXML(new PrintStream(output));
			events.addHandler(writer);
			InputStream inStream = new LZ4BlockInputStream(new BufferedInputStream(new FileInputStream(lz4Events1)));
			new MatsimEventsReader(events).readStream(inStream);
			writer.closeFile();
			long end = System.currentTimeMillis();
			timeLz42Lz4Gz= end - start;
		}
		
		long timeLz42Defl;
		{
			EventsManager events = new EventsManagerImpl();
			long start = System.currentTimeMillis();
			OutputStream output = new DeflaterOutputStream(new BufferedOutputStream(new FileOutputStream(deflEvents))); 
			EventWriter writer = new EventWriterXML(new PrintStream(output));
			events.addHandler(writer);
			InputStream inStream = new LZ4BlockInputStream(new BufferedInputStream(new FileInputStream(lz4Events1)));
			new MatsimEventsReader(events).readStream(inStream);
			writer.closeFile();
			long end = System.currentTimeMillis();
			timeLz42Defl = end - start;
		}

		long timeLz42Lz4Defl;
		{
			EventsManager events = new EventsManagerImpl();
			long start = System.currentTimeMillis();
			OutputStream output = new DeflaterOutputStream(new LZ4BlockOutputStream(new BufferedOutputStream(new FileOutputStream(lz4DeflEvents)), 1024*1024)); 
			EventWriter writer = new EventWriterXML(new PrintStream(output));
			events.addHandler(writer);
			InputStream inStream = new LZ4BlockInputStream(new BufferedInputStream(new FileInputStream(lz4Events1)));
			new MatsimEventsReader(events).readStream(inStream);
			writer.closeFile();
			long end = System.currentTimeMillis();
			timeLz42Lz4Defl = end - start;
		}

		long timeLz42Lz4Lz4;
		{
			EventsManager events = new EventsManagerImpl();
			long start = System.currentTimeMillis();
			OutputStream output = new LZ4BlockOutputStream(new LZ4BlockOutputStream(new BufferedOutputStream(new FileOutputStream(lz4lz4Events)), 1024*1024), 1024 * 1024); 
			EventWriter writer = new EventWriterXML(new PrintStream(output));
			events.addHandler(writer);
			InputStream inStream = new LZ4BlockInputStream(new BufferedInputStream(new FileInputStream(lz4Events1)));
			new MatsimEventsReader(events).readStream(inStream);
			writer.closeFile();
			long end = System.currentTimeMillis();
			timeLz42Lz4Lz4= end - start;
		}
		
		System.out.println("GZIP -> RAW  = " + timeGz2Raw / 1000 + "s   filesize = " + new File(rawEvents).length() / 1000 / 1000.0);
		System.out.println("GZIP -> GZIP = " + timeGz2Gz / 1000 + "s   filesize = " + new File(gzipEvents).length() / 1000 / 1000.0);
		System.out.println("GZIP -> LZ4  = " + timeGz2Lz4 / 1000 + "s   filesize = " + new File(lz4Events1).length() / 1000 / 1000.0);
		System.out.println("LZ4  -> LZ4  = " + timeLz42Lz4 / 1000 + "s   filesize = " + new File(lz4Events2).length() / 1000 / 1000.0);
		System.out.println("LZ4  -> GZIP  = " + timeLz42Gz / 1000 + "s   filesize = " + new File(gzipEvents2).length() / 1000 / 1000.0);
		System.out.println("LZ4  -> LZ4GZIP  = " + timeLz42Lz4Gz / 1000 + "s   filesize = " + new File(lz4gzipEvents).length() / 1000 / 1000.0);
		System.out.println("LZ4  -> LZ4LZ4  = " + timeLz42Lz4Lz4 / 1000 + "s   filesize = " + new File(lz4lz4Events).length() / 1000 / 1000.0);
		System.out.println("LZ4  -> DEFLATE = " + timeLz42Defl / 1000 + "s   filesize = " + new File(deflEvents).length() / 1000 / 1000.0);
		System.out.println("LZ4  -> LZ4DEFLATE = " + timeLz42Lz4Defl / 1000 + "s   filesize = " + new File(lz4DeflEvents).length() / 1000 / 1000.0);
	}
}
