package playground.dziemke.potsdam.analysis.disaggregated.adapted;

import java.util.List;

import org.matsim.api.core.v01.Id;

public class RemoveDuplicates {

	public static void remove( List <Id> persons){
		int index = 0;
		while (index < persons.size() - 1){
			Comparable c1 = (Comparable)persons.get(index);
			Comparable c2 = (Comparable)persons.get(index + 1);
			if (c1.compareTo(c2) == 0){
				persons.remove(index + 1);
				// new:
				System.out.println("Ein doppelter Agent wurde entfernt.");
			}
			else{
				index++;
			}
		}

	}
	
}