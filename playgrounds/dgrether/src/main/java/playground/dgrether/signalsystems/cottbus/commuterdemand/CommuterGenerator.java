/* *********************************************************************** *
 * project: org.matsim.*
 * CommuterGenerator
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
package playground.dgrether.signalsystems.cottbus.commuterdemand;

import java.io.IOException;
import java.util.Set;

import org.geotools.feature.Feature;
import org.matsim.core.utils.gis.ShapeFileReader;

/**
 * @author jbischoff
 *
 */
public class CommuterGenerator {

	public static void main(String[] args) throws IOException {
		
		CommuterDataReader cdr = new CommuterDataReader();
		cdr.addFilterRange(12071000);
		cdr.addFilter("12052000");
		cdr.readFile("C:\\Users\\Joschka Bischoff\\Documents\\Brandenburg\\CD_Pendler_Gemeindeebene_30_06_2009\\brandenburg_einpendler.csv");
		
		String gemeindenBrandenburgShapeFile = "";
		ShapeFileReader shapeReader = new ShapeFileReader();
		Set<Feature> gemeindenFeatures = shapeReader.readFileAndInitialize(gemeindenBrandenburgShapeFile);
		
		CommuterDemandWriter cdw = new CommuterDemandWriter(gemeindenFeatures, cdr.getCommuterRelations());
		cdw.setScalefactor(1.0);//1.0 is default already
		cdw.writeDemand("C:\\Users\\Joschka Bischoff\\Documents\\Brandenburg\\demand\\demand.xml");
		
	}

}
