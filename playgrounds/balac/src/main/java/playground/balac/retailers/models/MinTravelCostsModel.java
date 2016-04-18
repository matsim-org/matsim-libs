package playground.balac.retailers.models;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.RouteFactoryImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;

import playground.balac.retailers.data.LinkRetailersImpl;
import playground.balac.retailers.data.PersonPrimaryActivity;
import playground.balac.retailers.data.PersonRetailersImpl;
import playground.balac.retailers.utils.Utils;



public class MinTravelCostsModel extends RetailerModelImpl
{
  private static final Logger log = Logger.getLogger(MaxActivityModel.class);

  private TreeMap<Id<Link>, LinkRetailersImpl> availableLinks = new TreeMap<>();

  public MinTravelCostsModel(MatsimServices controler, Map<Id<ActivityFacility>, ActivityFacilityImpl> retailerFacilities)
  {
    this.controler = controler;
    this.retailerFacilities = retailerFacilities;
      this.controlerFacilities = this.controler.getScenario().getActivityFacilities();
    this.shops = findScenarioShops(this.controlerFacilities.getFacilities().values());

      for (Person p : controler.getScenario().getPopulation().getPersons().values()) {
      Person pi = p;
      this.persons.put(pi.getId(), pi);
    }
  }

  public void init(TreeMap<Integer, String> first)
  {
    this.first = first;

    setInitialSolution(this.first.size());
    log.info("Initial solution = " + getInitialSolution());
    findScenarioShops(this.controlerFacilities.getFacilities().values());
    Gbl.printMemoryUsage();
    for (Person pi : this.persons.values()) {
      PersonRetailersImpl pr = new PersonRetailersImpl((PersonImpl) pi);
      this.retailersPersons.put(pr.getId(), pr);
    }
    Utils.setPersonPrimaryActivityQuadTree(Utils.createPersonPrimaryActivityQuadTree(this.controler));
    Utils.setShopsQuadTree(Utils.createShopsQuadTree(this.controler));

      RouteFactoryImpl routeFactory = ((PopulationFactoryImpl) this.controler.getScenario().getPopulation().getFactory()).getRouteFactory();
    
    for (Integer i = Integer.valueOf(0); i.intValue() < first.size(); i = Integer.valueOf(i.intValue() + 1)) {
      String linkId = this.first.get(i);
      double scoreSum = 0.0D;
        LinkRetailersImpl link = new LinkRetailersImpl(this.controler.getScenario().getNetwork().getLinks().get(Id.create(linkId, Link.class)), this.controler.getScenario().getNetwork(), Double.valueOf(0.0D), Double.valueOf(0.0D));
      Collection<PersonPrimaryActivity> primaryActivities = Utils.getPersonPrimaryActivityQuadTree().getDisk(link.getCoord().getX(), link.getCoord().getY(), 3000.0D);
      
      
      for (PersonPrimaryActivity ppa : primaryActivities)
      {
          Network network = this.controler.getScenario().getNetwork();
        TravelTime travelTime = this.controler.getLinkTravelTimes();
        TravelDisutility travelCost = this.controler.getTravelDisutilityFactory().createTravelDisutility(travelTime);

        LeastCostPathCalculator routeAlgo = this.controler.getLeastCostPathCalculatorFactory().createPathCalculator(network, travelCost, travelTime);

        //PlansCalcRoute pcr = new PlansCalcRoute(this.services.getConfig().plansCalcRoute(), network, travelCost, travelTime, this.services.getLeastCostPathCalculatorFactory(), routeFactory);

        LegImpl li = new LegImpl(TransportMode.car);
        li.setDepartureTime(0.0D);
        //log.info("fromLink " + link);
        //log.info("toLink " + (Link)this.controler.getNetwork().getLinks().get(ppa.getActivityLinkId()));
          handleCarLeg(li, link, this.controler.getScenario().getNetwork().getLinks().get(ppa.getActivityLinkId()), network, routeAlgo);

          Plan plan = this.controler.getScenario().getPopulation().getFactory().createPlan();
        plan.addActivity(null);
        plan.addLeg(li);
        plan.addActivity(null);

        ScoringFunction scoringFunction = this.controler.getScoringFunctionFactory().createNewScoringFunction(plan.getPerson());
        double score = getLegScore(li, scoringFunction);

        //log.info("Score: " + score);
        //log.info("Travel Time: " + li.getTravelTime() + ", Arrival Time: " + li.getArrivalTime() + ", Departure Time: " + li.getDepartureTime());
        scoreSum += score;
      }
      link.setPotentialCustomers(scoreSum/primaryActivities.size());
      this.availableLinks.put(link.getId(), link);
    }
  }

  private double getLegScore(Leg leg, ScoringFunction function)
  {
	  if ((leg instanceof LegImpl))
	    {
	      function.handleLeg(leg);
	    }

	    function.finish();
	    return function.getScore();
  }

  private double handleCarLeg(Leg leg, Link fromLink, Link toLink, Network network, LeastCostPathCalculator routeAlgo)
    throws RuntimeException
  {
    NetworkRoute route;
    double travTime = 0.0D;
    double depTime = leg.getDepartureTime();

    if (fromLink == null) throw new RuntimeException("fromLink missing.");
    if (toLink == null) throw new RuntimeException("toLink missing.");

    Node startNode = fromLink.getToNode();
    Node endNode = toLink.getFromNode();

    LeastCostPathCalculator.Path path = null;
    if (toLink != fromLink)
    {
      path = routeAlgo.calcLeastCostPath(startNode, endNode, depTime, null, null);
      if (path == null) throw new RuntimeException("No route found from node " + startNode.getId() + " to node " + endNode.getId() + ".");

      route = new LinkNetworkRouteImpl(fromLink.getId(), toLink.getId());
      route.setLinkIds(fromLink.getId(), NetworkUtils.getLinkIds(path.links), toLink.getId());
      route.setTravelTime((int)path.travelTime);
      route.setTravelCost(path.travelCost);
      route.setDistance(RouteUtils.calcDistanceExcludingStartEndLink(route, network));
      leg.setRoute(route);
      travTime = (int)path.travelTime;
    }
    else {
      route = new LinkNetworkRouteImpl( fromLink.getId(), toLink.getId());
      route.setTravelTime(0.0D);
      route.setDistance(0.0D);
      leg.setRoute(route);
      travTime = 0.0D;
    }

    leg.setDepartureTime(depTime);
    leg.setTravelTime(travTime);
    ((LegImpl)leg).setArrivalTime(depTime + travTime);
    return travTime;
  }

  @Override
	public double computePotential(ArrayList<Integer> solution)
  {
    Double Fitness = Double.valueOf(0.0D);
    
    //TODO:compute potential based on the location of the shops in the new solution
    
    for (int s = 0; s < this.retailerFacilities.size(); ++s) {
      String linkId = this.first.get(solution.get(s));
      Fitness = Double.valueOf(Fitness.doubleValue() + this.availableLinks.get(Id.create(linkId, Link.class)).getPotentialCustomers());
    }

    return Fitness.doubleValue();
  }

  public Map<Id<ActivityFacility>, ActivityFacilityImpl> getScenarioShops() {
    return this.shops;
  }
}
