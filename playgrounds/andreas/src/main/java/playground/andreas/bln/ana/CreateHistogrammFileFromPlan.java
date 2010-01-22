package playground.andreas.bln.ana;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.TreeMap;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.utils.misc.Time;

import playground.andreas.bln.pop.NewPopulation;

/**
 * Filter persons, not using a specific TransportMode.
 *
 * @author aneumann
 *
 */
public class CreateHistogrammFileFromPlan extends NewPopulation {
	private final int planswritten = 0;
	private int personshandled = 0;

	private final LinkedList<Integer> group1 = new LinkedList<Integer>();
	private final LinkedList<Integer> group2 = new LinkedList<Integer>();

	private TreeMap<Integer, Integer> group1Count = new TreeMap<Integer, Integer>();
	private TreeMap<Integer, Integer> group2Count = new TreeMap<Integer, Integer>();

	private BufferedWriter writer1;

	public CreateHistogrammFileFromPlan(Network network, Population plans, String filename) {
		super(network, plans, filename);

		try {
			this.writer1 = new BufferedWriter(new FileWriter("./passenger1.txt"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void run(Person person) {

		Plan plan = person.getSelectedPlan();
		ActivityImpl act = (ActivityImpl) plan.getPlanElements().get(0);
		act.getEndTime();

			if(person.getId().toString().contains("passenger1")){
				this.group1.add(Integer.valueOf(Double.valueOf(act.getEndTime()).intValue()));
				this.personshandled++;
			} else if(person.getId().toString().contains("passenger2")){
				this.group2.add(Integer.valueOf(Double.valueOf(act.getEndTime()).intValue()));
				this.personshandled++;
			} else {
				System.err.println("Person " + person.getId() + " could not been identified");
			}
	}

	public void createHistogrammTable() throws IOException{


		Collections.sort(this.group1);
		Collections.sort(this.group2);

		int binSize = 30; // seconds
		int binStartTime = Math.min(this.group1.peek().intValue(), this.group2.peek().intValue()) - (Math.min(this.group1.peek().intValue(), this.group2.peek().intValue()) % binSize);

		this.group1Count = createHistogrammTable(binSize, binStartTime, this.group1);
		this.group2Count = createHistogrammTable(binSize, binStartTime, this.group2);

		this.writer1.write("Time Time group1 group2");
		this.writer1.newLine();

		for (int i = binStartTime; i <= this.group1Count.lastKey().intValue() || i <= this.group2Count.lastKey().intValue(); i += binSize) {

			String group1str;

			if (this.group1Count.get(Integer.valueOf(i)) == null){
				group1str = "0";
			} else {
				group1str = this.group1Count.get(Integer.valueOf(i)).toString();
			}

			String group2str;

			if (this.group2Count.get(Integer.valueOf(i)) == null){
				group2str = "0";
			} else {
				group2str = this.group2Count.get(Integer.valueOf(i)).toString();
			}

			this.writer1.write(i + " " + Time.writeTime(i) + " " + group1str + " " + group2str);
			this.writer1.newLine();

		}





	}

	private TreeMap<Integer, Integer> createHistogrammTable(int binSize, int givenBinStartTime, LinkedList<Integer> startTimes){

		int binStartTime = givenBinStartTime;

		TreeMap<Integer, Integer> countMap = new TreeMap<Integer, Integer>();

		int runningCounter = 0;

		while(binStartTime + binSize < startTimes.peek().intValue()){
			countMap.put(Integer.valueOf(binStartTime), Integer.valueOf(runningCounter));
			binStartTime = binStartTime + binSize;
		}

		for (Integer entry : startTimes) {

			if (entry.intValue() < binStartTime + binSize){
				runningCounter++;
			} else {
				countMap.put(Integer.valueOf(binStartTime), Integer.valueOf(runningCounter));
				runningCounter = 0;
				binStartTime = binStartTime + binSize;

				while(binStartTime + binSize < entry.intValue()){
					countMap.put(Integer.valueOf(binStartTime), Integer.valueOf(runningCounter));
					binStartTime = binStartTime + binSize;
				}
				runningCounter++;
			}

		}

		countMap.put(Integer.valueOf(binStartTime), Integer.valueOf(runningCounter));
		return countMap;

	}


	public void flush(){
		try {
			this.writer1.flush();
			this.writer1.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(final String[] args) {
		Gbl.startMeasurement();

		ScenarioImpl sc = new ScenarioImpl();

		String networkFile = "./net.xml";
		String inPlansFile = "./1000.plans.xml.gz";
		String outPlansFile = "./baseplan_car_pt_only.xml.gz";

		NetworkLayer net = sc.getNetwork();
		new MatsimNetworkReader(sc).readFile(networkFile);

		PopulationImpl inPop = sc.getPopulation();
		PopulationReader popReader = new MatsimPopulationReader(sc);
		popReader.readFile(inPlansFile);

		CreateHistogrammFileFromPlan dp = new CreateHistogrammFileFromPlan(net, inPop, outPlansFile);
		dp.run(inPop);
		System.out.println(dp.personshandled + " persons handled; " + dp.planswritten + " plans written to file");
		dp.writeEndPlans();
		try {
			dp.createHistogrammTable();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		dp.flush();

		Gbl.printElapsedTime();
	}
}
