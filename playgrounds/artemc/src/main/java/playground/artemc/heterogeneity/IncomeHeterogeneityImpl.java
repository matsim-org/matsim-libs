package playground.artemc.heterogeneity;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.MatsimRandom;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

public class IncomeHeterogeneityImpl implements IncomeHeterogeneity {

	private String name = null;
	private String type = null;

	private static final Logger log = Logger.getLogger(IncomeHeterogeneityImpl.class);

	private double lambda_income;

	static boolean heterogeneitySwitch = false;
	static boolean homogeneousIncomeFactorSwitch = false;

	private Config config;

	private HashMap<Id<Person>, Double> incomeFactors;
	private HashMap<Id<Person>, Double> betaFactors;
	private Population population;

	public IncomeHeterogeneityImpl(Population population){
		//Initialize all maps
		this.incomeFactors = new HashMap<Id<Person>, Double>();
		this.betaFactors = new HashMap<Id<Person>, Double>();
		this.population = population;
		for(Id<Person> personId:population.getPersons().keySet()){
			this.incomeFactors.put(personId, 1.0);
			this.betaFactors.put(personId, 1.0);
		}
	}

	public double getLambda_income() {
		return lambda_income;
	}

	public void setLambda_income(final double lambda_income){this.lambda_income=lambda_income;}

	@Override
	public HashMap<Id<Person>, Double> getIncomeFactors() {
		return incomeFactors;
	}

	@Override
	public HashMap<Id<Person>, Double> getBetaFactors() {
		return betaFactors;
	}

	public void setName(final String name) {
		this.name = name;
	}

	@Override
	public String getName() {
		return this.name;
	}

	public void setType(final String type) {
		this.type = type.intern();
	}

	@Override
	public String getType() {
		return this.type;
	}
}
