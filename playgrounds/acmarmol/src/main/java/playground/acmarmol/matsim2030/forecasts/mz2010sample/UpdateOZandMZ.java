package playground.acmarmol.matsim2030.forecasts.mz2010sample;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;

import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;

import playground.acmarmol.matsim2030.forecasts.Loader;
import playground.acmarmol.utils.MyCollectionUtils;

public class UpdateOZandMZ {

	/**
	 * @author acmarmol
	 * 
	 * Updates the municipalities' information of Oberzentrum and Mittelzentrum obtained from an old database (~2000),
	 * according to the new municipality constitutions until 2010 (http://www.portal-stat.admin.ch/gde-tool/core/xshared/gewo.php?lng=de-de)
	 * to be used in the creation of the MZ2010 database for model estimation
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		// STEP1: Read and save in a HashMap the new constitution
		// STEP2: Update the Gemeindezuordnung MZ+OZ_ARE.txt adding the information for the new municipalities
		//it is assumed that if mun. A and mun. B are merged together to form mun. C, then:
		//	i) mun. A and mun. B have the same OZ and MZ
		//	ii) mun C. will have the same OZ and MZ.
		
		
		//--------------
		//----STEP1-----
		//--------------
		
		String base = "C:/local/marmolea/input/Activity Chains Forecast/";
				
		
		BufferedWriter out = IOUtils.getBufferedWriter(base + "updated Gemeindezuordnung MZ+OZ_ARE.txt");
			
		HashMap<String, Tuple<String, String>> code_pairs = Loader.loadCreatedMunicipalitiesDatabase(base + "municipalities/nuovi costituzione 01.01.2000-31.12.2010.txt");
		
		System.out.println(code_pairs.size());

		//--------------
		//----STEP2-----
		//--------------
		
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(base + "Gemeindezuordnung MZ+OZ_ARE.txt"), "ISO-8859-1"));
				
		 String curr_line = br.readLine(); // Skip header
		 out.write(curr_line);
		 out.newLine();
		 
		 String code;
		 HashSet<String> updated = new HashSet<String>();
		 
		 int counter = 0;
		 
		 while ((curr_line = br.readLine()) != null) {
				

				
				String[] entries = curr_line.split("\t", -1);
				
				code = entries[2];
				
			 	out.write(curr_line);
				out.newLine();

				
				if(code_pairs.containsKey(code)){
					
					String new_code = code_pairs.get(code).getFirst();
					String new_name = code_pairs.get(code).getSecond();
					
					if(!updated.contains(new_code)){
						
						updated.add(new_code);
						entries[2] = new_code;
						entries[3] = new_name;
							
						curr_line = MyCollectionUtils.arrayToTabSeparatedString(entries);
						out.write(curr_line);
						out.newLine();
						counter++;
						
						
					}
					
					
					
				}else{
				 	out.write(curr_line);
					out.newLine();
				}
				
			
				
		 }
		
		
		
		out.close();
		System.out.println(counter);
		
		
		
		
	}

}
