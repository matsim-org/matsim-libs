package playground.mmoyo.PTCase2;

import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkFactory;
import org.matsim.core.network.NetworkLayer;

import playground.mmoyo.TransitSimulation.ToTransitScheduleConverter;
import playground.mmoyo.Validators.NetValidator;
import playground.mmoyo.Validators.PlanValidator;
import playground.mmoyo.input.PTLineAggregator;

public class PTControler2 {
    private static String path = "../shared-svn/studies/schweiz-ivtch/pt-experimental/"; 
    //private static String path = "C://Users/manuel/Desktop/TU/scenarios/Zuerich/";
    //private static String path = "C://Users/manuel/Desktop/TU/scenarios/5x5";
    
	private static final String CONFIG=  path  + "config.xml";
	private static final String ZURICHPTN= path + "network.xml";
	private static final String ZURICHPTTIMETABLE= path + "PTTimetable.xml";
	private static final String INPTNETFILE= path + "inptnetfile.xml";
	private static final String ZURICHPTPLANS= path + "plans.xml";
	private static final String ALLPLANS= path + "@All_plans .xml";
	private static final String OUTPUTPLANS= path + "output_plans.xml";
	private static final String INPTNEWLINES= path + "TestCase/InPTNewLines.xml";
	private static final String DIVNODES= path + "TestCase/DivNodes.xml";
	
	public static void main(String[] args){
		PTOb pt= new PTOb(CONFIG, INPTNETFILE, ZURICHPTN, ZURICHPTTIMETABLE,ZURICHPTPLANS, OUTPUTPLANS); 
		
		int option =3;
		
		if (option>0){pt.readPTNet(ZURICHPTN);}
		switch (option){
			case -6:
				PlanValidator planValidator = new PlanValidator(); 
				int num = planValidator.PlanCounter(pt.getPtNetworkLayer(), ZURICHPTPLANS);
				System.out.println(num);
				break;
			case -5:  //create net from diva and route agents
				NetworkLayer net = new NetworkLayer();
				pt.setPtNetworkLayer(net);
				PTLineAggregator ptLineAggregator = new PTLineAggregator(INPTNEWLINES, pt.getPtNetworkLayer(), pt.getPtTimeTable());
				ptLineAggregator.AddLines();
				
				pt.getPtNetworkFactory().createTransferLinks(pt.getPtNetworkLayer(), pt.getPtTimeTable());
				pt.getPtNetworkFactory().CreateDetachedTransfers(pt.getPtNetworkLayer(), 300, pt.getPtTimeTable());
				
				pt.writeNet(ZURICHPTN);
				pt.createRouter();
				PTActWriter ptActWriter1 = new PTActWriter(pt);
	    		ptActWriter1.writePTActsLegs();
				break;
			case -4:
			
				break;
			case -3:
				//pt.createPTNetWithTLinks(INPTNETFILE,ZURICHPTN);
	    		pt.readPTNet(INPTNETFILE);
				ptLineAggregator = new PTLineAggregator(INPTNEWLINES, pt.getPtNetworkLayer(), pt.getPtTimeTable());
				ptLineAggregator.AddLine();
	    		pt.writeNet(ZURICHPTN);
				break;
			case -2:
				pt.createPTNetWithTLinks(INPTNETFILE);
				pt.writeNet(ZURICHPTN);
				pt.readPTNet(ZURICHPTN);
	    		NetValidator netValidator = new NetValidator(pt.getPtNetworkLayer(), pt.getPtTimeTable());
	    		netValidator.printNegativeTransferCosts(28800);
				break;
			case -1:
				pt.createPTNetWithTLinks(INPTNETFILE);
				pt.getPtNetworkFactory().CreateDetachedTransfers(pt.getPtNetworkLayer(), 300, pt.getPtTimeTable());
				pt.writeNet(ZURICHPTN);
	    		pt.readPTNet(ZURICHPTN);
	    		PTActWriter ptActWriter2 = new PTActWriter(pt);
	    		ptActWriter2.writePTActsLegs();
	    		
				break;
			case 0: 
	    		pt.createPTNetWithTLinks(INPTNETFILE);
	    		//Map<String, List<IdImpl>> intersecionMap = pt.getPtNetworkFactory().createIntersecionMap(pt.getPtTimeTable());
	    		//new StationValidator().validateStations(pt.getPtNetworkLayer(),intersecionMap);
	    		break;
	    	
			case 1:
				pt.createPTNetWithTLinks(INPTNETFILE);
				pt.getPtNetworkFactory().CreateDetachedTransfers(pt.getPtNetworkLayer(), 300, pt.getPtTimeTable());
				pt.writeNet(ZURICHPTN);
	    		pt.readPTNet(ZURICHPTN);
	    		PTActWriter ptActWriter3 = new PTActWriter(pt);
	    		ptActWriter3.writePTActsLegs();
				break;

	    	case 3:
	    		//pt.getPtNetworkFactory().setDetNextLinks(pt.getPtNetworkLayer(), pt.getPtTimeTable());
	    		PTActWriter ptActWriter = new PTActWriter(pt);
	    		ptActWriter.writePTActsLegs();
	    		//ptActWriter.SimplifyPtLegs();
	    		//ptActWriter.ptTravelTime.costValidator.printNegativeVaues();
	    		//System.out.println(ptActWriter.ptTravelTime.costValidator==null);
	    		break;
	 
	    	case 4:
	    		PTTester pttester= new PTTester(pt);
	    		pttester.countRoutes();
	    		break;
	    	case 7:
	    		NetworkLayer netDiv= new NetworkLayer(new NetworkFactory());
	    		MatsimNetworkReader matsimNetworkReader = new MatsimNetworkReader(netDiv);
	    		matsimNetworkReader.readFile(DIVNODES);
	    		
	    		playground.mmoyo.Validators.StationValidator sv = new playground.mmoyo.Validators.StationValidator(pt.getPtNetworkLayer());
	    		sv.validateIds(netDiv);
	    		break;
	    	case 8:
	    		ToTransitScheduleConverter converter = new ToTransitScheduleConverter();	    		
	    		converter.createTransitSchedule(pt.getPtTimeTable(), path + "PtFacilities.xml", path + "transitSchedule.xml");
	    		//converter.createFacilities("../shared-svn/studies/schweiz-ivtch/pt-experimental/PtFacilities.xml");
	    		break;
		}//switch
	}//main

	
}//Class
