package playground.dziemke.feathersMatsim.ikea.Simulation;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.vehicles.Vehicle;




public class ResidentialAreaTravelDisutilityCalculator implements TravelDisutility{

	private TravelTime timeCalculator;
	private double marginalUtlOfMoney;
	private double distanceCostRateCar;
	private double marginalUtlOfTravelTime;
	private String dataFile="C:/Users/jeffw_000/Desktop/Dropbox/Uni/Master/Masterarbeit/MT/workspace new/ikeaStudy/input/residentialArea.csv";
	private List<Coord> nodeList = new ArrayList<Coord>();
	private List<String> linkList = new ArrayList<String>();
	private int index_nodeID=1, index_nodeX=2, index_nodeY=3, index_linkID=5;

	public ResidentialAreaTravelDisutilityCalculator(TravelTime timeCalculator, PlanCalcScoreConfigGroup cnScoringGroup) {
		this.timeCalculator = timeCalculator;
		this.marginalUtlOfMoney = cnScoringGroup.getMarginalUtilityOfMoney();
		this.distanceCostRateCar = cnScoringGroup.getModes().get(TransportMode.car).getMonetaryDistanceRate();
		this.marginalUtlOfTravelTime = (-cnScoringGroup.getModes().get(TransportMode.car).getMarginalUtilityOfTraveling() / 3600.0) + (cnScoringGroup.getPerforming_utils_hr() / 3600.0);

		try {
			BufferedReader reader=new BufferedReader(new FileReader(dataFile));
			String line=reader.readLine();
			while ((line=reader.readLine()) != null){
				String parts[] = line.split(";");
				if(!parts[index_nodeID].isEmpty()){
					Coord nodeCoord=new Coord(Double.parseDouble(parts[index_nodeX]),Double.parseDouble(parts[index_nodeY]));
					nodeList.add(nodeCoord);}
				if(!parts[index_linkID].isEmpty()){
					linkList.add(parts[index_linkID]);
				}
			}
			reader.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public double getLinkTravelDisutility(Link link, double time,
			Person person, Vehicle vehicle) {
		double linkTravelTime = this.timeCalculator.getLinkTravelTime(link, time, person, vehicle);
		double linkTravelTimeDisutility = this.marginalUtlOfTravelTime * linkTravelTime ;

		double distance = link.getLength();
		double distanceCost = - this.distanceCostRateCar * distance;
		double linkDistanceDisutility = this.marginalUtlOfMoney * distanceCost;

		double linkTravelDisutility = linkTravelTimeDisutility + linkDistanceDisutility;

		PlanElement pe=person.getSelectedPlan().getPlanElements().get(0);
		Activity homeActivity=(Activity)pe;
		Coord homeCoord=homeActivity.getCoord();

		if(!nodeList.contains(homeCoord)){
			if(link.toString().contains("id=Link4Residents2")||link.toString().contains("id=Link4Residents1")){

				linkTravelDisutility=linkTravelDisutility*10000;}
		}


		return linkTravelDisutility;
	}

	@Override
	public double getLinkMinimumTravelDisutility(Link link) {
		throw new UnsupportedOperationException();
	}

}
