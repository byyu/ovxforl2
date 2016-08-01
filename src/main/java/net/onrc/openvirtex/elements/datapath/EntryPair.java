package net.onrc.openvirtex.elements.datapath;

import java.util.Arrays;

import net.onrc.openvirtex.messages.actions.OVXActionOutput;
import net.onrc.openvirtex.protocol.OVXMatch;

public class EntryPair {
	
	private OVXMatch ovxmatch;
	private OVXActionOutput ovxaction;
	private int count;
	
	public EntryPair(OVXMatch match,OVXActionOutput action){
		this.ovxmatch = match;
		this.ovxaction = action;
		count=1;
	}
	
	public OVXMatch getMatch(){
		return this.ovxmatch;
	}
	
	public OVXActionOutput getAction(){
		return this.ovxaction;
	}
	
	public void incCount(){
		count++;
	}
	
	public void decCount(){
		count--;
	}
	
	public int getCount(){
		return count;
	}
	
	public boolean equals(EntryPair entity){
		if(Arrays.equals(this.ovxmatch.getDataLayerDestination(), entity.ovxmatch.getDataLayerDestination())
				&& Arrays.equals(this.ovxmatch.getDataLayerSource(), entity.ovxmatch.getDataLayerSource())
				&& this.ovxaction.equals(entity.getAction())){
			return true;
		}else
			return false;
		
	}
	
}
