package playground.acmarmol.matsim2030.microcensus2000;

import java.io.BufferedReader;
import java.io.FileReader;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.utils.objectattributes.ObjectAttributes;

import playground.acmarmol.matsim2030.microcensus2010.Etappe;
import playground.acmarmol.matsim2030.microcensus2010.MZConstants;

/**
 * 
 * Parses the etappen.dat file from MZ2005
 *
 * @author acmarmol
 * 
 */


public class MZ2000EtappenParser {


	//////////////////////////////////////////////////////////////////////
	//member variables
	//////////////////////////////////////////////////////////////////////
		
		private ObjectAttributes wegeAttributes;

	//////////////////////////////////////////////////////////////////////
	//constructors
	//////////////////////////////////////////////////////////////////////

		public MZ2000EtappenParser(ObjectAttributes wegeAttributes) {
			super();
			this.wegeAttributes = wegeAttributes;
		}	


	//////////////////////////////////////////////////////////////////////
	//private methods
	//////////////////////////////////////////////////////////////////////

		public void parse(String etappenFile) throws Exception{

			FileReader fr = new FileReader(etappenFile);
			BufferedReader br = new BufferedReader(fr);
			String curr_line = br.readLine(); // Skip header
							
			while ((curr_line = br.readLine()) != null) {
				
					
				String[] entries = curr_line.split("\t", -1);
					
				//household number
				String hhnr = entries[0].trim();
				
				//zielpnr number
				String zielpnr = entries[1].trim();
				
				//wege number
				String wegnr = entries[3].trim();
				
				//etappen number
				String etnr = entries[4].trim();
				
				String wid = (hhnr.concat(zielpnr)).concat("-").concat(wegnr);
				
				//wege mode
				String mode = entries[6].trim();
				int modeInt = 0; //save mode as integer to be able to use hierarchy
				if(mode.equals("1")){modeInt = 15;}
				else if(mode.equals("2")){modeInt = 14;}
				else if(mode.equals("3")){modeInt = 13;}
				else if(mode.equals("4") || mode.equals("5") || mode.equals("6")){modeInt = 12;}
				else if(mode.equals("7") || mode.equals("8")){modeInt = 9;}
				else if(mode.equals("9")){modeInt = 2;}
				else if(mode.equals("10")){modeInt = 3;}
				else if(mode.equals("11")){modeInt = 6;}
				else if(mode.equals("12")){modeInt = 5;}
				else if(mode.equals("13")){modeInt = 11;}
				else if(mode.equals("14")){modeInt = 8;}
				else if(mode.equals("15")){modeInt = 10;}
				else if(mode.equals("16")){modeInt = 4;}
				else if(mode.equals("17")){modeInt = 1;}
				else if(mode.equals("18") || mode.equals("20")){modeInt = 17;}
				else if(mode.equals("19")){modeInt = 16;}
				else if(mode.equals("-99")){modeInt = 99;} else
					throw new RuntimeException("This should never happen!  Mode: " +  mode + " doesn't exist");
				
//				//car type
				String carType = "";
//				String carType = entries[8].trim();
//				if(carType.equals("1")){carType = "household car";}
//				else if(carType.equals("2")){carType = "company car";}
//				else if(carType.equals("3")){carType = "rental car";}
//				else if(carType.equals("4")){carType = "car sharing";}
//				else if(carType.equals("5")){carType = "other";}
//				else if(carType.equals("-97")){carType = "not car mode!";}
//				else if(carType.equals("-98")){carType = MZConstants.UNSPECIFIED;}
//				else if(carType.equals("-99")){carType = MZConstants.NOT_KNOWN;}
//				else Gbl.errorMsg("This should never happen!  Mode: " +  mode + " doesn't exist");
				
				//start coordinate - CH1903 (23,24)
				Coord start_coord = new CoordImpl(entries[23].trim(),entries[24].trim());
				
						
				//end coordinate - CH1903 (34,35)
				Coord end_coord = new CoordImpl(entries[34].trim(),entries[35].trim());
				
				// departure time (min => sec.)
				int departure = Integer.parseInt(entries[9].trim())*60;
								
				// arrival time (min => sec.)
				int arrival;
				if(entries[71].trim().equals("")){
					arrival = departure;
				} else{
				   arrival = departure + Integer.parseInt(entries[71].trim())*60;
				}
				
				
				//start country and end country (for cross-border handling)
				String sland = entries[30].trim();
				String zland = entries[41].trim();
				
				
				int nr_etappen = (Integer) wegeAttributes.getAttribute(wid,MZConstants.NUMBER_STAGES)+1;
				this.wegeAttributes.putAttribute(wid, MZConstants.STAGE.concat( String.valueOf(nr_etappen)) , new Etappe(departure, arrival, start_coord, end_coord, modeInt, sland,zland, carType));
				this.wegeAttributes.putAttribute(wid, MZConstants.NUMBER_STAGES, nr_etappen);
				
				
				
			}
			
		}
}
			
