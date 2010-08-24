package playground.wrashid.lib;

import java.util.LinkedList;
import java.util.PriorityQueue;

/**
 * The idea is, that you define intervals, where the function is defined.
 * Everywhere else the function is undefined and throws an error.
 * 
 * Each part of the function is defined by two points, between which a linear
 * interpolation is done.
 * 
 * @author wrashid
 * 
 */
public class LinearFunction {

	private PriorityQueue<LinearFunctionPart> linearFunctionParts = new PriorityQueue<LinearFunctionPart>();

	/**
	 * 
	 * attention: add in sequence from "left" to "right" the function parts,
	 * else an exception will occur while adding the function part
	 * 
	 * precondition: make sure, that the function fits together with the other
	 * parts of the existing function!!!
	 * 
	 * e.g. if we are inserting (startCoord.x=1.0,startCoord.y=5.0), then the next function part must have
	 * endCoord.x=1.0
	 * 
	 * 
	 * 
	 * if these preconditions are not met, an runtime exception is thrown.
	 * 
	 * @param lfp
	 */
	public void defineFunctionPart(LinearFunctionPart lfp) {

		// just add the new function
		linearFunctionParts.add(lfp);

		// perform checks, if the conditions were violated
		LinkedList<LinearFunctionPart> list = new LinkedList<LinearFunctionPart>();

		LinearFunctionPart currentFuncPart = linearFunctionParts.poll();

		while (linearFunctionParts.peek() != null) {
			// compare the ending of the current and the starting of the next
			// part
			if (currentFuncPart.endCoord.getX() != linearFunctionParts.peek().startCoord.getX()) {
				throw new RuntimeException("currentFuncPart.endCoord.getX()!=linearFunctionParts.peek().startCoord.getX()");
			}

			if (currentFuncPart.endCoord.getY() != linearFunctionParts.peek().startCoord.getY()) {
				// it is not required, that the next function part continues with the same y-value
				
				//throw new RuntimeException("currentFuncPart.endCoord.getY()!=linearFunctionParts.peek().startCoord.getY()");
			}

			list.add(currentFuncPart);

			currentFuncPart = linearFunctionParts.poll();
		}

		// put back the removed elements
		linearFunctionParts.add(currentFuncPart);
		linearFunctionParts.addAll(list);
	}

	/**
	 * 
	 * precondition: make sure, function is defined for the x-value, else run
	 * time exception will occur (thrown by linearFunctionParts). not suitable
	 * for concurrent access!
	 * 
	 * 
	 * Consecutive functions are defined for [x1,x2) and [x2,x3) (endCoord x-value is not inclusive)
	 * 
	 * @param x
	 * @return
	 */
	public double getYValue(double x) {
		LinkedList<LinearFunctionPart> list = new LinkedList<LinearFunctionPart>();

		if (linearFunctionParts.peek().startCoord.getX() > x) {
			throw new RuntimeException("x is outside of range");
		}

		// this will cause a null pointer exception, if x is outside of the
		// range
		while (linearFunctionParts.peek() != null && linearFunctionParts.peek().endCoord.getX() <= x) {
			list.add(linearFunctionParts.poll());
		}

		if (linearFunctionParts.peek().startCoord.getX() > x) {
			throw new RuntimeException("x is outside of range");
		}

		double y = linearFunctionParts.peek().getYValue(x);

		// put back the removed elements
		linearFunctionParts.addAll(list);

		return y;
	}

}
