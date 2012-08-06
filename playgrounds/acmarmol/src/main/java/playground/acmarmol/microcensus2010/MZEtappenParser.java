package playground.acmarmol.microcensus2010;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.households.Households;
import org.matsim.utils.objectattributes.ObjectAttributes;

/**
 * 
 * Parses the etappen.dat file from MZ2010
 *
 * @author acmarmol
 * 
 */


public class MZEtappenParser {


	//////////////////////////////////////////////////////////////////////
	//member variables
	//////////////////////////////////////////////////////////////////////
		
		private ObjectAttributes wegeAttributes;
		
		private static final String UNSPECIFIED = "unspecified";
		private static final String NOT_KNOWN = "not known";


	//////////////////////////////////////////////////////////////////////
	//constructors
	//////////////////////////////////////////////////////////////////////

		public MZEtappenParser(ObjectAttributes wegeAttributes) {
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
				
				Id wid = new IdImpl((hhnr.concat(zielpnr)).concat("-").concat(wegnr));
				
				//wege mode
				String mode = entries[7].trim();
				if(mode.equals("1")){mode = "walk";}
				else if(mode.equals("2")){mode = "bicycle";}
				else if(mode.equals("3")){mode = "mofa";}
				else if(mode.equals("4") || mode.equals("5") || mode.equals("6")){mode = "motorcycle";}
				else if(mode.equals("7") || mode.equals("8")){mode = "car";}
				else if(mode.equals("9")){mode = "train";}
				else if(mode.equals("10")){mode = "postauto";}
				else if(mode.equals("11")){mode = "bus";}
				else if(mode.equals("12")){mode = "tram";}
				else if(mode.equals("13")){mode = "taxi";}
				else if(mode.equals("14")){mode = "reisecar";}
				else if(mode.equals("15")){mode = "truck";}
				else if(mode.equals("16")){mode = "ship";}
				else if(mode.equals("17")){mode = "plane";}
				else if(mode.equals("18") || mode.equals("20")){mode = "other";}
				else if(mode.equals("19")){mode = "plane";}
				else if(mode.equals("20")){mode = "skateboard/skates";}
				else if(mode.equals("-99")){mode = "Pseudoetappe";}
				else Gbl.errorMsg("This should never happen!  Mode: " +  mode + " doesn't exist");
				
				//car type
				String carType = entries[8].trim();
				if(carType.equals("1")){carType = "household car";}
				else if(carType.equals("2")){carType = "company car";}
				else if(carType.equals("3")){carType = "rental car";}
				else if(carType.equals("4")){carType = "car sharing";}
				else if(carType.equals("5")){carType = "other";}
				else if(carType.equals("-97")){carType = "not car mode!";}
				else if(carType.equals("-98")){carType = UNSPECIFIED;}
				else if(carType.equals("-99")){carType = NOT_KNOWN;}
				else Gbl.errorMsg("This should never happen!  Mode: " +  mode + " doesn't exist");
				
				//start coordinate - WGS84 (29,30) & CH1903 (31,32)
				Coord start_coord = new CoordImpl(entries[31].trim(),entries[32].trim());
				
						
				//end coordinate (round to hectare) - WGS84 (49,50) & CH1903 (51,52)
				Coord end_coord = new CoordImpl(entries[51].trim(),entries[52].trim());
				
				// departure time (min => sec.)
				int departure = Integer.parseInt(entries[12].trim())*60;
								
				// arrival time (min => sec.)
				int arrival = Integer.parseInt(entries[14].trim())*60;
				
				int nr_etappen = (Integer) wegeAttributes.getAttribute(wid.toString(),"number of etappen")+1;
				this.wegeAttributes.putAttribute(wid.toString(), "etappe".concat( String.valueOf(nr_etappen)) , new Etappe(departure, arrival, start_coord, end_coord, mode));
				this.wegeAttributes.putAttribute(wid.toString(), "number of etappen", nr_etappen);
				
				
				
			}
			
		}
}
			
