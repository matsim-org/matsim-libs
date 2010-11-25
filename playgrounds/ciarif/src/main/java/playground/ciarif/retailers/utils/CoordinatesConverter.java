package playground.ciarif.retailers.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.transformations.CH1903LV03toWGS84;

public class CoordinatesConverter {
	
	private List<Data> lines = null;
	private String header;
	private String separator = "\t";
//	private String separator = ",";
	private Charset charset = Charset.forName("ISO-8859-1");
	//private Charset charset = Charset.forName("UTF-8");
	
	private File inFile = new File("../../matsim/input/triangle/preprocess/chCoord.dat");
	private File outFile = new File("../../matsim/output/preprocess/outputCoord.txt");
	
	public static void main(String [] args) {
		Gbl.startMeasurement();
		final CoordinatesConverter converter = new CoordinatesConverter();
		converter.readFile();
		converter.writeFile();
		Gbl.printElapsedTime();
	}
	
	public List<Data> readFile()
	{
		FileInputStream fis = null;
		InputStreamReader isr = null;
	    BufferedReader br = null;
	       
    	try 
    	{
    		fis = new FileInputStream(inFile);
    		isr = new InputStreamReader(fis, charset);
			br = new BufferedReader(isr);
			
			// skip first Line
			header = br.readLine();
						
			lines = new ArrayList<Data>(); 
			String line;
			while((line = br.readLine()) != null)
			{
				String[] cols = line.split(separator);
				Data data = new Data();
				
				data.line = line;
				data.location = cols[0].trim();
				data.CH_Lon = cols[1].trim();
				data.CH_Lat = cols[2].trim();
				{
					CH1903LV03toWGS84 ChToWg = new CH1903LV03toWGS84();
					CoordImpl chCoord = new CoordImpl (data.CH_Lon, data.CH_Lat);
					CoordImpl wgCoord = (CoordImpl) ChToWg.transform(chCoord);
					data.WGS84_Lat = ((Double) wgCoord.getX()).toString();
					data.WGS84_Lon = ((Double) wgCoord.getY()).toString();
				}
				lines.add(data);
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
		
		return lines;
	}
	
	public void writeFile()
	{
		FileOutputStream fos = null; 
		OutputStreamWriter osw = null; 
	    BufferedWriter bw = null;
	    
	    
	    try 
	    {
			fos = new FileOutputStream(outFile);
			osw = new OutputStreamWriter(fos, charset);
			bw = new BufferedWriter(osw);
			
			// write Header
			bw.write("Location" + separator + "CH_Lat" + separator + "CH_Lon" + separator);
			//bw.write(header);
			bw.write("\n");
			
			// write Values
			for (Data data : lines)
			{	
				bw.write(data.location + separator);
				bw.write(data.WGS84_Lat + separator);
				bw.write(data.WGS84_Lon + separator);
				//bw.write(data.line + separator);
				bw.write("\n");
			}
			
			bw.close();
			osw.close();
			fos.close();
		}
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}
	
	public static class Data
	{
		public String CH_Lon = "";
		public String CH_Lat = "";
		
		public String line;
		public String location;
		public String WGS84_Lon;
		public String WGS84_Lat;
	
	}

}
