package freight;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.charts.XYLineChart;
import org.matsim.core.utils.io.IOUtils;

import freight.ShipperAgent.TotalCost;

public class TLCCostChartListener implements ShipperTotalCostListener{

	private Map<Id,List<Double>> costMap = new HashMap<Id, List<Double>>();
	
	private String filename;
	
	public TLCCostChartListener(String filename) {
		super();
		this.filename = filename;
	}

	@Override
	public void inform(TotalCost totalCost) {
		if(costMap.containsKey(totalCost.shipperId)){
			costMap.get(totalCost.shipperId).add(totalCost.totCost);
		}
		else{
			List<Double> costs = new ArrayList<Double>();
			costs.add(totalCost.totCost);
			costMap.put(totalCost.shipperId, costs);
		}	
	}

	@Override
	public void reset(int iteration) {
		
		
	}

	@Override
	public void finish() {
		XYLineChart chart = new XYLineChart("Total Logistics Costs","iteration","costs");
		double[] iterationArr = getIterationArr();
		for(Id shipperId : costMap.keySet()){
			List<Double> cost = costMap.get(shipperId);
			double[] costArr = getArray(cost);
			chart.addSeries(shipperId.toString(), iterationArr, costArr);
		}
		chart.saveAsPng(filename, 800, 600);
		createTxtFile();
	}

	private void createTxtFile() {
		try{
			int index = filename.indexOf(".png");
			String name = filename.substring(0, index);
			BufferedWriter writer = IOUtils.getBufferedWriter(name + ".txt");
			double total = 0.0;
			for(Id shipperId : costMap.keySet()){
				List<Double> l = costMap.get(shipperId);
				Double cost = l.get(l.size()-1);
				writer.write(shipperId.toString() + ";" + cost + "\n");
				total += cost;
			}
			writer.write("total;"+total+"\n");
			writer.close();
		}
		catch(IOException e){
			e.printStackTrace();
		}
	}

	private double[] getIterationArr() {
		int size = costMap.values().iterator().next().size();
		double[] iterArr = new double[size];
		for(int i=0;i<size;i++){
			iterArr[i]=i+1;
		}
		return iterArr;
	}

	public static double[] getArray(List<Double> cost) {
		double[] arr = new double[cost.size()];
		for(int i=0;i<cost.size();i++){
			arr[i]=cost.get(i);
		}
		return arr;
	}
}
