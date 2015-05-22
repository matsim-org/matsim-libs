/* *********************************************************************** *
 * project: org.matsim.*
 * LinkVolumesWriter.java
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

package playground.christoph.evacuation.analysis;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.PolylineFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.core.utils.io.IOUtils;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.christoph.analysis.PassengerVolumesAnalyzer;

import com.vividsolutions.jts.geom.Coordinate;

public class LinkVolumesWriter implements IterationEndsListener {

	private final static Logger log = Logger.getLogger(LinkVolumesWriter.class);
	
	public static final String linkVolumesAbsoluteFile = "linkVolumesAbsolute"; //.txt.gz";
	public static final String linkVolumesRelativeFile = "linkVolumesRelative"; //.txt.gz";
	public static final String linkVolumesAbsoluteSHPFile = "linkVolumesAbsolute"; //.shp";
	public static final String linkVolumesRelativeSHPFile = "linkVolumesRelative"; //.shp";
	
	public static final String newLine = "\n";
	public static final String delimiter = "\t";
	
	private final VolumesAnalyzer volumesAnalyzer;
	private final Network network;
	private final int maxTime;
	private final double scaleFactor;
	private final boolean ignoreExitLinks;
	
	private String crsString = "EPSG:21781";

	public static void main(String[] args) {
		
//		String runId = "evac.1";
//		String crsString = "EPSG:21781";
//		String networkFile = "../../matsim/mysimulations/census2000V2/output_10pct_evac/evac.1.output_network.xml.gz";
//		String outputPath = "../../matsim/mysimulations/census2000V2/output_10pct_evac/";

		if (args.length != 5) return;
		String runId = args[0];
		String crsString = args[1];
		String scaleFactorString = args[2];
		String networkFile = args[3];
		String outputPath = args[4];
				
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(networkFile);
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		/*
		 * Create two OutputDirectoryHierarchies that point to the analyzed run's output directory.
		 * Since we do not want to overwrite existing results we add an additional prefix
		 * to the re-created outputs.
		 */
		OutputDirectoryHierarchy dummyInputDirectoryHierarchy;
		if (outputPath == null) outputPath = scenario.getConfig().controler().getOutputDirectory();
		if (outputPath.endsWith("/")) {
			outputPath = outputPath.substring(0, outputPath.length() - 1);
		}
		dummyInputDirectoryHierarchy = new OutputDirectoryHierarchy(
				outputPath,
				runId,
						true ? OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles : OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
		
		// add another string to the runId to not overwrite old files
		OutputDirectoryHierarchy dummyOutputDirectoryHierarchy;
		if (outputPath == null) outputPath = scenario.getConfig().controler().getOutputDirectory();
		if (outputPath.endsWith("/")) {
			outputPath = outputPath.substring(0, outputPath.length() - 1);
		}
		dummyOutputDirectoryHierarchy = new OutputDirectoryHierarchy(
				outputPath,
				runId + ".postprocessed",
						true ? OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles : OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
		
		EventsManager eventsManager = EventsUtils.createEventsManager();
		int timeSlice = 900;
		int maxTime = 36 * 3600;
		VolumesAnalyzer volumesAnalyzer = new PassengerVolumesAnalyzer(timeSlice, maxTime, scenario.getNetwork());
		eventsManager.addHandler(volumesAnalyzer);
				
		String eventsFile = dummyInputDirectoryHierarchy.getIterationFilename(0, Controler.FILENAME_EVENTS_XML);
		new EventsReaderXMLv1(eventsManager).parse(eventsFile);
		
		/*
		 * Write results to files.
		 */
		log.info("Writing results to files...");
		String absoluteVolumesFileName = dummyOutputDirectoryHierarchy.getIterationFilename(0, linkVolumesAbsoluteFile);
		String relativeVolumesFileName = dummyOutputDirectoryHierarchy.getIterationFilename(0, linkVolumesRelativeFile);
		String absoluteVolumesSHPFileName = dummyOutputDirectoryHierarchy.getIterationFilename(0, linkVolumesAbsoluteSHPFile);
		String relativeVolumesSHPFileName = dummyOutputDirectoryHierarchy.getIterationFilename(0, linkVolumesRelativeSHPFile);
		
		LinkVolumesWriter linkVolumesWriter = new LinkVolumesWriter(volumesAnalyzer, scenario.getNetwork(), timeSlice, maxTime, 
				Double.parseDouble(scaleFactorString), true);
		linkVolumesWriter.writeAbsoluteVolumes(absoluteVolumesFileName);
		linkVolumesWriter.writeRelativeVolumes(relativeVolumesFileName);
		linkVolumesWriter.writeAbsoluteSHPVolumes(absoluteVolumesSHPFileName, MGC.getCRS(crsString));
		linkVolumesWriter.writeRelativeSHPVolumes(relativeVolumesSHPFileName, MGC.getCRS(crsString));
		log.info("done.");
	}
	
	public LinkVolumesWriter(VolumesAnalyzer volumesAnalyzer, Network network, int timeslice, int maxTime, double scaleFactor, boolean ignoreExitLinks) {
		this.volumesAnalyzer = volumesAnalyzer;
		this.network = network;
		this.maxTime = maxTime;
		this.scaleFactor = scaleFactor;
		this.ignoreExitLinks = ignoreExitLinks;
	}
	
	public void writeAbsoluteVolumes(final String file) {
		
		try {
			Set<String> modes = this.volumesAnalyzer.getModes();
			Map<String, BufferedWriter> writers = new HashMap<String, BufferedWriter>();
			
			for (String mode : modes) {
				BufferedWriter writer = IOUtils.getBufferedWriter(file + "_" + mode + ".txt.gz");
				writers.put(mode, writer);
				writeHeader(writer);
			}
			
			writeRows(writers, true);
			
			for (BufferedWriter writer : writers.values()) {
				writer.flush();
				writer.close();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void writeRelativeVolumes(final String file) {
		
		try {
			Set<String> modes = this.volumesAnalyzer.getModes();
			Map<String, BufferedWriter> writers = new HashMap<String, BufferedWriter>();
			
			// relative volumes only make sense for car mode since we do not know the capacities for other modes
//			for (String mode : modes) {
//				BufferedWriter writer = IOUtils.getBufferedWriter(file + "_" + mode + ".txt.gz");
//				writers.put(mode, writer);
//				writeHeader(writer);
//			}
			if (modes.contains(TransportMode.car)) {
				String mode = TransportMode.car;
				BufferedWriter writer = IOUtils.getBufferedWriter(file + "_" + mode + ".txt.gz");
				writers.put(mode, writer);
				writeHeader(writer);
			}
			
			writeRows(writers, false);
			
			for (BufferedWriter writer : writers.values()) {
				writer.flush();
				writer.close();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private void writeHeader(BufferedWriter writer) throws IOException {
		writer.write("linkId");
		writer.write(delimiter);
		writer.write("capacity");
		writer.write(delimiter);
		writer.write("length");
		writer.write(delimiter);
		writer.write("freespeed");
		
		int hours = (int) (this.maxTime / 3600.0);
		for (int i = 0; i < hours; i++) {		
			writer.write(delimiter);
			writer.write(String.valueOf(i + 1));
//			writer.write(String.valueOf(i * this.timeSlice));
		}		
		writer.write(newLine);
	}
	
	private void writeRows(Map<String, BufferedWriter> writers, boolean absolute) throws IOException {
		
		for (Link link : this.network.getLinks().values()) {
			
			if (ignoreExitLinks) {
				String string = link.getId().toString().toLowerCase();
				if (string.contains("rescue") || string.contains("exit")) continue;
			}
			
			for (String mode : writers.keySet()) {
				BufferedWriter writer = writers.get(mode);
				writer.write(link.getId().toString());
				writer.write(delimiter);
				writer.write(String.valueOf(link.getCapacity()));
				writer.write(delimiter);
				writer.write(String.valueOf(link.getLength()));
				writer.write(delimiter);
				writer.write(String.valueOf(link.getFreespeed()));
				
				int hours = (int) (this.maxTime / 3600.0);
				double[] modeVolumes = this.volumesAnalyzer.getVolumesPerHourForLink(link.getId(), mode);
				
				for (int i = 0; i < hours; i++) {
					writer.write(delimiter);
					double volume = 0.0;
					if (modeVolumes != null) volume = modeVolumes[i];
					volume *= this.scaleFactor;
					if (absolute) {
						writer.write(String.valueOf(volume));
					} else {
						double capacity = link.getCapacity(i * 3600.0);
						double relativeVolume = volume / capacity;
						writer.write(String.valueOf(relativeVolume));
						if (relativeVolume > 1.0) {
							log.warn("Link's capacity limits seems to be exceeded: link " + link.getId() + 
									", capacity " + link.getCapacity() + ", measured volume " + volume);
						}
					}
				}		
				writer.write(newLine);
			}
		}
	}
		
	public void writeAbsoluteSHPVolumes(String file, CoordinateReferenceSystem crs) {
		try {
			Map<String, Collection<SimpleFeature>> fts = generateSHPFileData(crs, this.network, true);
			Set<String> modes = fts.keySet();
			for (String mode : modes) {
				Collection<SimpleFeature> ft = fts.get(mode);
				ShapeFileWriter.writeGeometries(ft, file + "_" + mode + ".shp");
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void writeRelativeSHPVolumes(String file, CoordinateReferenceSystem crs) {
		try {
			Map<String, Collection<SimpleFeature>> fts = generateSHPFileData(crs, this.network, false);
			Set<String> modes = fts.keySet();

			// relative volumes only make sense for car mode since we do not know the capacities for other modes
//			for (String mode : modes) {			
//				Collection<SimpleFeature> ft = fts.get(mode);
//				ShapeFileWriter.writeGeometries(ft, file + "_" + mode + ".shp");
//			}
			
			if (modes.contains(TransportMode.car)) {
				String mode = TransportMode.car;
				Collection<SimpleFeature> ft = fts.get(mode);
				ShapeFileWriter.writeGeometries(ft, file + "_" + mode + ".shp");
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private Map<String, Collection<SimpleFeature>> generateSHPFileData(CoordinateReferenceSystem crs, Network network, boolean absolute) throws Exception {

		Map<String, Collection<SimpleFeature>> map = new TreeMap<String, Collection<SimpleFeature>>();
		for (String mode : this.volumesAnalyzer.getModes()) map.put(mode, new ArrayList<SimpleFeature>());
		
		PolylineFeatureFactory.Builder builder = new PolylineFeatureFactory.Builder()
			.setCrs(crs)
			.setName("links")
			.addAttribute("ID", String.class)
			.addAttribute("capacity", Double.class)
			.addAttribute("length", Double.class)
			.addAttribute("freespeed", Double.class);
		
		int hours = (int) (this.maxTime / 3600.0);
		for (int i = 0; i < hours; i++) {
			builder.addAttribute(String.valueOf(i + 1), Double.class);
		}

		PolylineFeatureFactory factory = builder.create();
		for (Link link : network.getLinks().values()) {
			
			if (ignoreExitLinks) {
				String string = link.getId().toString().toLowerCase();
				if (string.contains("rescue") || string.contains("exit")) continue;
			}
			
			for (String mode : map.keySet()) {
				
				Collection<SimpleFeature> features = map.get(mode);
				
				Coordinate[] coordArray = new Coordinate[] {coord2Coordinate(link.getFromNode().getCoord()), coord2Coordinate(link.getCoord()), coord2Coordinate(link.getToNode().getCoord())};
				
				Object[] attributes = new Object[4 + hours];
				attributes[0] = link.getId().toString();
				attributes[1] = link.getCapacity();
				attributes[2] = link.getLength();
				attributes[3] = link.getFreespeed();
				
				double[] modeVolumes = this.volumesAnalyzer.getVolumesPerHourForLink(link.getId(), mode);

				for (int i = 0; i < hours; i++) {
					
					double volume = 0.0;
					if (modeVolumes != null) volume = modeVolumes[i];
					volume *= this.scaleFactor;
					if (absolute) {
						attributes[4 + i] = volume;
					} else {
						double capacity = link.getCapacity(i * 3600.0);
						double relativeVolume = volume / capacity;
						attributes[4 + i] = relativeVolume;
					}
				}	
				SimpleFeature ft = factory.createPolyline(coordArray, attributes, link.getId().toString());
				features.add(ft);	
			}
		}
				
		return map;
	}
	
	/**
	 * Converts a MATSim {@link org.matsim.api.core.v01.Coord} into a Geotools <code>Coordinate</code>
	 * @param coord MATSim coordinate
	 * @return Geotools coordinate
	 */
	private Coordinate coord2Coordinate(final Coord coord) {
		return new Coordinate(coord.getX(), coord.getY());
	}
	
	public String getCrsString() {
		return crsString;
	}

	public void setCrsString(String crsString) {
		this.crsString = crsString;
	}
	
	/*
	 * Write results to files.
	 */
	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		OutputDirectoryHierarchy outputDirectoryHierarchy = event.getControler().getControlerIO();
		
		log.info("Writing results to files...");
		String absoluteVolumesFileName = outputDirectoryHierarchy.getIterationFilename(0, linkVolumesAbsoluteFile);
		String relativeVolumesFileName = outputDirectoryHierarchy.getIterationFilename(0, linkVolumesRelativeFile);
		String absoluteVolumesSHPFileName = outputDirectoryHierarchy.getIterationFilename(0, linkVolumesAbsoluteSHPFile);
		String relativeVolumesSHPFileName = outputDirectoryHierarchy.getIterationFilename(0, linkVolumesRelativeSHPFile);
		
		this.writeAbsoluteVolumes(absoluteVolumesFileName);
		this.writeRelativeVolumes(relativeVolumesFileName);
		this.writeAbsoluteSHPVolumes(absoluteVolumesSHPFileName, MGC.getCRS(crsString));
		this.writeRelativeSHPVolumes(relativeVolumesSHPFileName, MGC.getCRS(crsString));
		log.info("done.");
	}
}
