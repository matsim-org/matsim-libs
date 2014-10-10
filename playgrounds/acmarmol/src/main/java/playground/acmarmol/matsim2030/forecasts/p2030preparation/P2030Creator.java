package playground.acmarmol.matsim2030.forecasts.p2030preparation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.io.IOUtils;

import playground.acmarmol.matsim2030.forecasts.Loader;
import playground.acmarmol.utils.MyCollectionUtils;

public class P2030Creator {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		
		Municipalities municipalities = new Municipalities();
		Loader.loadARE2030MunicipalityTotals(municipalities);
		createNewP2030(municipalities);
		//write(municipalities);
		
		System.out.println(municipalities.getMunicipalities().size());

	}

	private static void createNewP2030(Municipalities municipalities) throws IOException {
		
		String inputBase = "C:/local/marmolea/input/Activity Chains Forecast/";
		String outputBase = "C:/local/marmolea/output/Activity Chains Forecast/";
		
		
		Municipalities oldMunicipalities = Loader.loadOldP2030Totals(); //old group totals (<25, 25<55, >55)
		TreeMap<String, Integer> gem_type = Loader.loadGemeindetypologieARE(inputBase + "Gemeindetypen_ARE_2010_VZ.txt");
		HashMap<String, String> mun_changes = Loader.loadEliminatedMunicipalitiesDatabase(inputBase + "eliminated municipalities 01.01.2000 - 31.12.2010.txt");
				
		String inputfile =  inputBase + "P2030/P2030Original.txt";
		String outputfile = outputBase + "P2030/NEWP2030.txt";
		
		BufferedWriter out = IOUtils.getBufferedWriter(outputfile);
		

		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(inputfile), "ISO-8859-1"));
		String curr_line = br.readLine(); // Skip header
		out.write(curr_line);
		out.newLine();

		
			while ((curr_line = br.readLine()) != null) {
				
				String[] entries = curr_line.split("\t", -1);
				
				Id<Municipality> id = Id.create(entries[1], Municipality.class);
				
				//update group size according to new totals
				int group = Integer.parseInt(entries[5]);
				double group_size = Double.parseDouble(entries[7]);
				if(group<=6){
					entries[7] = String.valueOf(group_size/oldMunicipalities.getMunicipality(id).getPopulation()[0]*municipalities.getMunicipality(id).getPopulation()[0]);
				}else if(group<=12){
					entries[7] = String.valueOf(group_size/oldMunicipalities.getMunicipality(id).getPopulation()[1]*municipalities.getMunicipality(id).getPopulation()[1]);
				}else{
					entries[7] = String.valueOf(group_size/oldMunicipalities.getMunicipality(id).getPopulation()[2]*municipalities.getMunicipality(id).getPopulation()[2]);
				}

				
				//update number of M and F according to new totals, using old distributions
				
				double m_share = Double.parseDouble(entries[8])/group_size;
				double f_share = Double.parseDouble(entries[9])/group_size;
				
				entries[8] = String.valueOf(m_share*Double.parseDouble(entries[7]));
				entries[9] = String.valueOf(f_share*Double.parseDouble(entries[7]));
				
				entries[12] = String.valueOf(MyCollectionUtils.sum(municipalities.getMunicipality(id).getPopulation()));
				entries[13] = String.valueOf(MyCollectionUtils.sum(municipalities.getMunicipality(id).getPopulation())/1000);		
				entries[14] = String.valueOf(Math.log(MyCollectionUtils.sum(municipalities.getMunicipality(id).getPopulation())));
				
				
				String gem_nr = entries[1];
				if(mun_changes.containsKey(gem_nr)){
				gem_nr = mun_changes.get(gem_nr).toString();
				entries[1]= gem_nr;
				}
				
				for(int i=0;i<6;i++){
					entries[23+i]="0";
				}
				
							
				
				int gem_type_nr = gem_type.get(gem_nr);
				entries[22] = String.valueOf(gem_type_nr);
				entries[22 + gem_type_nr] = "1";
				
				out.write(MyCollectionUtils.arrayToTabSeparatedString(entries));
				out.newLine();
			}
		
		out.flush();	
		out.close();	
	}


	
	
	public static void write(Municipalities municipalities) throws IOException{
		
		String outputBase = "C:/local/marmolea/output/Activity Chains Forecast/P2030/";
		BufferedWriter out = IOUtils.getBufferedWriter(outputBase+"P2030New.txt");
			 
		out.write("ID \t NAME \t  <25 \t  25<55 \t >55 ");
		out.newLine();
		
		 for(Id<Municipality> id : municipalities.getMunicipalities().keySet()) {
			 Municipality m = municipalities.getMunicipality(id);
			 out.write(m.getId() + "\t" + m.getName() +"\t" + m.getPopulation()[0] +"\t" + m.getPopulation()[1] +"\t" + m.getPopulation()[2] );
			 out.newLine();
		     }
		
		
		
		
		out.close();
		
		
	}
	


}
