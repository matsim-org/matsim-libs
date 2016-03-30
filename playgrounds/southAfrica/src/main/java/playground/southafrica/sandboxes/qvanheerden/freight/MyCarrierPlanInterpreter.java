package playground.southafrica.sandboxes.qvanheerden.freight;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.contrib.freight.carrier.CarrierPlanXmlReaderV2;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.contrib.freight.carrier.ScheduledTour;
import org.matsim.contrib.freight.carrier.Tour.Leg;
import org.matsim.contrib.freight.carrier.Tour.ServiceActivity;
import org.matsim.contrib.freight.carrier.Tour.TourActivity;
import org.matsim.contrib.freight.carrier.Tour.TourElement;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vehicles.Vehicle;

import playground.southafrica.utilities.Header;
import playground.southafrica.utilities.filesampler.MyFileFilter;
import playground.southafrica.utilities.filesampler.MyFileSampler;

public class MyCarrierPlanInterpreter {

	public final static Logger log = Logger.getLogger(MyCarrierPlanInterpreter.class);
	public static String outputDir;
	public static Network network;

	public static void main(String[] args) {
		Header.printHeader(MyCarrierPlanInterpreter.class.toString(), args);
		/*
		 * Specify a directory in which the carrier plans are.  This will allow
		 * to more easily run generic analyses on all plan files.
		 */
		String carrierPlanDirectory = args[0];
		outputDir = args[1];
		String networkFile = args[2];

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimNetworkReader reader = new MatsimNetworkReader(scenario.getNetwork());
		reader.readFile(networkFile);
		network = scenario.getNetwork();

		MyFileSampler mfs = new MyFileSampler(carrierPlanDirectory);
		List<File> listOfPlans = mfs.sampleFiles(Integer.MAX_VALUE, new MyFileFilter(".xml"));

		for(File file : listOfPlans){
			Carriers carriers = new Carriers();
			carriers.getCarriers().clear();
			new CarrierPlanXmlReaderV2(carriers).read(file.getAbsolutePath());
			Carrier carrier = carriers.getCarriers().get(Id.create("MyCarrier", Carrier.class));
			CarrierPlan plan = carrier.getSelectedPlan();

			//use filename to generate output filename
			String outputFile = file.getName().substring(0, file.getName().indexOf("."));
			MyCarrierPlanInterpreter.analyseTours(plan, outputFile);
		}

		Header.printFooter();
	}

	public static void analyseTours(CarrierPlan plan, String outputFile){
		int number = 0;
		
		Collection<ScheduledTour> tours = plan.getScheduledTours();
		Map<Id, List<Double>> tourMap = new HashMap<Id, List<Double>>();
		Map<Id, Tuple<List<Double>,List<Double>>> detailMap = new HashMap<Id, Tuple<List<Double>,List<Double>>>();
		List<Double> tourList;
		double planScore = plan.getScore();

		for(ScheduledTour tour : tours){
			tourList = new ArrayList<Double>();
			//vehicle ID
			Id<Vehicle> vehId = tour.getVehicle().getVehicleId();

			List<Double> interActDistance = new ArrayList<Double>();
			List<Double> interActTravelTime = new ArrayList<Double>();
			double totalDistance = 0;
			double totalTravelTime = 0;
			double totalActivityTime = 0;
			int numOfAct = 0;
			Id prevLoc = null;
			boolean isSameLoc = false;

			//distance & time
			for(TourElement te : tour.getTour().getTourElements()){
				if(te instanceof TourActivity){
					TourActivity act = (TourActivity) te;
					totalActivityTime += act.getDuration();
					
					if(prevLoc==null){
						prevLoc = act.getLocation();
						isSameLoc = false;
					}else{
						if(act.getLocation().equals(prevLoc)){
							isSameLoc = true;
						}else{
							isSameLoc = false;
						}
					}
					
					//calculate number of activities per chain
					if(te instanceof ServiceActivity && !isSameLoc){
						numOfAct++;
					}
				}

				if(te instanceof Leg){
					Leg leg = (Leg) te;

					double distance = RouteUtils.calcDistanceExcludingStartEndLink((NetworkRoute) leg.getRoute(), network);

					if(distance>0){
						interActDistance.add(distance);
					}
					totalDistance += distance;
					double travelTime = leg.getExpectedTransportTime();
					if(travelTime>0){
						interActTravelTime.add(travelTime);
					}
					totalTravelTime += travelTime;
				}
			}
			//add to list and add to map
			tourList.add(planScore);
			tourList.add((double) numOfAct);
			tourList.add(totalDistance);
			tourList.add(totalTravelTime);
			tourList.add(totalActivityTime);
			
			if(tourMap.keySet().contains(vehId) || detailMap.keySet().contains(vehId)){
				vehId = Id.create(vehId.toString() + "_" + number, Vehicle.class);
				number++;
			}
			
			tourMap.put(vehId, tourList);

			//add lists to detail map
			detailMap.put(vehId, new Tuple<List<Double>, List<Double>>(interActDistance,interActTravelTime));

		}
		MyCarrierPlanInterpreter.writeTotalValuesToFile(outputDir + "/" + outputFile + "_sumValues.csv", tourMap);
		MyCarrierPlanInterpreter.writeListDetailsToFile(detailMap, outputDir + "/" + outputFile + "_distances.csv", outputDir + "/" + outputFile + "_times.csv");
	}

