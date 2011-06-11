package playground.ciarif.flexibletransports.controler.listeners;

import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import playground.ciarif.flexibletransports.config.FtConfigGroup;
import playground.ciarif.flexibletransports.data.MyTransportMode;
import playground.ciarif.flexibletransports.IO.CarSharingSummaryWriter;
import playground.ciarif.flexibletransports.IO.PersonsSummaryWriter;
import playground.ciarif.flexibletransports.router.CarSharingStation;
import playground.ciarif.flexibletransports.router.CarSharingStations;
import playground.ciarif.flexibletransports.router.PlansCalcRouteFtInfo;

public class CarSharingListener
  implements IterationEndsListener
{
  private static final Logger log = Logger.getLogger(CarSharingListener.class);
  private Controler controler;
  //private CarSharingSummaryWriter csw = new CarSharingSummaryWriter("/data/matsim/ciarif/output/zurich_10pc/CarSharing/CarSharingSummary");
  //private PersonsSummaryWriter psw = new PersonsSummaryWriter("/data/matsim/ciarif/output/zurich_10pc/CarSharing/PersonsSummary");
  private CarSharingSummaryWriter csw;
  private PersonsSummaryWriter psw;
  private FtConfigGroup configGroup;
  private PlansCalcRouteFtInfo plansCalcRouteFtInfo;

  public CarSharingListener(FtConfigGroup configGroup)
  {
    this.configGroup = configGroup;
    this.plansCalcRouteFtInfo = new PlansCalcRouteFtInfo(configGroup);
    this.csw = new CarSharingSummaryWriter(this.configGroup.getCsSummaryWriterFilename());
    this.psw = new PersonsSummaryWriter(this.configGroup.getPersonSummaryWriterFilename());
  }

  public void notifyIterationEnds(IterationEndsEvent event)
  {
    this.controler = event.getControler();
    if (this.controler.getIterationNumber().intValue() != this.controler.getLastIteration())
      return;
    Network network = this.controler.getNetwork();
    this.plansCalcRouteFtInfo.prepare(network);
    for (Person person : this.controler.getPopulation().getPersons().values()) {
      Plan plan = person.getSelectedPlan();
      PersonImpl p = (PersonImpl)person;
      for (PlanElement pe : plan.getPlanElements()) {
        if (pe instanceof LegImpl) {
          LegImpl leg = (LegImpl)pe;
          ActivityImpl actBefore = (ActivityImpl)plan.getPlanElements().get(plan.getPlanElements().indexOf(leg) - 1);
          ActivityImpl actAfter = (ActivityImpl)plan.getPlanElements().get(plan.getPlanElements().indexOf(leg) + 1);
          if (leg.getMode().equals(MyTransportMode.carsharing)) {
            LinkImpl startLink = (LinkImpl)network.getLinks().get(leg.getRoute().getStartLinkId());
            CarSharingStation fromStation = this.plansCalcRouteFtInfo.getCarStations().getClosestLocation(startLink.getCoord());
            LinkImpl endLink = (LinkImpl)network.getLinks().get(leg.getRoute().getEndLinkId());
            CarSharingStation toStation = this.plansCalcRouteFtInfo.getCarStations().getClosestLocation(endLink.getCoord());
            this.csw.write(p, startLink, fromStation, toStation, endLink, leg.getDepartureTime(), leg.getArrivalTime(), actBefore, actAfter);
          }
        }
      }

      this.psw.write(p);
    }
    this.psw.close();
    this.csw.close();
  }
}