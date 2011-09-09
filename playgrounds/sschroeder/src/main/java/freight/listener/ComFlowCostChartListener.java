package freight.listener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.charts.XYLineChart;

import freight.CommodityFlow;
import freight.DetailedCostStatusEvent;

public class ComFlowCostChartListener implements ShipperDetailedCostStatusHandler {

	private Map<Id,Map<CommodityFlow,List<DetailedCostStatusEvent>>> costMap = new HashMap<Id, Map<CommodityFlow,List<DetailedCostStatusEvent>>>();
	
	private String filename;
	
	
	public ComFlowCostChartListener(String filename) {
		super();
		this.filename = filename;
	}

	@Override
	public void handleEvent(DetailedCostStatusEvent event) {
		if(costMap.containsKey(event.getShipperId())){
			Map<CommodityFlow, List<DetailedCostStatusEvent>> innerMap = costMap.get(event.getShipperId());
			if(innerMap.containsKey(event.getComFlow())){
				innerMap.get(event.getComFlow()).add(event);
			}
			else{
				List<DetailedCostStatusEvent> costList = new ArrayList<DetailedCostStatusEvent>();
				costList.add(event);
				innerMap.put(event.getComFlow(), costList);
			}
		}
		else{
			Map<CommodityFlow,List<DetailedCostStatusEvent>> innerMap = new HashMap<CommodityFlow, List<DetailedCostStatusEvent>>();
			List<DetailedCostStatusEvent> costList = new ArrayList<DetailedCostStatusEvent>();
			costList.add(event);
			innerMap.put(event.getComFlow(), costList);
			costMap.put(event.getShipperId(), innerMap);
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
			Map<CommodityFlow, List<DetailedCostStatusEvent>> innerMap = costMap.get(shipperId);
			for(CommodityFlow flow : innerMap.keySet()){
				double[] tlc = getTLCArr(innerMap.get(flow));
				chart.addSeries((shipperId.toString() + "_" + flow.toString()), iterations, tlc);
			}
		}
		chart.saveAsPng(filename, 800, 600);
	}

	private double[] getTLCArr(List<DetailedCostStatusEvent> list) {
		double[] tlcArr = new double[list.size()];
		for(int i=0;i<list.size();i++){
			tlcArr[i]=list.get(i).getTlc();
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
