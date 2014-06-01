package playground.vbmh.util;

import java.util.LinkedList;

public class RemoveDuplicate {
	public static void RemoveDuplicate (LinkedList list){
		LinkedList emptyList = new LinkedList();
		

		for (Object element : list){
			if(!emptyList.contains(element)){
				emptyList.add(element);
			}else{
				//System.out.println("Duplikat geloescht");
			}
		}
		
		list.clear();
		list.addAll(emptyList);
		
		
	}
}
