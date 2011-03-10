package playground.anhorni.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import org.apache.log4j.Logger;

public abstract class ConfigReader {
	
	private String path;
	private String cFile = "src/main/java/playground/anhorni/input/cFile.txt";
	
	private final static Logger log = Logger.getLogger(ConfigReader.class);
	
	public ConfigReader() {
	  try {
          BufferedReader bufferedReader = new BufferedReader(new FileReader(cFile));
          String line = bufferedReader.readLine();
          String parts[] = line.split("\t");
          this.path = parts[1];	   
          log.info("Config path: " + this.path);
        } // end try
        catch (IOException e) {
        	e.printStackTrace();
        }	
	}
	
	public abstract void read();

}
