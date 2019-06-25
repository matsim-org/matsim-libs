package ft.utils.ctDemandPrep;

import org.matsim.api.core.v01.Coord;

public class Company {
	public Coord coord;
	public String zone;
	public String companyClass;


	Company(Coord coord, String zone, String companyClass) {
		this.coord = coord;
		this.zone = zone;
		this.companyClass = companyClass;

	}

}
