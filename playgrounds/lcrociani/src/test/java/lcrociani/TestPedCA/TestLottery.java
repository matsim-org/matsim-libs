package lcrociani.TestPedCA;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Random;

import org.junit.Test;

import pedCA.environment.grid.GridPoint;
import pedCA.environment.grid.WeightedCell;
import pedCA.utility.Lottery;

public class TestLottery {
	
	@Test
	public void testLotteryMultiple(){
		int nTests = 100;
		for (int i=0;i<nTests;i++)
			testLottery();
	}
	
	@Test
	public void testLottery(){
		ArrayList<WeightedCell> pValues = randomDistribution(5);
		//System.out.println(pValues);
		
		int extractions = 10000;
		Couple average = average(pValues);		
		Couple sum=new Couple(0.,0.);
		for (int i=0;i<extractions;i++){
			WeightedCell extracted = Lottery.pickWinner(pValues);
			sum.x += extracted.x;
			sum.y += extracted.y;		
		}
		sum.x/=extractions;
		sum.y/=extractions;
		//System.out.println(sum + "  " + average);
		boolean test=verifyEquality(sum,average);		
		assertEquals(test,true);
	}
	
	private boolean verifyEquality(Couple sum, Couple expectedResult) {
		boolean result = false;
		double acceptedError = 5.;
		if (Math.abs(sum.x-expectedResult.x)<=acceptedError && Math.abs(sum.y-expectedResult.y)<=acceptedError)
			result = true;
		return result;
	}

	private Couple average(ArrayList<WeightedCell> pValues) {
		Couple result = new Couple(0, 0);
		for (int i=0;i<pValues.size();i++){
			result.x += pValues.get(i).x * pValues.get(i).p;
			result.y += pValues.get(i).y * pValues.get(i).p;
		}
		return result;
	}

	public ArrayList<WeightedCell> homogeneousDistribution(int size){
		ArrayList<WeightedCell> pValues = new ArrayList<WeightedCell>();
		Random r = new Random();
		for (int i=0; i<size;i++)
			pValues.add(new WeightedCell(new GridPoint(r.nextInt(100),r.nextInt(100)), 1./size));
		return pValues;
	}
	
	public ArrayList<WeightedCell> randomDistribution(int size){
		int weigthRange = 100;
		ArrayList<WeightedCell> pValues = new ArrayList<WeightedCell>();
		Random r = new Random();
		double weightSum = 0.;
		for (int i=0; i<size;i++){
			double weigth = r.nextDouble()*weigthRange;
			pValues.add(new WeightedCell(new GridPoint(r.nextInt(100),r.nextInt(100)), weigth));
			weightSum+=weigth;
		}
		for (int i=0; i<size;i++)
			pValues.get(i).p /= weightSum;  
		return pValues;
	}
}

class Couple{
	double x=0.;
	double y=0.;
	
	public Couple(double x, double y){
		this.x = x; this.y = y;
	}
	
	public String toString(){
		return "("+x+","+y+")";
	}
}