	public static void writeTotalValuesToFile(String filename, Map<Id, List<Double>> tourMap){
		log.info("Writing total values...");
		BufferedWriter bw = IOUtils.getBufferedWriter(filename);

		try {
			bw.write("vehId, score, numOfAct, totDist, totTravelTime, totActTime");
			bw.newLine();

			for(Id id : tourMap.keySet()){
				bw.write(id.toString());
				bw.write(",");

				for(int i = 0; i < tourMap.get(id).size()-1; i++){
					if(i == 1){
						bw.write(String.format("%.0f", tourMap.get(id).get(i)));
					}else{
						bw.write(String.format("%.2f", tourMap.get(id).get(i)));
					}
					bw.write(",");
				}
				//write last value in list without comma afterwards
				bw.write(String.format("%.2f", tourMap.get(id).get(tourMap.get(id).size()-1)));
				bw.newLine();
			}

		} catch (IOException e) {
			log.error("Could not write to file: " + filename);
		}finally{
			try {
				bw.close();
			} catch (IOException e) {
				log.error("Could not close writer");
			}
		}
		log.info("Finished writing total values...");
	}

	public static void writeListDetailsToFile(Map<Id, Tuple<List<Double>, List<Double>>> map, String filenameDist, String filenameTime){
		BufferedWriter bwDist = IOUtils.getBufferedWriter(filenameDist);
		BufferedWriter bwTime = IOUtils.getBufferedWriter(filenameTime);

		log.info("Writing times and distances...");
		for(Id id : map.keySet()){

			try {
				bwDist.write(id.toString());
				bwDist.write(",");

				List<Double> list = map.get(id).getFirst();
				for(int i = 0; i < list.size() -1; i++){
					bwDist.write(String.format("%.2f", list.get(i)));
					bwDist.write(",");
				}
				//write last value in list
				bwDist.write(String.format("%.2f", list.get(list.size()-1)));
				bwDist.newLine();

			} catch (IOException e) {
				log.error("Could not write to file: " + filenameDist);
			}
			
			try {
				bwTime.write(id.toString());
				bwTime.write(",");
				
				List<Double> list = map.get(id).getSecond();
				for(int i = 0; i < list.size() -1; i++){
					bwTime.write(String.format("%.0f", list.get(i)));
					bwTime.write(",");
				}
				//write last value in list
				bwTime.write(String.format("%.0f", list.get(list.size()-1)));
				bwTime.newLine();
				
			} catch (IOException e1) {
				log.error("Could not write to file: " + filenameTime);
			}
		}

		try {
			bwDist.close();
			log.info("Finished writing distances...");
		} catch (IOException e2) {
			log.error("Could not close distance writer");
		}
		try {
			bwTime.close();
			log.info("Finished writing times...");
		} catch (IOException e3) {
			log.error("Could not close time writer");
		}
	}

}
