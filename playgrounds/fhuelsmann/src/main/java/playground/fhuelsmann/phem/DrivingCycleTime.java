package playground.fhuelsmann.phem;
import java.util.Map;
import java.util.TreeMap;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

public class DrivingCycleTime {

	
	
	private Map<String,Map<String,Map<String,String>>> dataStructure  =
		new TreeMap<String,Map<String,Map<String,String>>>();




	public Map<String, Map<String, Map<String, String>>> getDataStructure() {
		return dataStructure;
	}




	public void setDataStructure(
			Map<String, Map<String, Map<String, String>>> dataStructure) {
		this.dataStructure = dataStructure;
	}




	public void read(String file){
		
		File directory = new File(file);
				
		File files[] = directory.listFiles();// read all days
			for (File f : files) {
				Map<String,Map<String,String>> tempMap = 
					new TreeMap<String,Map<String,String>>();
				this.dataStructure.put(f.getName(), tempMap); // create a day , Date
				//System.out.println("Folder" + f.getName());
				File SubFiles[] = f.listFiles(); // excel Datei
					for (File excelDatei : SubFiles) { // read all excel files
						Map<String,String> tempMap1 = 
							new TreeMap<String,String>();
						String s = excelDatei.getName().split(".csv")[0];
						this.dataStructure.get(f.getName()).put(s, tempMap1);
						try{
							
							FileInputStream fstream = new FileInputStream(excelDatei);
						    // Get the object of DataInputStream
							//System.out.println("   Excel" + excelDatei.getName());

						    DataInputStream in = new DataInputStream(fstream);
					        BufferedReader br = new BufferedReader(new InputStreamReader(in));
						    String strLine;
						    //Read File Line By Line
						    br.readLine();
						    br.readLine();
						    br.readLine();

						    int count=1;
							    while ((strLine = br.readLine()) != null)   {
							    	//for all lines (whole text) we split the line to an array 
							    
							       	String[] array = strLine.split(",");
							       	
							       	
							       		Map<String,String> tempMap3 = new TreeMap<String,String>();
						    			tempMap3.put(array[0],""+ count);
				
						    			this.dataStructure.get(f.getName()).get(s).put( array[0],""+count);
							    		count++;
							       		
							       	
							       	
							    	
							    }
							    //Close the input stream
							    in.close();
						}catch (Exception e){//Catch exception if any
						      System.err.println("Error: " + e.getMessage());
						    }
				
			

			}// subFILES
		}// FILES
	}// read(String)
}//class
							
