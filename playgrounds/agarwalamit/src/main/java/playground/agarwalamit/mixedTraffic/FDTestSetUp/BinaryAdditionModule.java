package playground.agarwalamit.mixedTraffic.FDTestSetUp;

import java.util.List;

/**
 * @author ssix
 *
 */
public class BinaryAdditionModule {

	private final List<Integer> maxValues;
	private final List<Integer> steps;
	private final Integer[] point;
	
	public BinaryAdditionModule(final List<Integer> maxValues, final List<Integer> steps, final Integer[] point){
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
				Integer newIndexValue = Integer.valueOf(point[index].intValue() + this.steps.get(index).intValue());
				point[index] = newIndexValue;
			} else {
				point[index] = Integer.valueOf(0);
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
