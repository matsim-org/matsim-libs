///* *********************************************************************** *
// * project: org.matsim.*
// * CommonUtilities.java
// *                                                                         *
// * *********************************************************************** *
// *                                                                         *
// * copyright       : (C) 2010 by the members listed in the COPYING,        *
// *                   LICENSE and WARRANTY file.                            *
// * email           : info at matsim dot org                                *
// *                                                                         *
// * *********************************************************************** *
// *                                                                         *
// *   This program is free software; you can redistribute it and/or modify  *
// *   it under the terms of the GNU General Public License as published by  *
// *   the Free Software Foundation; either version 2 of the License, or     *
// *   (at your option) any later version.                                   *
// *   See also COPYING, LICENSE and WARRANTY file                           *
// *                                                                         *
// * *********************************************************************** */
//
///**
// * 
// */
//package org.matsim.contrib.matsim4opus.utils;
//
//import java.net.URL;
//
//import org.matsim.contrib.matsim4opus.constants.Constants;
//
///**
// * @author thomas
// *
// */
//public class CommonUtilities {
//
//	/**
//	 * makes sure that a path ends with "/"
//	 * @param any desired path
//	 * @return path that ends with "/"
//	 */
//	public static String checkPathEnding(String path){
//		
//		path.replace('\\', '/');
//		
//		if(path.endsWith("/"))
//			return path;
//		else
//			return path + "/";
//	}
//	
//	/**
//	 * returns the path of the current directory
//	 * @return class path
//	 */
//	@SuppressWarnings("all")
//	public static String getCurrentPath(Class classObj){
//		try{
//			URL dirUrl = classObj.getResource("./"); // get directory of given class
//			return dirUrl.getFile();
//		}catch(Exception e){
//			e.printStackTrace();
//			return null;
//		}
//	}
//	
//	/**
//	 * replaces parts of a path with another subPath for a given directory
//	 * hirachy (depth).
//	 * Example:
//	 * Path = "/home/username/dir/"
//	 * Subpath = "/anotherDir/"
//	 * 
//	 * depth = 0 leads to:
//	 * "/home/username/dir/anotherDir
//	 * 
//	 * pepth = 1 leads to:
//	 * "/home/username/anotherDir
//	 * 
//	 * @param depth level of directory hirachy
//	 * @param path
//	 * @param subPath
//	 * @return path that incorporates a given path and subpath
//	 */
//	public static String replaceSubPath(int depth, String path, String subPath){
//		
//		StringBuffer newPath = new StringBuffer("/");
//		
//		path.replace("\\", "/");
//		
//		String[] pathArray = path.split("/");
//		String[] subPathArray = subPath.split("/");
//		
//		int iterations = pathArray.length - depth;
//		if(pathArray.length >= iterations){
//			
//			for(int i = 0; i < iterations; i++)
//				if(!pathArray[i].equalsIgnoreCase(""))
//					newPath.append( pathArray[i] + "/" );
//			for(int i = 0; i < subPathArray.length; i++)
//				if(!subPathArray[i].equalsIgnoreCase(""))
//					newPath.append( subPathArray[i] + "/");
//			
//			// remove last "/"
//			newPath.deleteCharAt( newPath.length()-1 );
//			return newPath.toString().trim();
//		}
//		return null;
//	}
//	
//	/**
//	 * returns the directory to the UrbanSim input data for MATSim Warm Start
//	 * @return directory to the UrbanSim input data
//	 */
//	@SuppressWarnings("all")
//	public static String getWarmStartUrbanSimInputData(Class<?>  classObj){		
////		return concatPath(1, "matsimTestData/warmstart/urbanSimOutput/", classObj);
//		return CommonUtilities.checkPathEnding( CommonUtilities.getCurrentPath( classObj ) + Constants.MATSIM_TEST_DATA_WARM_START_URBANSIM_OUTPUT );
//	}
//	
//	/**
//	 * returns the directory to the input plans file for MATSim Warm Start
//	 * @return directory to the input plans file
//	 */
//	@SuppressWarnings("all")
//	public static String getWarmStartInputPlansFile(Class<?>  classObj){		
////		return concatPath(1, "matsimTestData/warmstart/inputPlan/", classObj);
//		return CommonUtilities.checkPathEnding( CommonUtilities.getCurrentPath( classObj ) + Constants.MATSIM_TEST_DATA_WARM_START_INPUT_PLANS);
//	}
//	
//	/**
//	 * returns the directory to the MATSim Warm Start network
//	 * @return directory to the network
//	 */
//	@SuppressWarnings("all")
//	public static String getWarmStartNetwork(Class<?>  classObj){		
////		return concatPath(1, "matsimTestData/warmstart/network/", classObj);
//		return CommonUtilities.checkPathEnding( CommonUtilities.getCurrentPath( classObj ) + Constants.MATSIM_TEST_DATA_WARM_START_NETWORK );
//	}
//	
//	/**
//	 * returns the directory to the UrbanSim input data for MATSim
//	 * @return directory to the UrbanSim input data
//	 */
//	@SuppressWarnings("all")
//	public static String getTestUrbanSimInputDataDir(Class<?>  classObj){
//		return CommonUtilities.checkPathEnding( CommonUtilities.getCurrentPath( classObj ) + Constants.MATSIM_TEST_DATA_DEFAULT_URBANSIM_OUTPUT );
//		
////		return concatPath(1, "matsimTestData/urbanSimOutput/", classObj);
//	}
//	
//}

