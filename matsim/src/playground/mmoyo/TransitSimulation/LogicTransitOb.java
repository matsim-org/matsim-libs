package playground.mmoyo.TransitSimulation;

import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkFactory;
import org.matsim.core.network.NetworkLayer;
import org.xml.sax.SAXException;
import playground.marcel.pt.transitSchedule.TransitSchedule;
import playground.marcel.pt.transitSchedule.TransitScheduleReaderV1;
import playground.mmoyo.PTRouter.PTRouter2;
import playground.mmoyo.PTRouter.PTTimeTable2;

public class LogicTransitOb {

	private NetworkLayer logicNet;
	private NetworkLayer plainNet;
	private TransitSchedule logicTransitSchedule;
	//private Map<Id,Id> logicStopMap;
	
	/**constructor based on files*/
	public LogicTransitOb(final String plainNetworkFile, final String logicNetworkFile, final String logicTransitScheduleFile) {
		
		/**read plain Net*/
		this.plainNet= new NetworkLayer(new NetworkFactory());
		new MatsimNetworkReader(plainNet).readFile(plainNetworkFile);
		
		/**read logicNet*/
		this.logicNet = new NetworkLayer(new NetworkFactory());
		new MatsimNetworkReader(logicNet).readFile(logicNetworkFile);
		
		/**reads logicTransitSchedule*/
		this.logicTransitSchedule = new TransitSchedule();
		try {
			new TransitScheduleReaderV1(this.logicTransitSchedule, logicNet).readFile(logicTransitScheduleFile);
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}

	/**constructor based on transitSchedule*/
	public LogicTransitOb(TransitSchedule transitSchedule){
 		LogicFactory logicFactory = new LogicFactory(transitSchedule);
 		this.logicTransitSchedule = logicFactory.getLogicTransitSchedule();
 		this.logicNet= logicFactory.getLogicNet();
 		this.plainNet = logicFactory.getPlainNet();
 		//this.logicStopMap = logicFactory.getLogicStopMap();
	}
	
	public PTRouter2 getPTRouter(){
		PTTimeTable2 logicPTTimeTable = new PTTimeTable2();
		TransitTravelTimeCalculator transitTravelTimeCalculator = new TransitTravelTimeCalculator(logicTransitSchedule,logicNet);
		transitTravelTimeCalculator.fillTimeTable(logicPTTimeTable);
		return new PTRouter2(plainNet, logicNet, logicPTTimeTable);
	}

	public NetworkLayer getLogicNet() {
		return logicNet;
	}

	public NetworkLayer getPlainNet() {
		return plainNet;
	}

	public TransitSchedule getLogicTransitSchedule() {
		return logicTransitSchedule;
	}
}
