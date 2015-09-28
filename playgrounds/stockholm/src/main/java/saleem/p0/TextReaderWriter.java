package saleem.p0;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;

public class TextReaderWriter {
	public void writeToTextFile(ArrayList<Double> values, String path){
		Iterator<Double> iter = values.iterator();
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
	public ArrayList<Double> readFromTextFile(String path){
		ArrayList<Double> values = new ArrayList<>();
		try { 
			for (String line : Files.readAllLines(Paths.get(path))) {
			    for (String part : line.split(" ")) {
			        Double i = Double.valueOf(part);
			        values.add(i);
			    }
			}
	       
	    } catch(Exception ex) {
	        //catch logic here
	    }
		return values;
	}
}
