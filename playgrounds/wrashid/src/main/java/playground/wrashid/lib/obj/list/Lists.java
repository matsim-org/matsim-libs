package playground.wrashid.lib.obj.list;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Arrays;

public class Lists {

	public static ArrayList linkedListToArrayList(LinkedList linkedList) {
		ArrayList arrayList = new ArrayList();

		arrayList.addAll(linkedList);

		return arrayList;
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

}
