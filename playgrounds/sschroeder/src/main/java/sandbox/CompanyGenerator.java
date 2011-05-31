package sandbox;

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.gbl.MatsimRandom;

import sandbox.SandBoxTrafficGenerator.Company;
import sandbox.SandBoxTrafficGenerator.Region;



public class CompanyGenerator {

	private List<Region> regions;
	
	private List<Company> companies;
	
	public CompanyGenerator(List<Region> regions, List<Company> companies) {
		this.regions = regions;
		this.companies = companies;
	}
	
	public void run(){
		for(int j=0;j<1;j++){
			Region region = regions.get(j);
			int nOfCompanies = generateNofCompanies();
			for(int i=0;i<nOfCompanies;i++){
				Company company = generateCompany(region,(i+1));
				companies.add(company);
			}
		}
	}

	private Company generateCompany(Region region, int companyCounter) {
		Company company = new Company(makeId(region.getId().toString() + "_" + companyCounter));
		company.setLinkId(region.getLinkId());
		company.setnOfEmployees(10);
		company.setWirtschaftszweig("handel");
		return company;
	}

	private Id makeId(String string) {
		return new IdImpl(string);
	}

	private int generateNofCompanies() {
		return MatsimRandom.getRandom().nextInt(10)+1;
	}

}
