package playground.fhuelsmann.emission;

import java.io.IOException;

import org.matsim.api.core.v01.Id;

public interface AnalysisModuleCold {
	
	public void calculateColdEmissionsPerLink(Id personId, double actDuration,double distance, HbefaColdEmissionTable hbefaColdTable);
	
	public void calculatePerPersonPtBikeWalk(Id personId, Id linkId,HbefaColdEmissionTable hbefaColdTable);
	

}
