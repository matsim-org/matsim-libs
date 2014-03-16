package playground.qvanheerden.utils;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.Iterator;
import java.util.TreeSet;

/**
 * This class provides the ability to filter files in a directory by a specified
 * filter extension, by implementing the {@link FilenameFilter} interface. It provides
 * an option to pass only one string value or a string array with multiple extensions.
 * It also provides the added ability to filter files by more than just a suffix 
 * (e.g. ".txt") as one can specify e.g. "_1.csv" and all files with such an extension
 * will be filtered.  The same holds for multiple extensions e.g. {"_1.csv", "_2.csv"},
 * which will filter and keep all files with those extensions.
 * 
 * @author qvanheerden
 *
 */

public class MyExtendedFilenameFilter implements FilenameFilter{
	private TreeSet<String> extensions = new TreeSet<String>() ;

	public MyExtendedFilenameFilter(String extension) {
		extensions.add(extension.toLowerCase());
	}

	public MyExtendedFilenameFilter(String[] extArray) {
		Iterator<String> extentionList = Arrays.asList(extArray).iterator();
		//add all extensions of the filters passed
		while (extentionList.hasNext()) {
			extensions.add(extentionList.next().toLowerCase());
		}
	}

	public boolean accept(File directory, String name) {
		//iterate through the extensions of filters passed
		final Iterator<String> extensionList = extensions.iterator();
		while (extensionList.hasNext()) {
			String extension = extensionList.next();
			boolean hasOnlySuffix = extension.substring(0, 1).equals(".");

			String suffix = extension.substring(extension.indexOf("."), extension.length());
			//check if filename suffix matches filter suffix
			if (name.toLowerCase().endsWith(suffix)) {
				//check if the filter extension contains only a suffix
				if(!hasOnlySuffix){
					//get the length of the string of characters before the period for both the filename and filter
					int charBeforePeriodName = name.substring(0, name.indexOf(".")).length();
					int charBeforePeriodExt = extension.substring(0, extension.indexOf(".")).length();

					//check if number of characters of filename is at least the number of characters before the period of the filter
					if(charBeforePeriodName >= charBeforePeriodExt){
						//get the substring before the period of both the filename and filter for the number of characters before the period of the filter
						String partBeforePeriodName = name.substring(name.indexOf(".")-charBeforePeriodExt, name.indexOf("."));
						String partBeforePeriodExt = extension.substring(extension.indexOf(".")-charBeforePeriodExt, extension.indexOf("."));
						//check if the two substrings match
						if(partBeforePeriodName.equalsIgnoreCase(partBeforePeriodExt)){
							return true;
						}
					}
				}else{
					return true;
				}
			}
		}
		return false;
	}
}
