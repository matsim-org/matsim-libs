package playground.acmarmol.matsim2030.forecasts.p2030preparation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;

import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.io.IOUtils;



public class OldP2030{

	private Municipalities municipalities;
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
	
		String inputBase = "C:/local/marmolea/input/Activity Chains Forecast/P2030/";
		String outputBase = "C:/local/marmolea/output/Activity Chains Forecast/P2030/";
		
		OldP2030 oldP2030 = new OldP2030();

		oldP2030.read(inputBase + "P2030Original.txt");
		oldP2030.write(outputBase + "P2030Old.txt");
		


	}
	
	public OldP2030(){
		this.municipalities = new Municipalities();
	}

	
	public void read(String filename) throws IOException{
		
		
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "ISO-8859-1"));
		String curr_line = br.readLine(); // Skip header
		
			while ((curr_line = br.readLine()) != null) {
				
				String[] entries = curr_line.split("\t", -1);
				Municipality municipality;
				
				
//				int original_length = entries[1].length();
//				for(int i=1; i<=4-original_length;i++){
//					entries[1]= "0".concat(entries[1]);
//				}
				
				Id<Municipality> id = Id.create(entries[1], Municipality.class);
				if(!municipalities.getMunicipalities().containsKey(id)){
					municipality = new Municipality(id);
					municipality.setName(entries[3]);
					municipalities.addMunicipality(municipality);
					
				}else{
					municipality = municipalities.getMunicipalities().get(id);
				}
				
				double total_gr = Double.parseDouble(entries[7]);
				int group = Integer.parseInt(entries[5]);
				
				if(group<=6){
					municipality.getPopulation()[0] += total_gr;
				}else if(group<=12){
					municipality.getPopulation()[1] += total_gr;
				}else{
					municipality.getPopulation()[2] += total_gr;
				}
					
				
				
			}
		
		
	}
	
	public void write(String filename) throws IOException{
		
		BufferedWriter out = IOUtils.getBufferedWriter(filename);
		
		
		Iterator<Id<Municipality>> it = municipalities.getMunicipalities().keySet().iterator();
		 
		out.write("ID \t NAME \t  <25 \t  25<55 \t >55 ");
		out.newLine();
		
		 while (it.hasNext()) {
			 Municipality m = municipalities.getMunicipality(it.next());
			 out.write(m.getId() + "\t" + m.getName() +"\t" + m.getPopulation()[0] +"\t" + m.getPopulation()[1] +"\t" + m.getPopulation()[2] );
			 out.newLine();
		        it.remove(); // avoids a ConcurrentModificationException
		    }
		
		
		
		
		out.close();
		
		
	}
	
	public Municipalities getMunicipalitiesObject(){
		return this.municipalities;
	}
	
	
	
}
