package playground.jhackney.algorithms;

import org.matsim.core.api.experimental.population.PlanElement;
import org.matsim.core.api.population.Person;
import org.matsim.core.api.population.PersonAlgorithm;
import org.matsim.core.api.population.Plan;
import org.matsim.core.api.population.Population;
import org.matsim.core.population.LegImpl;
import org.matsim.core.utils.charts.XYScatterChart;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;

public class PlansPlotScoreDistance extends AbstractPersonAlgorithm implements PersonAlgorithm{
	double[] dist=null;
	double[] score=null;
	int i=0;

	public PlansPlotScoreDistance(Population plans) {
		super();
		this.dist= new double[plans.getPersons().size()];
		this.score= new double[plans.getPersons().size()];
	}

	@Override
	public void run(Person person) {
		// TODO Auto-generated method stub
		Plan p = person.getSelectedPlan();
		dist[i]=0;
		score[i]= p.getScore().doubleValue();
		for (PlanElement pe : p.getPlanElements()) {
			if (pe instanceof LegImpl) {
				LegImpl l = (LegImpl) pe;
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
