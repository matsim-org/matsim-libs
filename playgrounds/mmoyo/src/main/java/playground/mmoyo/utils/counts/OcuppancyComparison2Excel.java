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

package playground.mmoyo.utils.counts;

import java.io.File;

import org.matsim.api.core.v01.Id;
import playground.mmoyo.analysis.counts.reader.CountsReader;
import playground.mmoyo.io.TextFileWriter;

public class OcuppancyComparison2Excel {

	private void run(final String occupCompFile){
		final String tab= ("\t");
		final String nl= ("\n");
		StringBuffer sBuff = new StringBuffer("id\tReal counts\tSimulted scaled\n");
		
		CountsReader countReader = new CountsReader(occupCompFile);
		for (Id id : countReader.getStopsIds()){
			sBuff.append(id+ tab + countReader.getRealValues(id)[0] + tab + countReader.getSimulatedScaled(id)[0] + nl);		
		}
		
		System.out.println("writing output plan file...");
		File file = new File(occupCompFile);
		new TextFileWriter().write(sBuff.toString(), file.getParent() + "/tabTextFile.txt", false);
		System.out.println("done");
	}
	
	public static void main(String[] args) {
		String cntCmpareOccupFilePath = "../../input/New Folder/0.cadytsSimCountCompareOccupancy.txt";
		OcuppancyComparison2Excel ocuppancyComparison2Excel = new OcuppancyComparison2Excel();
		ocuppancyComparison2Excel.run(cntCmpareOccupFilePath);
	}

}
