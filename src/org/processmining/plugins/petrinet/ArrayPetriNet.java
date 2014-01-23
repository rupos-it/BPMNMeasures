package org.processmining.plugins.petrinet;

import java.util.ArrayList;
import java.util.List;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.semantics.petrinet.Marking;


public class ArrayPetriNet {
	List<Petrinet> listpn = new ArrayList<Petrinet>();
	List<Marking> listm = new ArrayList<Marking>();
	public List<Petrinet> getListpn() {
		return listpn;
	}

	public void setElementListpn(Petrinet pn, Marking m) {
		this.listpn.add(pn);
		this.listm.add(m);
	}

	public void setElementListpn(PetrinetGraph net, Marking m) {
		this.listpn.add((Petrinet) net);
		this.listm.add(m);
		
	}
	
	public int size() {
		return listpn.size();
	}

	public Petrinet getp(int index) {
		return listpn.get(index);
	}

	public Marking getm(int index) {
		return listm.get(index);
	}
	
	
}
