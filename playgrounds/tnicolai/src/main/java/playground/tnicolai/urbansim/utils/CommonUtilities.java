/* *********************************************************************** *
 * project: org.matsim.*
 * CommonUtilities.java
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

/**
 * 
 */
package playground.tnicolai.urbansim.utils;

import java.net.URL;

/**
 * @author thomas
 *
 */
public class CommonUtilities {

	/**
	 * makes sure that a path ends with "/"
	 * @param any desired path
	 * @return path that ends with "/"
	 */
	public static String checkPathEnding(String path){
		
		path.replace('\\', '/');
		
		if(path.endsWith("/"))
			return path;
		else
			return path + "/";
	}
	
	/**
	 * returns the path of the current directory
	 * @return class path
	 */
	@SuppressWarnings("all")
	public static String getCurrentPath(Class classObj){
		try{
			URL dirUrl = classObj.getResource("./"); // get directory of given class
			return dirUrl.getFile();
		}catch(Exception e){
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * replaces parts of a path with another subPath for a given directory
	 * hirachy (depth).
	 * Example:
	 * Path = "/home/username/dir/"
	 * Subpath = "/anotherDir/"
	 * 
	 * depth = 0 leads to:
	 * "/home/username/dir/anotherDir
	 * 
	 * pepth = 1 leads to:
	 * "/home/username/anotherDir
	 * 
	 * @param depth level of directory hirachy
	 * @param path
	 * @param subPath
	 * @return path that incorporates a given path and subpath
	 */
	public static String replaceSubPath(int depth, String path, String subPath){
		
		StringBuffer newPath = new StringBuffer("/");
		
		path.replace("\\", "/");
		
		String[] pathArray = path.split("/");
		String[] subPathArray = subPath.split("/");
		
		int iterations = pathArray.length - depth;
		if(pathArray.length >= iterations){
			
			for(int i = 0; i < iterations; i++)
				if(!pathArray[i].equalsIgnoreCase(""))
					newPath.append( pathArray[i] + "/" );
			for(int i = 0; i < subPathArray.length; i++)
				if(!subPathArray[i].equalsIgnoreCase(""))
					newPath.append( subPathArray[i] + "/");
			
			// remove last "/"
			newPath.deleteCharAt( newPath.length()-1 );
			return newPath.toString().trim();
		}
		return null;
	}
	
}

