package playground.jbischoff.div;

import java.io.IOException;
import java.util.Map;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.signalsystems.MatsimSignalSystemsReader;
import org.matsim.signalsystems.data.signalgroups.v20.SignalGroupData;
import org.matsim.signalsystems.data.signalgroups.v20.SignalGroupDataImpl;
import org.matsim.signalsystems.data.signalgroups.v20.SignalGroupsData;
import org.matsim.signalsystems.data.signalgroups.v20.SignalGroupsDataImpl;
import org.matsim.signalsystems.data.signalgroups.v20.SignalGroupsReader20;
import org.matsim.signalsystems.data.signalgroups.v20.SignalGroupsWriter20;
import org.xml.sax.SAXException;

public class SgT {
public static void main(String args0[]) throws JAXBException, SAXException, ParserConfigurationException, IOException{
	SignalGroupsData sgd = new SignalGroupsDataImpl();
	SignalGroupsReader20 sgr = new SignalGroupsReader20(sgd,MatsimSignalSystemsReader.SIGNALGROUPS20);
	sgr.readFile("/Users/JB/workspace/matsim/test/input/org/matsim/signalsystems/data/signalgroups/v20/testSignalGroups_v2.0.xml");
	
	/*System.out.println(sgd);
	SignalGroupData sd = new SignalGroupDataImpl(new IdImpl("1"));
	sd.setId(new IdImpl("0"));
	sd.addSignalId(new IdImpl("2"));
	sd.addSignalId(new IdImpl("7"));
	sd.addSignalId(new IdImpl("5"));
	SignalGroupData sd1 = new SignalGroupDataImpl(new IdImpl("1"));
	sd1.setId(new IdImpl("2"));
	sd1.addSignalId(new IdImpl("3"));
	sd1.addSignalId(new IdImpl("8"));
	sd1.addSignalId(new IdImpl("6"));
	
	SignalGroupData sd2 = new SignalGroupDataImpl(new IdImpl("2"));
	sd2.setId(new IdImpl("0"));
	sd2.addSignalId(new IdImpl("2"));
	sd2.addSignalId(new IdImpl("7"));
	sd2.addSignalId(new IdImpl("5"));
	
	sgd.addSignalGroupData(sd);
	sgd.addSignalGroupData(sd1);
	sgd.addSignalGroupData(sd2);
	
	SignalGroupsWriter20 sgw = new SignalGroupsWriter20(sgd);
	sgw.write("/Users/JB/test.xml");*/
	
	


	
	Map<Id,SignalGroupData> ss42 = sgd.getSignalGroupDataBySystemId(new IdImpl("23"));
	System.out.println(ss42.get(new IdImpl("1")).getSignalIds());
	System.out.println(ss42.get(new IdImpl("2")).getSignalIds());
	

	

	
}
}
