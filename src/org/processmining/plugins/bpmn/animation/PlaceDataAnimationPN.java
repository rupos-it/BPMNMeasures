package org.processmining.plugins.bpmn.animation;

import java.util.Collection;

import java.util.Vector;

import org.processmining.models.graphbased.directed.petrinet.elements.Arc;


public class PlaceDataAnimationPN implements Cloneable{


	private long placetime;
	private Vector<Arc> placecollarc;
	
	public PlaceDataAnimationPN(long time){
		this.placetime=time;
		placecollarc=new Vector<Arc>();
	}
	
	public PlaceDataAnimationPN(long time,Vector<Arc> collarc){
		this.placetime=time;
		this.placecollarc=collarc;
	}
	
	
	
	public long getPlacetime() {
		return placetime;
	}
	public void setPlacetime(long placetime) {
		this.placetime = placetime;
	}
	public Collection<Arc> getPlacecollarc() {
		return placecollarc;
	}
	public void setPlacecollarc(Vector<Arc> placecollarc) {
		this.placecollarc = placecollarc;
	}
	
	
	public void add(Arc arc) {
		this.placecollarc.add(arc);
	}
	
	public PlaceDataAnimationPN clone(){
		
		Vector<Arc> newvector = new Vector<Arc>();
		newvector = (Vector<Arc>) this.placecollarc.clone();
		
		return new PlaceDataAnimationPN(this.placetime,newvector) ;
	}
	
}
