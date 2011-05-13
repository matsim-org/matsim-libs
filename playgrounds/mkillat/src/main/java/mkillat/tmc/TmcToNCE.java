package mkillat.tmc;

import java.util.List;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkChangeEventsWriter;


public class TmcToNCE {


	public static void main(String[] args) {


		List <Message> messages = MessageReader.read("\\Users\\Marie\\workspace\\Events.csv");
		
		List <EventCodeCategorys> categorys = CategoryReader.read("\\Users\\Marie\\workspace\\EventCode_und_Faktor.csv");

		
//		Die Liste Messages wird mit dem zu ändernden Attribut und dem dazu gehöhrigen Faktor ergänzt.
		FactorAdder aa = new FactorAdder();
		List <MessagesPlusFactor> messagesPlusFactor = aa.add(messages, categorys);
		
		
		MessageToNCE bb = new MessageToNCE();
		List <NetworkChangeEvent> events = bb.transform(messagesPlusFactor);
	
		NetworkChangeEventsWriter test = new NetworkChangeEventsWriter();
		test.write("\\Users\\Marie\\workspace\\Ergebnis.xml", events);
	
		
	}

}