package org.processmining.plugins.bpmn.exporting.metrics;

public class TaskConfMetrics {
	int unsoundExecutions; // Attivazione non previste del task
	int interruptedExecutions; // Il processo è fermo dopo il compltetamento del task
	int missingCompletitions; // L'attività non è terminata
	int internalFailure; // Visto il complete ma non lo start
	
	
	
	public TaskConfMetrics(int unsoundExecutions, int interruptedExecutions,
			int missingCompletitions, int internalFailure) {
		super();
		this.unsoundExecutions = unsoundExecutions;
		this.interruptedExecutions = interruptedExecutions;
		this.missingCompletitions = missingCompletitions;
		this.internalFailure = internalFailure;
	}
	
	public TaskConfMetrics() {
		
		this.unsoundExecutions = 0;
		this.interruptedExecutions = 0;
		this.missingCompletitions = 0;
		this.internalFailure = 0;
	}
	
	public int getUnsoundExecutions() {
		return unsoundExecutions;
	}
	public void setUnsoundExecutions(int unsoundExecutions) {
		this.unsoundExecutions = unsoundExecutions;
	}
	public int getInterruptedExecutions() {
		return interruptedExecutions;
	}
	public void setInterruptedExecutions(int interruptedExecutions) {
		this.interruptedExecutions = interruptedExecutions;
	}
	public int getMissingCompletitions() {
		return missingCompletitions;
	}
	public void setMissingCompletitions(int missingCompletitions) {
		this.missingCompletitions = missingCompletitions;
	}
	public int getInternalFailure() {
		return internalFailure;
	}
	public void setInternalFailure(int internalFailure) {
		this.internalFailure = internalFailure;
	}
	
	public void addUnsoundExecutions() {
		unsoundExecutions++;
	}
	public void addInterruptedExecutions() {
		interruptedExecutions++;
	}
	public void addMissingCompletitions() {
		 missingCompletitions++;
	}
	public void addInternalFailure() {
		 internalFailure++;
	}

	public void updateMetric(TaskConfMetrics metritask) {
		
		this.unsoundExecutions += metritask.getUnsoundExecutions();
		this.interruptedExecutions += metritask.getInterruptedExecutions();
		this.missingCompletitions += metritask.getMissingCompletitions();
		this.internalFailure += metritask.getInternalFailure();
	}

	public boolean isEmpty(){
		if(unsoundExecutions>0 || interruptedExecutions>0 ||
			internalFailure>0 || missingCompletitions>0){
			return false;
		}
		return true;
	}
	
	@Override
	public String toString() {
		return "TaskConfMetrics [unsoundExecutions=" + unsoundExecutions
				+ ", interruptedExecutions=" + interruptedExecutions
				+ ", missingCompletitions=" + missingCompletitions
				+ ", internalFailure=" + internalFailure + "]";
	}
	
	
	
}
