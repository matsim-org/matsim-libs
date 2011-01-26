package playground.fhuelsmann.phem;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.TreeMap;


public class DepatureTime {

	
	private Map<String,Map<String,String>> dataStructure  =
		new TreeMap<String,Map<String,String>>();

	public void read(String file){
		
		File directory = new File(file);
				
		File files[] = directory.listFiles();// read all days
			for (File f : files) {
				Map<String,String> tempMap = 
					new TreeMap<String,String>();
				this.dataStructure.put(f.getName(), tempMap); // create a day , Date
				System.out.println("Folder" + f.getName());
				File SubFiles[] = f.listFiles(); // text Datei
					for (File txtDatei : SubFiles) {
						
					try{
							FileInputStream fstream = new FileInputStream(txtDatei);
						    // Get the object of DataInputStream
							System.out.println("   .Text" + txtDatei.getName());

						    DataInputStream in = new DataInputStream(fstream);
					        BufferedReader br = new BufferedReader(new InputStreamReader(in));
						    String strLine;
						    //Read File Line By Line
						br.readLine();    
							    while ((strLine = br.readLine()) != null)   {
							    	//for all lines (whole text) we split the line to an array 
							    
							       	String[] array = strLine.split(";");
							       	
							       	
				
						    			this.dataStructure.get(f.getName()).put(array[1],array[2]+";"+array[0]);
							    		
							       		
							       	
							       	
							    	
							    }
							    //Close the input stream
							    in.close();
						}catch (Exception e){//Catch exception if any
						      System.err.println("Error: " + e.getMessage());
						    }
				
						
						
					}
				
				
				
			}
			
	}

	public Map<String, Map<String, String>> getDataStructure() {
		return dataStructure;
	}

	public void setDataStructure(Map<String, Map<String, String>> dataStructure) {
		this.dataStructure = dataStructure;
	}

	
}
