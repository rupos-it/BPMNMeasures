package org.processmining.plugins.bpmn.exporting.metrics;

public class TaskConfMetrics {
	int unsoundExecutions; // Attivazione non previste del task
	int interruptedBranchExecutions; // Il processo è fermo dopo il compltetamento del task
	int missingCompletitions; // L'attività non è terminata
	int internalFailure; // Visto il complete ma non lo start
	
	
	
	public TaskConfMetrics(int unsoundExecutions, int interruptedExecutions,
			int missingCompletitions, int internalFailure) {
		super();
		this.unsoundExecutions = unsoundExecutions;
		this.interruptedBranchExecutions = interruptedExecutions;
		this.missingCompletitions = missingCompletitions;
		this.internalFailure = internalFailure;
	}
	
	public TaskConfMetrics() {
		
		this.unsoundExecutions = 0;
		this.interruptedBranchExecutions = 0;
		this.missingCompletitions = 0;
		this.internalFailure = 0;
	}
	
	public int getUnsoundExecutions() {
		return unsoundExecutions;
	}
	public void setUnsoundExecutions(int unsoundExecutions) {
		this.unsoundExecutions = unsoundExecutions;
	}
	public int getInterruptedBranchExecutions() {
		return interruptedBranchExecutions;
	}
	public void setInterruptedExecutions(int interruptedExecutions) {
		this.interruptedBranchExecutions = interruptedExecutions;
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
	public void addInterruptedBranchExecutions() {
		interruptedBranchExecutions++;
	}
	public void addMissingCompletitions() {
		 missingCompletitions++;
	}
	public void addInternalFailure() {
		 internalFailure++;
	}

	public void updateMetric(TaskConfMetrics metritask) {
		
		this.unsoundExecutions += metritask.getUnsoundExecutions();
		this.interruptedBranchExecutions += metritask.getInterruptedBranchExecutions();
		this.missingCompletitions += metritask.getMissingCompletitions();
		this.internalFailure += metritask.getInternalFailure();
	}

	public boolean isEmpty(){
		if(unsoundExecutions>0 || interruptedBranchExecutions>0 ||
			internalFailure>0 || missingCompletitions>0){
			return false;
		}
		return true;
	}
	
	@Override
	public String toString() {
		return "Task_Conformance_Metrics [Unsound_Executions=" + unsoundExecutions
				+ ", Interrupted_Branch_Executions=" + interruptedBranchExecutions
				+ ", Missing_Completitions=" + missingCompletitions
				+ ", Internal_Failure=" + internalFailure + "]";
	}
	
	
	
}
