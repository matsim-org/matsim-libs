package playground.andreas.fggeoinfo;

import java.util.HashMap;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.utils.geometry.transformations.GK4toWGS84;

import playground.andreas.utils.ana.acts2kml.KMLActsWriter;
import playground.andreas.utils.pop.NewPopulation;

/**
 * Move acts from TXL and SXF to BBI coords
 *
 * @author aneumann
 *
 */
public class FilterPersonActs extends NewPopulation {

	Coord minSXF;
	Coord maxSXF;
	
	Coord minTXL;
	Coord maxTXL;
	
	Coord coordBBI;
		
	HashMap<String, Integer> actSXF = new HashMap<String, Integer>();
	HashMap<String, Integer> actTXL = new HashMap<String, Integer>();
		
	private int planswritten = 0;
	private int personshandled = 0;
	private int nAct = 0;
	
	private boolean kmlOutputEnabled = false;
	private KMLActsWriter kmlWriter = new KMLActsWriter();

	public FilterPersonActs(Network network, Population plans, String filename,
			Coord minSXF, Coord maxSXF, Coord minTXL, Coord maxTXL, Coord coordBBI) {
		super(network, plans, filename);
		this.minSXF = minSXF;
		this.maxSXF = maxSXF;
		this.minTXL = minTXL;
		this.maxTXL = maxTXL;
		this.coordBBI = coordBBI;
	}

	@Override
	public void run(Person person) {

		this.personshandled++;
		
//		boolean keepPerson = true;

		Plan plan = person.getSelectedPlan();
		
		for (PlanElement plan_element : plan.getPlanElements()) {
			if (plan_element instanceof ActivityImpl){
				this.nAct++;
				ActivityImpl act = (ActivityImpl) plan_element;
				if(checkIsSXF(act)){
					if(this.actSXF.get(act.getType()) == null){
						this.actSXF.put(act.getType(), new Integer(1));
					} else {
						this.actSXF.put(act.getType(), new Integer(this.actSXF.get(act.getType()).intValue() + 1 ));
					}
					if(this.kmlOutputEnabled){
						this.kmlWriter.addActivity(new ActivityImpl(act));
					}
					act.getCoord().setXY(this.coordBBI.getX(), this.coordBBI.getY());
					person.setId(new IdImpl(person.getId().toString() + "_SXF-BBI"));
				}
				
				if(checkIsTXL(act)){
					if(this.actTXL.get(act.getType()) == null){
						this.actTXL.put(act.getType(), new Integer(1));
					} else {
						this.actTXL.put(act.getType(), new Integer(this.actTXL.get(act.getType()).intValue() + 1 ));
					}
					if(this.kmlOutputEnabled){
						this.kmlWriter.addActivity(new ActivityImpl(act));
					}
					act.getCoord().setXY(this.coordBBI.getX(), this.coordBBI.getY());
					person.setId(new IdImpl(person.getId().toString() + "_TXL-BBI"));
				}
			}
		}
		
		
//		keepPerson = false;
		
//		if(keepPerson){
			this.popWriter.writePerson(person);
			this.planswritten++;
//		}

	}

	private boolean checkIsSXF(Activity act){
		boolean isSXF = true;
		
		if(act.getCoord().getX() < this.minSXF.getX()){isSXF = false;}
		if(act.getCoord().getX() > this.maxSXF.getX()){isSXF = false;}
		
		if(act.getCoord().getY() < this.minSXF.getY()){isSXF = false;}
		if(act.getCoord().getY() > this.maxSXF.getY()){isSXF = false;}
		
		return isSXF;
	}
	
	private boolean checkIsTXL(Activity act){
		boolean isTXL = true;

		if(act.getCoord().getX() < this.minTXL.getX()){isTXL = false;}
		if(act.getCoord().getX() > this.maxTXL.getX()){isTXL = false;}
		
		if(act.getCoord().getY() < this.minTXL.getY()){isTXL = false;}
		if(act.getCoord().getY() > this.maxTXL.getY()){isTXL = false;}
		
		return isTXL;
	}
	
	public static void filterPersonActs(String networkFile, String inPlansFile, String outPlansFile,
			Coord minSXF, Coord maxSXF, Coord minTXL, Coord maxTXL, Coord coordBBI,
			String kmzOutputDir, String kmzOutputFile){
		Gbl.startMeasurement();

		ScenarioImpl sc = new ScenarioImpl();

		NetworkImpl net = sc.getNetwork();
		new MatsimNetworkReader(sc).readFile(networkFile);

		Population inPop = sc.getPopulation();
		PopulationReader popReader = new MatsimPopulationReader(sc);
		popReader.readFile(inPlansFile);

		FilterPersonActs dp = new FilterPersonActs(net, inPop, outPlansFile, minSXF, maxSXF, minTXL, maxTXL, coordBBI);
		dp.enableKMLOutput(kmzOutputFile, kmzOutputDir);
		dp.run(inPop);
		System.out.println(dp.personshandled + " persons handled; " + dp.planswritten + " plans written to file");
		
		System.out.println("SXF nActs:");
		for (Entry<String, Integer> entry : dp.actSXF.entrySet()) {
			System.out.println(entry.getKey() + " " + entry.getValue());
		}
		System.out.println("TXL nActs:");
		for (Entry<String, Integer> entry : dp.actTXL.entrySet()) {
			System.out.println(entry.getKey() + " " + entry.getValue());
		}
		dp.writeEndPlans();
		dp.writeKML();

		Gbl.printElapsedTime();
	}

	private void enableKMLOutput(String name, String dir) {
		this.kmlOutputEnabled = true;
		this.kmlWriter.setCoordinateTransformation(new GK4toWGS84());
		this.kmlWriter.setKmzFileName(name);
		this.kmlWriter.setOutputDirectory(dir);
	}
	
	private void writeKML(){
		this.kmlWriter.writeFile();
	}
}
