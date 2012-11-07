package playground.acmarmol.matsim2030.forecasts.p2030preparation;

import java.util.TreeMap;
import org.matsim.api.core.v01.Id;


public class Municipalities {

	private TreeMap<Id, Municipality> municipalities;
	

	public Municipalities(){
		this.municipalities = new TreeMap<Id,Municipality>();
		
	}


	public TreeMap<Id, Municipality> getMunicipalities() {
		return municipalities;
	}

    public void addMunicipality(Municipality municipality){
    	this.municipalities.put(municipality.getId(), municipality);
    }
	
	public Municipality getMunicipality(Id id){
		return this.municipalities.get(id);
	}
	
	
}
