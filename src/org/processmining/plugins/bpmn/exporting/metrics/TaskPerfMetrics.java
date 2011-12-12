package org.processmining.plugins.bpmn.exporting.metrics;

public class TaskPerfMetrics {
	float activationTime; // Tempo trascorso tra l'abilitazione del task ed il suo inizio
	float executionTime; // Tempo di esecuzione
	
	
	
	public TaskPerfMetrics(float activationTime, float executionTime) {
		
		this.activationTime = activationTime;
		this.executionTime = executionTime;
	}
	
	public TaskPerfMetrics() {
		this.activationTime = 0;
		this.executionTime = 0;
	}
	
	public float getActivationTime() {
		return activationTime;
	}
	public void setActivationTime(float activationTime) {
		this.activationTime = activationTime;
	}
	public float getExecutionTime() {
		return executionTime;
	}
	public void setExecutionTime(float executionTime) {
		this.executionTime = executionTime;
	}

	
	public String toString() {
		return "Task_Perferformace_Metrics [activation_Time=" + activationTime
				+ ", execution_Time=" + executionTime + "]";
	}

	public boolean isEmpty(){
		if(executionTime>0 || activationTime>0){
			return false;
		}
		return true;
	}
	public void updateMetric(TaskPerfMetrics taskmetric) {
		this.activationTime += taskmetric.getActivationTime();
		this.executionTime += taskmetric.getExecutionTime();
		
	}
	
	
}
