package playground.mmoyo.Validators;

import org.matsim.core.network.LinkImpl;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;

/** Validates a given path with the next conditions:
 * No null
 * must have at least 3 links     //-> not absolutely. A path can be also only walking
 * starts and ends with walking links
 * transfer links must be always between standard links
 * must not have two adjacent walking links
 */
public class PathValidator {
	final String TRANSFERTYPE = "Transfer";
	final String STANDARDTYPE = "Standard";
	final String DETTRANSFERTYPE = "DetTransfer";
	final String WALKINGTYPE = "Walking";
	final String NULLTYPE = null;
	
	public PathValidator (){
	
	}
	
	@Deprecated
	public boolean isValid(Path path) {
		boolean valid = true;
		if (path!=null){
			LinkImpl lastLink = null;
			for (LinkImpl link : path.links) {
				if (lastLink!= null){
					if (!canPassLink(lastLink, link)){
						return false;
					}
					lastLink= link;
				}
			}
		}else{
			valid=false;
		}
		return valid;
	}
	
	
	public boolean canPassLink(final LinkImpl lastLink, final LinkImpl link){
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
