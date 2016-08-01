package net.onrc.openvirtex.elements.datapath;

import java.util.Arrays;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.onrc.openvirtex.messages.actions.OVXActionOutput;
import net.onrc.openvirtex.protocol.OVXMatch;

public class EntryPair {
	
	private static Logger log = LogManager.getLogger(EntryPair.class
            .getName());
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
		this.log.info("Compare DL_Dst : {}, {}",this.ovxmatch.getDataLayerDestination(),entity.ovxmatch.getDataLayerDestination());
		this.log.info("Compare DL_Srt : {}, {}", this.ovxmatch.getDataLayerSource(), entity.ovxmatch.getDataLayerSource());
		this.log.info("Compare output : {}, {}", this.ovxaction.getPort(),this.ovxaction.getPort());
		if(Arrays.equals(this.ovxmatch.getDataLayerDestination(), entity.ovxmatch.getDataLayerDestination())
				&& Arrays.equals(this.ovxmatch.getDataLayerSource(), entity.ovxmatch.getDataLayerSource())
				&& (this.ovxaction.getPort() == entity.ovxaction.getPort())){
			return true;
		}else
			return false;
		
	}
	
}
