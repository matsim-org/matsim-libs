package usecases.chessboard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

public class InnerOuterCityScenarioCreator {
	

	public List<Id<Link>> getInnerCityLinks(){
		List<Id<Link>> innerCityLinks = new ArrayList<>();
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
				innerCityLinks.add(Id.create("j("+i+","+j+")", Link.class));
				innerCityLinks.add(Id.create("j("+i+","+j+")R", Link.class));
				innerCityLinks.add(Id.create("i("+i+","+j+")R", Link.class));
				innerCityLinks.add(Id.create("i("+i+","+j+")", Link.class));
			}
		}
		return innerCityLinks;
	}
	
	public List<Id> getAccessLinksToInnerCity(){
		List<Id> accessLinks = new ArrayList<Id>();
		List<String> linkStrings = Arrays.asList("j(4,7)R","j(6,7)R","j(5,3)","j(3,3)",
				"i(3,4)","i(3,6)","i(7,3)R","i(7,5)R");
		for(String idString : linkStrings) accessLinks.add(Id.create(idString, Link.class));
		return accessLinks;
	}

}
