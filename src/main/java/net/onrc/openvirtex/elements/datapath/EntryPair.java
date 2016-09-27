package net.onrc.openvirtex.elements.datapath;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.onrc.openvirtex.messages.actions.OVXActionOutput;
import net.onrc.openvirtex.protocol.OVXMatch;

//byyu
public class EntryPair {
	
	private Logger log = LogManager.getLogger(EntryPair.class
            .getName());
	private OVXMatch ovxmatch;
	private OVXActionOutput ovxaction;
	private int count;
	private List<Long> cookieSet = new ArrayList<Long>();
	
	public EntryPair(OVXMatch match,OVXActionOutput action, long cookie){
		this.ovxmatch = match;
		this.ovxaction = action;
		this.cookieSet.add(cookie);
		log.info("This cookie is : {} ", cookie);
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
	
	public void addCookie(long cookie){
		this.cookieSet.add(cookie);
	}
	
	public List<Long> getCookieSet(){
		return this.cookieSet;
	}
	
	public boolean equals(EntryPair entity){
		if(Arrays.equals(this.ovxmatch.getDataLayerDestination(), entity.ovxmatch.getDataLayerDestination())
				&& Arrays.equals(this.ovxmatch.getDataLayerSource(), entity.ovxmatch.getDataLayerSource())
				&& (this.ovxaction.getPort() == entity.ovxaction.getPort())){
			return true;
		}else
			return false;
	}
	
	public void tostring(){
		String ret = "";
		ret +="Cookie : ";
		for(Long c : cookieSet){
			ret += c + "\t";
		}
		ret += "MAC Source Address : "+ this.ovxmatch.getDataLayerSource().toString()
				+ "\nMAC Destination Addresse: "+this.ovxmatch.getDataLayerDestination().toString()
				+ "\nOutput port : " + this.ovxaction.getPort()
				+"\n";

        this.log.info("OVXFlowTable \n========================\n" + ret
                + "========================\n");
	}
	
}
