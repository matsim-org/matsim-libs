package playground.mmoyo.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

/**
 *  Deletes 
 *  iterations folders that are not needed for output analysis (non 10 multiples)
 *  plans, events, 
 *  
 */
public class cleaner {


	public static void main(String[] args) {
		String itersDir = "";
		
		if (args.length>0){
			itersDir= args[0];
		}else{
			itersDir = "../../";
		}

		File file = new File(itersDir);
		
		
//		for(int x=0; x <=100; x+=10){
//			File itDir = new File(itersDir + "it." + x);
//			itDir.mkdir();
//			itDir = new File(itersDir + "it." + (x/10));
//			itDir.mkdir();
//		}
//		System.exit(0);
		
		
		final String it = "it.";
		final String sep = "/";
		final String events = ".events.xml.gz";
		final String plans = ".plans.xml.gz";
		final String linkstats = ".linkstats.txt.gz";		

		if (file.exists()){
			List<String> deleteList = new ArrayList<String>();		
			
			
			//find out the highest iteration
			List<Integer> intList = new ArrayList<Integer>();
			for (String dirName: file.list()){
				String strItNum = dirName.substring(3, dirName.length());
				int itNum = Integer.valueOf (strItNum);
				intList.add(itNum);
			}
			
			Collections.sort(intList);
			//Collections.max(intList);
			intList.remove(intList.size()-1);
			intList.remove(intList.size()-1);

			for (int itNum: intList){
				
				String itDirPath= itersDir + it + itNum + sep;
				if (!(itNum%10 == 0)){
					deleteList.add(itDirPath);
				}else if(!(itNum%50 == 0)){
					String evntFile = itDirPath + itNum + events;
					String popFile =itDirPath + itNum + plans;
					String linkstatFile =itDirPath + itNum + linkstats;
					deleteList.add(evntFile);
					deleteList.add(popFile);
					deleteList.add(linkstatFile);
				}
			}	
		
			for (String s: deleteList){
				File f = new File(s);
				if(f.exists() ) {
					deleteDirectory (f);
				}
			}
		}
	}
	
	 static public boolean deleteDirectory(File fileObj) {
		 if( fileObj.exists() ) {
			 if (fileObj.isDirectory()){ 
				 File[] files = fileObj.listFiles();
		    	 for(int i=0; i<files.length; i++) {
		    		 if(files[i].isDirectory()) {
		    			 deleteDirectory(files[i]);
		    		 }else {
		    			 files[i].delete();
		    		 }
		    	 }
			 }else{
				 boolean deleted = fileObj.delete(); 
				 System.out.println(fileObj.getPath() + deleted + 1);
				 return deleted;
			 }
		 }
		 boolean deleted = fileObj.delete(); 
		 System.out.println(fileObj.getPath() + deleted + 2);		 
		 return(deleted );
	 }
	

}
