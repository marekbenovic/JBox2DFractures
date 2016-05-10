package org.jbox2d.fracture.poly2Tri;

import java.util.Comparator;

public class PointbaseComparatorCoordinatesReverse implements Comparator {

	public int compare(Object o1, Object o2) {
		Pointbase pb1 = (Pointbase)o1;
		Pointbase pb2 = (Pointbase)o2;
		return (-pb1.compareTo(pb2));
	}

}
