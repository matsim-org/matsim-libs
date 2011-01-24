/**
 * 
 */
package playground.mzilske.freight.vrp;

import java.util.Comparator;

/**
 * @author schroeder
 *
 */
public class DescendingOrderSavingComparator implements Comparator<Saving> {

	public int compare(Saving o1, Saving o2) {
		if(o1.getSaving() < o2.getSaving()){
			return 1;
		}
		else if(o1.getSaving() > o2.getSaving()){
			return -1;
		}
		else{
			return 0;
		}
	}

}
