package org.processmining.plugins.bpmn.animation;

import org.processmining.models.graphbased.directed.petrinet.PetrinetEdge;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;

public class ArcTime {
	
	private PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> arc;
	
	private long startTime;
	private long endTime;
	
	public ArcTime(PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> a,long start,long end) {
		arc=a;
		startTime=start;
		endTime=end;
		
	}
	public ArcTime(PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> a,long start) {
		arc=a;
		startTime=start;		
	}
	
	
	public PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> getArc() {
		return arc;
	}

	public void setArc(PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> arc) {
		this.arc = arc;
	}

	public long getStartTime() {
		return startTime;
	}

	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}

	public long getEndTime() {
		return endTime;
	}

	public void setEndTime(long endTime) {
		this.endTime = endTime;
	}
	public void setEndDeltaTime(long delta) {
		this.endTime = this.startTime+delta;
	}
	public long delta(){
		return endTime-startTime;
	}

}
