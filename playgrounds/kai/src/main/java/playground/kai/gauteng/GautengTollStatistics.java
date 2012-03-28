/**
 * 
 */
package playground.kai.gauteng;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.AgentMoneyEvent;
import org.matsim.core.api.experimental.events.handler.AgentMoneyEventHandler;
import org.matsim.core.events.handler.EventHandler;

import playground.kai.gauteng.roadpricingscheme.SanralTollFactor;
import playground.kai.gauteng.roadpricingscheme.SanralTollFactor.Type;

/**
 * @author nagel
 *
 */
public class GautengTollStatistics implements EventHandler, AgentMoneyEventHandler {

	private Map<Type,Double> typeMoneyMap = new HashMap<Type,Double>() ;
	private Map<Type,Double> typeCountMap = new HashMap<Type,Double>() ;

	@Override
	public void reset(int iteration) {
		typeMoneyMap.clear() ;
		typeCountMap.clear() ;
	}

	@Override
	public void handleEvent(AgentMoneyEvent event) {
		Id personId = event.getPersonId() ;
		Type agentType = SanralTollFactor.typeOf(personId) ;
		if ( typeMoneyMap .get(agentType)==null ) {
			typeMoneyMap.put(agentType,0.) ;
			typeCountMap.put(agentType,0.) ;
		}
		double currentMoney = typeMoneyMap.get(agentType) ;
		double currentCount = typeCountMap.get(agentType) ;
		
//		Logger.getLogger(this.getClass()).warn( " currentMoneyBefore: " + currentMoney ) ;
		currentMoney += event.getAmount() ;
//		Logger.getLogger(this.getClass()).warn( " amount: " + event.getAmount() + " currentMoney: " + currentMoney ) ;
		currentCount ++ ;
		
		typeMoneyMap.put( agentType, currentMoney ) ;
		typeCountMap.put( agentType, currentCount ) ;
		
	}
	
	public void printTollInfo() {
		for ( Type type : typeMoneyMap.keySet() ) {
			Logger.getLogger(this.getClass()).warn( "type: " + type.toString() 
					+ " av toll per money event: " + (typeMoneyMap.get(type)/typeCountMap.get(type)) ) ;
			// will have to change when money event is no longer just at end of day.
		}
	}

}
