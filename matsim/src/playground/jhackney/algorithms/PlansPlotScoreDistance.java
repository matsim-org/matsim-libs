package playground.jhackney.algorithms;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.utils.charts.XYScatterChart;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;

public class PlansPlotScoreDistance extends AbstractPersonAlgorithm {
	double[] dist=null;
	double[] score=null;
	int i=0;

	public PlansPlotScoreDistance(PopulationImpl plans) {
		super();
		this.dist= new double[plans.getPersons().size()];
		this.score= new double[plans.getPersons().size()];
	}

	@Override
	public void run(Person person) {
		Plan p = person.getSelectedPlan();
		dist[i]=0;
		score[i]= p.getScore().doubleValue();
		for (PlanElement pe : p.getPlanElements()) {
			if (pe instanceof Leg) {
				Leg l = (Leg) pe;
				dist[i]+=l.getRoute().getDistance();
			}
		}
		i++;
	}
	public double[] getDist(){
		return this.dist;
	}
	public double[] getScore(){
		return this.score;
	}
	public void plot(String outdir, String name){
		String filename=outdir+"DistanceScore"+name+".png";
		String plotname="Distance vs. Score"+name;
		createXYScatterChart(filename, plotname, "Distance m", "Score", dist, score);
	}
	
	private void createXYScatterChart(final String filename, final String title, final String xname, final String yname, double[] x, double[] y) {
		XYScatterChart chart = new XYScatterChart(title, xname, yname);
		chart.addSeries(xname, x, y);
//		chart.addSeries("serie 2", new double[] {1.0, 5.0, 2.0, 4.0, 3.0}, new double[] {2.0, 3.0, 3.0, 1.5, 4.5});
		chart.saveAsPng(filename, 800, 600);
	}
}
