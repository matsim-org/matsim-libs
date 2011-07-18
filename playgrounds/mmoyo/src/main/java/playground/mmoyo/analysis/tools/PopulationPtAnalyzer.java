package playground.mmoyo.analysis.tools;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.LegImpl;
import org.matsim.core.utils.geometry.CoordUtils;

import playground.mmoyo.Validators.PlanValidator;
import playground.mmoyo.io.TextFileWriter;
import playground.mmoyo.utils.DataLoader;
import playground.mmoyo.utils.PlanFragmenter;


/**Calculates and saves the global travel time, travel distance, transfers, det-transfers, walk distance and waiting time of a population*/ 
public class PopulationPtAnalyzer {
	private final static Logger log = Logger.getLogger(PopulationPtAnalyzer.class);
	
	final String outFile;
	private PlanValidator planValidator = new PlanValidator();
	private playground.mmoyo.io.TextFileWriter textWriter;
	private final String STR_PTINTERACTION = "pt interaction";
	private final String TAB= "\t";
	
	//constant strings for logging
	final String strPopFile = "popFile:  \t";
	final String strPlansBeforeFragm = "plansBeforeFragm:  \t";
	final String strPlansAfterFrag = "plansAfterFrag:  \t";
	final String strPTLegs =  "PTLegs:\t";
	final String strTrWalkLegs =  "trWalkLegs:\t";
	final String strDirectWalks =  "directWalks:  \t";
	final String strInVehTravTime =  "inVehTravTime:  \t";
	final String strInVehDist =  "inVehDist:  \t";
	final String strTotalLinearDistance =  "totalLinearDistance:  \t";
	final String strTransfers =  "transfers:   \t";
	final String strWalkTime =  "walkTime:    \t";
	final String strWalkDistance = "walkDistance:\t";
	
	public PopulationPtAnalyzer(final String outFile){
		this.outFile = outFile;
		String header = "popFile\tplansBefFragm\tplansAfterFrag\tPTlegs\ttrWalkLegs\tdirectWalks\tinVehTravTime:\tinVehDist:\ttravelDist2:\ttransfers:\twalkTime:\twalkDistance:";
		textWriter = new TextFileWriter();
		textWriter.write(header,this.outFile,false);
		header= null;
	}
	
	public void run (final String popName, final Population population){
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
		
		final int plansBefFragm = population.getPersons().size();
		
		
		//validate the sequence act-lec-act
		if (!planValidator.hasSecqActLeg(population)) { 
			throw new RuntimeException("inadequate sequence of acts and legs");
		}	
		
		//plans must be fragmented
		Population fragPop = new PlanFragmenter().run(population);

		for (Person person : fragPop.getPersons().values() ){
			Plan plan =  person.getSelectedPlan();  
			Activity aAct = ((Activity)plan.getPlanElements().get(0));
			Activity bAct = ((Activity)plan.getPlanElements().get(plan.getPlanElements().size()-1));
			
			int i = 0;
			Activity lastAct= null;
			Activity nextAct;
			//Leg lastLeg = null;
			//Leg nextLeg;
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
							
							//if (leg.getRoute() instanceof ExperimentalTransitRoute){
							//ExperimentalTransitRoute expTrRoute = ((ExperimentalTransitRoute)leg.getRoute());
								//RouteUtils.calcDistance(leg.getRoute(), network)
								//inVehDist += leg.getRoute().getDistance();
								inVehTravTime +=  leg.getTravelTime();
							//}
						}
					}else{
						otherModes++;
					}
			//		lastLeg= leg;
				}
				i++;
			}//for planelement
		}//	for person

		
		log.info(this.strPopFile + popName);
		log.info(this.strPlansBeforeFragm + plansBefFragm);
		log.info(this.strPlansAfterFrag + fragPop.getPersons().size());
		log.info(this.strPTLegs + ptLegs);
		log.info(this.strTrWalkLegs + trWalkLegs);
		log.info(this.strDirectWalks + directWalks);
		log.info(this.strInVehTravTime + inVehTravTime);
		log.info(this.strInVehDist + inVehDist);
		log.info(this.strTotalLinearDistance + totLinDist);
		log.info(this.strTransfers + transfers);
		log.info(this.strWalkTime + walkTime);
		log.info(this.strWalkDistance + walkDistance);
		
		String result = "\n" + popName + TAB + plansBefFragm + TAB + fragPop.getPersons().size() + TAB+ ptLegs + TAB + trWalkLegs + TAB + directWalks + TAB + 
				inVehTravTime + TAB + inVehDist + TAB + totLinDist + TAB + transfers +  TAB + walkTime + TAB + walkDistance;
	
		this.textWriter.write(result,this.outFile,true);
	}


	
	
	/*
	private static void loadManyScenarios(List<Tuple<String, Population>> popsList){
		PopulationPtAnalyzer populationPtAnalyzer = new PopulationPtAnalyzer ();
		for (Tuple<String, Population> tuple : popsList){
			String popName = tuple.getFirst();
			Population pop = tuple.getSecond();
			populationPtAnalyzer.run(popName, pop);
		}
		populationPtAnalyzer.write(dirPath + "data.txt");
	}
	
	private static void loadOneScenario(String popFile){
		DataLoader loader = new DataLoader();
		Scenario scn = loader.loadScenario(popFile);
		
		new PopulationPtAnalyzer().run(scn.getPopulation(), popFile);
	}
	*/
	
	public static void main(String[] args) throws IOException {
		/*
		String configFile = null;
		if (args.length==1){
			configFile = args[0];
		}else{
			configFile = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/calibration/100plans_bestValues_config.xml";
			configFile = "../playgrounds/mmoyo/output/alltest/";
		}
		*/
		
		final String outFile = "../../input/mut_rou/outFile.txt";
		
		PopulationPtAnalyzer popAnalyzer = new PopulationPtAnalyzer(outFile);
		DataLoader dataloader = new DataLoader();
		
		//--> find a better way to do this
		String name1= "mut_rout";
		Population pop1= dataloader.readPopulation("../../input/mut_rou/6planSample.xml");
		popAnalyzer.run(name1, pop1);	
		pop1=null;
		
		/*
		String name2 ="rout_mut";
		Population pop2 = dataloader.readPopulation("../../input/popAnalysis/rou_mut_500.plans.xml.gz");
		popAnalyzer.run(name2, pop2);	
		pop2= null;
		*/
	}
}
