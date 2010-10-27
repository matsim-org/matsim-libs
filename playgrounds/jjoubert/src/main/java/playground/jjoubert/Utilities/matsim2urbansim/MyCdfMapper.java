/* *********************************************************************** *
 * project: org.matsim.*
 * MyCdfMapper.java
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

package playground.jjoubert.Utilities.matsim2urbansim;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import nl.knaw.dans.common.dbflib.CorruptedTableException;
import nl.knaw.dans.common.dbflib.Field;
import nl.knaw.dans.common.dbflib.IfNonExistent;
import nl.knaw.dans.common.dbflib.Record;
import nl.knaw.dans.common.dbflib.Table;

import org.apache.log4j.Logger;
import org.matsim.demandmodeling.primloc.CumulativeDistribution;


public class MyCdfMapper {
	private final Logger log = Logger.getLogger(MyCdfMapper.class);
	private CumulativeDistribution cdfCar;
	
	public MyCdfMapper() {
		// TODO Auto-generated constructor stub
	}
	
	public void readCarDbf(String filename) throws CorruptedTableException, IOException{
		log.info("Attempting to read dbf from " + filename);
		Table t = new Table(new File(filename));
		t.open(IfNonExistent.ERROR);

		List<Double> values = new ArrayList<Double>();
		double minValue = Double.POSITIVE_INFINITY;
		double maxValue = Double.NEGATIVE_INFINITY;
			
		try{
			List<Field> fields = t.getFields();
			boolean hasThreefields = fields.size() == 3;
			boolean nameFieldOne = fields.get(0).getName().equalsIgnoreCase("fromZone");
			boolean nameFieldTwo = fields.get(1).getName().equalsIgnoreCase("toZone");
			boolean nameFieldthree = fields.get(2).getName().equalsIgnoreCase("ttCar");
			if(!(hasThreefields && nameFieldOne && nameFieldTwo && nameFieldthree)){
				throw new RuntimeException("The table " + t.getName() + " does not have the right fields.");
			}
			Iterator<Record> it = t.recordIterator();
			while(it.hasNext()){
				Record r = it.next();
				double d = r.getNumberValue("ttCar").doubleValue();
				minValue = Math.min(minValue, d);
				maxValue = Math.max(maxValue, d);
				values.add(d);
			}
			log.info("Read dbf completely (" + values.size() + " entries)");
		} finally{
			t.close();
		}
		if(values.size() > 0){
			log.info("Building cumulative distribution function.");
			cdfCar = new CumulativeDistribution(minValue, maxValue, 100);
			for(Double d : values){
				cdfCar.addObservation(d);
			}
			log.info("Completed cumulative distribution function.");
		} else{
			log.warn("Could not build cumulative distribution function - no records in " + filename);
		}
			

	}
	
	public CumulativeDistribution getCdfCar(){
		return this.cdfCar;
	}
	

}

