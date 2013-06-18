package org.matsim.contrib.matsim4urbansim.utils.io;

import java.io.File;
import java.net.URL;


public class Paths {
	
	// MATSim4OPUS Test environment (relative paths)
	public static final String WARM_START_URBANSIM_OUTPUT 	= "warmstart/urbanSimOutput";
	public static final String WARM_START_INPUT_PLANS 		= "warmstart/inputPlan";
	public static final String WARM_START_NETWORK 			= "warmstart/network";
	public static final String DEFAULT_URBANSIM_OUTPUT 		= "urbanSimOutput";
	
	/**
	 * verifying if args argument contains a valid path. 
	 * @param args
	 */
	public static void isValidPath(String matsimConfiFile){
		// test the path to matsim config xml
		if( matsimConfiFile==null || matsimConfiFile.length() <= 0 || !pathExsits( matsimConfiFile ) )
			throw new RuntimeException(matsimConfiFile + " is not a valid path to a MATSim configuration file. SHUTDOWN MATSim!");
	}
	
	/**
	 * Checks if a given path exists
	 * @param arg path
	 * @return true if the given file exists
	 */
	public static boolean pathExsits(String path){
		return( (new File(path)).exists() );
	}
	
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
	
	/**
	 * returns the directory to the UrbanSim input data for MATSim Warm Start
	 * @return directory to the UrbanSim input data
	 */
	@SuppressWarnings("all")
	public static String getWarmStartUrbanSimInputData(Class<?>  classObj){		
		return Paths.checkPathEnding( Paths.getCurrentPath( classObj ) + WARM_START_URBANSIM_OUTPUT );
	}
	
	/**
	 * returns the directory to the input plans file for MATSim Warm Start
	 * @return directory to the input plans file
	 */
	@SuppressWarnings("all")
	public static String getWarmStartInputPlansFile(Class<?>  classObj){		
		return Paths.checkPathEnding( Paths.getCurrentPath( classObj ) + WARM_START_INPUT_PLANS);
	}
	
	/**
	 * returns the directory to the MATSim Warm Start network
	 * @return directory to the network
	 */
	@SuppressWarnings("all")
	public static String getWarmStartNetwork(Class<?>  classObj){		
		return Paths.checkPathEnding( Paths.getCurrentPath( classObj ) + WARM_START_NETWORK );
	}
	
	/**
	 * returns the directory to the UrbanSim input data for MATSim
	 * @return directory to the UrbanSim input data
	 */
	@SuppressWarnings("all")
	public static String getTestUrbanSimInputDataDir(Class<?>  classObj){
		return Paths.checkPathEnding( Paths.getCurrentPath( classObj ) + DEFAULT_URBANSIM_OUTPUT );
	}
}
