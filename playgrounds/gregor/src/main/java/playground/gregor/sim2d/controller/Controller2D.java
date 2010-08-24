package playground.gregor.sim2d.controller;


import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.controler.Controler;

import playground.gregor.sim2d.peekabot.Sim2DVis;
import playground.gregor.sim2d.scenario.ScenarioLoader2DImpl;
import playground.gregor.sim2d.simulation.Sim2D;
import playground.gregor.sim2d.simulation.StaticForceField;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;

public class Controller2D extends Controler {

	private Map<MultiPolygon,List<Link>> mps;


	private StaticForceField sff;

	
	private boolean vis = true;

	private Sim2DVis sim2DVis = null;



	public Controller2D(String[] args) {
		super(args);
		this.setOverwriteFiles(true);


	}




	@Override
	protected void loadData() {
		if (!this.scenarioLoaded) {
			this.loader = new ScenarioLoader2DImpl(this.scenarioData);
			this.loader.loadScenario();
			this.mps = ((ScenarioLoader2DImpl)this.loader).getFloorLinkMapping();
			this.sff = ((ScenarioLoader2DImpl)this.loader).getStaticForceField();
			this.network = this.loader.getScenario().getNetwork();
			this.population = this.loader.getScenario().getPopulation();
			this.scenarioLoaded = true;
		}
		if (this.vis) {
			intiSim2DVis();
		}
	}

	private void intiSim2DVis() {
		double minX = Double.POSITIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		for (MultiPolygon mp : this.mps.keySet()) {
			Geometry geo = mp.getBoundary();
			Coordinate[] coords = geo.getCoordinates();
			for (int i = 0; i < coords.length; i++) {
				if (coords[i].x < minX) {
					minX = coords[i].x;
				}
				if (coords[i].y < minY) {
					minY = coords[i].y;
				}
				
			}
			
		}
		this.sim2DVis = new Sim2DVis(minX, minY);
		this.sim2DVis.drawFloorPlans(this.mps);
		
	}




	@Override
	protected void runMobSim() {
		
		Sim2D sim = new Sim2D(this.network,this.mps,this.population,this.events,this.sff, this.config);
		if (this.getIterationNumber() % 10 == 0) {
			sim.setSim2DVis(this.sim2DVis );
		}
		sim.run();
	}

	public static void main(String [] args){
		Controler controller = new Controller2D(args);
		controller.run();

	}

}
