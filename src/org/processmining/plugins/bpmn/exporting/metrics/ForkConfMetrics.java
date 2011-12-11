package org.processmining.plugins.bpmn.exporting.metrics;

import java.util.LinkedHashMap;
import java.util.Map;

public class ForkConfMetrics {
	Map<String, Integer> interruptedBranches; // nodo uscente in cui si è fermata l'esecuzione
	//String rappresenta il nome dei nodi connessi dall'arco

	
	
	
	
	public ForkConfMetrics() {
		this.interruptedBranches = new LinkedHashMap<String, Integer>();
	}

	public Map<String, Integer> getInterruptedBranches() {
		return interruptedBranches;
	}

	public void setInterruptedBranches(Map<String, Integer> interruptedBranches) {
		this.interruptedBranches = interruptedBranches;
	}

	public void addInterruptedBranches(String key , Integer val) {
		this.interruptedBranches.put(key, val);
	}
	
	public String toString(){
		
		return "Interrupted_onFork_Branches"+ interruptedBranches.toString();
		
	}

}
