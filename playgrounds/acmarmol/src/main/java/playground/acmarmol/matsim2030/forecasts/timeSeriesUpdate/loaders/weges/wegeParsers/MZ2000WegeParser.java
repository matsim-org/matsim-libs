package playground.acmarmol.matsim2030.forecasts.timeSeriesUpdate.loaders.weges.wegeParsers;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.TreeMap;

import playground.acmarmol.matsim2030.forecasts.timeSeriesUpdate.loaders.weges.Wege;
import playground.acmarmol.matsim2030.microcensus2010.MZConstants;

/**
 * 
 * Parses the wegeinland.dat file from MZ2000
 *
 * @author acmarmol
 * 
 */


public class MZ2000WegeParser {


	//////////////////////////////////////////////////////////////////////
	//member variables
	//////////////////////////////////////////////////////////////////////
		
		private TreeMap<String, ArrayList<Wege>> weges;

	//////////////////////////////////////////////////////////////////////
	//constructors
	//////////////////////////////////////////////////////////////////////

		public MZ2000WegeParser(TreeMap<String, ArrayList<Wege>> weges) {
			super();
			this.weges = weges;
		}	




		public void parse(String wegeFile) throws Exception{

			FileReader fr = new FileReader(wegeFile);
			BufferedReader br = new BufferedReader(fr);
			String curr_line = br.readLine(); // Skip header	
							
			while ((curr_line = br.readLine()) != null) {
				
					
				String[] entries = curr_line.split("\t", -1);
				
				//household number
				String hhnr = entries[1].trim();
							
				//person number (intnr)
				String pid = entries[0].trim();
				
				//wege number
				int wege_nr = Integer.parseInt(entries[2].trim());
				
				
				//WP
				String person_weight = entries[3].trim();

				
				//purpose
				String purpose ="";
				String wzweck1 = entries[11].trim();
				if(wzweck1.equals("0")){purpose = MZConstants.CHANGE ;}
				else if(wzweck1.equals("1")){purpose =  MZConstants.WORK;}
				else if(wzweck1.equals("2")){purpose =  MZConstants.EDUCATION;}
				else if(wzweck1.equals("3")){purpose =  MZConstants.SHOPPING;}
				else if(wzweck1.equals("4")){purpose =  MZConstants.BUSINESS;}
				else if(wzweck1.equals("5")){purpose =  MZConstants.DIENSTFAHRT;}
				else if(wzweck1.equals("6")){purpose =  MZConstants.LEISURE;}
				else if(wzweck1.equals("7")){purpose =  MZConstants.ERRANDS;}
				else if(wzweck1.equals("8")){purpose = MZConstants.ACCOMPANYING;}
				else if(wzweck1.equals("9")){purpose=  MZConstants.NO_ANSWER;} else
					throw new RuntimeException("This should never happen!  Purpose wzweck1: " +  wzweck1 + " doesn't exist");
				
				//distance (no missing values)
				String distance = entries[5].trim();
				
				//duration (no missing values)
				String duration = entries[7]; 
		
				boolean skip = false;
				
				
					//create new etappe
					Wege wege = new Wege();
					wege.setWeight(person_weight);
					wege.setPurpose(purpose);
					wege.setDuration(duration);
					wege.setDistance(distance);
					wege.setWegeNr(wege_nr);
					
					
					
					//add it to list
					if(!this.weges.containsKey(pid)){
						this.weges.put(pid, new ArrayList<Wege>());
					}
					this.weges.get(pid).add(wege);
								
				
				
			}
			
		}
		

}
			
