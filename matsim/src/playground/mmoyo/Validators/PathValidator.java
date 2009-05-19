package playground.mmoyo.Validators;

import org.matsim.core.api.network.Link;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;

public class PathValidator {
	final String TRANSFERTYPE = "Transfer";
	final String STANDARDTYPE = "Standard";
	final String DETTRANSFERTYPE = "DetTransfer";
	final String WALKINGTYPE = "Walking";
	final String NULLTYPE = null;
	
	public PathValidator (){
	
	}
	
	/*Conditions:
	 * No null
	 * must have at least 3 links
	 * starts and ends with walking links
	 * transfer links must be always between standard links
	 * must not have two adjacent walking links
	 */
	public boolean isValid(Path path) {
		boolean valid = true;
		
		if (path!=null){
			//that path starts and ends with walking links
			//if (path.links.size()>2 && path.links.get(0).getType().equals("Walking") && path.links.get(path.links.size()-1).getType().equals("Walking") ){
				//boolean hasStandardLinks = false;
				boolean first = true;
			
				Link lastLink = null;
				for (Link link : path.links) {
					//if (!first){
						if (!canPassLink(lastLink, link)){
							return false;
						}
					
					//}
					
					//first=false;
					lastLink= link;
					/*
					linkType= link.getType();
					if (linkType.equals("Standard")) {
						hasStandardLinks=true;
					}else if(linkType.equals("Transfer")){
						if (i>0){  
							if (!path.links.get(i-1).getType().equals("Standard") || !path.links.get(i+1).equals("Standard")){ //TODO:check that transfer links are only between standard link
								return false;
							}
						}
					}else if (linkType.equals("Walking")){
						if (i>0){
							if (path.links.get(i-1).getType().equals("Walking")){ //TODO:check that the path does not have two adjacent walking link
								return false;
							}
						}
					}else if(linkType.equals("DetTransfer")){
						if (i>0){  
							if (!path.links.get(i-1).getType().equals("Standard") || !path.links.get(i+1).getType().equals("Standard")){
								return false;
							}
						}
					}//linktype
					i++;
					*/
				}//for interator
				
				//if(!hasStandardLinks) valid= false;
			//}else{
			//	valid=false;
			//}//if pathlinks
		}else{
			valid=false;
		}//path!=null
		return valid;
	}//is valid
	
	public boolean canPassLink(final Link lastLink, final Link link){
		boolean pass = false;
		String type= link.getType();
		
		if (lastLink==null){
			if (type.equals(WALKINGTYPE)) pass= true;
		}else{
			String lastType = lastLink.getType();	
			if (type.equals(TRANSFERTYPE)){
				if (lastType.equals(TRANSFERTYPE)) 			pass = false; 
				else if (lastType.equals(STANDARDTYPE)) 	pass = true;
				else if (lastType.equals(DETTRANSFERTYPE)) 	pass = false;
				else if (lastType.equals(WALKINGTYPE)) 		pass = false;
				else if (lastType.equals(NULLTYPE)) 		pass = false;
			}else if (type.equals(WALKINGTYPE)){
				if (lastType.equals(TRANSFERTYPE)) 			pass = false;
				else if (lastType.equals(STANDARDTYPE)) 	pass = true;
				else if (lastType.equals(DETTRANSFERTYPE)) 	pass = false;
				else if (lastType.equals(WALKINGTYPE)) 		pass = false;
				else if (lastType.equals(NULLTYPE)) 		pass = true;
			}else if (type.equals(STANDARDTYPE)){
				if (lastType.equals(TRANSFERTYPE)) 			pass = true;
				else if (lastType.equals(STANDARDTYPE)) 	pass = true;
				else if (lastType.equals(DETTRANSFERTYPE)) 	pass = true;
				else if (lastType.equals(WALKINGTYPE)) 		pass = true;
				else if (lastType.equals(NULLTYPE)) 		pass = false;
			}else if (type.equals(DETTRANSFERTYPE)){
				if (lastType.equals(TRANSFERTYPE)) 			pass = false;
				else if (lastType.equals(STANDARDTYPE)) 	pass = true;
				else if (lastType.equals(DETTRANSFERTYPE)) 	pass = false;
				else if (lastType.equals(WALKINGTYPE)) 		pass = false;
				else if (lastType.equals(NULLTYPE)) 		pass = false;
			}
		}
		return pass;
	}

	
	
	/*    //-> Is this possible?
	 if (type.equals(null)){
			if (lastType.equals(TRANSFERTYPE)) 			pass = false;
			else if (lastType.equals(STANDARDTYPE)) 	pass = false;
			else if (lastType.equals(DETTRANSFERTYPE)) 	pass = false;
			else if (lastType.equals(WALKINGTYPE)) 		pass = false;
			else if (lastType.equals(NULLTYPE)) 		pass = false;
	}
	*/
	
	
	public void printPath(Path path){
		System.out.print(path.toString());
		/*
		for (Link link : path.links){
			System.out.print(b);
			//link.getId()
		}
		*/	
	}
	
	
}//class
