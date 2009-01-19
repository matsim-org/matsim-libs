package playground.mmoyo.PTCase2;

import org.matsim.router.util.LeastCostPathCalculator.Path;
import org.matsim.network.Link;


public class PTPathValidator {

	public PTPathValidator (){
	
	}
	/*Conditions:
	 * No null
	 * must have at least 3 links
	 * starts and ends with walking links
	 * transfer links must be always between standard links
	 * not have two adjacent walking link
	 */
	public boolean isValid(Path path) {
		boolean valid = true;
		
		if (path!=null){
			//that path starts and ends with walking links
			if (path.links.size()>2 && path.links.get(0).getType().equals("Walking") && path.links.get(path.links.size()-1).getType().equals("Walking") ){
				boolean hasStandardLinks = false;
				String linkType;
				int i=0;
				
				for (Link link : path.links) {
					linkType= link.getType();
					if (linkType.equals("Standard")) {
						hasStandardLinks=true;
					}else if(linkType=="Transfer"){
						if (i>0){  
							if (!path.links.get(i-1).getType().equals("Standard") || !path.links.get(i+1).getType().equals("Standard")){ //TODO:check that transfer links are only between standard link
								return false;
							}
						}//if i>0
					}else if (linkType=="Walking"){
						if (i>0){
							if (path.links.get(i-1).getType().equals("Walking")){ //TODO:check that do not have two adjacent walking link
								return false;
							}
						}
					}//linktype
					i++;
				}//for interator
				if(hasStandardLinks==false){return false;}
			}else{
				valid=false;
			}//if pathlinks
		}else{
			valid =false;
		}//path1=null
		
		return valid;
	}//is valid
	
}//class
