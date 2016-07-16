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
package playground.wrashid.parkingSearch.ppSim.ttmatrix;

import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.Matrix;

public class TTMatrixFromStoredTable extends TTMatrix {
	
	public static void main(String[] args) {
		String networkFile="C:/data/parkingSearch/chessboard/output/output_network.xml.gz";
		TTMatrixFromStoredTable ttMatrix=new TTMatrixFromStoredTable("c:/tmp2/table.txt",networkFile);
		ttMatrix.writeTTMatrixToFile("c:/tmp2/table3.txt");
	}

	public TTMatrixFromStoredTable(String ttMatrixFile, String networkFile){
		this.network = GeneralLib.readNetwork(networkFile);
		readMatrix(ttMatrixFile);
	}
	
	public TTMatrixFromStoredTable(String ttMatrixFile, Network network){
		this.network = network;
		readMatrix(ttMatrixFile);
	}

	private void readMatrix(String ttMatrixFile) {
		Matrix sm=GeneralLib.readStringMatrix(ttMatrixFile);
		
		for (int i=0;i<sm.getNumberOfColumnsInRow(0);i++){
			if (sm.getString(0, i).contains("simulatedTimePeriod")){
				simulatedTimePeriod= Integer.parseInt(sm.getString(0, i).replaceAll("simulatedTimePeriod=", ""));
			}
			if (sm.getString(0, i).contains("timeBinSizeInSeconds")){
				timeBinSizeInSeconds= Integer.parseInt(sm.getString(0, i).replaceAll("timeBinSizeInSeconds=", ""));
			}
		}
		
		linkTravelTimes=new HashMap<Id<Link>, double[]>();
		int numberOfBins = getNumberOfBins();
		
		for (int i=1;i<sm.getNumberOfRows();i++){
			Id<Link> linkId=Id.create(sm.getString(i, 0), Link.class);
			double[] d=new double[numberOfBins];
			for (int j=1;j<sm.getNumberOfColumnsInRow(i);j++){
				 d[j-1]= sm.getDouble(i, j);
			}
			linkTravelTimes.put(linkId, d);
		}
	}



}

