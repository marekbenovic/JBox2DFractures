package org.jbox2d.fracture.poly2Tri;

import org.jbox2d.fracture.poly2Tri.splayTree.BTreeNode;
import org.jbox2d.fracture.poly2Tri.splayTree.SplayTreeAction;

public class UpdateKey implements SplayTreeAction {

	public void action(BTreeNode node, double y) {
		((Linebase)node.data()).setKeyValue(y);
	}

}
