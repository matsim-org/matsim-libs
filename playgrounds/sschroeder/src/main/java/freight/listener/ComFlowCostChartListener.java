package freight.listener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.charts.XYLineChart;

import freight.CommodityFlow;
import freight.ShipperAgent;
import freight.ShipperAgent.DetailedCost;

public class ComFlowCostChartListener implements ShipperDetailedCostListener {

	private Map<Id,Map<CommodityFlow,List<DetailedCost>>> costMap = new HashMap<Id, Map<CommodityFlow,List<DetailedCost>>>();
	
	private String filename;
	
	
	public ComFlowCostChartListener(String filename) {
		super();
		this.filename = filename;
	}

	@Override
	public void inform(DetailedCost detailedCost) {
		if(costMap.containsKey(detailedCost.shipperId)){
			Map<CommodityFlow, List<DetailedCost>> innerMap = costMap.get(detailedCost.shipperId);
			if(innerMap.containsKey(detailedCost.comFlow)){
				innerMap.get(detailedCost.comFlow).add(detailedCost);
			}
			else{
				List<DetailedCost> costList = new ArrayList<ShipperAgent.DetailedCost>();
				costList.add(detailedCost);
				innerMap.put(detailedCost.comFlow, costList);
			}
		}
		else{
			Map<CommodityFlow,List<DetailedCost>> innerMap = new HashMap<CommodityFlow, List<DetailedCost>>();
			List<DetailedCost> costList = new ArrayList<ShipperAgent.DetailedCost>();
			costList.add(detailedCost);
			innerMap.put(detailedCost.comFlow, costList);
			costMap.put(detailedCost.shipperId, innerMap);
		}
	}

	@Override
	public void reset(int iteration) {
		
	}

	@Override
	public void finish() {
		double[] iterations = getIterArr();
		XYLineChart chart = new XYLineChart("TLC per ComFlow","iteration","costs");
		for(Id shipperId : costMap.keySet()){
			Map<CommodityFlow, List<DetailedCost>> innerMap = costMap.get(shipperId);
			for(CommodityFlow flow : innerMap.keySet()){
				double[] tlc = getTLCArr(innerMap.get(flow));
				chart.addSeries((shipperId.toString() + "_" + flow.toString()), iterations, tlc);
			}
		}
		chart.saveAsPng(filename, 800, 600);
	}

	private double[] getTLCArr(List<DetailedCost> list) {
		double[] tlcArr = new double[list.size()];
		for(int i=0;i<list.size();i++){
			tlcArr[i]=list.get(i).tlc;
		}
		return tlcArr;
	}

	private double[] getIterArr() {
		int size = costMap.values().iterator().next().values().iterator().next().size();
		double[] iter = new double[size];
		for(int i=0;i<size;i++){
			iter[i]=i+1;
		}
		return iter;
	}

}
