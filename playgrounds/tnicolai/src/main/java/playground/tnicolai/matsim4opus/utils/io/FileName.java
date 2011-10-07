package playground.tnicolai.matsim4opus.utils.io;

import java.io.File;

public class FileName {
	
	  private String fullPath;
	  private char pathSeparator, extensionSeparator;
	  
	  public FileName(String str, char sep, char ext) {
	    fullPath = str;
	    pathSeparator = sep;
	    extensionSeparator = ext;
	  }

	  public String extension() {
	    int dot = fullPath.lastIndexOf(extensionSeparator);
	    if(dot < 0)
	    	return null;
	    return fullPath.substring(dot + 1);
	  }

	  public String filename() { // gets filename without extension
	    int sep = fullPath.lastIndexOf(pathSeparator);
	    return fullPath.substring(sep + 1);
	  }

	  public String path() {
	    int sep = fullPath.lastIndexOf(pathSeparator);
	    return fullPath.substring(0, sep);
	  }

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		String f1 = "/Users/thomas/Development/opus_home/matsim4opus/tmp/output_plans.xml.gz";
		String f2 = "/Users/thomas/Development/opus_home/matsim4opus/tmp/";
		String f3 = "/Users/thomas/Development/opus_home/matsim4opus/tmp";
		
		FileName fm1 = new FileName(f1, File.separatorChar, '.');
		FileName fm2 = new FileName(f2, File.separatorChar, '.');
		FileName fm3 = new FileName(f3, File.separatorChar, '.');
		
		System.out.println(fm1.extension());
		System.out.println(fm1.filename());
		System.out.println(fm1.path());
		System.out.println(fm2.extension());
		System.out.println(fm2.filename());
		System.out.println(fm2.path());
		System.out.println(fm3.extension());
		System.out.println(fm3.filename());
		System.out.println(fm3.path());
	}

}
