package org.tit.matsim;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

public class Tools {
	private static Logger log = Logger.getLogger(Tutorial.class);

	//copy files in output directory
	public static void copyCurrentOutput(String description) throws IOException {
		int cnt = 1;
		File dir=new File("outputCopies/output_" + 1);
		if(description==null){
			
		    while(dir.isDirectory()){
		    	cnt++;
		    	dir =new File("outputCopies/output_" + cnt);
		    	}
		}
		else {
			dir = new File("outputCopies/output_" + description);
		}
		log.info("Creating output directory status: " + dir.mkdirs() + ", "+ dir.getPath() + " directory created.");
		
		try {
			FileUtils.copyDirectory(new File("output"), dir);
			log.info("Output successfully copied");
		} catch (IOException e) {
		    e.printStackTrace();
		}
	}
	
	
}
