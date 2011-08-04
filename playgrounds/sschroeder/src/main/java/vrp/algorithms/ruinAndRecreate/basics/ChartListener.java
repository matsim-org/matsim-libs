package vrp.algorithms.ruinAndRecreate.basics;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.core.utils.charts.XYLineChart;

import vrp.algorithms.ruinAndRecreate.RuinAndRecreateEvent;
import vrp.algorithms.ruinAndRecreate.api.RuinAndRecreateListener;


/**
 * 
 * @author stefan schroeder
 *
 */

public class ChartListener implements RuinAndRecreateListener {
	
	private static Logger log = Logger.getLogger(ChartListener.class);
	
	private double[] bestResults;
	
	private double[] tentativeResults;
	
	private List<Double> bestResultList = new ArrayList<Double>();
	
	private List<Double> tentativeResultList = new ArrayList<Double>();
	
	private String filename = null;
	
	public void setFilename(String filename) {
		this.filename = filename;
	}

	public void inform(RuinAndRecreateEvent event) {
		bestResultList.add(event.getCurrentResult());
		tentativeResultList.add(event.getTentativeSolution());
//		tentativeResult.add(event.getCurrentMutation(), event.getTentativeSolution());
//		bestResult.add(event.getCurrentMutation(),event.getTentativeSolution());
	}

	public void finish() {
		bestResults = new double[bestResultList.size()];
		tentativeResults = new double[tentativeResultList.size()];
		double[] mutation = new double[bestResultList.size()];
		for(int i=0;i<bestResultList.size();i++){
			bestResults[i] = bestResultList.get(i);
			tentativeResults[i] = tentativeResultList.get(i);
			mutation[i] = i+1;
		}
		XYLineChart chart = new XYLineChart("Results","mutation","costs");
		chart.addSeries("bestResults", mutation, bestResults);
		chart.addSeries("tentativeResults", mutation, tentativeResults);
		chart.saveAsPng(filename, 800, 600);
	}

}
