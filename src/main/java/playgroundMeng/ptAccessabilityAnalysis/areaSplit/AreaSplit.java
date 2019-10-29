package playgroundMeng.ptAccessabilityAnalysis.areaSplit;

import java.util.List;
import java.util.Map;

import playgroundMeng.ptAccessabilityAnalysis.linksCategoryAnalysis.LinkExtendImp;
import playgroundMeng.ptAccessabilityAnalysis.stopInfoCellector.TransitStopFacilityExtendImp;

public interface AreaSplit {
	public Map<String, List<LinkExtendImp>> getLinksClassification();
	public Map<String, List<TransitStopFacilityExtendImp>> getStopsClassification();
	
}
