package playground.fhuelsmann.emissions;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
public class Main {
	
	public static void main (String[]args) throws Exception{

		String eventsFile = "../../detailedEval/teststrecke/sim/inputEmissions/events_0807_it.0.txt";
		String visumRoadFile = "../../detailedEval/teststrecke/sim/inputEmissions/visumnetzlink.txt";
		String visumRoadHebefaRoadFile = "../../detailedEval/teststrecke/sim/inputEmissions/road_types.txt";
		String Hbefa_Traffic = "../../detailedEval/teststrecke/sim/inputEmissions/hbefa_emission_factors.txt";

		//create an event object
		EventsManager events = new EventsManagerImpl();	

		//create the handler
		TravelTimeCalculation handler = new TravelTimeCalculation();
	
		//add the handler
		events.addHandler(handler);

		//create the reader and read the file
		MatsimEventsReader matsimEventReader = new MatsimEventsReader(events);
		
		matsimEventReader.readFile(eventsFile);
		
		VelocityAverageCalculate vac = new VelocityAverageCalculate(handler.getTravelTimes(),visumRoadFile);
		vac.calculateAverageVelocity1();
	//	System.out.println(vac.getNewtravelTimesWithLengthAndAverageSpeed().get("52799758"));
		//System.out.println(vac.getOldtravelTimesWithLengthAndAverageSpeed());

		
		//vac.printVelocities();
	//	System.out.println(vac.getmapOfSingleEvent().get("592536888").get("10040_1").getFirst().getTravelTime());
		
		HbefaTable hbefaTable = new HbefaTable();
		hbefaTable.makeHabefaTable(Hbefa_Traffic);
		HbefaVisum hbefaVisum = new HbefaVisum(vac.getmapOfSingleEvent());
		hbefaVisum.creatRoadTypes(visumRoadHebefaRoadFile);
		hbefaVisum.createMapWithHbefaRoadTypeNumber();	
		
		EmissionFactor emissionFactor = new EmissionFactor(hbefaVisum.map,hbefaTable.getHbefaTableWithSpeedAndEmissionFactor());
		emissionFactor.createEmissionTables();
		emissionFactor.printEmissionTable();
	//	System.out.println(emissionFactor.getmap().get("592536888").get("24072"));
		
		
		}
	}	
	
