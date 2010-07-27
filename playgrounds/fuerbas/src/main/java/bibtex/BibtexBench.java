/*
 * Created on Dec 14, 2003
 * 
 * @author henkel@cs.colorado.edu
 *  
 */
package bibtex;

import java.io.File;
import java.io.FileFilter;

/**
 * @author henkel
 */
public class BibtexBench {

	public static void main(String[] args) {

		String bibtexExampleDir = "examples";

		File dir = new File(bibtexExampleDir);
		File files[] = dir.listFiles(new FileFilter() {
			public boolean accept(File pathname) {
				if (!pathname.isFile())
					return false;
				if (!pathname.getName().endsWith(".bib"))
					return false;
				return true;
			}
		});
		String parameters[] = new String[] { "-expandCrossReferences", "-expandPersonLists", //"-noOutput",
			"dummy" };
		long start = System.currentTimeMillis();
		for (int i = 0; i < files.length; i++) {
			String path = files[i].getAbsolutePath();
			parameters[parameters.length - 1] = path;
			Main.main(parameters);
		}
		System.err.println("This took "+(System.currentTimeMillis()-start)+".");
	}
}
