package playground.mkillat.tmc;

import java.util.ArrayList;
import java.util.List;

public class FactorAdder {

	public List <MessagesPlusFactor> add (List <Message> messages, List <EventCodeCategorys> categorys){
		List <MessagesPlusFactor> output = new ArrayList <MessagesPlusFactor>();
		for (int i=0; i<messages.size(); i++){
			for (int a=0; a<categorys.size(); a++){
				Message currentM = messages.get(i);
				EventCodeCategorys currentE = categorys.get(a);
				if (currentM.eventCode == currentE.eventCode){
					MessagesPlusFactor current = new MessagesPlusFactor(currentM.msId, currentM.link, 
							currentM.eventCode, currentM.startTime, currentM.endTime, 
							currentE.flowCapacityChange, currentE.freespeedChange, currentE.lanesChange, currentE.factor);
					output.add(current);
				}
			
				
				
			}
		}
	
		return output;
		
	}
	
}