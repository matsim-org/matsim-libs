package playground.mzilske.vis;

import java.io.File;
import java.io.IOException;

public class DirectoryUtils {

	public static File createTempDirectory() {
		final File temp;
	
		try {
			temp = File.createTempFile("otfvis", Long.toString(System.nanoTime()));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	
		if (!(temp.delete())) {
			throw new RuntimeException("Could not delete temp file: "
					+ temp.getAbsolutePath());
		}
	
		if (!(temp.mkdir())) {
			throw new RuntimeException("Could not create temp directory: "
					+ temp.getAbsolutePath());
		}
	
		return (temp);
	}

	static public boolean deleteDirectory(File path) {
	    if( path.exists() ) {
	      File[] files = path.listFiles();
	      for(int i=0; i<files.length; i++) {
	         if(files[i].isDirectory()) {
	           deleteDirectory(files[i]);
	         }
	         else {
	           files[i].delete();
	         }
	      }
	    }
	    return( path.delete() );
	  }

}
