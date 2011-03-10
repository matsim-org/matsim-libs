package playground.mmoyo.analysis.tools;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.LegImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.utils.geometry.CoordUtils;

import playground.mmoyo.Validators.PlanValidator;
import playground.mmoyo.utils.DataLoader;
import playground.mmoyo.utils.PlanFragmenter;

/**Calculates and saves the global travel time, travel distance, transfers, det-transfers, walk distance and waiting time of a population*/ 
public class PopulationPtAnalyzer {
	private final static Logger log = Logger.getLogger(PopulationPtAnalyzer.class);
	
	private PlanValidator planValidator = new PlanValidator();
	private DataLoader dataLoader = new DataLoader();
	private BufferedWriter bufferedWriter;
	private final String STR_PTINTERACTION = "pt interaction";
	private final String TAB= "\t";
	private final String dirPath;
	private StringBuffer stringBuffer; 
	
	public PopulationPtAnalyzer(final String dirPath){
		this.dirPath = dirPath;
		this.stringBuffer = new StringBuffer("popFile\tplansBefFragm\tplansAfterFrag\tPTlegs\ttrWalkLegs\tdirectWalks\tinVehTravTime:\tinVehDist:\ttravelDist2:\ttransfers:\twalkTime:\twalkDistance:");
	}
	
	public void run (final String popFile){
		double inVehTravTime = 0.0;
		double inVehDist = 0.0;
		double totLinDist = 0.0;
		double walkTime = 0.0;
		int transfers = 0;
		int directWalks=0;
		int walkDistance = 0;
		int ptLegs = 0;
		int trWalkLegs=0;
		int otherModes =0;
		
		final String filePath = this.dirPath + popFile; 
		log.info("Starting with " + filePath);
		Population population = dataLoader.readPopulation(filePath);
		final int plansBefFragm = population.getPersons().size();
		
		//validate the sequence act-lec-act
		if (!planValidator.hasSecqActLeg(population)) { 
			throw new RuntimeException("inadequate sequence of acts and legs");
		}	
		
		//plans must be fragmented
		population = new PlanFragmenter().run(population);

		for (Person person : population.getPersons().values() ){
			Plan plan =  person.getSelectedPlan();  
			Activity aAct = ((Activity)plan.getPlanElements().get(0));
			Activity bAct = ((Activity)plan.getPlanElements().get(plan.getPlanElements().size()-1));
			
			int i = 0;
			Activity lastAct= null;
			Activity nextAct;
			Leg lastLeg = null;
			Leg nextLeg;
			boolean hasPtConnection=false;
			
			for (PlanElement pe : plan.getPlanElements()){
				if (pe instanceof Activity) {
					Activity act = (Activity)pe;
					if (act!= aAct  && act!=bAct ){ 
						nextAct= (Activity) plan.getPlanElements().get(i+2);
						//nextLeg= (Leg) plan.getPlanElements().get(i+1);
						if(act.getType().equals(STR_PTINTERACTION) && lastAct.getType().equals(STR_PTINTERACTION) && nextAct.getType().equals(STR_PTINTERACTION) /* && lastLeg.getMode().equals(TransportMode.transit_walk) && nextLeg.getMode().equals(TransportMode.transit_walk) */){
							transfers++;
						}
					}
					hasPtConnection= (hasPtConnection || act.getType().equals(STR_PTINTERACTION));
					lastAct= act;
				}else{
					LegImpl leg = (LegImpl)pe;
					nextAct= (Activity) plan.getPlanElements().get(i+1);
					double legDist=  CoordUtils.calcDistance(lastAct.getCoord() , nextAct.getCoord());
					totLinDist += legDist;
					
					///find out walk distances
					if (leg.getMode().equals(TransportMode.transit_walk)){
						trWalkLegs ++; 
						walkDistance += legDist;
						walkTime += leg.getTravelTime();
						
						//System.out.println("legWalkDist:" + legWalkDist);
						if(lastAct.equals(aAct) && nextAct.equals(bAct)){
							directWalks++;
						}
					}else if (leg.getMode().equals(TransportMode.pt)){							
						if (leg.getRoute()!= null) {
							ptLegs ++;
							inVehDist += leg.getRoute().getDistance();
							inVehTravTime +=  leg.getTravelTime();
						}
					}else{
						otherModes++;
					}
					lastLeg= leg;
				}
				i++;
			}//for planelement
		}//	for person

		System.out.println("popFile:	\t" + popFile);
		System.out.println("plansBeforeFragm:  \t" + plansBefFragm);
		System.out.println("plansAfterFrag:  \t" + population.getPersons().size());
		System.out.println("PTLegs:\t" + ptLegs);
		System.out.println("trWalkLegs:\t" + trWalkLegs);
		System.out.println("directWalks:  \t" + directWalks);
		System.out.println("inVehTravTime:  \t" + inVehTravTime);
		System.out.println("inVehDist:  \t" + inVehDist);
		System.out.println("totalLinDistance:  \t" + totLinDist);
		System.out.println("transfers:   \t" + transfers);
		System.out.println("walkTime:    \t" + walkTime);
		System.out.println("walkDistance:\t" + walkDistance);
		
		stringBuffer.append("\n" + popFile + TAB + plansBefFragm + TAB + population.getPersons().size() + TAB+ ptLegs + TAB + trWalkLegs + TAB + directWalks + TAB + 
				inVehTravTime + TAB + inVehDist + TAB + totLinDist + TAB + transfers +  TAB + walkTime + TAB + walkDistance);
	}

	protected void write(final String outFile){
		try {
			bufferedWriter = new BufferedWriter(new FileWriter(outFile));
			bufferedWriter.write(stringBuffer.toString());
			bufferedWriter.close(); 
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static void loadManyScenarios(final String dirPath){
		File folder = new File (dirPath);
		final String PREFIX = "routedPlan";
		PopulationPtAnalyzer populationPtAnalyzer = new PopulationPtAnalyzer ( dirPath );
		for (int i=0; i< folder.list().length ; i++){
			String file = folder.list()[i];
			
			if (file.startsWith(PREFIX)){
				populationPtAnalyzer.run(file);
			}
		}
		populationPtAnalyzer.write(dirPath + "data.txt");
	}
	
	private static void loadOneScenario(String configFile){
		ScenarioLoaderImpl scenarioLoader = ScenarioLoaderImpl.createScenarioLoaderImplAndResetRandomSeed(configFile);
		ScenarioImpl scenario = scenarioLoader.getScenario();
		scenarioLoader.loadScenario();
		//new PopulationPtAnalyzer (scenario, "results.txt").run();
	}
	
	public static void main(String[] args) throws IOException {
		String configFile = null;
		if (args.length==1){
			configFile = args[0];
		}else{
			configFile = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/calibration/100plans_bestValues_config.xml";
			configFile = "../playgrounds/mmoyo/output/alltest/";
		}
		  
		//for many scenarios resulted from incrementing time priority
		loadManyScenarios(configFile);

		//for one scenario
		//loadOneScenario(configFile);
		
	}
}
