package playground.wrashid.lib.obj.list;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Random;

public class Lists {

	public static ArrayList linkedListToArrayList(LinkedList linkedList) {
		ArrayList arrayList = new ArrayList();

		arrayList.addAll(linkedList);

		return arrayList;
	}
	
	public static double[] getArray(Collection<Double> collection){
		double[] array=new double[collection.size()];
		
		int i=0;
		for (Double d:collection){
			array[i]=d;
			i++;
		}
		
		return array;
	}
	
	public static double getSum(Collection<Double> collection){
		int sum=0;
		
		for (Double d:collection){
			sum+=d;
		}
		
		return sum;
	}

	public static LinkedList arrayListToLinkedList(ArrayList arrayList) {
		LinkedList linkedList = new LinkedList();

		linkedList.addAll(arrayList);

		return linkedList;
	}
	
	public static char[] getCharsOfAllArrayItemsWithNewLineCharacterInbetween(ArrayList<String> list){
		char[] result;
		
		int totalNumberOfChars=0;
		
		for (int i=0;i<list.size();i++){
			totalNumberOfChars+=list.get(i).length();
			totalNumberOfChars++;
			// one extra char is added for the new line character
		}
		
		result=new char[totalNumberOfChars];
		
		int j=0;
		for (int i=0;i<list.size();i++){
			for (int k=0;k<list.get(i).length();k++){
				result[j]=list.get(i).charAt(k);
				j++;
			}
			
			// add new line character
			result[j]='\n';
			j++;
		}
		
		return result;
	}

	public static LinkedList randomizeObjectSequence(LinkedList list){
		LinkedList tmpList=new LinkedList();
		LinkedList randomizedList=new LinkedList();
		Random rand=new Random();
		
		tmpList.addAll(list);
		
		while (tmpList.size()>0){
			int index = rand.nextInt(tmpList.size());
			randomizedList.add(tmpList.get(index));
			tmpList.remove(index);
		}
		
		return randomizedList;
	}
	
}
