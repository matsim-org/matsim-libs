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

package playground.droeder.utils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

public class AnalyzeMyShapeFile {
	private static final Logger log = Logger
			.getLogger(AnalyzeMyShapeFile.class);
	
	public static void main(String[] args) throws IOException{
		if(args.length != 1){
			log.error("nr of arguments not correct");
			System.exit(-1);
		}
		new AnalyzeMyShapeFile().run(args[0]);
	}
	
	private Map<Integer, Set<String>> values = new HashMap<Integer, Set<String>>();
	
	public void run(String shapeFile) throws IOException{
		SimpleFeature ft = null;
		for (SimpleFeature f : ShapeFileReader.getAllFeatures(shapeFile)) {
			if (ft == null) {
				ft = f;
			}
			
			for(int i = 0; i < f.getAttributeCount(); i++){
				if(this.values.containsKey(i)){
					if(!this.values.get(i).contains(f.getAttribute(i).toString()) && !(this.values.get(i).size() > 100)){
						this.values.get(i).add(f.getAttribute(i).toString());
					}
				}else{
					Set<String> temp = new TreeSet<String>();
					temp.add(f.getAttribute(i).toString());
					this.values.put(i, temp);
				}
			}
		}
		
		for(Entry<Integer, Set<String>> e: this.values.entrySet()){
			System.out.print(e.getKey() + "\t");
			for(String s : e.getValue()){
				System.out.print(s + "\t");
			}
			System.out.println();
		}
		
		for(int i = 0; i < ft.getAttributeCount(); i++){
			System.out.print(i + " " + ft.getFeatureType().getAttributeDescriptors().get(i).getName() + "\t" + ft.getAttribute(i).getClass().toString());
			System.out.println();
		}
	}

}
