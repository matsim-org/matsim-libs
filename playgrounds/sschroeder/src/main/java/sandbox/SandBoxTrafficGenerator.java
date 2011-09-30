package sandbox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.mzilske.freight.carrier.Carrier;
import playground.mzilske.freight.carrier.CarrierVehicle;


public class SandBoxTrafficGenerator {
	
	public static class Region {
		
		private Id id;
		
		private Coord coord;
		
		private Id linkId;
		
		private List<Company> companies = new ArrayList<SandBoxTrafficGenerator.Company>();
		
		public List<Company> getCompanies() {
			return companies;
		}

		private Map<String,String> attributes = new HashMap<String, String>();

		public Region(Id id, Coord coord) {
			super();
			this.coord = coord;
			this.id = id;
		}

		public Coord getCoord() {
			return coord;
		}

		public Map<String, String> getAttributes() {
			return attributes;
		}

		public Id getId() {
			return id;
		}
		
		public void setLinkId(Id linkId){
			this.linkId = linkId;
		}

		public Id getLinkId() {
			return linkId;
		}	
	}
	
	public static class Company {
		private Id id;
		
		private Id linkId;
		
		public Id getLinkId() {
			return linkId;
		}

		public void setLinkId(Id linkId) {
			this.linkId = linkId;
		}

		private String wirtschaftszweig;
		
		private int nOfEmployees;
		
		private Id regionId;
		
		private Carrier carrier;
		
		public Id getId() {
			return id;
		}

		public Company(Id id) {
			super();
			this.id = id;
		}

		public void setCarrier(Carrier carrier) {
			this.carrier = carrier;
		}

		public String getWirtschaftszweig() {
			return wirtschaftszweig;
		}

		public void setWirtschaftszweig(String wirtschaftszweig) {
			this.wirtschaftszweig = wirtschaftszweig;
		}

		public int getnOfEmployees() {
			return nOfEmployees;
		}

		public void setnOfEmployees(int nOfEmployees) {
			this.nOfEmployees = nOfEmployees;
		}

		public Carrier getCarrier() {
			return carrier;
		}

	}
	
	public static class Pkw extends CarrierVehicle{

		public Pkw(Id vehicleId, Id location) {
			super(vehicleId, location);
		}
		
	}
	
	public static class Lkw extends CarrierVehicle{

		public Lkw(Id vehicleId, Id location) {
			super(vehicleId, location);
		}
		
	}

	
	private List<Region> regions = new ArrayList<SandBoxTrafficGenerator.Region>();
	
	private List<Company> companies = new ArrayList<SandBoxTrafficGenerator.Company>();
	
	private TourGenerator tourGenerator;
	
	private List<Carrier> carriers = new ArrayList<Carrier>();

	private CompanyGenerator companyGenerator;

	public SandBoxTrafficGenerator(List<Carrier> carriers) {
		super();
		this.carriers = carriers;
	}

	public void run(){
		iniSandbox();
		companyGenerator.run();
		tourGenerator.run();
	}

	private void iniSandbox() {
		for(int i=0;i<8;i++){
			for(int j=0;j<8;j++){
				Region region = getRegion(i, j);
				region.setLinkId(makeLinkId(i+1,j+1));
				regions.add(region);
			}
		}
		companyGenerator = new CompanyGenerator(regions,companies);
		tourGenerator = new TourGenerator(regions, companies, carriers);
	}


	private Id makeLinkId(int i, int j) {
		return new IdImpl("j(" + i + "," + j + ")");
	}

	private Region getRegion(int i, int j) {
		return new Region(makeId(i,j),new CoordImpl(i,j));
	}

	private Id makeId(int i, int j) {
		return new IdImpl(i + "_" + j);
	}
	
	

}
