package playground.vbmh.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;

public class CSVWriter {
	File file;
	FileWriter fileWriter;


	public CSVWriter(String fileName){
		file=new File (fileName+".csv");
		fileWriter = null;
		try {
			fileWriter = new FileWriter(file);
			//System.out.println("File offen");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void writeAll(LinkedList<LinkedList<String>> zeilen){
		for (LinkedList<String> zeile : zeilen){
			String zeilenText = null;
			for(String spalte : zeile){
				if(zeilenText!=null){
					zeilenText=zeilenText+",";
				}else{
					//System.out.println("neuer zeilentext");
					zeilenText="";
				}
				if(spalte==null){
					spalte=" ";
				}
				
				
				zeilenText=zeilenText+spalte;
			}
			writeLine(zeilenText);
		}
		
	}

	public void writeLine(String text){
		try {
			fileWriter.write(text+"\n");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void close(){
		try {
			fileWriter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
