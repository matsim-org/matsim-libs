package playground.julia.incomeScoring;

import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.basic.v01.IdImpl;

public class RoadUsedHandler implements LinkLeaveEventHandler{

	static boolean mer;
	int i;
	String usedRoadVariant = new String("");
	
public RoadUsedHandler(int j) {
		i= j;// TODO Auto-generated constructor stub
	}

@Override
public void reset(int iteration) {
	System.out.println("reset...");
}

@Override
public void handleEvent(LinkLeaveEvent event) {
	//System.out.println("LinkId"+event.getLinkId().toString());
	if (event.getLinkId().equals(new IdImpl(Integer.toString(i)))){
		mer = true;
	}
	else{
		if(event.getLinkId().equals(new IdImpl(Integer.toString(9)))) usedRoadVariant="9";
		if(event.getLinkId().equals(new IdImpl(Integer.toString(11)))) usedRoadVariant="11";
		if(event.getLinkId().equals(new IdImpl(Integer.toString(13)))) usedRoadVariant="13";
		
	}
}


public static boolean getWasRoadSelected() {
	return mer;
}

public static boolean getWasRoadSelectedDummyTrue(){
	return true;
}

public String getLink() {
	return usedRoadVariant;
}



}
