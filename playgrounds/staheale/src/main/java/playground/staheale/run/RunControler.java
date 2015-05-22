package playground.staheale.run;

import org.matsim.api.core.v01.Id;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.gbl.Gbl;

import playground.staheale.occupancy.FacilityOccupancy;

import java.util.TreeMap;

public class RunControler extends Controler {
	private TreeMap<Id, FacilityOccupancy> facilityOccupancies = new TreeMap<Id, FacilityOccupancy>();;


	public RunControler(final String[] args) {
		super(args);
		this.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );

		throw new RuntimeException( Gbl.SET_UP_IS_NOW_FINAL ) ;
	}

	public static void main(String[] args) {
		RunControler controler = new RunControler( args ) ;	
		controler.run();
	}

//	protected void setUp() {
//
//        this.getConfig().controler().setCreateGraphs(true);
//        this.setDumpDataAtEnd(false);
//		this.getConfig().controler().setWriteEventsInterval(0);
//
//		ObjectAttributes attributes = new ObjectAttributes();
//		ObjectAttributesXmlReader attributesReader = new ObjectAttributesXmlReader(attributes);
//		attributesReader.parse("./input/facilityAttributes.xml");
//
//		//---------------location choice module---------------------
//
//		this.getConfig().setParam("locationchoice", "restraintFcnFactor", "0.0");
//
//		ActivitiesHandler defineFlexibleActivities = new ActivitiesHandler((DestinationChoiceConfigGroup) this.getConfig().getModule("locationchoice"));
//		ScaleEpsilon scaleEpsilon = defineFlexibleActivities.createScaleEpsilon();
//
//		ActTypeConverter actTypeConverter = defineFlexibleActivities.getConverter();
//
//		//---------------location choice module end-------------------
//
//		// get objects that are required as parameter for the AgentInteractionScoringFunctionFactory 
//		PlanCalcScoreConfigGroup planCalcScoreConfigGroup = this.getConfig().planCalcScore();
//
//        ActivityFacilities facilities = getScenario().getActivityFacilities();
//        Network network = getScenario().getNetwork();
//
//		// create the AgentInteractionScoringFunctionFactory
//		AgentInteractionScoringFunctionFactory factory = new AgentInteractionScoringFunctionFactory(this, this.getConfig(), planCalcScoreConfigGroup,
//				facilities, network, Double.parseDouble(this.getConfig().findParam("locationchoice", "scaleFactor")),
//				facilityOccupancies, attributes, scaleEpsilon, actTypeConverter,
//				defineFlexibleActivities.getFlexibleTypes());
//
//
//		// set the AgentInteractionScoringFunctionFactory as default in the controler 
//		this.setScoringFunctionFactory(factory);
//		super.setUp();	
//
//		addControlerListener(new FacilitiesOccupancyCalculator(this.facilityOccupancies, AgentInteraction.numberOfTimeBins, AgentInteraction.scaleNumberOfPersons));
//
//		this.addControlerListener(new DistanceStats(this.getConfig(), "best", "s", actTypeConverter, "car"));
//		this.addControlerListener(new DistanceStats(this.getConfig(), "best", "l", actTypeConverter, "car"));
//	}
}
