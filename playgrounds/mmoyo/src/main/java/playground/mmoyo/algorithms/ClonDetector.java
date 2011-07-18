package playground.mmoyo.algorithms;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Population;

public class ClonDetector {
	private final static Logger log = Logger.getLogger(ClonDetector.class);
	final String criterium;
	int max =0; //maxNum of found clones
	
	public ClonDetector (final String crit){
		this.criterium = crit;
	}
	
	/**return the list of clones of the population detecting them because they have "X" as suffix start*/
	public List<Id> run (final Population pop){
		final String strClon = "clon: ";
		final String strNoClon = "no clon: ";
		final String SEP = " ";
		int noClonsNum =0;
			
		List<Id> clonsList = new ArrayList<Id>();
		for (Id id : pop.getPersons().keySet()){
			int clonIndex = getClonIndex(id.toString());
			if (clonIndex!=-1){
				if (clonIndex>this.max){
					this.max=clonIndex;
				}
				//log.info(strClon + id.toString() + SEP + clonIndex);
				clonsList.add(id);
			}else{
				//log.info(strNoClon + id.toString() + SEP + clonIndex);
				noClonsNum++;
			}
				
		}
		log.info("clones: " + clonsList.size() + "; no clones:" + noClonsNum);
			
		return clonsList;
	}
	

	/**returns a int representing the clon suffix, or -1 if it is not a clon*/
	public int getClonIndex(final String strId){
		int i =-1;
		int xIndex = strId.indexOf(criterium);
		if (xIndex!=-1){
			char n = strId.charAt(xIndex+1);
			i = Integer.valueOf(Character.toString(n));	
		}
		return i;
	}
	
	/**if a clon id is given, it returns the original clon id. if it is an original, it returns the same as given*/
	public String getOriginalId(final String strId){
		int xIndex = strId.indexOf(this.criterium);
		String strOriginal = xIndex>-1? strId.substring(0, xIndex): strId;
		return strOriginal;
	}
	
	public static void main(String[] args) {
	

	}

}
