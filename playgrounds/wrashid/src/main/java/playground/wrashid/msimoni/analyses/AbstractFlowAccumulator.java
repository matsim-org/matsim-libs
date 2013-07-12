package playground.wrashid.msimoni.analyses;

import org.matsim.api.core.v01.Id;

public abstract class AbstractFlowAccumulator {

	protected abstract int[] getFlow(Id linkId);
	public int[] getAccumulatedFlow(Id linkId){
		int[] flow=getFlow(linkId);
		int[] array=new int[flow.length];
		
		array[0]=flow[0];
		
		for (int i=1;i<array.length;i++){
			array[i]=array[i-1]+flow[i];
		}
		return array;
	}
	
	
}
