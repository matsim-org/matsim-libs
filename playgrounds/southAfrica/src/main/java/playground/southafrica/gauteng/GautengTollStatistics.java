/**
 * 
 */
package playground.southafrica.gauteng;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.PersonMoneyEventHandler;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.utils.io.IOUtils;

import playground.southafrica.gauteng.roadpricingscheme.TollFactorI;
import playground.southafrica.gauteng.roadpricingscheme.SanralTollVehicleType;

/**
 * @author nagel
 *
 */
public class GautengTollStatistics implements EventHandler, PersonMoneyEventHandler {

	private Map<Id,Double> personMoneyMap = new HashMap<Id,Double>() ;
	private Map<Id,Double> personCountMap = new HashMap<Id,Double>() ;
	
	private Map<SanralTollVehicleType,Double> typeMoneyMap = new HashMap<SanralTollVehicleType,Double>() ;
	private Map<SanralTollVehicleType,Double> typeCountMap = new HashMap<SanralTollVehicleType,Double>() ;

	private final TollFactorI tollFactor ;

	public GautengTollStatistics( TollFactorI tollFactor ) {
		this.tollFactor = tollFactor ;
	}
	
	@Override
	public void reset(int iteration) {
//		Logger.getLogger(this.getClass()).warn("calling reset ...") ;
		personMoneyMap.clear() ;
		personCountMap.clear() ;
		typeMoneyMap.clear() ;
		typeCountMap.clear() ;
	}

	@Override
	public void handleEvent(PersonMoneyEvent event) {
		if ( event.getAmount()==0. ) {
			return ;
		}
		Id personId = event.getPersonId() ;
		if ( personMoneyMap.get(personId) == null ) {
			personMoneyMap.put(personId,0.) ;
			personCountMap.put(personId,0.) ;
		}
		double currentMoney = personMoneyMap.get(personId) ;
		double currentCount = personCountMap.get(personId) ;
		
		currentMoney += event.getAmount() ;
		currentCount++ ;
		
		personMoneyMap.put(personId,currentMoney) ;
		personCountMap.put(personId,currentCount) ;
	}
		
	static int moneyToBin( double amount ) {
		return (int)(amount/5.) ;
	}

	public enum SimplifiedType {privateCar, commercialVehicle, otherVehicle } ;  

	class TwoWayTable<K,L,V> {
		Map<String,Double> values = new HashMap<String,Double>() ;
		String createKey( K kk, L ll ) {
			return kk.toString() + ":" + ll.toString() ;
		}
		Double getEntry( String key ) {
			if ( values.get(key) == null ) {
				values.put( key, 0. ) ;
			}
			return values.get(key) ;
		}
		Double putEntry( String key, Double value ) {
			return values.put( key, value ) ;
		}
	}

	public void printTollInfo(String directoryname) {
		TwoWayTable<SimplifiedType,Integer,Double> countsTable = new TwoWayTable<SimplifiedType,Integer,Double>() ;
		TwoWayTable<SimplifiedType,Integer,Double> moneyTable = new TwoWayTable<SimplifiedType,Integer,Double>() ;
		Map<SimplifiedType,Integer> maxIdx = new HashMap<SimplifiedType,Integer>() ;
		for ( Id personId : personMoneyMap.keySet() ) {
			double amount = -personMoneyMap.get(personId) ; 
			if ( amount==0. ) {
				continue ;
			}
			Integer idx = moneyToBin( amount ) ;
//			System.err.println( " person: " + personId + " money: " + amount ) ;
			SanralTollVehicleType agentType = tollFactor.typeOf(personId) ;
			SimplifiedType sType = simplifiedTypeOf(agentType);

			if ( sType != null ) { 
				String key = countsTable.createKey( sType, idx ) ;
				{
					double currentCount = countsTable.getEntry( key ) ;
					currentCount++ ;
					countsTable.putEntry( key, currentCount ) ;
				}{
					double currentMoney = moneyTable.getEntry(key) ;
					currentMoney += amount ;
					moneyTable.putEntry( key, currentMoney ) ;
				}{
					Integer currentMax = maxIdx.get(sType) ;
					if ( currentMax==null || currentMax < idx ) {
						maxIdx.put(sType,idx) ;
					}
				}
			}
			
		}

	
		for ( SimplifiedType sType : SimplifiedType.values() ) {
			String filename = directoryname + sType.toString() + ".txt" ;
			BufferedWriter out = IOUtils.getBufferedWriter( filename );

			if ( maxIdx.get(sType)==null ) {
				// this may happen if nobody of the type paid toll.
				continue ;
			}
			try {
				out.write( "#AV_TOLL\tN_VEHS\n" ) ;
			}catch ( IOException e) {
				e.printStackTrace();
			}
			for ( int idx=0 ; idx<=maxIdx.get(sType) ; idx++ ) {
				String key = countsTable.createKey( sType, idx ) ;
				if ( countsTable.getEntry(key) != 0. ) {
					String str =  (moneyTable.getEntry(key)/countsTable.getEntry(key)) + "\t" + countsTable.getEntry(key) + "\n" ;
					try {
						out.write( str ) ;
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			try {
				out.close() ;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
	}

	/**
	 * @param agentType
	 * @return
	 */
	public static SimplifiedType simplifiedTypeOf(SanralTollVehicleType agentType) {
		SimplifiedType sType = null ;
		switch ( agentType ) {
		case carWithoutTag:
		case carWithTag:
		case commercialClassAWithTag:
		case commercialClassAWithoutTag:
			sType = SimplifiedType.privateCar ; break ;
		case commercialClassBWithoutTag:
		case commercialClassBWithTag:
		case commercialClassCWithoutTag:
		case commercialClassCWithTag:
			sType = SimplifiedType.commercialVehicle ; break ;
		case busWithTag:
		case busWithoutTag:
		case taxiWithTag:
		case taxiWithoutTag:
		case extWithTag:
		case extWithoutTag:
			sType = SimplifiedType.otherVehicle ; break ;
		default:
			throw new RuntimeException("case statement missing for type: " + agentType ) ;
		}
		return sType;
	}

}
