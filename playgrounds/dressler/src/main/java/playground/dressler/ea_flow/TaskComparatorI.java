package playground.dressler.ea_flow;

import java.util.Comparator;

public interface TaskComparatorI extends Comparator<BFTask>  {
	public int compare(BFTask first, BFTask second);
	public int getValue(BFTask task);
}
