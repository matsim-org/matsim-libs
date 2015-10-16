package playground.balac.sbbproject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.router.TransitRouterWrapper;
import org.matsim.core.router.DefaultRoutingModules;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.WGS84toCH1903LV03;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.pt.config.TransitRouterConfigGroup;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterNetwork;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import playground.balac.twowaycarsharingredisigned.scenario.TwoWayCSFacility;
import playground.balac.twowaycarsharingredisigned.scenario.TwoWayCSFacilityImpl;
import playground.balac.utils.NetworkLinkUtils;
import playground.balac.utils.TimeConversion;
import playground.balac.utils.TransitRouterImplFactory;

import com.google.inject.Provider;

public class PTRoutingClaude
{
private static Provider<TransitRouter> transitRouterFactory;
  public static void main(String[] args)
    throws IOException
  {
    PTRoutingClaude pTRoutingClaude = new PTRoutingClaude();
    pTRoutingClaude.run(args);
  }

  public double getDepartureTime(List<? extends PlanElement> route)
  {
    if (route.size() == 1) {
      return ((Leg)route.get(0)).getDepartureTime();
    }
    for (PlanElement pe : route)
    {
      if (((pe instanceof Leg)) && (((Leg)pe).getMode().equals("pt")))
      {
        return ((Leg)pe).getDepartureTime();
      }

    }

    return 0.0D;
  }

  public double getTraveltime(List<? extends PlanElement> route)
  {
    double travelTime = 0.0D;

    for (PlanElement pe : route)
    {
      if ((pe instanceof Leg))
      {
        travelTime += ((Leg)pe).getTravelTime();
      }

    }

    return travelTime;
  }

  public int getNumberOfTransfers(List<? extends PlanElement> route) {
    int count = 0;

    for (PlanElement pe : route) {
      if (((pe instanceof Leg)) && (((Leg)pe).getMode().equals("pt"))) {
        count++;
      }
    }

    return -1 + count;
  }

  public boolean isDominatedWithTran(List<? extends PlanElement> route, ArrayList<List<? extends PlanElement>> allRoutes)
  {
	  for (List<? extends PlanElement> r : allRoutes) {
			
			if (((Leg)r.get(0)).getDepartureTime() + getTraveltime(r) == 
					((Leg)route.get(0)).getDepartureTime() + getTraveltime(route) && getNumberOfTransfers(r) == getNumberOfTransfers(route)) {
				
				
				return true;
				
			}
			
		}

    return false;
  }

  public boolean isDominatedWithoutTran(List<? extends PlanElement> route, ArrayList<List<? extends PlanElement>> allRoutes) {
	  for (List<? extends PlanElement> r : allRoutes) {
			
			if (((Leg)r.get(0)).getDepartureTime() + getTraveltime(r) == 
					((Leg)route.get(0)).getDepartureTime() + getTraveltime(route)) {
				
				
				return true;
				
			}
			
		}

    return false;
  }

