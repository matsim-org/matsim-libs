package playground.dressler.control;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;

import playground.dressler.ea_flow.Flow;
import playground.dressler.ea_flow.TimeExpandedPath;
import playground.dressler.util.CPUTimer;
import playground.dressler.util.ImportSimpleNetwork;

public class DanielMain {
	
	/**
	 * main method to run an EAF algorithm on the specified scenario
	 * @param args CLI arguments
	 *
	 */
	public static void main(final String[] args) {

		// parse the arguments
		
		String networkfile = null;
		String plansfile = null;
		String sinkid = null;
		String simplenetworkfile = null;		
		boolean writeflow = false;
		int uniformDemands = 0;
		
		FlowCalculationSettings settings = new FlowCalculationSettings();
		
		{
			int i = 0;
			String error = "";
			boolean argsokay = true;

			while (i < args.length) {
				String s = args[i].trim().toLowerCase();
				if (s.equals("--networkdat")) {
					i++;
					if (i < args.length) {
						simplenetworkfile = args[i];
					} else {
						argsokay= false;
						error += "--networkdat requires an argument (filename)\n";
					}
				} else if (s.equals("--networkxml")) {
					i++;
					if (i < args.length) {
						networkfile = args[i];
					} else {
						argsokay= false;
						error += "--networkxml requires an argument (filename)\n";
					}
				} else if (s.equals("--config")) {
					i++;
					if (i < args.length) {
						String text = "";
						String line;

						try {
							BufferedReader in = new BufferedReader(new FileReader(args[i]));						
							while ((line = in.readLine()) != null) {
								text += line + "\n"; 
							}
							in.close();
						} catch (IOException e) {
							argsokay = false;
							error += "Error reading --config file.\n";
						}
						settings.readConfig(text);
					} else {
						argsokay= false;
						error += "--config requires an argument (filename)\n";
					}			
				} else if (s.equals("--sinkid")) {
					i++;
					if (i < args.length) {
						sinkid = args[i];
					} else {
						argsokay= false;
						error += "--sinkid requires an argument (string)\n";
					}
				} else if (s.equals("--uniform")) {
					i++;
					if (i < args.length) {
						uniformDemands = Integer.parseInt(args[i]);
					} else {
						argsokay= false;
						error += "--uniform requires an argument (int)\n";
					}
				}  else if (s.equals("--plans")) {
					i++;
					if (i < args.length) {
						plansfile = args[i];
					} else {
						argsokay= false;
						error += "--plans requires an argument (filename)\n";
					}
				}  else if (s.equals("--writeflow")) {
					i++;
					if (i < args.length) {
						writeflow = Boolean.parseBoolean(args[i]);
					} else {
						argsokay= false;
						error += "--writeflow requires an argument (boolean)\n";
					}
				} else if (s.equals("--help")) {
					argsokay= false;
					error += "Possible command line options\n";
					error += "* Data input *\n";
					error += "--networkxml filename\n";
					error += "--networkdat filename\n";
					error += "--plans filename\n";
					error += "--sinkid string\n";
					error += "--uniform int\n";
					error += "* Scaling * \n";
					error += "--timehorizon int\n";
					error += "--timestep double\n";
					error += "--flowfactor double\n";
					error += "* Search Settings *\n";
					error += "--config filenameyet\n";
					error += "* and many more ... *\n";
				} else {
					String options;
					i++;
					if (i < args.length) {
						options = s + "\n" + args[i];
					} else {
						options = s;
					}

					String tmperror = settings.readConfig(options);

					if (tmperror.length() > 0) {					
						argsokay = false;
						error += "unknown option: " + tmperror;
					}
				}	
				
				i++;
			}

			if (!argsokay) {
				System.out.println(error);
				
				return;
			} 
		}
		
		System.out.println("Starting to read network etc.");
		
		
		ScenarioImpl scenario = new ScenarioImpl();
		NetworkImpl network = scenario.getNetwork();
		HashMap<Node, Integer> demands = null;
		Node sink = null;

		//read network
		if (networkfile != null) {
			scenario = new ScenarioImpl();
			network = scenario.getNetwork();
			MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario);
			networkReader.readFile(networkfile);
			sink = network.getNodes().get(new IdImpl(sinkid));
			if (sink == null){
				System.out.println("sink not found");
			}
		} else if (simplenetworkfile != null) {
			ImportSimpleNetwork importer = new ImportSimpleNetwork(simplenetworkfile);
			try {
				network = importer.getNetwork();
				demands = importer.getDemands();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		
		//read demands
		if(plansfile!=null){
			demands = MultiSourceEAF.readPopulation(scenario, plansfile);
		} else if (simplenetworkfile != null) {
			// we should already have read those ...
		} else {
			// uniform demands
			demands = new HashMap<Node, Integer>();
			for (Node node : network.getNodes().values()) {
				if (!node.getId().equals(sink.getId())) {
					demands.put(node, Math.max(uniformDemands,0));
				}
			}

			// Hack for those networks with en1->en2
			// set demand of en2 to 0, because it cannot be satisfied if en1 is the sink
			if (sinkid != null && sinkid.equals("en1")) {
				Node sink2 = network.getNodes().get(new IdImpl("en2"));
				if (sink2 != null) {
					Integer d = demands.get(sink2);
					if (d != null && d > 0) {
						demands.put(sink2, 0);
					}
				}
			}
		}
		
		int totaldemands = 0;
		for (int i : demands.values()) {
			if (i > 0)
			  totaldemands += i;
		}
		
		//check if demands and sink are set
		if (demands.isEmpty()) {
			System.out.println("Error: Demands not found, aborting.");
			return;
		}

		settings.setNetwork(network);
		settings.setDemands(demands);
		settings.supersink = sink;

		System.out.println("Finished reading input.");
		
		/* --------- the actual work starts --------- */

		boolean settingsOkay = settings.prepare();
		settings.printStatus();
		System.out.println("settings as parameters:");
		System.out.println(settings.writeConfig());
		System.out.println();
		
		if(!settingsOkay) {
			System.out.println("Error: Something was bad in the settings, aborting.");
			return;
		}

		Flow fluss;

		fluss = MultiSourceEAF.calcEAFlow(settings, null);
		
		//fluss.writePathflow(false);

		/* --------- the actual work is done --------- */
		

		// decompose the flow
		Flow reconstructedFlow = new Flow(settings);
		CPUTimer Tdecompose = new CPUTimer("Path Decomposition");
		CPUTimer Treconstruct = new CPUTimer("Flow Reconstruction");
		
		Tdecompose.onoff();
		LinkedList<TimeExpandedPath> decomp = fluss.doPathDecomposition(); 
		Tdecompose.onoff();
		
		Treconstruct.onoff();
		if( decomp !=null) {
			System.out.println("reconstructing flow");			
			for (TimeExpandedPath path : decomp) {
				reconstructedFlow.augment(path,path.getFlow());
			}
			reconstructedFlow.cleanUp();
		}		
		Treconstruct.onoff();
		
		fluss = reconstructedFlow;
		
		System.out.println("== After decomposition & reconstruction ==");
		System.out.println(Tdecompose);
		System.out.println(Treconstruct);
		
		int[] arrivals2 = fluss.arrivals();
		long totalcost2 = 0;
		for (int i = 0; i < arrivals2.length; i++) {
			totalcost2 += i*arrivals2[i];
		}

		System.out.println("Total cost: " + totalcost2);
		System.out.println("Collected " + fluss.getPaths().size() + " paths.");

		System.out.println(fluss.arrivalsToString());
		System.out.println(fluss.arrivalPatternToString());
		System.out.println("unsatisfied demands:");
		for (Node node : fluss.getDemands().keySet()){
			int demand = fluss.getDemands().get(node);
			if (demand > 0) {
				// this can be a lot of text				
				System.out.println("node:" + node.getId().toString()+ " demand:" + demand);
			}
		}
		
		if (writeflow) {
			System.out.println();
			fluss.writePathflow(false);
		}
		
		System.out.println("Done.");
		return;
	}
		
}
