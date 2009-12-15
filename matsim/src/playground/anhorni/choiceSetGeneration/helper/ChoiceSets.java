package playground.anhorni.choiceSetGeneration.helper;

import java.util.Iterator;
import java.util.List;

public class ChoiceSets {
	
	private List<ChoiceSet> carChoiceSets = null;
	private List<ChoiceSet> walkChoiceSets = null;
	
	private double avgIncomeCarCS;
	private double avgHouseHoldSizeCarCS;
	
	private double avgIncomeWalkCS;
	private double avgHouseHoldSizeWalkCS;
	
	
	public List<ChoiceSet> getCarChoiceSets() {
		return carChoiceSets;
	}
	public void setCarChoiceSets(List<ChoiceSet> carChoiceSets) {
		this.carChoiceSets = carChoiceSets;
	}
	public List<ChoiceSet> getWalkChoiceSets() {
		return walkChoiceSets;
	}
	public void setWalkChoiceSets(List<ChoiceSet> walkChoiceSets) {
		this.walkChoiceSets = walkChoiceSets;
	}
	
	
	private void calculateAverageHHIncomeSize(int i) {
		double income = 0.0;
		int indexIncome = 0;
		
		double houseHoldSize = 0.0;
		int indexHouseHoldSize = 0;
		
		Iterator<ChoiceSet> choiceSet_it;
		if (i == 0) {
			choiceSet_it = this.carChoiceSets.iterator();
		}
		else {
			choiceSet_it = this.walkChoiceSets.iterator();
		}
		while (choiceSet_it.hasNext()) {
			ChoiceSet choiceSet = choiceSet_it.next();
			
			double csIncome = choiceSet.getPersonAttributes().getIncomeHH();
			if (csIncome > 0.0) {
				income += csIncome;
				indexIncome++;
			}
			double csHouseHoldSize = choiceSet.getPersonAttributes().getNumberOfPersonsHH();
			if (csHouseHoldSize > 0.0) {
				houseHoldSize += csHouseHoldSize;
				indexHouseHoldSize++;
			}
		}
		
		if (i == 0) {
			this.avgIncomeCarCS = income / indexIncome;
			this.avgHouseHoldSizeCarCS = houseHoldSize / indexHouseHoldSize;
		}
		else {
			this.avgIncomeWalkCS = income / indexIncome;
			this.avgHouseHoldSizeWalkCS = houseHoldSize / indexHouseHoldSize;
		}
		
	}
	
	private void calculateAttributeAverages() {	
		this.calculateAverageHHIncomeSize(0);
		this.calculateAverageHHIncomeSize(1);
	}
	
	private void calculateAdditionalTravelEffort() {
	
		Iterator<ChoiceSet>  choiceSet_it = this.carChoiceSets.iterator();
		while (choiceSet_it.hasNext()) {
			ChoiceSet choiceSet = choiceSet_it.next();
			choiceSet.calculateAdditonalTravelEffort();
		}
		
		choiceSet_it = this.walkChoiceSets.iterator();
		while (choiceSet_it.hasNext()) {
			ChoiceSet choiceSet = choiceSet_it.next();
			choiceSet.calculateAdditonalTravelEffort();
		}
	}
	
	public double getAvgIncomeCarCS() {
		return avgIncomeCarCS;
	}
	public void setAvgIncomeCarCS(double avgIncomeCarCS) {
		this.avgIncomeCarCS = avgIncomeCarCS;
	}
	public double getAvgHouseHoldSizeCarCS() {
		return avgHouseHoldSizeCarCS;
	}
	public void setAvgHouseHoldSizeCarCS(double avgHouseHoldSizeCarCS) {
		this.avgHouseHoldSizeCarCS = avgHouseHoldSizeCarCS;
	}
	public double getAvgIncomeWalkCS() {
		return avgIncomeWalkCS;
	}
	public void setAvgIncomeWalkCS(double avgIncomeWalkCS) {
		this.avgIncomeWalkCS = avgIncomeWalkCS;
	}
	public double getAvgHouseHoldSizeWalkCS() {
		return avgHouseHoldSizeWalkCS;
	}
	public void setAvgHouseHoldSizeWalkCS(double avgHouseHoldSizeWalkCS) {
		this.avgHouseHoldSizeWalkCS = avgHouseHoldSizeWalkCS;
	}
	
	public void finish() {
		this.calculateAdditionalTravelEffort();
		this.calculateAttributeAverages();
	}
}
