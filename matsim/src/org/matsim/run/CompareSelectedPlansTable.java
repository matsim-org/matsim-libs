package org.matsim.run;

import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.basic.v01.BasicPlan.LegIterator;
import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.network.NetworkLayerBuilder;
import org.matsim.plans.Leg;
import org.matsim.plans.MatsimPlansReader;
import org.matsim.plans.Person;
import org.matsim.plans.Plans;
import org.matsim.plans.PlansReaderI;
import org.matsim.utils.identifiers.IdI;
import org.matsim.utils.io.IOUtils;

public class CompareSelectedPlansTable {
	
	private Plans plans0;
	private Plans plans1;
	private String header="person_id\tsex\tage\tlicense\tcar_avail\t" +
			"employed\thome_x\thome_y\thome_link\tscore_0\tscore_1\ttravel_time_0\ttravel_time_1\t";
	private NetworkLayer network;
	
	
	
	/**
	 * @param args: 
	 * arg 0: path to plans file 0
	 * arg 1: path to plans file 1
	 * arg 2: name of output file
	 * arg 3: path to network file
	 */
	public static void main(String[] args) {
				
		if (args.length < 4) {
			System.out.println("Too few arguments.");
			printUsage();
			System.exit(1);
		}
		
		Gbl.startMeasurement();
		CompareSelectedPlansTable table=new CompareSelectedPlansTable();
		table.run(args[0], args[1], args[2], args[3]);
				
		Gbl.printElapsedTime();
	}
	
	private void init(String networkPath) {
		this.plans0=new Plans(false);
		this.plans1=new Plans(false);
		
		System.out.println("  reading the network...");
		NetworkLayerBuilder.setNetworkLayerType(NetworkLayerBuilder.NETWORK_DEFAULT);
		network = (NetworkLayer)Gbl.getWorld().createLayer(NetworkLayer.LAYER_TYPE,null);
		new MatsimNetworkReader(network).readFile(networkPath);		
	}
	
	private void readFiles(String plansfilePath0, String plansfilePath1) {
		System.out.println("  reading file "+plansfilePath0);
		PlansReaderI plansReader0 = new MatsimPlansReader(plans0);
		plansReader0.readFile(plansfilePath0);
		
		System.out.println("  reading file "+plansfilePath1);
		PlansReaderI plansReader1 = new MatsimPlansReader(plans1);
		plansReader1.readFile(plansfilePath1);	
	} 
	
	private void writeSummaryFile(String outfile) {
		try {
			BufferedWriter out = IOUtils.getBufferedWriter(outfile);
			out.write(this.header + "\n");
			
			for (IdI person_id : plans0.getPersons().keySet()) {
				
				// method person.toString() not appropriate
				out.write(person_id.toString()+"\t");
				Person person=plans0.getPerson(person_id);
				out.write(person.getSex()+"\t");
				out.write(person.getAge()+"\t");
				out.write(person.getLicense()+"\t");
				out.write(person.getCarAvail()+"\t");
				out.write(person.getEmployed()+"\t");
				
				
				if (person.getSelectedPlan().getFirstActivity().getType().substring(0,1).equals("h")) {
					out.write(person.getSelectedPlan().getFirstActivity().getCoord().getX()+"\t");
					out.write(person.getSelectedPlan().getFirstActivity().getCoord().getY()+"\t");
					out.write(person.getSelectedPlan().getFirstActivity().getLinkId()+"\t");
				}
				else {
					// no home activity in the plan -> no home activity in the knowledge
					out.write("-\t-\t-\t");
				}
				
				out.write(person.getSelectedPlan().getScore()+"\t");
				
				Person person_comp=plans1.getPerson(person_id);
				out.write(person_comp.getSelectedPlan().getScore()+"\t");
				
				out.write(this.getTravelTime(person)+"\t");
				out.write(this.getTravelTime(person_comp)+"\t");
				
				out.write("\n");
				out.flush();
			}
			out.close();
		}
		catch (IOException e) {
			Gbl.errorMsg(e);
		}
	}

	private double getTravelTime(Person person) {
		
		double travelTime=0.0;
		LegIterator leg_it = person.getSelectedPlan().getIteratorLeg();
		while (leg_it.hasNext()) {
			Leg leg = (Leg)leg_it.next();
			travelTime+=leg.getTravTime();
		}		
		return travelTime;		
	}
	
	private void run(String plansfilePath0, String plansfilePath1, String outfile, String networkPath) {
		this.init(networkPath);
		readFiles(plansfilePath0, plansfilePath1);
		writeSummaryFile(outfile);
	}	
	
	private static void printUsage() {
		System.out.println();
		System.out.println("WinnerLoserSummary:");
		System.out.println();
		System.out.println("Creates an agent-based winner-loser table including all agent \n" +
				"attributes, the selected plan score and the total travel time");
		System.out.println();
		System.out.println("usage: WinnerLoserSummary args");
		System.out.println(" arg 0: path to plans file 0 (required)");
		System.out.println(" arg 1: path to plans file 1 (required)");
		System.out.println(" arg 2: name of output file (required)");
		System.out.println(" arg 3: path to network file (required)");

		System.out.println("----------------");
		System.out.println("2008, matsim.org");
		System.out.println();
	}
	
}
