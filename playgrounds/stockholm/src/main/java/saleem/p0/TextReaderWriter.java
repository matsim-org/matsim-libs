package saleem.p0;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Iterator;

public class TextReaderWriter {
	public void writeToTextFile(ArrayList<Double> capacitieslink, String path){
		Iterator<Double> iter = capacitieslink.iterator();
		try { 
			File file=new File(path);
			String text="";
			 while(iter.hasNext()){
		        	double d = iter.next();
		        	text = text + d + " ";
		        }
		    FileOutputStream fileOutputStream=new FileOutputStream(file);
		    fileOutputStream.write(text.getBytes());
		    fileOutputStream.close();
	       
	    } catch(Exception ex) {
	        //catch logic here
	    }
	}
}
