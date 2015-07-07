package playground.smeintjes;

import java.util.ArrayList;
import java.util.Comparator;

import org.matsim.core.utils.collections.Tuple;

public class IntegerTupleComparator implements Comparator<Tuple<Integer, Integer>>{

	/**
	 * Constructor.
	 * 
	 * @param map
	 * 		map containing QuadEdge and Double
	 */
	public IntegerTupleComparator() {
		
	}
	
	/**
	 * Method of comparison. Ranks the QuadEdge in descending order.
	 * 
	 * @param fromEdge
	 * 		fromEdge to compare
	 * @param toEdge
	 * 		toEdge to compare
	 * @return
	 * 		-1 fromEdge is less than toEdge
	 * 		0 if edges are equal,
	 * 		1 otherwise (fromEdge greater than toEdge)
	 */
	//@Override
		public int compare(Tuple<Integer, Integer> fromEdge, Tuple<Integer, Integer> toEdge) {
			Integer fromSource = fromEdge.getFirst();
			Integer fromDestination = fromEdge.getSecond();
			Integer toSource = toEdge.getFirst();
			Integer toDestination = toEdge.getSecond();
			
			if ((fromSource.intValue() < toSource.intValue()) | ((fromSource.equals(toSource)) && (fromDestination.intValue() < toDestination.intValue())) | (fromSource.intValue() > toSource.intValue())) {
				return -1;
			}  else if ((fromSource.equals(toSource)) && (fromDestination.intValue() > toDestination.intValue())) {
				return 1;
			} else if((fromSource.equals(toSource)) && (fromDestination.equals(toDestination))){
				return 0;
			} else
				return 1;

		}

}
