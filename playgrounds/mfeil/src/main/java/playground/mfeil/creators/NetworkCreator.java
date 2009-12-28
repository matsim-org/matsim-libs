/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkCreator.java
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

package playground.mfeil.creators;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import org.matsim.core.gbl.Gbl;

public class NetworkCreator {

	/**
	 * Creates a Manhattan-like network.
	 */
	public static void main(String[] args) {
		
		int networkSize 	= 10;
		double distance 	= 1000;
		double capacity		= 15.0;
		
		int nodeID 			= 1;
		int linkID 			= 1;
	
	
		try{
			 FileWriter fw = new FileWriter("output/network.xml");
			 BufferedWriter out = new BufferedWriter(fw);
			 
			 out.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
			 out.write("<!DOCTYPE network SYSTEM \"http://www.matsim.org/files/dtd/network_v1.dtd\">\n\n");
			 
			 out.write("<network name=\"Network_Test1\">\n\n");
			 
			 out.write("<!-- ====================================================================== -->\n\n");
			 
			 out.write("\t<nodes>\n");
			 for (int i=0;i<networkSize;i++){
				 for (int j=0;j<networkSize;j++){
					 out.write("\t\t<node id=\""+nodeID+"\" x=\""+i*distance+"\" y=\""+j*distance+"\" />\n");
					 nodeID++;
				 }
			 }
			 out.write("\t</nodes>\n");
			 
			 out.write("<!-- ====================================================================== -->\n\n");
			 
			 out.write("\t<links capperiod=\"01:00:00\" effectivecellsize=\"7.5\" effectivelanewidth=\"3.75\">\n");
			 for (int i=1;i<networkSize+1;i+=2){
				 for (int j=i;j<networkSize*networkSize-networkSize;j+=networkSize){
					 out.write("\t\t<link id=\""+linkID+"\" from=\""+j+"\" to=\""+(j+networkSize)+"\" length=\""+distance+"\" freespeed=\"7.5\" capacity=\""+capacity+"\" permlanes=\"1.0\" oneway=\"1\" origid=\""+linkID+"\" type=\"1\" />\n");
					 linkID++;
				 }
			 }
			 for (int i=2;i<networkSize+2;i+=2){
				 for (int j=i;j<networkSize*networkSize-networkSize+1;j+=networkSize){
					 out.write("\t\t<link id=\""+linkID+"\" from=\""+(j+networkSize)+"\" to=\""+j+"\" length=\""+distance+"\" freespeed=\"7.5\" capacity=\""+capacity+"\" permlanes=\"1.0\" oneway=\"1\" origid=\""+linkID+"\" type=\"1\" />\n");
					 linkID++;
				 }
			 }
			 for (int i=1;i<networkSize*networkSize+1;i+=2*networkSize){
				 for (int j=i;j<i+networkSize-1;j++){
					 out.write("\t\t<link id=\""+linkID+"\" from=\""+(j+1)+"\" to=\""+(j)+"\" length=\""+distance+"\" freespeed=\"7.5\" capacity=\""+capacity+"\" permlanes=\"1.0\" oneway=\"1\" origid=\""+linkID+"\" type=\"1\" />\n");
					 linkID++;
				 }
			 }
			 for (int i=networkSize+1;i<networkSize*networkSize;i+=2*networkSize){
				 for (int j=i;j<i+networkSize-1;j++){
					 out.write("\t\t<link id=\""+linkID+"\" from=\""+(j)+"\" to=\""+(j+1)+"\" length=\""+distance+"\" freespeed=\"7.5\" capacity=\""+capacity+"\" permlanes=\"1.0\" oneway=\"1\" origid=\""+linkID+"\" type=\"1\" />\n");
					 linkID++;
				 }
			 }
			 out.write("\t</links>\n");
			 
			 out.write("<!-- ====================================================================== -->\n\n");
			 
			 out.write("</network>");
			 
			 out.flush();
			 out.close();
			 fw.close();
			 
		}catch (IOException e) {
			Gbl.errorMsg(e);
		}

		
		
	}

}
