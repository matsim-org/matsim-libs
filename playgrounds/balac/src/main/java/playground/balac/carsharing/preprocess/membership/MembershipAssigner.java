package playground.balac.carsharing.preprocess.membership;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.ActivityFacility;

import playground.balac.carsharing.data.FlexTransPersonImpl;
import playground.balac.carsharing.router.CarSharingStation;
import playground.balac.carsharing.router.CarSharingStations;
import playground.balac.carsharing.utils.MembershipUtils;

public class MembershipAssigner
  implements SupplySideModel
{
  private static final Logger log = Logger.getLogger(MembershipAssigner.class);
  private MutableScenario scenario;
  private Map<Id<ActivityFacility>, ? extends ActivityFacility> facilities;
  private TreeMap<Integer, Coord> solutionDecoder = new TreeMap<Integer, Coord>();
  private CarSharingStations carStations;
  private QuadTree<Person> personsQuadTree;
  private int counter;
  private SupplySideModel membershipModel;
  private ArrayList<Person> personsWithLicense = new ArrayList<Person>();
  //private String stationfilePath = "C:/Users/balacm/Desktop/Stations_GreaterZurich_2x.txt";
  //private String newfilePath = "C:/Users/balacm/Documents/MobilityData/Stations_GreaterZurich.txt";

  private String stationfilePath = "/Network/Servers/kosrae.ethz.ch/Volumes/ivt-home/balacm/MATSim/input/FreeFloatingTransportation2014/CS_Stations.txt";

  protected ArrayList<Integer> initialSolution = new ArrayList<Integer>();
  private ArrayList<LinkImpl> availableLinks = new ArrayList< LinkImpl>();

  public MembershipAssigner(MutableScenario scenario) {
	  this.scenario = scenario;
	  this.facilities = scenario.getActivityFacilities().getFacilities();
	  init();
  }

  public MembershipAssigner(MutableScenario scenario, FacilitiesPortfolio carStations, SupplySideModel model) throws IOException
  {
	  this.facilities = scenario.getActivityFacilities().getFacilities();
	  this.scenario = scenario;
	  this.carStations = ((CarSharingStations)carStations);
	  init(model);
  }

  private double computeAccessCSWork(Person pi)
  {
    Vector<CarSharingStation> closestStations = new Vector<CarSharingStation>();
    Coord c = new Coord((1.0D / 0.0D), (1.0D / 0.0D));
    double access = 0.0D;
    
    
    for (PlanElement pe : pi.getSelectedPlan().getPlanElements())
    {
      if ((pe instanceof Activity))
      {
        Activity act = (Activity)pe;

        if (act.getType().contains("work")) {
          c = ((ActivityFacility)this.facilities.get(act.getFacilityId())).getCoord();
          break;
        }
      }
    }

    closestStations = this.carStations.findClosestStations(c, 3, 5000.0D);
    for (CarSharingStation station : closestStations)
    {
      access += station.getCars() * Math.exp(-2.0D * CoordUtils.calcEuclideanDistance(station.getCoord(), c) / 1000.0D);
    }

    return access;
  }

  private double computeAccessCSHome(Person pi)
  {
    Vector<CarSharingStation> closestStations = new Vector<CarSharingStation>();
    Coord c = new Coord((1.0D / 0.0D), (1.0D / 0.0D));
    double access = 0.0D;
    for (PlanElement pe : pi.getSelectedPlan().getPlanElements())
    {
      if ((pe instanceof Activity))
      {
        Activity act = (Activity)pe;

        if (act.getType().equals("home")) {
          c = ((ActivityFacility)this.facilities.get(act.getFacilityId())).getCoord();
          break;
        }
      }

    }

    closestStations = this.carStations.findClosestStations(c, 3, 5000.0D);
    for (CarSharingStation station : closestStations)
    {
      access += station.getCars() * Math.exp(-2.0D * CoordUtils.calcEuclideanDistance(station.getCoord(), c) / 1000.0D);
    }

    return access;
  }

  public void run()
  {
    findPersonsWithdrivingLicense();
    modifyPlans();
  }

  public void run(SupplySideModel model)
  {
    modifyPlans();
  }

  private void init()
  {
    log.info("Reading car stations...");
    this.carStations = new CarSharingStations(this.scenario.getNetwork());
    try {
      this.carStations.readFile(this.stationfilePath);
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    log.info("Reading car stations...done.");
    this.personsQuadTree = MembershipUtils.createPersonQuadTree(this.scenario);
    this.membershipModel = new MembershipModel();
  }

  private void init(SupplySideModel model) throws IOException
  {
    LinksCSReader linkCSR = new LinksCSReader(this.scenario, this.carStations);
    this.membershipModel = model;
    findPersonsWithdrivingLicense();
    this.personsQuadTree = MembershipUtils.createPersonQuadTree(this.scenario);
    this.availableLinks.addAll(linkCSR.getCurrentLinks());
    
   /* BufferedWriter output = new BufferedWriter(new FileWriter(new File("/Network/Servers/kosrae.ethz.ch/Volumes/ivt-home/balacm/MATSim/input/CarsharingStationLocations/links17.txt")));

    for(LinkImpl link:linkCSR.getCurrentLinks()) {
    	
    	output.write(link.getId() + " ");
    }
    output.newLine();
    output.write("freelinks" );
    for(LinkImpl link:linkCSR.getFreeLinks()) {
    	
    	output.write(link.getId() + " ");
    }
    
    
    output.flush();
    output.close();*/
    this.availableLinks.addAll(linkCSR.getFreeLinks());
    setInitialSolution(this.availableLinks.size());
    log.info("availableLinks = " + this.availableLinks);
    setSolutionDecoder();
  }

  private void setSolutionDecoder()
  {
    Integer i = Integer.valueOf(0);
    for (LinkImpl link : this.availableLinks)
    {
      this.solutionDecoder.put(i, link.getCoord());
      i = Integer.valueOf(i.intValue() + 1);
    }
  }

  private void findPersonsWithdrivingLicense()
  {
    for (Person person : this.scenario.getPopulation().getPersons().values())
    {
      Person pi = person;
      if (person.getPlans().size() > 1) {
        log.error("More than one plan for person: " + pi.getId());
      }
      if (PersonUtils.getLicense(pi).equalsIgnoreCase("yes")) {
        Person personWithLicense = PopulationUtils.createPerson(pi.getId());
        PersonUtils.setAge(personWithLicense, PersonUtils.getAge(pi));
        personWithLicense.addPlan(pi.getSelectedPlan());
        PersonUtils.setCarAvail(personWithLicense, PersonUtils.getCarAvail(pi));
        PersonUtils.setEmployed(personWithLicense, PersonUtils.isEmployed(pi));
        PersonUtils.setSex(personWithLicense, PersonUtils.getSex(pi));
        PersonUtils.setLicence(personWithLicense, PersonUtils.getLicense(pi));
        this.personsWithLicense.add(personWithLicense);
      }
    }

    log.info("There are " + this.personsWithLicense.size() + " persons with license, out of " + this.scenario.getPopulation().getPersons().values().size() + " persons in the scenario");
  }

  private void modifyPlans() {
	  MatsimRandom.reset(46442);

    for (Person person : this.personsWithLicense)
    {
      if (person.getPlans().size() > 1) {
        log.error("More than one plan for person: " + person.getId());
      }
      assignCarSharingMembership(person);
    }
    log.info("number of Mobility members = " + this.counter);
    Random r = new Random();;
    MatsimRandom.reset(r.nextLong());
  }

  private void assignCarSharingMembership(Person pi)
  {
    FlexTransPersonImpl ftPerson = new FlexTransPersonImpl(pi);
    addFTAttributes(ftPerson);
    int choice = this.membershipModel.calcMembership(ftPerson);
    if (choice == 0)
    {
      PersonUtils.addTravelcard(pi, "ch-HT-mobility");
      PersonUtils.addTravelcard(this.scenario.getPopulation().getPersons().get(pi.getId()), "ch-HT-mobility");
      this.counter += 1;
    }
    else
    {
      PersonUtils.addTravelcard(pi, "unknown");
    }

    if (PersonUtils.getTravelcards(pi).contains("ch-HT-mobility"))
      PersonUtils.addTravelcard(ftPerson, "Mobility");
  }

  private void addFTAttributes(FlexTransPersonImpl ftPerson)
  {
    ftPerson.setAccessWork(computeAccessCSWork(ftPerson));
    ftPerson.setAccessHome(computeAccessCSHome(ftPerson));
  }

  private double computeDensityHome(FlexTransPersonImpl pi)
  {
    Coord c = new Coord((1.0D / 0.0D), (1.0D / 0.0D));

    for (PlanElement pe : pi.getSelectedPlan().getPlanElements())
    {
      if ((pe instanceof Activity))
      {
        Activity act = (Activity)pe;

        if (act.getType().equals("home")) {
          c = ((ActivityFacility)this.facilities.get(act.getFacilityId())).getCoord();
          break;
        }
      }
    }

    int density = this.personsQuadTree.getDisk(c.getX(), c.getY(), 0.05D).size();
   // log.info("density " + density);
    return density;
  }

  public void setInitialSolution(int size) {
    for (int i = 0; i < size; i++)
      this.initialSolution.add(Integer.valueOf(i));
  }

  public ArrayList<Integer> getInitialSolution()
  {
    return this.initialSolution;
  }

  public double computePotential(ArrayList<Integer> solution) {
    this.counter = 0;
    updateStations(solution);
    modifyPlans();
    return this.counter;
  }

  private void updateStations(ArrayList<Integer> solution)
  {
	
    TreeMap<Integer, Coord> coordCode = new TreeMap<Integer, Coord>();
    TreeMap<Integer, LinkImpl> linkCode = new TreeMap<Integer, LinkImpl>();
    int j = 0;
    for (Integer i : solution)
    {
      coordCode.put(j, this.solutionDecoder.get(i));
      linkCode.put(j, this.availableLinks.get(i));
      j++;
    }

    int k = 0;

    for (CarSharingStation csStation : this.carStations.getStationsArr())
    {
      Coord coord = coordCode.get(k);
      this.carStations.getQuadStations().remove(csStation.getCoord().getX(), csStation.getCoord().getY(), csStation);
      csStation.setCoord(coord);
      csStation.setLink(linkCode.get(k));
      this.carStations.getQuadStations().put(csStation.getCoord().getX(), csStation.getCoord().getY(), csStation);
      

      k++;
    }
  }

  public int calcMembership(FlexTransPersonImpl ftPerson)
  {
    return 0;
  }
}