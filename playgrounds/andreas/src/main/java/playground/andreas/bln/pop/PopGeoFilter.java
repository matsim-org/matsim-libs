package playground.andreas.bln.pop;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.population.routes.NetworkRouteWRefs;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;


/**
 * Analyzes the plans of a given routed population and drops those not passing through a given area.
 * In case of public transit is involved, the transit lines of the given area have to be specified.
 * 
 * Input:<br>
 * String wholeRoutedPlansFile : The routed plans file
 * String wholeBigNetworkFile : The network used to route the plans of wholeRoutedPlansFile
 * String unroutedWholePlansFile : The unrouted plansfile containing the same agents as wholeRoutedPlansFile		
 * String outPlansFile : Output filename and path
 * String ptLinesToKeep : ','-separated two columns file with first column line name, second column line ID, first line can be header
 * 
 * @author aneumann
 *
 */
public class PopGeoFilter extends NewPopulation implements TabularFileHandler {
	
	private static final Logger log = Logger.getLogger(PopGeoFilter.class);
	
	private TabularFileParserConfig tabFileParserConfig;

	private int planswritten = 0;
	private int personshandled = 0;
	private PopulationImpl unRoutedPlans;
	private Coord xyMin;
	private Coord xyMax;
	private ArrayList<String> ptLinesToKeep;

	public PopGeoFilter(PopulationImpl wholeRoutedPop, String outPlansFile, PopulationImpl unroutedWholePop, Coord xyMin, Coord xyMax) {
		super(wholeRoutedPop, outPlansFile);
		this.unRoutedPlans = unroutedWholePop;
		this.xyMin = xyMin;
		this.xyMax = xyMax;
	}

	
	@Override
	public void run(Person person) {
		
		this.personshandled++;
		
		if(person.getPlans().size() != 1){
			log.error("Person " + person.getId() + " got more than one plan. Don't know what to do.");
		} else {
			
			Plan plan = person.getPlans().get(0);
			boolean keepPlan = false;

			for (PlanElement planElement : plan.getPlanElements()) {
				
				if(planElement instanceof ActivityImpl){
					
					double actX = ((ActivityImpl) planElement).getCoord().getX();
					double actY = ((ActivityImpl) planElement).getCoord().getY();
					if(actX >= this.xyMin.getX() && actX <= this.xyMax.getX() && actY >= this.xyMin.getY() && actY <= this.xyMax.getY()){
						keepPlan = true;
						break;
					}				
					
				} else if(planElement instanceof LegImpl){
					if(((LegImpl) planElement).getRoute() instanceof NetworkRouteWRefs){
						for (Node node : ((NetworkRouteWRefs)((LegImpl) planElement).getRoute()).getNodes()) {
							
							double nodeX = node.getCoord().getX();
							double nodeY = node.getCoord().getY();
							if(nodeX >= this.xyMin.getX() && nodeX <= this.xyMax.getX() && nodeY >= this.xyMin.getY() && nodeY <= this.xyMax.getY()){
								keepPlan = true;
								break;
							}				
							
						}
					}
				}
					
				if(keepPlan){
					break;
				}
			}
			
			if(keepPlan){
				this.popWriter.writePerson(this.unRoutedPlans.getPersons().get(person.getId()));
				this.planswritten++;
			}			
		}
	}
	
	public static void main(final String[] args) {
		Gbl.startMeasurement();
		
		ScenarioImpl bigScenario = new ScenarioImpl();

		String wholeBigNetworkFile = "D:/Berlin/BVG/berlin-bvg09/pt/baseplan_900s_bignetwork/network.multimodal.xml.gz";
		String unroutedWholePlansFile = "D:/Berlin/BVG/berlin-bvg09/pop/baseplan_900s.xml.gz";
		String wholeRoutedPlansFile = "D:/Berlin/BVG/berlin-bvg09/pt/baseplan_900s_bignetwork/baseplan_900s.routedOevModell.xml.gz";
		String outPlansFile = "./subset_pop.xml.gz";
		String ptLinesToKeep = "D:/Berlin/BVG/berlin-bvg09/net/pt/linien_im_untersuchungsgebiet.txt";
		
		Coord xyMin = new CoordImpl(4590999.0, 5805999.0);
		Coord xyMax = new CoordImpl(4606021.0, 5822001.0);
		
//		String wholeBigNetworkFile = "./network.multimodal.xml.gz";
//		String unroutedWholePlansFile = "./baseplan_10x_900s.xml.gz";
//		String wholeRoutedPlansFile = "./baseplan_10x_900s.routedOevModell.xml.gz";
//		String outPlansFile = "./subset_pop.xml.gz";
//		String ptLinesToKeep = "./linien_im_untersuchungsgebiet.txt";
		
		System.out.println("Reading network " + wholeBigNetworkFile);
		NetworkLayer wholeBigNet = bigScenario.getNetwork();
		new MatsimNetworkReader(wholeBigNet).readFile(wholeBigNetworkFile);

		System.out.println("Reading routed population: " + wholeRoutedPlansFile);
		PopulationImpl wholeRoutedPop = new PopulationImpl();
		PopulationReader popReader = new MatsimPopulationReader(new SharedNetScenario(bigScenario, wholeRoutedPop));
		popReader.readFile(wholeRoutedPlansFile);
		
		System.out.println("Reading unrouted population: " + unroutedWholePlansFile);
		PopulationImpl unroutedWholePop = new PopulationImpl();
		PopulationReader origPopReader = new MatsimPopulationReader(new SharedNetScenario(bigScenario, unroutedWholePop));
		origPopReader.readFile(unroutedWholePlansFile);

		PopGeoFilter dp = new PopGeoFilter(wholeRoutedPop, outPlansFile, unroutedWholePop, xyMin, xyMax);
		System.out.println("Reading pt lines to keep from " + ptLinesToKeep);
		dp.readFile(ptLinesToKeep);
		System.out.println("Filtering...");
		dp.run(wholeRoutedPop);
		
		System.out.println("Finished: " + dp.personshandled + " persons handled; " + dp.planswritten + " plans written to " + outPlansFile);
		dp.writeEndPlans();

		Gbl.printElapsedTime();
	}
	
	protected void printStatistics() {
		log.info("Finished: " + this.personshandled + " persons handled; " + this.planswritten);
	}
	
	
	// TabReader below
	protected void readFile(String filename) {
		
		log.info("Reading pt lines to keep from " + filename);
		
		this.ptLinesToKeep = new ArrayList<String>();
		this.tabFileParserConfig = new TabularFileParserConfig();
		this.tabFileParserConfig.setFileName(filename);
		this.tabFileParserConfig.setDelimiterTags(new String[] {","}); // \t

		try {
			new TabularFileParser().parse(this.tabFileParserConfig, this);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}	

	public void startRow(String[] row) throws IllegalArgumentException {
		
		if(row[0].contains("beschreibung")){
			StringBuffer tempBuffer = new StringBuffer();
			for (String string : row) {
				tempBuffer.append(string);
				tempBuffer.append(", ");
			}
			log.info("Header found: " + tempBuffer);
		} else {
			if(row.length == 2){
				this.ptLinesToKeep.add(row[1]);
				log.info("Id found for line " + row[0] + " > plans with line " + row[1] + " will be included");
			} else if(row.length == 1){
				log.info("Id NOT found for line " + row[0] + " > line ist ignored");
			}

		}
		
	}

}
