package playground.agarwalamit.mixedTraffic.FDTestSetUp;

import java.util.List;

public class BinaryAdditionModule {

	private List<Integer> maxValues;
	private List<Integer> steps;
	private Integer[] point;
	
	public BinaryAdditionModule(List<Integer> maxValues, List<Integer> steps, Integer[] point){
		this.maxValues = maxValues;
		this.steps = steps;
		this.point = point;
	}
	
	public boolean furtherAdditionPossible() {
		for (int i=0; i<point.length; i++){
			if ( (point[i].intValue()+this.steps.get(i).intValue()) <= this.maxValues.get(i).intValue() ){
				return true;
			}
		}
		return false;
	}
	
	public void add1(){
		add1To(point, point.length-1);
	}
	
	public void add1To(Integer[] point, int index){
		if (furtherAdditionPossible()){
			if ( ! ((point[index].intValue()+this.steps.get(index).intValue()) > this.maxValues.get(index).intValue())){
				Integer newIndexValue = new Integer(point[index].intValue() + this.steps.get(index).intValue());
				point[index] = newIndexValue;
			} else {
				point[index] = new Integer(0);
				add1To(point, index-1);
			}
		} else {
			GenerateFundamentalDiagramData.LOG.info("Already tried too many combinations!!!");
			return;
		}
	}

	public Integer[] getPoint() {
		return point;
	}
	
 }
