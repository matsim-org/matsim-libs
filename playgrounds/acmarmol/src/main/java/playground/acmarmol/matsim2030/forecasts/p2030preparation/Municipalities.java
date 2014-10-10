package playground.acmarmol.matsim2030.forecasts.p2030preparation;

import java.util.TreeMap;

import org.matsim.api.core.v01.Id;


public class Municipalities {

	private TreeMap<Id<Municipality>, Municipality> municipalities;


	public Municipalities(){
		this.municipalities = new TreeMap<>();

	}


	public TreeMap<Id<Municipality>, Municipality> getMunicipalities() {
		return municipalities;
	}

	public void addMunicipality(Municipality municipality){
		this.municipalities.put(municipality.getId(), municipality);
	}

	public Municipality getMunicipality(Id<Municipality> id){
		return this.municipalities.get(id);
	}


}
