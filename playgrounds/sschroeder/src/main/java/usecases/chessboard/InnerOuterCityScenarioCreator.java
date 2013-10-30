package usecases.chessboard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;

public class InnerOuterCityScenarioCreator {
	

	public List<Id> getInnerCityLinks(){
		List<Id> innerCityLinks = new ArrayList<Id>();
		/*
		 * j(6,7)R, j(5,7), ..., j(3,7)[R|]
		 * .
		 * .
		 * .
		 * j(6,3), j(5,3), ..., j(3,3)
		 * 
		 * i(3,6),i(4,6), ..., i(7,6)
		 * .
		 * .
		 * .
		 * i(3,3),i(4,3),...., i(7,3)
		 */
		
		for(int i=3;i<8;i++){
			for(int j=3;j<8;j++){
				innerCityLinks.add(new IdImpl("j("+i+","+j+")"));
				innerCityLinks.add(new IdImpl("j("+i+","+j+")R"));
				innerCityLinks.add(new IdImpl("i("+i+","+j+")R"));
				innerCityLinks.add(new IdImpl("i("+i+","+j+")"));
			}
		}
		return innerCityLinks;
	}
	
	public List<Id> getAccessLinksToInnerCity(){
		List<Id> accessLinks = new ArrayList<Id>();
		List<String> linkStrings = Arrays.asList("j(4,7)R","j(6,7)R","j(5,3)","j(3,3)",
				"i(3,4)","i(3,6)","i(7,3)R","i(7,5)R");
		for(String idString : linkStrings) accessLinks.add(new IdImpl(idString));
		return accessLinks;
	}

}
