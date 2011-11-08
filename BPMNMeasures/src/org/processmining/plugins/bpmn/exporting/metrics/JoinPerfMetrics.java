package org.processmining.plugins.bpmn.exporting.metrics;

import java.util.LinkedHashMap;
import java.util.Map;

public class JoinPerfMetrics {

	Map<String, Float> synchronizationTime; //  NomeNodoEntrante => Tempo di sincronizzazione
	//String rappresenta il nome dei nodi connessi dall'arco

	
	public JoinPerfMetrics() {
		
		this.synchronizationTime =  new LinkedHashMap<String, Float>();
	}

	public Map<String, Float> getSynchronizationTime() {
		return synchronizationTime;
	}

	public void setSynchronizationTime(Map<String, Float> synchronizationTime) {
		this.synchronizationTime = synchronizationTime;
	}

	public void setSynchronizationTime(String key, Float synchronizationTime) {
		this.synchronizationTime.put(key, synchronizationTime);
	}

	@Override
	public String toString() {
		return "JoinPerfMetrics [synchronizationTime=" + synchronizationTime
				+ "]";
	}

	

}
