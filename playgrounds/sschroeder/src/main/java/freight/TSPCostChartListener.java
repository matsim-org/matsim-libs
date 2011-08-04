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

import playground.mzilske.freight.TSPTotalCostListener;

public class TSPCostChartListener implements TSPTotalCostListener{
	
	private Map<Id,List<TSPCostEvent>> costEventMap = new HashMap<Id, List<TSPCostEvent>>();

	private String filename;

	public TSPCostChartListener(String filename) {
		super();
		this.filename = filename;
	}

	@Override
	public void inform(TSPCostEvent costEvent) {
		if(costEventMap.containsKey(costEvent.id)){
			costEventMap.get(costEvent.id).add(costEvent);
		}
		else{
			List<TSPCostEvent> list = new ArrayList<TSPTotalCostListener.TSPCostEvent>();
			list.add(costEvent);
			costEventMap.put(costEvent.id, list);
		}
		
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void finish() {
		double[] iterations = getIterArr();
		createVolumeChart(iterations);
		createTxtFile();
	}
	
	private void createTxtFile() {
		try{
			int index = filename.indexOf(".png");
			String name = filename.substring(0, index);
			BufferedWriter writer = IOUtils.getBufferedWriter(name + ".txt");
			double total = 0.0;
			for(Id tspId : costEventMap.keySet()){
				List<TSPCostEvent> l = costEventMap.get(tspId);
				TSPCostEvent cost = l.get(l.size()-1);
				writer.write(tspId.toString() + ";" + cost.volume + "\n");
				total += cost.volume;
			}
			writer.write("total;"+total+"\n");
			writer.close();
		}
		catch(IOException e){
			e.printStackTrace();
		}
		
	}

	private void createVolumeChart(double[] iterations) {
		XYLineChart chart = new XYLineChart("TSP Volumes","iteration","volumes");
		for(Id carrierId : costEventMap.keySet()){
			double[] distanceArr = getVolumeArr(costEventMap.get(carrierId));
			chart.addSeries(carrierId.toString(), iterations, distanceArr);
		}
		double[] totArr = getTotVolumeArr();
		chart.addSeries("total", iterations, totArr);
		String filename = getFileName("volumes");
		chart.saveAsPng(filename, 800, 600);
		
	}

	private String getFileName(String addition) {
		if(filename.endsWith(".png")){
			int index = filename.indexOf(".png");
			String name = filename.substring(0, index);
			name += ("_" + addition + ".png");
			return name;
		}
		else{
			return filename + "_" + addition + ".png";
		}
		
	}

	private double[] getTotVolumeArr() {
		int size = costEventMap.values().iterator().next().size();
		double[] totArr = new double[size];
		for(int i=0;i<size;i++){
			double totPerformance = 0.0;
			for(Id carrierId : costEventMap.keySet()){
				totPerformance += costEventMap.get(carrierId).get(i).volume;
			}
			totArr[i] = totPerformance;
		}
		return totArr;
	}

	private double[] getVolumeArr(List<TSPCostEvent> list) {
		double[] arr = new double[list.size()];
		for(int i=0;i<list.size();i++){
			arr[i]=list.get(i).volume;
		}
		return arr;
	}

	private double[] getIterArr() {
		int size = costEventMap.values().iterator().next().size();
		double[] iterations = new double[size];
		for(int i=0;i<size;i++){
			iterations[i]=i+1;
		}
		return iterations;
	}
	
	

}