  public void run(String[] args) throws IOException
  {
    double timeStep = 60.0D;

    Config config = ConfigUtils.createConfig();
    config.global().setNumberOfThreads(16);

    config.network().setInputFile("./mmNetwork.xml.gz");

    config.transit().setTransitScheduleFile("./mmSchedule.xml.gz");
    config.transit().setVehiclesFile("./mmVehicles.xml.gz");

    config.transit().setUseTransit(true);
    config.scenario().setUseVehicles(true);

    Scenario scenario = ScenarioUtils.loadScenario(config);

	Set<String> vehiclesTrain = new TreeSet<String>();
    vehiclesTrain.add("S");
    vehiclesTrain.add("R");
    vehiclesTrain.add("EC");
    vehiclesTrain.add("ICE");
    vehiclesTrain.add("RJ");
    vehiclesTrain.add("IC");
    vehiclesTrain.add("RE");
    vehiclesTrain.add("ICN");
    vehiclesTrain.add("IR");
    vehiclesTrain.add("TGV");
    vehiclesTrain.add("D");
    vehiclesTrain.add("VAE");

	Set<String> vehiclesBus = new TreeSet<String>();
    vehiclesBus.add("NFB");
    vehiclesBus.add("BUS");
    vehiclesBus.add("KB");
    vehiclesBus.add("NFO");
	Set<String> vehiclesFun = new TreeSet<String>();
    vehiclesFun.add("FUN");

	Set<String> vehiclesTram = new TreeSet<String>();
    vehiclesTram.add("T");
    vehiclesTram.add("NFT");
	Set<String> vehiclesMetro = new TreeSet<String>();
    vehiclesMetro.add("M");

    TransitRouterNetwork routerNetwork = TransitRouterNetwork.createFromSchedule(scenario.getTransitSchedule(), 100.0D);
    ((PlanCalcScoreConfigGroup)config.getModule("planCalcScore")).setUtilityOfLineSwitch(-2.0D);
    final double travelingWalk = -12.0D;
    ((PlanCalcScoreConfigGroup)config.getModule("planCalcScore")).getModes().get(TransportMode.walk).setMarginalUtilityOfTraveling(travelingWalk);

    PlansCalcRouteConfigGroup routeConfigGroup = scenario.getConfig().plansCalcRoute();
    routeConfigGroup.getModeRoutingParams().get("walk").setBeelineDistanceFactor(1.2);
    routeConfigGroup.getModeRoutingParams().get("walk").setTeleportedModeSpeed(4.2 / 3.6);
    
    TransitRouterConfig transitRouterConfig = new TransitRouterConfig(config.planCalcScore(), 
      config.plansCalcRoute(), config.transitRouter(), config.vspExperimental());

    transitRouterFactory = new TransitRouterImplFactory(scenario.getTransitSchedule(), transitRouterConfig, routerNetwork);
    BufferedReader readLink = IOUtils.getBufferedReader("./coord_" + args[0] + ".txt");

    BufferedWriter outLink = IOUtils.getBufferedWriter("./travelTimesPT_" + args[0] + ".txt");
    BufferedWriter outLinkF = IOUtils.getBufferedWriter("./travelTimesPTFr_" + args[0] + ".txt");
    BufferedWriter outLinkOccupancy = IOUtils.getBufferedWriter("./travelTimesPTOccupancy_" + args[0] + ".txt");

    BufferedWriter outFrequency = IOUtils.getBufferedWriter("./frequency_" + args[0] + ".txt");
    ((TransitRouterConfigGroup)config.getModule("transitRouter")).setSearchRadius(2000.0D);

    String s = readLink.readLine();
    s = readLink.readLine();

    NetworkLinkUtils lUtils = new NetworkLinkUtils(scenario.getNetwork());

    
    System.out.println(routeConfigGroup.getModeRoutingParams().get("walk").getBeelineDistanceFactor() + " " + routeConfigGroup.getModeRoutingParams().get("walk").getTeleportedModeSpeed());
    TransitRouterWrapper routingModule = new TransitRouterWrapper(transitRouterFactory.get(),
            scenario.getTransitSchedule(),
            scenario.getNetwork(), // use a walk router in case no PT path is found
            DefaultRoutingModules.createTeleportationRouter(TransportMode.transit_walk, scenario.getPopulation().getFactory(), 
			        routeConfigGroup.getModeRoutingParams().get( TransportMode.walk ) ));

    System.out.println("starting to parse the input file");

    WGS84toCH1903LV03 transformation = new WGS84toCH1903LV03();

    ReadSBBData sbbData = new ReadSBBData(args[1], args[2]);
    sbbData.read();

    int countTrainSeg = 0;

    while (s != null)
    {
      String[] arr = s.split("\\t");

      if ((!arr[1].startsWith("-")) && (!arr[2].startsWith("-")) && (!arr[3].startsWith("-")) && (!arr[4].startsWith("-")))
      {
        Coord coordStartT = new Coord(Double.parseDouble(arr[2]), Double.parseDouble(arr[1]));

        Coord coordStart = transformation.transform(coordStartT);

        System.out.println(coordStart.getX());

        Link lStart = lUtils.getClosestLink(coordStart);

        Coord coordEndT = new Coord(Double.parseDouble(arr[4]), Double.parseDouble(arr[3]));

        Coord coordEnd = transformation.transform(coordEndT);

        Link lEnd = lUtils.getClosestLink(coordEnd);

        Person person = scenario.getPopulation().getFactory().createPerson(Id.createPersonId(arr[0]));

        double m = TimeConversion.convertTimeToDouble(arr[5]);

        TwoWayCSFacilityImpl startFacility = new TwoWayCSFacilityImpl(Id.create("100", TwoWayCSFacility.class), coordStart, lStart.getId());

        TwoWayCSFacilityImpl endFacility = new TwoWayCSFacilityImpl(Id.create("101", TwoWayCSFacility.class), coordEnd, lEnd.getId());

		ArrayList<List<? extends PlanElement>> allRoutes = new ArrayList<List<? extends PlanElement>>();

		List<? extends PlanElement> route =  routingModule.calcRoute(startFacility, endFacility, m * 60, person);
        ((Leg)route.get(0)).setDepartureTime(m * 60.0D);

        System.out.println("routed for the initial time");

        double departureTime = m * 60.0D + ((Leg)route.get(0)).getTravelTime();

        System.out.println(departureTime);
        double travelTime = getTraveltime(route);
        int numberOfTransfers = getNumberOfTransfers(route);

        double firstTime = 0.0D;
        double lastTime = 0.0D;

        int count = 0;

		ArrayList<List<? extends PlanElement>> allRoutesFirst = new ArrayList<List<? extends PlanElement>>();

        if (route.size() == 1) {
          System.out.println(((Leg)route.get(0)).getTravelTime());
          System.out.println(((Leg)route.get(0)).getMode());
        }

        if (route.size() != 1) {
          allRoutes.add(route);
          for (double time = departureTime + 7200.0D; time >= departureTime - 7200.0D; time -= timeStep)
          {
				List<? extends PlanElement> routeNew =  routingModule.calcRoute(startFacility, endFacility, time, person);

            double travelTimeNew = getTraveltime(routeNew);

            ((Leg)routeNew.get(0)).setDepartureTime(time);

            if ((!isDominatedWithTran(routeNew, allRoutes)) && 
              (routeNew.size() != 1))
            {
              allRoutesFirst.add(routeNew);
              if ((travelTimeNew < 1.3D * travelTime) && (numberOfTransfers + 2 > getNumberOfTransfers(routeNew)) && 
                (!isDominatedWithoutTran(routeNew, allRoutes)))
              {
                allRoutes.add(routeNew);
              }

            }

          }

          System.out.println("found all the routes");

          double lastArrival = ((Leg)route.get(0)).getDepartureTime();

          int countTransfers = -1;

          double transferTime = 0.0D;

          boolean writtenAccessTime = false;

          double egressTime = 0.0D;

          double distance = 0.0D;

          double accessTime = 0.0D;

          double firstWaitingTime = 0.0D;

          for (PlanElement pe1 : route) {
            boolean countDidokFrom = false;
            boolean countDidokTo = false;

            if (((pe1 instanceof Leg)) && (((Leg)pe1).getMode().equals("pt"))) {
              countTransfers++;

              ExperimentalTransitRoute tr1 = (ExperimentalTransitRoute)((Leg)pe1).getRoute();
              double temp = 1.7976931348623157E+308D;

              Departure departure = null;

              for (Departure d : ((TransitRoute)((TransitLine)scenario.getTransitSchedule().getTransitLines().get(tr1.getLineId())).getRoutes().get(tr1.getRouteId())).getDepartures().values())
              {
                double fromStopArrivalOffset = ((TransitRoute)((TransitLine)scenario.getTransitSchedule().getTransitLines().get(tr1.getLineId())).getRoutes().get(tr1.getRouteId())).getStop((TransitStopFacility)scenario.getTransitSchedule().getFacilities().get(tr1.getAccessStopId())).getDepartureOffset();

                if ((d.getDepartureTime() + fromStopArrivalOffset >= lastArrival) && (d.getDepartureTime() + fromStopArrivalOffset < temp))
                {
                  temp = d.getDepartureTime() + fromStopArrivalOffset;
                  departure = d;
                }
              }

              distance += ((Leg)pe1).getRoute().getDistance();

              double transfertTimePart = temp - lastArrival;

              if (countTransfers == 0) {
                firstWaitingTime = transfertTimePart;
              }
              else
              {
                transferTime += transfertTimePart;
              }
              lastArrival += ((Leg)pe1).getTravelTime();
				Collection<Departure> dep = scenario.getTransitSchedule().getTransitLines().get(tr1.getLineId()).getRoutes().get(tr1.getRouteId()).getDepartures().values();

              Departure d = (Departure)dep.toArray()[0];
              String vehId = d.getVehicleId().toString().split("_")[0];
              double occupancy1 = 0.0D;
              double occupancy2 = 0.0D;

              double vehicleTime = 0.0D;

              String transitNumber = "";
              if (departure != null) {
                transitNumber = departure.getId().toString().split("_")[1];
              }
              else
              {
                vehId = "";
                outLink.write(arr[0]);
                outLink.newLine();
              }
              if (vehiclesTrain.contains(vehId)) {
					ArrayList<TrainDataPerSegment> trainData = sbbData.getTrainData(Integer.toString((int)Double.parseDouble(transitNumber)));
                countTrainSeg++;
                if (trainData == null) {
                  System.out.println("train data je null! " + tr1.getRouteId().toString() + " ");
                  outLinkOccupancy.write(arr[0] + " ");
                  outLinkOccupancy.write("TRAIN ");
                  outLinkOccupancy.write(d.getVehicleId().toString() + " ");
                  outLinkOccupancy.write(temp + " " + Double.toString(temp + ((Leg)pe1).getTravelTime() - transfertTimePart) + " ");
                  outLinkOccupancy.write(Double.toString(((Leg)pe1).getRoute().getDistance()) + " ");

                  outLinkOccupancy.write("-1.0 -1.0");
                  outLinkOccupancy.newLine();
                }
                else
                {
                  for (TrainDataPerSegment segment : trainData) {
                    if ((int)Double.parseDouble(segment.getDidokFrom()) == (int)Double.parseDouble(tr1.getAccessStopId().toString().substring(0, 7))) {
                      countDidokFrom = true;
                    }

                    if (countDidokFrom) {
                      double time = TimeConversion.convertTimeToDouble(segment.getArrivalTime()) - TimeConversion.convertTimeToDouble(segment.getDepartureTime());
                      occupancy1 += segment.getOccupancy1() * time;
                      occupancy2 += segment.getOccupancy2() * time;

                      vehicleTime += time;
                    }
                    if ((int)Double.parseDouble(segment.getDidokTo()) == (int)Double.parseDouble(tr1.getEgressStopId().toString().substring(0, 7))) {
                      countDidokTo = true;
                      break;
                    }

                  }

                  if (((trainData != null ? 1 : 0) & ((countDidokFrom) && (countDidokTo) ? 0 : 1)) != 0) {
                    System.out.println("One of didok stops has not been found " + tr1.getRouteId().toString() + " " + tr1.getAccessStopId().toString() + " " + tr1.getEgressStopId().toString());
                    outLinkOccupancy.write(arr[0] + " ");
                    outLinkOccupancy.write("TRAIN ");
                    outLinkOccupancy.write(d.getVehicleId().toString() + " ");

                    outLinkOccupancy.write(temp + " " + Double.toString(temp + ((Leg)pe1).getTravelTime() - transfertTimePart) + " ");
                    outLinkOccupancy.write(Double.toString(((Leg)pe1).getRoute().getDistance()) + " ");

                    outLinkOccupancy.write("-1.0 -1.0");
                    outLinkOccupancy.newLine();
                  }
                  else
                  {
                    outLinkOccupancy.write(arr[0] + " ");
                    outLinkOccupancy.write("TRAIN ");
                    outLinkOccupancy.write(d.getVehicleId().toString() + " ");

                    outLinkOccupancy.write(temp + " " + Double.toString(temp + ((Leg)pe1).getTravelTime() - transfertTimePart) + " ");
                    outLinkOccupancy.write(Double.toString(((Leg)pe1).getRoute().getDistance()) + " ");

                    outLinkOccupancy.write(Double.toString(occupancy1 / vehicleTime) + " " + Double.toString(occupancy2 / vehicleTime));
                    outLinkOccupancy.newLine();
                  }

                }

              }
              else if (vehiclesBus.contains(vehId)) {
                outLinkOccupancy.write(arr[0] + " ");
                outLinkOccupancy.write("BUS ");
                outLinkOccupancy.write(d.getVehicleId().toString() + " ");
                outLinkOccupancy.write(temp + " " + Double.toString(temp + ((Leg)pe1).getTravelTime() - transfertTimePart) + " ");
                outLinkOccupancy.write(Double.toString(((Leg)pe1).getRoute().getDistance()) + " ");

                outLinkOccupancy.write("-1.0 -1.0");
                outLinkOccupancy.newLine();
              }
              else if (vehiclesTram.contains(vehId)) {
                outLinkOccupancy.write(arr[0] + " ");
                outLinkOccupancy.write("TRAM ");
                outLinkOccupancy.write(d.getVehicleId().toString() + " ");
                outLinkOccupancy.write(temp + " " + Double.toString(temp + ((Leg)pe1).getTravelTime() - transfertTimePart) + " ");
                outLinkOccupancy.write(Double.toString(((Leg)pe1).getRoute().getDistance()) + " ");

                outLinkOccupancy.write("-1.0 -1.0");
                outLinkOccupancy.newLine();
              }
              else if (vehiclesMetro.contains(vehId)) {
                outLinkOccupancy.write(arr[0] + " ");
                outLinkOccupancy.write("METRO ");
                outLinkOccupancy.write(d.getVehicleId().toString() + " ");
                outLinkOccupancy.write(temp + " " + Double.toString(temp + ((Leg)pe1).getTravelTime() - transfertTimePart) + " ");
                outLinkOccupancy.write(Double.toString(((Leg)pe1).getRoute().getDistance()) + " ");

                outLinkOccupancy.write("-1.0 -1.0");
                outLinkOccupancy.newLine();
              }
              else
              {
                System.out.println("This should never happen " + vehId);
              }

            }
            else if ((pe1 instanceof Leg)) {
              lastArrival += ((Leg)pe1).getTravelTime();

              if (!writtenAccessTime)
              {
                if (route.size() == 1)
                {
                  departureTime = ((Leg)pe1).getDepartureTime();

                  accessTime = 0.0D;
                }
                else
                {
                  departureTime = ((Leg)pe1).getDepartureTime();
                  accessTime = ((Leg)pe1).getTravelTime();
                }

                writtenAccessTime = true;
              }

              egressTime = ((Leg)pe1).getTravelTime();
            }

          }

          System.out.println("written the route to output");

          outLinkF.write(arr[0] + " ");

          outLinkF.write(Double.toString(firstWaitingTime) + " ");

          outLinkF.write(Double.toString(departureTime) + " ");

          outLinkF.write(Double.toString(accessTime) + " ");

          outLinkF.write(Double.toString(transferTime) + " ");

          outLinkF.write(Integer.toString(countTransfers) + " ");

          if (route.size() == 1) {
            outLinkF.write(Double.toString(0.0D) + " ");
          }
          else {
            outLinkF.write(Double.toString(egressTime) + " ");
          }
          outLinkF.write(Double.toString(lastArrival - ((Leg)route.get(0)).getDepartureTime()) + " ");

          outLinkF.write(Double.toString(distance));

          outLinkF.newLine();

			for (List<? extends PlanElement> routeIter : allRoutes) {
          
            lastArrival = ((Leg)routeIter.get(0)).getDepartureTime();

            if (routeIter.size() != 1) {
              if (lastTime == 0.0D)
                lastTime = lastArrival;
              else if (lastTime < lastArrival) {
                lastTime = lastArrival;
              }
              if (firstTime == 0.0D)
                firstTime = lastArrival;
              else if (firstTime > lastArrival) {
                firstTime = lastArrival;
              }
              count++;
            }

          }

          System.out.println("found frequency");

          outFrequency.write(arr[0] + " ");

          if (count == 1)
            outFrequency.write(Integer.toString(0));
          else {
            outFrequency.write(Double.toString((lastTime - firstTime) / (count - 1)));
          }
          outFrequency.newLine();
        }

      }

      s = readLink.readLine();
      System.out.println(arr[0]);
    }

    System.out.println(countTrainSeg);
    outFrequency.flush();
    outFrequency.close();
    outLinkOccupancy.flush();
    outLinkOccupancy.close();
    outLink.flush();
    outLink.close();
    outLinkF.flush();
    outLinkF.close();

    new PopulationWriter(scenario.getPopulation(), scenario.getNetwork())
      .writeFileV4("./plans_pt_trips_" + args[0] + ".xml.gz");
  }
}