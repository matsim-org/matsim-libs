/**
 * 
 */
package playground.kai.urbansim;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.BasicLeg.Mode;
import org.matsim.population.Act;
import org.matsim.population.Plan;
import org.matsim.utils.geometry.Coord;
import org.matsim.utils.io.IOUtils;

/**
 * Various standalone utilities that don't need to be inside a specific package
 * 
 * @author nagel
 *
 */
public class Utils {

	public static void readKV( Map<Id,Id> yFromX, String key, IdBuilder keyIdBuilder, String value, IdBuilder valueIdBuilder, String filename ) {
		try {
			BufferedReader reader = IOUtils.getBufferedReader( filename ) ;
	
			String header = reader.readLine() ;
			Map<String,Integer> idxFromKey = Utils.createIdxFromKey( header ) ;
	
			String line = reader.readLine() ;
			while ( line != null ) {
				String[] parts = line.split("[\t\n]+");
	
				int idx = idxFromKey.get(value) ;
				Id valueId = valueIdBuilder.createId( parts[idx] ) ;
	
				idx = idxFromKey.get(key) ;
				Id keyId = keyIdBuilder.createId( parts[idx] ) ;
	
				yFromX.put(keyId, valueId) ;
	
				line = reader.readLine() ;
			}
	
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	
	}

//	public static void constructAFromC( Map<Id,Id> aFromC, Map<Id,Id> aFromB, Map<Id,Id> bFromC ) {
//		for ( Iterator<Entry<Id,Id>> it = bFromC.entrySet().iterator() ; it.hasNext(); ) {
//			Entry<Id,Id> entry = it.next();
//			Id cc = entry.getKey() ;
//			Id bb = entry.getValue();
//			
//			Id aa = aFromB.get(bb) ;
//			
//			aFromC.put(cc,aa) ;
//		}
//	}

	static Map<String,Integer> createIdxFromKey( String line ) {
		String[] keys = line.split("[ \t\n]+") ;
	
		Map<String,Integer> idxFromKey = new HashMap<String, Integer>() ;
		for ( int ii=0 ; ii<keys.length ; ii++ ) {
			idxFromKey.put(keys[ii], ii ) ;
		}
		return idxFromKey ;
	}
	
	private static final String ACT_HOME = "home" ;
	private static final String ACT_WORK = "work" ;
	static void makeHomePlan( Plan plan, Coord homeCoord ) {
		plan.createAct(ACT_HOME, homeCoord) ;
	}
	static void completePlanToHwh ( Plan plan, Coord workCoord ) {
		Act act = plan.getFirstActivity();
		act.setEndTime( 7.*3600. ) ;
		Coord homeCoord = act.getCoord();

		plan.createLeg(Mode.car);
		act = plan.createAct(ACT_WORK, workCoord ) ;
		act.setDuration( 8.*3600. ) ;

		plan.createLeg(Mode.car) ;
		plan.createAct(ACT_HOME, homeCoord ) ;				
	}

}
