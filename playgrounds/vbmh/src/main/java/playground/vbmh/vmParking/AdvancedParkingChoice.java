package playground.vbmh.vmParking;

import java.util.LinkedList;
import java.util.Random;

import playground.vbmh.controler.VMConfig;

/**
 * 
 * Implements the actual parking choice model which is described in the paper; It gets all the available options as input;
 * assigns scores to them and returns the best one;
 * 
 * 
 * 
 * @author Valentin Bemetz & Moritz Hohenfellner
 *
 */
public class AdvancedParkingChoice {
	/*
	 * LMOSC values between 0 and 1; 1 is the best
	 * LMReserve values between -1 and 0; 0 is the best
	 */
	double betaPayMoney;
	double betaWalkPMetre;
	double betaSOCMean;
	double betaSOCSD;
	Random random;
	double betaSOC=0;
	double betaReserve;
	double requiredRestOfDayBatPerc;
	LinkedList<Option> options = new LinkedList<Option>();
	
	public void startUp(){
		betaPayMoney = VMConfig.betaPayMoney; // in Util/$; should be <0
		//System.out.println("betaMoney : "+betaPayMoney);
		betaWalkPMetre = VMConfig.betaWalkPMetre; // in Util / m; should be <0
		//System.out.println("betaWalkPMetre : "+betaWalkPMetre);
		betaReserve = VMConfig.betaReserve; // in Util should be >0
		//System.out.println("betaReserve : "+betaReserve);
		betaSOCMean = VMConfig.betaSOCMean; // in $/1
		betaSOCSD = VMConfig.betaSOCSD;	// in $/1
		random = new Random();
		if(betaSOC==0){
			betaSOC = (betaSOCMean + (random.nextGaussian()*betaSOCSD))*betaPayMoney*(-1); //in Util/1
			//System.out.println("betaSOC : "+betaSOC);
		}
		//System.out.println("betaSOC : "+betaSOC);
	}
	
	
	
	public double getBetaSOC() {
		return betaSOC;
	}



	public void setBetaSOC(double betaSOC) {
		this.betaSOC = betaSOC;
	}



	public void setRequiredRestOfDayBatPerc(double requiredRestOfDayBatPerc) {
		this.requiredRestOfDayBatPerc = requiredRestOfDayBatPerc/100.0;
	}



	public double calcLMSOC(double soc){
		
		double a; // = -3.889; //x^2 term
		double b; // = 10.97; // x term
		double c; // = -2.159; // constant
		double d; //= 2.425; // constant outside of exp
		double e; // = 0.0454411; //shift down
		double f; //= 1.067; //scale up
		
		if(true){
			a = VMConfig.LMSOCa;
			b = VMConfig.LMSOCb;
			c = VMConfig.LMSOCc;
			d = VMConfig.LMSOCd;
			e = VMConfig.LMSOCe;
			f = VMConfig.LMSOCf;
		}
		
		
		
		double LMSOC = 0.0;
		LMSOC = (((Math.exp(a*soc*soc+b*soc+c))/(d+Math.exp(a*soc*soc+b*soc+c))-e)*f);
		//System.out.println("SOC : "+soc+" LMSOC : "+LMSOC);
		return LMSOC;
	}
	
	public double calcLMReserve(double requiredBatPercRestOfDay, double newSOC){
		double reserve = newSOC - requiredBatPercRestOfDay;
		double LMReserve=(Math.exp(1/0.8)-Math.exp(1/(1000*reserve+0.8))-2.4825)/2.5;
		if(reserve<0){
			LMReserve=-1;
		}
		if(LMReserve>0){
			LMReserve=0;
		}
		//System.out.println("LMReserve :"+LMReserve);
		return LMReserve;
	}
	
	public double calcUtil(double walkingDistance, double price, double LMReserve,  double newLMSOC){
		double walkAmount = betaWalkPMetre * walkingDistance;
		double priceAmount = betaPayMoney * price;
		double reserveAmount = betaReserve * LMReserve;
		double socAmount = betaSOC*newLMSOC;
//		if (newLMSOC<0.0) {
//			System.out
//					.println("-----------------------------------------------");
//			System.out.println("beta SOC" + betaSOC);
//			System.out.println("distance " + walkingDistance + " price "
//					+ price + " LMReserve " + LMReserve + " LMSOC " + newLMSOC);
//			System.out.println("walk " + walkAmount + " price " + priceAmount
//					+ " reserve " + reserveAmount + " soc " + socAmount);
//			double abc = 1/0;
//		}
		double util = walkAmount+priceAmount+reserveAmount+socAmount;
		//System.out.println("util "+util);
		return util;
	}
	
	public void addOption(Option option){
		options.add(option);
	}
	
	public Option selectBestOption(){
		//this.startUp();
		Option bestOption = null;
//		System.out.println("---------Pruefe optionen:-------");
		for(Option option : options){
			double LMSOC = this.calcLMSOC(option.newSOC);
//			System.out.println("option new SOC : "+option.newSOC);
			double price = option.price;
			double walkingDistance = option.walkingDistance;
			double LMReserve = this.calcLMReserve(this.requiredRestOfDayBatPerc, option.newSOC);
			if(requiredRestOfDayBatPerc==-1/100.0){ //Special value for NEVs
				LMSOC=0.0;
				LMReserve=0.0;
			}
			option.score=this.calcUtil(walkingDistance, price, LMReserve, LMSOC);
			if(bestOption==null || option.score>bestOption.score){
				bestOption=option;
			}
		}
		return bestOption;
	}
	
	
	public class Option{
		double price, walkingDistance, newSOC;
		double score=0;
		public ParkingSpot spot;

		public Option(ParkingSpot spot, double price, double walkingDistance, double newSOC) {
			super();
			this.spot = spot;
			this.price = price;
			this.walkingDistance = walkingDistance;
			this.newSOC = newSOC;
		}
		
		public String toString(){
			return "price : "+price+" walkingDistance "+walkingDistance+" newSOC "+newSOC+" score "+score;
		}
	}
	
	
}
