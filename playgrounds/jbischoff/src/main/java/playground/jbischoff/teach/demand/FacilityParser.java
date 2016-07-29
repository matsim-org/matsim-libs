/**
 * 
 */
package playground.jbischoff.teach.demand;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.GeotoolsTransformation;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;

/**
 * @author Tille
 *
 */
	public class FacilityParser implements TabularFileHandler{
			
		private Map<String,Coord> facilityMap = new HashMap<String, Coord>();	
		CoordinateTransformation ct = new GeotoolsTransformation("EPSG:4326", "EPSG:32633"); 
		
		@Override
		public void startRow(String[] row) {
			try{
			Double x = Double.parseDouble(row[2]);
			Double y = Double.parseDouble(row[1]);
			Coord coords = new Coord(x,y);
			this.facilityMap.put(row[0],ct.transform(coords));
			}
			catch (NumberFormatException e){
				//skips line
			}
		}
		
		public Map<String, Coord> getFacilityMap() {
			return facilityMap;
		}
		
		
	}
