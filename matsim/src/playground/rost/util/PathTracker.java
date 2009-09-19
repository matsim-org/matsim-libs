/******************************************************************************
 *project: org.matsim.*
 * PathTracker.java
 *                                                                            *
 * ************************************************************************** *
 *                                                                            *
 * copyright       : (C) 2009 by the members listed in the COPYING,           *
 *                   LICENSE and WARRANTY file.                               *
 * email           : info at matsim dot org                                   *
 *                                                                            *
 * ************************************************************************** *
 *                                                                            *
 *   This program is free software; you can redistribute it and/or modify     *
 *   it under the terms of the GNU General Public License as published by     *
 *   the Free Software Foundation; either version 2 of the License, or        *
 *   (at your option) any later version.                                      *
 *   See also COPYING, LICENSE and WARRANTY file                              *
 *                                                                            *
 ******************************************************************************/


package playground.rost.util;

public class PathTracker {
	
	private static final String resPath = "src/playground/rost/res/";
	private static final String resOutput = "src/playground/rost/res/output/";
	private static final String resLogger = "src/playground/rost/res/logger/";
	
	private static String[][] fileMap = new String[][]
	     {
			{"osmMap", resPath + "berlin2.osm"},
			{"highwayMapping", resOutput + "highwayMapping.xml"},
			{"highwayMappingDefault", resPath + "highwayMappingDefault.xml"},
			{"evacArea", resOutput + "evacArea.xml"},
			{"plan", resOutput + "plan.xml"},
			{"pplDistribution", resOutput + "pplDistribution.xml"},
			{"matMap", resOutput + "berlin2mat.xml"},
			{"matExtract", resOutput + "berlin2matExtract.xml"},
			{"events", resOutput + "events.txt"},
			{"distriLogger", resLogger + "logger.properties"},
			{"flatNetwork", resOutput + "flatNetwork.xml"},
			{"flatNetworkBlocks", resOutput + "flatNetworkBlocks.xml"},
			{"flatNetworkBorder", resOutput + "flatNetworkBorder.xml"},
			{"populationPoints", resOutput + "populationPoints.xml"},
			{"populationForNodes", resOutput + "populationForNodes.xml"}
	     };

	
	public static String resolve(String key)
	{
		for(int i = 0; i < fileMap.length; ++i)
		{
			if(fileMap[i][0].equals(key))
			{
				return fileMap[i][1];
			}
		}
		throw new RuntimeException("key not found!");
	}
	
	public static void main(String[] args)
	{
		System.out.println("PathTracker!");
		for(int i = 0; i < fileMap.length; ++i)
		{
			System.out.println();
			System.out.println(fileMap[i][0] + " --> " + fileMap[i][1]);
		}
	}
	
	public static final String[][] getFiles()
	{
		return fileMap;
	}
}
