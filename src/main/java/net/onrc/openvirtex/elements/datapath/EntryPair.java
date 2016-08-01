package net.onrc.openvirtex.elements.datapath;

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
	
}
