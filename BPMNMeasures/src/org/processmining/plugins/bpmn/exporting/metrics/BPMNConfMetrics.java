package org.processmining.plugins.bpmn.exporting.metrics;

import java.util.LinkedHashMap;
import java.util.Map;

import org.processmining.models.graphbased.directed.bpmn.elements.Activity;

public class BPMNConfMetrics {
	Map<String, TaskConfMetrics> taskMetrics;
	Map<String, ForkConfMetrics> forkMetrics;
	String TraceName=null;

	
	public BPMNConfMetrics() {
		taskMetrics = new LinkedHashMap<String, TaskConfMetrics>();
		forkMetrics = new LinkedHashMap<String, ForkConfMetrics>();
	}

	
	public String getTraceName() {
		return TraceName;
	}


	public void setTraceName(String traceName) {
		TraceName = traceName;
	}


	public Map<String, TaskConfMetrics> getTaskMetrics() {
		return taskMetrics;
	}


	public void setTaskMetrics(Map<String, TaskConfMetrics> taskMetrics) {
		this.taskMetrics = taskMetrics;
	}
	
	public void addTaskMetrics(String key, TaskConfMetrics taskMetrics) {
		this.taskMetrics.put(key, taskMetrics);
	}


	public Map<String, ForkConfMetrics> getForkMetrics() {
		return forkMetrics;
	}


	public void setForkMetrics(Map<String, ForkConfMetrics> forkMetrics) {
		this.forkMetrics = forkMetrics;
	}
	
	public void addForkMetrics(String key , ForkConfMetrics  forkMetrics) {
		this.forkMetrics.put(key, forkMetrics);
	}
	
	public String toString(){
		String task = taskMetrics.toString();
		String fork = forkMetrics.toString();
		return "NomeTraccia "+TraceName+"\n"+task+"\n"+fork+"\n\n";
		
	}
	

}
