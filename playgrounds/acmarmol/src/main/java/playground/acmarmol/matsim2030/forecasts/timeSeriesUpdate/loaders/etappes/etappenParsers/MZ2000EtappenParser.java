package playground.acmarmol.matsim2030.forecasts.timeSeriesUpdate.loaders.etappes.etappenParsers;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.TreeMap;

import playground.acmarmol.matsim2030.forecasts.timeSeriesUpdate.loaders.etappes.Etappe;
import playground.acmarmol.matsim2030.microcensus2010.MZConstants;

/**
 * 
 * Parses the etappen.dat file from MZ2000
 *
 * @author acmarmol
 * 
 */


public class MZ2000EtappenParser {


	//////////////////////////////////////////////////////////////////////
	//member variables
	//////////////////////////////////////////////////////////////////////
		
		private TreeMap<String, ArrayList<Etappe>> etappes;

	//////////////////////////////////////////////////////////////////////
	//constructors
	//////////////////////////////////////////////////////////////////////

		public MZ2000EtappenParser(TreeMap<String, ArrayList<Etappe>> etappes) {
			super();
			this.etappes = etappes;
		}	




		public void parse(String etappenFile) throws Exception{

			FileReader fr = new FileReader(etappenFile);
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
				String person_weight = entries[4].trim();
				
				//etappe mode
				String mode = entries[13].trim();
				
				int modeInt = 0; //save mode as integer to be able to use hierarchy
				if(mode.equals("1")){modeInt = 15;}
				else if(mode.equals("2")){modeInt = 14;}
				else if(mode.equals("3")){modeInt = 13;}
				else if(mode.equals("4") || mode.equals("5") || mode.equals("23")){modeInt = 12;}
				else if(mode.equals("6") || mode.equals("7")){modeInt = 9;}
				else if(mode.equals("8")){modeInt = 2;}
				else if(mode.equals("9")){modeInt = 3;}
				else if(mode.equals("10")){modeInt = 6;}
				else if(mode.equals("11")){modeInt = 5;}
				else if(mode.equals("12")){modeInt = 11;}
				else if(mode.equals("13")){modeInt = 8;}
				else if(mode.equals("14")){modeInt = 10;}
				else if(mode.equals("15")){modeInt = 4;}
				else if(mode.equals("16")){modeInt = 1;}
				else if(mode.equals("17") || mode.equals("90") || mode.equals("99")){modeInt = 17;} else
					throw new RuntimeException("This should never happen!  Mode: " +  mode + " doesn't exist");
				
				if(mode.equals("1")){mode =  MZConstants.WALK;}
				else if(mode.equals("2")){mode =  MZConstants.BICYCLE;}
				else if(mode.equals("3")){mode =  MZConstants.MOFA;}
				else if(mode.equals("23")){mode =  MZConstants.KLEINMOTORRAD;}
				else if(mode.equals("4")){mode =  MZConstants.MOTORRAD_FAHRER;}
				else if(mode.equals("5")){mode =  MZConstants.MOTORRAD_MITFAHRER;}
				else if(mode.equals("6")){mode =  MZConstants.CAR_FAHRER;}
				else if(mode.equals("7")){mode =  MZConstants.CAR_MITFAHRER;}
				else if(mode.equals("8")){mode =  MZConstants.TRAIN;}
				else if(mode.equals("9")){mode =  MZConstants.POSTAUTO;}
				else if(mode.equals("10")){mode =  MZConstants.BUS;}
				else if(mode.equals("11")){mode =  MZConstants.TRAM;}
				else if(mode.equals("12")){mode =  MZConstants.TAXI;}
				else if(mode.equals("13")){mode =  MZConstants.REISECAR;}
				else if(mode.equals("14")){mode =  MZConstants.TRUCK;}
				else if(mode.equals("15")){mode =  MZConstants.SHIP;}
				else if(mode.equals("16")){mode =  MZConstants.PLANE;}
				else if(mode.equals("17")){mode =  MZConstants.CABLE_CAR;}
				else if(mode.equals("90")){mode =  MZConstants.OTHER;}
				else if(mode.equals("99")){mode =  MZConstants.NO_ANSWER;} else
					throw new RuntimeException("This should never happen!  Mode: " +  mode + " doesn't exist");
				

				
				//total people in car
				String total_people = entries[15].trim();
				if(total_people.equals("9")){total_people =  MZConstants.NO_ANSWER;}
				
				//purpose
				String purpose ="";
				String wzweck1 = entries[29].trim();
				if(wzweck1.equals("0")){purpose = MZConstants.CHANGE ;}
				else if(wzweck1.equals("1")){purpose =  MZConstants.WORK;}
				else if(wzweck1.equals("2")){purpose =  MZConstants.EDUCATION;}
				else if(wzweck1.equals("3")){purpose =  MZConstants.SHOPPING;}
				else if(wzweck1.equals("4")){purpose =  MZConstants.BUSINESS;}
				else if(wzweck1.equals("5")){purpose =  MZConstants.DIENSTFAHRT;}
				else if(wzweck1.equals("6")){purpose =  MZConstants.LEISURE;}
				else if(wzweck1.equals("7")){purpose =  MZConstants.ERRANDS;}
				else if(wzweck1.equals("8")){purpose = MZConstants.ACCOMPANYING_NOT_CHILDREN;}
				else if(wzweck1.equals("9")){purpose = MZConstants.NO_ANSWER;} else
					throw new RuntimeException("This should never happen!  Purpose wzweck1: " +  wzweck1 + " doesn't exist");
				
				//distance (no missing values)
				String distance = entries[16].trim();
				
				//duration (no missing values)
				String duration = entries[32]; 
		
				boolean skip = false;
				
				//ausland etappe?  
				String ausland = entries[34].trim();
				if(ausland.equals("1")){
					//skip=true;
				}
				
				
				
				if(!skip){
					//create new etappe
					Etappe etappe = new Etappe();
					etappe.setWeight(person_weight);
					etappe.setMode(mode);
					etappe.setTotalPeople(total_people);
					etappe.setPurpose(purpose);
					etappe.setDuration(duration);
					etappe.setDistance(distance);
					etappe.setWegeNr(wege_nr);
					etappe.setModeInteger(modeInt);
					
					
					
					//add it to list
					if(!this.etappes.containsKey(pid)){
						this.etappes.put(pid, new ArrayList<Etappe>());
					}
					this.etappes.get(pid).add(etappe);
				}				
				
				
			}
			
		}
		

}
			
