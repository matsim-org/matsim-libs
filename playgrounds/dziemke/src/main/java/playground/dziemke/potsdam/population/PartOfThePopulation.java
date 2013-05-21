package playground.dziemke.potsdam.population;

import java.util.ArrayList;
import java.util.List;

public class PartOfThePopulation {
	
	public static List <Commuter> reduce (List <Commuter> commuters, int percentage){
		List <Commuter> output = new ArrayList <Commuter>();
		for (int i = 0; i<commuters.size(); i++){
			Commuter current = commuters.get(i);
			int beschaeftigteSozialversichert = ((current.getBeschaeftigteSozialversichert())/100) * percentage;
			int beschaeftigteGesamt = ((current.getBeschaeftigteGesamt())/100) * percentage ;
			int autofahrer = ((current.getAutofahrer())/100) * percentage;
			int oev = ((current.getOev())/100) * percentage;
			Commuter reduced = new Commuter(current.getResidence(), current.getId(), beschaeftigteSozialversichert, beschaeftigteGesamt, autofahrer, oev);
			output.add(reduced);
		}
		
		return output;
	}
}