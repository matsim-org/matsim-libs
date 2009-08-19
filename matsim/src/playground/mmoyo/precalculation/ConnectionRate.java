package playground.mmoyo.precalculation;

import java.util.List;

/**Selects the best connections*/  
public class ConnectionRate {

	public ConnectionRate (){
		
	}

	public List<StaticConnection> selectBestConnections(int number, List<StaticConnection> staticConnectionList){
		List<StaticConnection> bestConnectionList= null;
		
		int size = staticConnectionList.size();
		double parameters[][] = new double[3][size];
		
		for (int i= 0; i<size; i++){
			StaticConnection staticConnection =  bestConnectionList.get(i); 
			parameters[i][0] = staticConnection.getTransferNum(); 
			parameters[i][1] = staticConnection.getTravelTime(); 
			parameters[i][2] = staticConnection.getDistance();
			//parameters[i][3] = staticConnection.  monetary cost?
			//parameters[i][4] = staticConnection.  walk distances?
		
		}
		
		return bestConnectionList;
	}
	
	private void parametersRate (List<StaticConnection> staticConnectionList) {
		
		
		
	}
	
}
