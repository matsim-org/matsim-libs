package playground.wrashid.lib.obj.list;

import java.util.ArrayList;
import java.util.LinkedList;

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

}
