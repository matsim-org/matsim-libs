package playground.vbmh.vmParking;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.StartupListener;
import playground.vbmh.util.VMCharts;

import java.io.File;
import java.util.Map;

/**
 * Sets up event handlers and does some resets before and after each iteration.
 * 
 * 
 * 
 * @author Valentin Bemetz & Moritz Hohenfellner
 *
 */


public class ParkControlerListener implements StartupListener, IterationEndsListener, IterationStartsListener{
	private ParkHandler parkHandler = new ParkHandler();
	private String parkHistoryOutputFileName=null;
	IterEndStats iterEndStats;
	
	public ParkHandler getParkHandler() {
		return parkHandler;
	}



	public void setParkHandler(ParkHandler parkHandler) {
		this.parkHandler = parkHandler;
	}



	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		// TODO Auto-generated method stub
		
		//PH Writer beenden damit File geschlossen und geschrieben wird
		ParkHistoryWriter phwriter = new ParkHistoryWriter();
		phwriter.end();
		
		//Park Statistik Drucken und zuruecksetzen:
		this.getParkHandler().parkControl.printStatistics();
		this.getParkHandler().parkControl.resetStatistics();
		
		iterEndStats.run(getParkHandler().getParkControl());
		
		
		//Verzeichnisse erstellen
		try{
			File dir = new File(getParkHandler().getParkControl().controller.getConfig().getModule("controler").getValue("outputDirectory")+"/Charts/"+getParkHandler().getParkControl().controller.getIterationNumber());
			dir.mkdir();
		}catch(Exception e){
			System.out.println("Verzeichniss wurde nicht angelegt");
		}
		//-----
		
		
		VMCharts.printCharts(getParkHandler().getParkControl().controller.getConfig().getModule("controler").getValue("outputDirectory")+"/Charts/"+getParkHandler().getParkControl().controller.getIterationNumber(), getParkHandler().getParkControl().controller.getIterationNumber());
		VMCharts.clear();
		
		
	}
	
		

	@Override
	public void notifyStartup(StartupEvent event) {
		// TODO Auto-generated method stub
		
		event.getControler().getEvents().addHandler(getParkHandler());
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		// TODO Auto-generated method stub
		
		//Park History Writer Starten: 
		ParkHistoryWriter phwriter = new ParkHistoryWriter();

		phwriter.start(parkHistoryOutputFileName+"_"+event.getIteration()+".xml"); 
		
		//Parkplaetze zuruecksetzen 
		getParkHandler().parkControl.parkingMap.clearSpots();
		getParkHandler().parkControl.parkingMap.createSpots(getParkHandler().getParkControl().getPricing());
		getParkHandler().parkControl.clearAgents();
		
		//Diagnose: 
			//Spots Zaehlen

		int nevs = 0;
		int evs = 0;
		for(Parking parking : getParkHandler().getParkControl().parkingMap.getParkings()){
			int[] counts = parking.diagnose();
			evs+=counts[0];
			nevs+=counts[1];
		}
		
		System.out.println("ParkControlerListener Zaehlt Spots: EVS :"+evs+" NEVS :"+nevs);
		
			//EVs Zaehlen
		if(getParkHandler().getParkControl().evUsage){
			int i = 0;
            for(Person person : getParkHandler().getParkControl().controller.getScenario().getPopulation().getPersons().values()){
				if(getParkHandler().getParkControl().evControl.hasEV(person.getId())){
					i++;
				}
			}
			System.out.println("ParkControlerListener Zaehlt: EVS :"+i);
			
		}
		
		
		//Iter End Stats Zuruecksetzen
		iterEndStats=new IterEndStats();
		
		//Par control Statistik starten
		getParkHandler().getParkControl().iterEndStats=iterEndStats;
		getParkHandler().getParkControl().iterStart();
		
		
		
		
		
		//VM_Score_Keeper Zuruecksetzen:
        Map<Id<Person>, ? extends Person> population = event.getControler().getScenario().getPopulation().getPersons();
		for (Person person : population.values()){
			//person.getCustomAttributes().put("VMScoreKeeper", null);
			person.getCustomAttributes().remove("VMScoreKeeper");
			person.getCustomAttributes().put("ActCounter", 0);
		}
		
		
		
	}



	public String getParkHistoryOutputFileName() {
		return parkHistoryOutputFileName;
	}



	public void setParkHistoryOutputFileName(String parkHistoryOutputFileName) {
		this.parkHistoryOutputFileName = parkHistoryOutputFileName;
	}

}
