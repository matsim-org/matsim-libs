/* *********************************************************************** *
 * project: org.matsim.*
 * GeoReferencer.java
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

package playground.gregor.gis.referencing;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.core.utils.io.IOUtils;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.gregor.gis.referencing.CRN.CaseNode;

import com.vividsolutions.jts.geom.Coordinate;

public class GeoReferencer {
	
	private static final String HOME_ACT = "activities in the house other than work (if the place of work is at home)";
	private static final String WORK_ACT = "work";
	private static final String WORK_REL_ACT = "activities in support of work/business";
	private static final String SOC_ACT = "other social activities";
	private static final String HOME_LOCATION = "house (settlement area)";
	
	
	
	private final HashMap<Integer,Coordinate> homeLocations = new HashMap<Integer, Coordinate>();
	private final ArrayList<String> altHomeNames = new ArrayList<String>();
	
	private CRN crn;
	private final TextFileReader sfReader;
	private final FeatureGenerator ftGen;
	private ArrayList<String> otherFiles;
	private final HashMap<String, Feature> others;
	private final Coordinate dummyCoordinate = new Coordinate(644794,9896894);
	private final String output;
	
	


	public GeoReferencer(final String inputShapeFile, final String inputSurveyFile, final String output) {
		this.output = output;
		try {
			Collection<Feature> fts = readShapeFiles(inputShapeFile);
			fts.addAll(referenceBasedOnHomeLocations(inputSurveyFile));
			this.crn = new CRN(fts);
			this.sfReader = new TextFileReader(inputSurveyFile,',',36);
			
			this.others = getOthers(ShapeFileReader.readDataFile(this.otherFiles.get(0)),3);
			this.others.putAll(getOthers(ShapeFileReader.readDataFile(this.otherFiles.get(1)), 2));
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		CoordinateReferenceSystem crs = MGC.getCRS(TransformationFactory.WGS84_UTM47S);
		this.ftGen = new FeatureGenerator(crs,this.sfReader.readLine());
		initAltHomeNames();
		

		
	}
	
	private HashMap<String, Feature> getOthers(final FeatureSource others2,final int idx) {
		FeatureReader fr = new FeatureReader();
		final HashMap<String,Feature> features = new HashMap<String,Feature>();
		final Collection<Feature> fts = fr.getFeatures(others2);
		for (final Feature ft : fts) {
			final String key = (String) ft.getAttribute(idx);
			features.put(key.toLowerCase(),ft);

		}
		return features;
	}
	
	private Collection<Feature> referenceBasedOnHomeLocations(final String inputSurveyFile) {
		TextFileReader reader = new TextFileReader(inputSurveyFile,',',36);
		FeatureGenerator pfg = new FeatureGenerator(MGC.getCRS(TransformationFactory.WGS84_UTM47S),new String [] {"null1","null2","name"});
		String [] line = reader.readLine();
		line = reader.readLine();
		Collection<Feature> fts = new ArrayList<Feature>();
		while (line != null) {
			String actType = line[4].toLowerCase();
			int hhId = Integer.parseInt(line[1]);
			String location = line[6].toLowerCase();
			
			Feature ft = null;
			if (actType.equals(HOME_ACT) && !isHomeLocation(location)){
				Coordinate c = this.homeLocations.get(hhId);
				ft = pfg.getFeature(c, new String [] {"null", "null",location});
				fts.add(ft);
			}
			line = reader.readLine();
		}
		
		return fts;
		
	}

	private void initAltHomeNames() {
		this.altHomeNames.add("rawang");
		this.altHomeNames.add("rumah");
		this.altHomeNames.add("tetangga");
		this.altHomeNames.add("dekat");
		this.altHomeNames.add("sekitart");
		this.altHomeNames.add("depan rumah");
		this.altHomeNames.add("warung");
		this.altHomeNames.add("di kelurahan");
		this.altHomeNames.add("istirahat");
	}
	
	public void classify() {
		int missing = 0;
		String [] line = this.sfReader.readLine();
		Collection<Feature> fts = new ArrayList<Feature>();
		while (line != null) {
			String actType = line[4].toLowerCase();
			int hhId = Integer.parseInt(line[1]);
			String location = line[6].toLowerCase();
			
			Feature ft = null;
			if (actType.equals(HOME_ACT) || isHomeLocation(location)){
				Coordinate c = this.homeLocations.get(hhId);
				ft = this.ftGen.getFeature(c, line);
			} else {
				CaseNode resp = this.crn.getCase(location);
				if (resp == null || resp.getActivation() <= 0.96) {
					resp = this.crn.getCase("jalan " + location);
				} 

				if (resp != null && resp.getActivation() > 0.96) {
					ft = this.ftGen.getFeature(resp.getCoordinate(), line);
				}else  {
					for (final String str : this.others.keySet()) {
						if (location.contains(str)){
							ft = this.ftGen.getFeature(this.others.get(str).getDefaultGeometry().getCoordinate(), line);
							break;
						}
					}
				}
			}
		
			if (ft == null) {
				ft = this.ftGen.getFeature(this.dummyCoordinate , line);
				missing ++;
			}
			fts.add(ft);
			line = this.sfReader.readLine();
		}
		ShapeFileWriter.writeGeometries(fts,this.output);
		System.out.println("missing" + missing);
	}
	
	
	


	private boolean isHomeLocation(final String location) {
		for (final String str : this.altHomeNames) {
			if (location.contains(str)) {
				return true;
			}
		}
		return false;
	}


	private Collection<Feature> readShapeFiles(final String inputFile) throws FileNotFoundException, IOException {
		final Collection<Feature> fts = new ArrayList<Feature>();
		FeatureReader fr = new FeatureReader();
		BufferedReader reader = IOUtils.getBufferedReader(inputFile);
		String line = reader.readLine();
		int others = Integer.parseInt(line);
		int count = 1;
		line = reader.readLine();
		count++;
		Collection<Feature> homes = fr.getFeatures(ShapeFileReader.readDataFile(line));
		
		for (Feature ft : homes) {
			this.homeLocations.put(((Integer)ft.getAttribute(1)), ft.getDefaultGeometry().getCoordinate());
		}
		
		this.otherFiles = new ArrayList<String>();
		while ((line = reader.readLine()) != null) {
			count++;
			if (count >= others){
				this.otherFiles.add(line);
			}
			fts.addAll(fr.getFeatures(ShapeFileReader.readDataFile(line)));
		}
		
		reader.close();
		return fts;
	}

	public static void main(final String [] args) {
		
		if (args.length != 3) {
			throw new RuntimeException("GeoReferencer expectd exact 2 arguments!");
		}
		new GeoReferencer(args[0], args[1], args[2]).classify();
	}

}
