package saleem.p0;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Scanner;

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
		Scanner scan;
	    File file = new File(path);
	    try {
	        scan = new Scanner(file);

	        while(scan.hasNext())
	        {
	        	values.add(Double.parseDouble(scan.next()));
	        }
	        scan.close();

	    } catch(Exception ex) {
	        //catch logic here
	    }
		return values;
	}
}
