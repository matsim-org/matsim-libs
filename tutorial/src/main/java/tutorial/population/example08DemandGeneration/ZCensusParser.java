package tutorial.population.example08DemandGeneration;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class ZCensusParser {
		
	private String separator = "\t";
	private Charset charset = Charset.forName("UTF-8");

	public List<ZCensusEntry> readFile(String inFile)
	{
		List<ZCensusEntry> entries = new ArrayList<ZCensusEntry>();
		
		FileInputStream fis = null;
		InputStreamReader isr = null;
	    BufferedReader br = null;
	    
    	try 
    	{
    		fis = new FileInputStream(inFile);
    		isr = new InputStreamReader(fis, charset);
			br = new BufferedReader(isr);
			
			// skip first Line
			br.readLine();
			 
			String line;
			while((line = br.readLine()) != null)
			{
				ZCensusEntry censusEntry = new ZCensusEntry();
				
				String[] cols = line.split(separator);
								
				censusEntry.id_person = parseInteger(cols[0]);
				censusEntry.wp = parseDouble(cols[1]);
				censusEntry.tripnum = parseInteger(cols[2]);
				censusEntry.starttime = parseInteger(cols[3]);
				censusEntry.h_x = parseDouble(cols[4]);
				censusEntry.h_y = parseDouble(cols[5]);
				censusEntry.s_x = parseDouble(cols[6]);
				censusEntry.s_y = parseDouble(cols[7]);
				censusEntry.d_x = parseDouble(cols[8]);
				censusEntry.d_y = parseDouble(cols[9]);
				censusEntry.bike = parseInteger(cols[10]);
				censusEntry.age = parseInteger(cols[11]);
				censusEntry.gender = parseInteger(cols[12]);
				censusEntry.license = parseInteger(cols[13]);
				censusEntry.tickets = parseInteger(cols[14]);
				censusEntry.modechoice = parseInteger(cols[15]);
				censusEntry.caravailability = parseInteger(cols[16]);
				censusEntry.mobtools = parseInteger(cols[17]);
				censusEntry.inc1000 = parseDouble(cols[18]);
				censusEntry.day = parseInteger(cols[19]);
				censusEntry.tripmode = parseInteger(cols[20]);
				censusEntry.trippurpose = parseInteger(cols[21]);
				censusEntry.tripdistance = parseDouble(cols[22]);
				censusEntry.tripduration = parseInteger(cols[23]);
				censusEntry.id_tour = parseInteger(cols[24]);
								
				entries.add(censusEntry);
			}
			
			br.close();
			isr.close();
			fis.close();
    	}
    	catch (FileNotFoundException e) 
    	{
			e.printStackTrace();
		}
		catch (IOException e) 
		{
			e.printStackTrace();
		}
		
		return entries;
	}
	
	private int parseInteger(String string)
	{
		if (string == null) return 0;
		else if (string.trim().isEmpty()) return 0;
		else return Integer.valueOf(string);
	}
	
	private double parseDouble(String string)
	{
		if (string == null) return 0.0;
		else if (string.trim().isEmpty()) return 0.0;
		else return Double.valueOf(string);
	}
}
