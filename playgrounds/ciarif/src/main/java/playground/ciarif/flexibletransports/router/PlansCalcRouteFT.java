package playground.ciarif.flexibletransports.router;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.BasicLocation;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkFactoryImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.core.router.util.TravelTime;

import playground.balmermi.world.Layer;
import playground.ciarif.flexibletransports.data.MyTransportMode;
import playground.ciarif.flexibletransports.network.MyLinkUtils;
import playground.meisterk.kti.router.SwissHaltestelle;
import playground.meisterk.kti.router.SwissHaltestellen;

public class PlansCalcRouteFT extends PlansCalcRoute
{
  private final Network network;
  private final PlansCalcRouteFtInfo plansCalcRouteFtInfo;
  private MyLinkUtils myLinkUtils;
  private static final Logger log = Logger.getLogger(PlansCalcRouteFT.class);

  public PlansCalcRouteFT(PlansCalcRouteConfigGroup group, 
		  Network network, 
		  PersonalizableTravelCost costCalculator, 
		  PersonalizableTravelTime timeCalculator, 
		  LeastCostPathCalculatorFactory factory, 
		  PlansCalcRouteFtInfo ptRoutingInfo)
  {
    super(group, network, costCalculator, timeCalculator, factory);
    this.network = network;
    this.plansCalcRouteFtInfo = ptRoutingInfo;
    this.myLinkUtils = new MyLinkUtils(network);
  }

  public double handleLeg(Person person, Leg leg, Activity fromAct, Activity toAct, double depTime)
  {
    String mode = leg.getMode();


    double travelTime = 0.0D;

    Link fromLink = (Link)this.network.getLinks().get(fromAct.getLinkId());
    Link toLink = (Link)this.network.getLinks().get(toAct.getLinkId());
    if (fromLink.equals(toLink))
    {
      Route route = getRouteFactory().createRoute(mode, fromLink.getId(), toLink.getId());
      route.setTravelTime(travelTime);
      if (Double.isNaN(route.getDistance())) {
        route.setDistance(0.0D);
      }
      leg.setRoute(route);
      leg.setDepartureTime(depTime);
      leg.setTravelTime(travelTime);
      ((LegImpl)leg).setArrivalTime(depTime + travelTime);
    }

    /*if (mode.equals(TransportMode.pt))
    {
      travelTime = handleSwissPtLeg(fromAct, leg, toAct, depTime);
    }*/
    //else if
    if (mode.equals(MyTransportMode.carsharing))
    {
      travelTime = handleCarSharingLeg(person,(LegImpl) leg, (ActivityImpl)fromAct, (ActivityImpl)toAct, depTime);
    }
    else {
    	log.info("mode = " + mode.toString());
      travelTime = super.handleLeg(person, leg, fromAct, toAct, depTime);
    }

    return travelTime;
  }

  private double handleCarSharingLeg(Person person, LegImpl leg, ActivityImpl fromAct, ActivityImpl toAct, double depTime)
  {
    double travelTime = 0.0D;

    CarSharingStation fromStation = this.plansCalcRouteFtInfo.getCarStations().getClosestLocation(fromAct.getCoord());
    CarSharingStation toStation = this.plansCalcRouteFtInfo.getCarStations().getClosestLocation(toAct.getCoord());

    FtCarSharingRoute newRoute = new FtCarSharingRoute(fromAct.getLinkId(), toAct.getLinkId(), this.plansCalcRouteFtInfo, fromStation, toStation);
    leg.setRoute(newRoute);

    double timeInVehicle = calcCarTime(person, fromStation, toStation, depTime);

    double walkAccessDistance = newRoute.calcAccessDistance(fromAct);
    double walkEgressDistance = newRoute.calcAccessDistance(toAct);
    double walkAccessTime = getAccessEgressTime(walkAccessDistance, this.configGroup);
    double walkEgressTime = getAccessEgressTime(walkEgressDistance, this.configGroup);

    newRoute.setDistance(walkAccessDistance + walkEgressDistance + newRoute.calcCarDistance(fromAct));

    travelTime = walkAccessTime + walkEgressTime + timeInVehicle;

    leg.setDepartureTime(depTime);
    leg.setTravelTime(travelTime);
    leg.setArrivalTime(depTime + travelTime);

    return travelTime;
  }

  /*public double handleSwissPtLeg(Activity fromAct, Leg leg, Activity toAct, double depTime)
  {
    double travelTime = 0.0D;

    SwissHaltestelle fromStop = this.plansCalcRouteFtInfo.getHaltestellen().getClosestLocation(fromAct.getCoord());
    SwissHaltestelle toStop = this.plansCalcRouteFtInfo.getHaltestellen().getClosestLocation(toAct.getCoord());

    Layer municipalities = this.plansCalcRouteFtInfo.getLocalWorld().getLayer("municipality");
    List froms = municipalities.getNearestLocations(fromStop.getCoord());
    List tos = municipalities.getNearestLocations(toStop.getCoord());
    BasicLocation fromMunicipality = (BasicLocation)froms.get(0);
    BasicLocation toMunicipality = (BasicLocation)tos.get(0);

    FtPtRoute newRoute = new FtPtRoute(fromAct.getLinkId(), toAct.getLinkId(), this.plansCalcRouteFtInfo, fromStop, fromMunicipality, toMunicipality, toStop);
    leg.setRoute(newRoute);

    double timeInVehicle = newRoute.getInVehicleTime().doubleValue();

    double walkAccessEgressDistance = newRoute.calcAccessEgressDistance((ActivityImpl)fromAct, (ActivityImpl)toAct);
    double walkAccessEgressTime = getAccessEgressTime(walkAccessEgressDistance, this.configGroup);

    newRoute.setDistance(walkAccessEgressDistance + newRoute.calcInVehicleDistance());

    travelTime = walkAccessEgressTime + timeInVehicle;

    leg.setDepartureTime(depTime);
    leg.setTravelTime(travelTime);
    ((LegImpl)leg).setArrivalTime(depTime + travelTime);

    return travelTime;
  }*/

  public static double getAccessEgressTime(double distance, PlansCalcRouteConfigGroup group) {
    return (distance / group.getWalkSpeed());
  }
  
  public  PlansCalcRouteFtInfo getPlansCalcRouteFtInfo (){
	  return this.plansCalcRouteFtInfo;
  }

  protected double calcCarTime(Person person, CarSharingStation fromStation, CarSharingStation toStation, double carStartTime) {
	  //LegImpl leg = new LegImpl(MyTransportMode.carsharing);
	  LegImpl leg = new LegImpl(MyTransportMode.car);

    double travelTime = 0.0D;
    Activity getCarSharingCar = new ActivityImpl("getCarSharingCar", fromStation.getCoord(), fromStation.getLink().getId());
    Activity letCarSharingCar = new ActivityImpl("letCarSharingCar", toStation.getCoord(), toStation.getLink().getId());
    travelTime = super.handleLeg(person, leg, getCarSharingCar, letCarSharingCar, carStartTime);
    return travelTime;
  }
}
