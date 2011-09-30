package freight.listener;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.charts.XYLineChart;
import org.matsim.core.utils.io.IOUtils;

import playground.mzilske.freight.events.CarrierTotalCostHandler;

public class CarrierCostChartListener implements CarrierTotalCostHandler {

	public Map<Id,List<CarrierCostEvent>> costEventMap = new HashMap<Id, List<CarrierCostEvent>>();
	
	private String filename;
	
	
	public CarrierCostChartListener(String filename) {
		super();
		this.filename = filename;
	}

	@Override
	public void handleEvent(CarrierCostEvent costEvent) {
		if(costEventMap.containsKey(costEvent.getCarrierId())){
			costEventMap.get(costEvent.getCarrierId()).add(costEvent);
		}
		else{
			List<CarrierCostEvent> costList = new ArrayList<CarrierCostEvent>();
			costList.add(costEvent);
			costEventMap.put(costEvent.getCarrierId(), costList);
		}
	}

	@Override
	public void reset(int iteration) {
		
		
	}

	@Override
	public void finish() {
		double[] iterations = getIterArr();
		createDistanceChart(iterations);
		createPerformanceChart(iterations);
		createVolumeChart(iterations);
		createCapacityUsageChart(iterations);
		createTxtFileWithLastSolution();
	}

	private void createTxtFileWithLastSolution() {
		try{
			int index = filename.indexOf(".png");
			String name = filename.substring(0,index);
			BufferedWriter writer = IOUtils.getBufferedWriter(name + ".txt");
			double totalDist = 0.0;
			for(Id id : costEventMap.keySet()){
				Collection<CarrierCostEvent> costEvents = costEventMap.get(id);
				List<CarrierCostEvent> list = new ArrayList<CarrierTotalCostHandler.CarrierCostEvent>();
				list.addAll(costEvents);
				CarrierCostEvent event = list.get(list.size()-1);

				writer.write(id.toString() + ";" + event.distance + "\n");

				totalDist += event.distance;
			}
			writer.write("total;" + totalDist + "\n");
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}

	private void createCapacityUsageChart(double[] iterations) {
		XYLineChart chart = new XYLineChart("Carrier Capacity Usages","iteration","capacity usage [share of total capacity]");
		for(Id carrierId : costEventMap.keySet()){
			double[] volumeArr = getCapacityUsageArr(costEventMap.get(carrierId));
			chart.addSeries(carrierId.toString(), iterations, volumeArr);
		}
		String filename = getFileName("capacityUsage");
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

	private double[] getCapacityUsageArr(List<CarrierCostEvent> list) {
		double[] arr = new double[list.size()];
		for(int i=0;i<list.size();i++){
			arr[i]=list.get(i).capacityUse;
		}
		return arr;
	}

	private void createVolumeChart(double[] iterations) {
		XYLineChart chart = new XYLineChart("Carrier Volumes","iteration","volumes [number of units]");
		for(Id carrierId : costEventMap.keySet()){
			double[] volumeArr = getVolumeArr(costEventMap.get(carrierId));
			chart.addSeries(carrierId.toString(), iterations, volumeArr);
		}
		double[] totArr = getTotVolumeArr();
		chart.addSeries("total", iterations, totArr);
		String filename = getFileName("volume");
		chart.saveAsPng(filename, 800, 600);
		
	}

	private double[] getVolumeArr(List<CarrierCostEvent> list) {
		double[] arr = new double[list.size()];
		for(int i=0;i<list.size();i++){
			arr[i]=list.get(i).volume;
		}
		return arr;
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

	private void createPerformanceChart(double[] iterations) {
		XYLineChart chart = new XYLineChart("Carrier Performances","iteration","performance [unit*m]");
		for(Id carrierId : costEventMap.keySet()){
			double[] performanceArr = getPerformanceArr(costEventMap.get(carrierId));
			chart.addSeries(carrierId.toString(), iterations, performanceArr);
		}
		double[] totArr = getTotPerformanceArr();
		chart.addSeries("total", iterations, totArr);
		String filename = getFileName("performance");
		chart.saveAsPng(filename, 800, 600);
		
	}

	private double[] getTotPerformanceArr() {
		int size = costEventMap.values().iterator().next().size();
		double[] totArr = new double[size];
		for(int i=0;i<size;i++){
			double totPerformance = 0.0;
			for(Id carrierId : costEventMap.keySet()){
				totPerformance += costEventMap.get(carrierId).get(i).performance;
			}
			totArr[i] = totPerformance;
		}
		return totArr;
	}

	private double[] getPerformanceArr(List<CarrierCostEvent> list) {
		double[] arr = new double[list.size()];
		for(int i=0;i<list.size();i++){
			arr[i]=list.get(i).performance;
		}
		return arr;
	}

	private void createDistanceChart(double[] iterations) {
		XYLineChart chart = new XYLineChart("Carrier Distances","iteration","distance [m]");
		for(Id carrierId : costEventMap.keySet()){
			double[] distanceArr = getDistanceArr(costEventMap.get(carrierId));
			chart.addSeries(carrierId.toString(), iterations, distanceArr);
		}
		double[] totArr = getTotDistanceArr();
		chart.addSeries("total", iterations, totArr);
		String filename = getFileName("distance");
		chart.saveAsPng(filename, 800, 600);
	}

	private double[] getTotDistanceArr() {
		int size = costEventMap.values().iterator().next().size();
		double[] totArr = new double[size];
		for(int i=0;i<size;i++){
			double totCost = 0.0;
			for(Id carrierId : costEventMap.keySet()){
				totCost += costEventMap.get(carrierId).get(i).distance;
			}
			totArr[i] = totCost;
		}
		return totArr;
	}

	private double[] getIterArr() {
		int size = costEventMap.values().iterator().next().size();
		double[] iterations = new double[size];
		for(int i=0;i<size;i++){
			iterations[i]=i+1;
		}
		return iterations;
	}

	private double[] getDistanceArr(List<CarrierCostEvent> list) {
		double[] arr = new double[list.size()];
		for(int i=0;i<list.size();i++){
			arr[i]=list.get(i).distance;
		}
		return arr;
	}

}
