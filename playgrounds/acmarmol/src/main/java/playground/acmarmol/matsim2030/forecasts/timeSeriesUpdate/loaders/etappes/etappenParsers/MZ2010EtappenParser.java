package playground.acmarmol.matsim2030.forecasts.timeSeriesUpdate.loaders.etappes.etappenParsers;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.TreeMap;

import playground.acmarmol.matsim2030.forecasts.timeSeriesUpdate.loaders.etappes.Etappe;
import playground.acmarmol.matsim2030.microcensus2010.MZConstants;

/**
 * 
 * Parses the etappen.dat file from MZ2010
 *
 * @author acmarmol
 * 
 */


public class MZ2010EtappenParser {


	//////////////////////////////////////////////////////////////////////
	//member variables
	//////////////////////////////////////////////////////////////////////
		
		private TreeMap<String, ArrayList<Etappe>> etappes;

	//////////////////////////////////////////////////////////////////////
	//constructors
	//////////////////////////////////////////////////////////////////////

		public MZ2010EtappenParser(TreeMap<String, ArrayList<Etappe>> etappes) {
			super();
			this.etappes = etappes;
		}	




		public void parse(String etappenFile) throws IOException {

			FileReader fr = new FileReader(etappenFile);
			BufferedReader br = new BufferedReader(fr);
			String curr_line = br.readLine(); // Skip header
							
			while ((curr_line = br.readLine()) != null) {
				
					
				String[] entries = curr_line.split("\t", -1);
				
				//household number
				String hhnr = entries[0].trim();
							
				//person number (zielpnr)
				String zielpnr = entries[1].trim();
				String pid = hhnr.concat(zielpnr);
				
				//wege number
				int wege_nr = Integer.parseInt(entries[3].trim());
				
				//WP
				String person_weight = entries[2].trim();
				
				//etappe mode
				String mode = entries[7].trim();
				if(mode.equals("1")){mode =  MZConstants.WALK;}
				else if(mode.equals("2")){mode =  MZConstants.BICYCLE;}
				else if(mode.equals("3")){mode =  MZConstants.MOFA;}
				else if(mode.equals("4")){mode =  MZConstants.KLEINMOTORRAD;}
				else if(mode.equals("5")){mode =  MZConstants.MOTORRAD_FAHRER;}
				else if(mode.equals("6")){mode =  MZConstants.MOTORRAD_MITFAHRER;}
				else if(mode.equals("7")){mode =  MZConstants.CAR_FAHRER;}
				else if(mode.equals("8")){mode =  MZConstants.CAR_MITFAHRER;}
				else if(mode.equals("9")){mode =  MZConstants.TRAIN;}
				else if(mode.equals("10")){mode =  MZConstants.POSTAUTO;}
				else if(mode.equals("11")){mode =  MZConstants.BUS;}
				else if(mode.equals("12")){mode =  MZConstants.TRAM;}
				else if(mode.equals("13")){mode =  MZConstants.TAXI;}
				else if(mode.equals("14")){mode =  MZConstants.REISECAR;}
				else if(mode.equals("15")){mode =  MZConstants.TRUCK;}
				else if(mode.equals("16")){mode =  MZConstants.SHIP;}
				else if(mode.equals("17")){mode =  MZConstants.PLANE;}
				else if(mode.equals("18")){mode =  MZConstants.CABLE_CAR;}
				else if(mode.equals("19")){mode =  MZConstants.SKATEBOARD;}
				else if(mode.equals("20")){mode =  MZConstants.OTHER;}
				else if(mode.equals("-99")){mode =  MZConstants.PSEUDOETAPPE;} else
					throw new RuntimeException("This should never happen!  Mode: " +  mode + " doesn't exist");
				
				//total people in car
				String total_people = entries[10].trim();
				
				//purpose
				String purpose ="";
				String wzweck1 = entries[69].trim();
				if(wzweck1.equals("1")){purpose = MZConstants.CHANGE ;}
				else if(wzweck1.equals("2")){purpose =  MZConstants.WORK;}
				else if(wzweck1.equals("3")){purpose =  MZConstants.EDUCATION;}
				else if(wzweck1.equals("4")){purpose =  MZConstants.SHOPPING;}
				else if(wzweck1.equals("5")){purpose =  MZConstants.ERRANDS;}
				else if(wzweck1.equals("6")){purpose =  MZConstants.BUSINESS;}
				else if(wzweck1.equals("7")){purpose =  MZConstants.DIENSTFAHRT;}
				else if(wzweck1.equals("8")){purpose =  MZConstants.LEISURE;}
				else if(wzweck1.equals("9")){purpose =  MZConstants.ACCOMPANYING_CHILDREN;}
				else if(wzweck1.equals("10")){purpose = MZConstants.ACCOMPANYING_NOT_CHILDREN;}
				else if(wzweck1.equals("11")){purpose=  MZConstants.FOREIGN_PROPERTY;}
				else if(wzweck1.equals("12")){purpose =  MZConstants.OTHER;}
				else if(wzweck1.equals("13")){purpose = MZConstants.BORDER_CROSSING;}
				else if(wzweck1.equals("-99")){purpose = MZConstants.PSEUDOETAPPE;} else
					throw new RuntimeException("This should never happen!  Purpose wzweck1: " +  wzweck1 + " doesn't exist");
				
				//distance
				boolean skip = false;//skip etappe if no distance can be estimated <1%
				double distance = Double.parseDouble(entries[113].trim());
				if(distance == -99){ //S_QAL <> 1 oder Z_QAL <>1. Although precision is not the best, duration will anyway be computed as
											//the euclidian distance
					double  xs = Double.parseDouble(entries[31]); 
					double  ys = Double.parseDouble(entries[32]); 
					double  xz = Double.parseDouble(entries[51]); 
					double  yz = Double.parseDouble(entries[52]); 
					
					if(xs!=-97 && ys!=-97 && xz!=-97 && yz!=-97){				
						distance = Math.sqrt(Math.pow((xz-xs)/1000, 2) + Math.pow((yz-ys)/1000, 2)); 
					}else{
						skip = true;
					}
				}
				
				//duration
				String duration = entries[97];
				if(duration.equals("-99")){ //pseudoetappe
					skip = true;
				}
				
				//ausland etappe?  
				String ausland = entries[114].trim();
				if(ausland.equals("1")){
					skip=true;
				}
				
				//create new etappe
				if(!skip){
					Etappe etappe = new Etappe();
					etappe.setWeight(person_weight);
					etappe.setMode(mode);
					etappe.setTotalPeople(total_people);
					etappe.setPurpose(purpose);
					etappe.setDuration(duration);
					etappe.setDistance(String.valueOf(distance));
					etappe.setWegeNr(wege_nr);
					
					
					//add it to list
					if(!this.etappes.containsKey(pid)){
						this.etappes.put(pid, new ArrayList<Etappe>());
					}
					this.etappes.get(pid).add(etappe);
				}
				
				
			}
			
		}

}
			
