package playground.acmarmol.matsim2030.forecasts.mz2010sample;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.io.IOUtils;

import playground.acmarmol.matsim2030.forecasts.p2030preparation.Municipalities;
import playground.acmarmol.matsim2030.forecasts.p2030preparation.Municipality;


public class PopulationTotals {

	/**
	 * Just rewrites the municipalities' total population that were extracted from BFS website into "total_population_municipalities.txt"
	 * with a more clean format (tab separated) for posterior use.
	 */
	public static void main(String[] args) throws IOException {
		
		Municipalities municipalities_web = new Municipalities();
		
		String inputBase = "C:/local/marmolea/input/Activity Chains Forecast/";
		String outputBase = "C:/local/marmolea/output/Activity Chains Forecast/";
		
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(inputBase + "total_population_municipalities.txt"), "ISO-8859-1"));
		 BufferedWriter out = IOUtils.getBufferedWriter(outputBase + "municipality_population.txt");
		
		String curr_line = br.readLine(); // Skip header
		
		while ((curr_line = br.readLine()) != null) {
		String gem_nr = null;
		String name = null;
		String population = null;
		
			
			String[] entries = curr_line.split("\t", -1);
			if(entries[0].contains("......")){
				gem_nr = entries[0].substring(entries[0].lastIndexOf("......")+6, entries[0].lastIndexOf("......")+10);
				name = entries[0].substring(entries[0].lastIndexOf("......")+11).trim();
				population = entries[1].replaceAll("\\s","");
			
				Municipality municipality = new Municipality(Id.create(gem_nr, Municipality.class));
				municipality.setName(name);
				municipalities_web.addMunicipality(municipality);
				
				
				out.write(gem_nr +"\t");
				out.write(name +"\t");
				out.write(population +"\t");
				out.newLine();

				
			}
			else{System.out.println(entries[0]);}
			
		}
		
		out.flush();
		out.close();
	
	}

}
