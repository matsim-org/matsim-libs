package playground.fhuelsmann.phem;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.TreeMap;


public class ModaleOutput {

	private Map<String,Map<String,Map<String,Map<String,Map<String,ModaleObject>>>>> ModaleOutputDataStructure =
		new TreeMap<String,Map<String,Map<String,Map<String,Map<String,ModaleObject>>>>>();

	
	public void read(String file){
		
		File directory = new File(file);
				
		File files[] = directory.listFiles();// read all days
			for (File f : files) {
				Map<String,Map<String,Map<String,Map<String,ModaleObject>>>> tempMap = 
					new TreeMap<String,Map<String,Map<String,Map<String,ModaleObject>>>>();
				this.ModaleOutputDataStructure.put(f.getName(), tempMap); // create a day , Date
				File SubFiles[] = f.listFiles(); // excel Datei
					for (File SATDatei : SubFiles) { // read all excel files
						String eu="";
						String GD= SATDatei.getName().split("_")[2];
						//String time = SATDatei.getName().split("_")[3].split(".sta")[0];
			            String time="";
						if (SATDatei.getName().split("_")[3].equals("DPF")){
                            time = SATDatei.getName().split("_")[4].split(".sta")[0];
    						 eu= SATDatei.getName().split("_")[1]+"DPF";


                      }
                      else{
                            time = SATDatei.getName().split("_")[3].split(".sta")[0];
    						 eu= SATDatei.getName().split("_")[1];

                      }
	
					try{
							
							FileInputStream fstream = new FileInputStream(SATDatei);
						    // Get the object of DataInputStream
							//System.out.println("   Excel" + excelDatei.getName());

						    DataInputStream in = new DataInputStream(fstream);
					        BufferedReader br = new BufferedReader(new InputStreamReader(in));
						    String strLine;
						    //Read File Line By Line
						    
						    
						    	br.readLine();br.readLine();br.readLine();br.readLine();
						    	br.readLine();br.readLine();br.readLine();br.readLine();
						    	br.readLine();br.readLine();br.readLine();br.readLine();
						    	br.readLine();br.readLine();br.readLine();br.readLine();
						    	br.readLine();br.readLine();br.readLine();br.readLine();
						    	br.readLine();br.readLine();br.readLine();br.readLine();
						    	br.readLine();br.readLine();br.readLine();br.readLine();
						    	br.readLine();br.readLine();br.readLine();br.readLine();
						    	br.readLine();br.readLine();br.readLine();br.readLine();
						    	br.readLine();br.readLine();br.readLine();br.readLine();
						    	br.readLine();br.readLine();br.readLine();br.readLine();
						    	br.readLine();br.readLine();br.readLine();br.readLine();
						    	br.readLine();br.readLine();br.readLine();



						    
							    while ((strLine = br.readLine()) != null)   {
							    	//for all lines (whole text) we split the line to an array 
							    
							       	String[] array = strLine.split(",");
							       	
							       		ModaleObject object = new ModaleObject(
							       				array[0],
							       				array[1],
							       				array[2],
							       				array[3],
							       				array[4],
							       				array[5],
							       				array[6],
							       				array[7],
							       				array[8]);
							       		
		
							       		
										if (this.ModaleOutputDataStructure.get(f.getName()).get(GD) != null){

												if(this.ModaleOutputDataStructure.get(f.getName()).get(GD).get(eu)!=null){
													
													if (this.ModaleOutputDataStructure.get(f.getName()).get(GD).get(eu).get(time) == null) {
														
														Map<String,ModaleObject> tempMap1 = // sec,object 
															new TreeMap<String,ModaleObject>();
														
														Map<String,Map<String,ModaleObject>> tempMap2 = // zeit,sec,object 
														
															new TreeMap<String,Map<String,ModaleObject>>();
														
														tempMap1.put(array[0], object);
														
														this.ModaleOutputDataStructure.get(f.getName()).get(GD).get(eu).put(time, tempMap1);

													}
													else{
														
														this.ModaleOutputDataStructure.get(f.getName()).get(GD).get(eu).get(time).put(array[0], object);

												}
											}
												
										else if(this.ModaleOutputDataStructure.get(f.getName()).get(GD).get(eu)==null ){			//if(this.ModaleOutputDataStructure.get(GD).get(eu) !=null){
													
												Map<String,Map<String,ModaleObject>> tempMapOfTime = 
													new TreeMap<String,Map<String,ModaleObject>>();
												Map<String,ModaleObject> tempMapOfTime1 = 
													new TreeMap<String,ModaleObject>();
												tempMapOfTime1.put(array[0], object);
												tempMapOfTime.put(time, tempMapOfTime1);
												this.ModaleOutputDataStructure.get(f.getName()).get(GD).put(eu, tempMapOfTime);
											}
										}
										else if (this.ModaleOutputDataStructure.get(GD) == null){
										

											Map<String,ModaleObject> tempMap1 = // sec,object 
												new TreeMap<String,ModaleObject>();
											
											Map<String,Map<String,ModaleObject>> tempMap2 = // zeit,sec,object 
											
												new TreeMap<String,Map<String,ModaleObject>>();
											
											
											Map<String,Map<String,Map<String,ModaleObject>>> tempMap3 = 
												new TreeMap<String,Map<String,Map<String,ModaleObject>>>(); // eu,zeit,sec,object
											
											tempMap1.put(array[0], object);
											tempMap2.put(time, tempMap1);
											tempMap3.put(eu, tempMap2);
											this.ModaleOutputDataStructure.get(f.getName()).put(GD, tempMap3);
											
										}
										//System.out.println("Sucess     " + f.getName() + "   "+ SATDatei.getName() +"  " + time );

							    }
							    //Close the input stream
							    in.close();
						}catch (Exception e){//Catch exception if any
						     
							
							System.err.println(e +"     " + f.getName() + "   "+ SATDatei.getName() +"  " + time );
						    }
				
			

			}// subFILES
		}// FILES
	}// read(String)


	public Map<String, Map<String, Map<String, Map<String, Map<String, ModaleObject>>>>> getModaleOutputDataStructure() {
		return ModaleOutputDataStructure;
	}


	public void setModaleOutputDataStructure(
			Map<String, Map<String, Map<String, Map<String, Map<String, ModaleObject>>>>> modaleOutputDataStructure) {
		ModaleOutputDataStructure = modaleOutputDataStructure;
	}


}
