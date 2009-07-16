package playground.mmoyo.Validators;

import org.matsim.core.api.experimental.network.Link;
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
	final String TRANSFER = "Transfer";
	final String STANDARD = "Standard";
	final String DETTRANSFER = "DetTransfer";
	final String WALKING = "Walking";
	final String ACCESS = "Access";
	
	public PathValidator (){
	
	}
	
	@Deprecated
	public boolean isValid(Path path) {
		boolean valid = true;
		if (path!=null){
			Link lastLink = null;
			for (Link link : path.links) {
				if (lastLink!= null){
					if (!canPassLink((LinkImpl) lastLink, (LinkImpl) link)){
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
			pass = type.equals(ACCESS);
		}else{
			String lastType = lastLink.getType();	
			
			if (type.equals(DETTRANSFER)){
				pass = lastType.equals(STANDARD);
			}else if (type.equals(TRANSFER)){
				pass= lastType.equals(STANDARD);
			}else if (type.equals(STANDARD)){
				//lastType null and walk are rejected
				pass= (lastType.equals(DETTRANSFER) || lastType.equals(TRANSFER) || lastType.equals(STANDARD) || lastType.equals(ACCESS)); 
			}else if (type.equals(WALKING)){
				pass= lastType.equals(STANDARD);
			}
		}
		return pass;
	}

}
