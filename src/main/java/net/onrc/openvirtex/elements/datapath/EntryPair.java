package net.onrc.openvirtex.elements.datapath;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openflow.protocol.action.OFActionOutput;

import net.onrc.openvirtex.messages.actions.OVXActionOutput;
import net.onrc.openvirtex.protocol.OVXMatch;
import net.onrc.openvirtex.util.MACAddress;

//byyu
public class EntryPair {
	
	private Logger log = LogManager.getLogger(EntryPair.class
            .getName());
	private OVXMatch ovxmatch;
	private OFActionOutput ofaction;
	private List<Long> cookieSet = new ArrayList<Long>();
	
	public EntryPair(OVXMatch match,OFActionOutput action, long cookie){
		this.ovxmatch = match;
		this.ofaction = action;
		this.cookieSet.add(cookie);
		log.info("This cookie is : {} ", cookie);
	}
	
	public OVXMatch getMatch(){
		return this.ovxmatch;
	}
	
	public OFActionOutput getAction(){
		return this.ofaction;
	}

	
	public void addCookie(long cookie){
		this.cookieSet.add(cookie);
	}
	
	public List<Long> getCookieSet(){
		return this.cookieSet;
	}
	
	public boolean equals(EntryPair entity){
		if(Arrays.equals(this.ovxmatch.getDataLayerDestination(), entity.ovxmatch.getDataLayerDestination())
				&& (this.ovxmatch.getInputPort() == entity.ovxmatch.getInputPort())
				&& (this.ofaction.getPort() == entity.ofaction.getPort())){
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
		ret += "\nInport : "+ this.ovxmatch.getInputPort()
				+ "\nMAC Destination Addresse: "+ MACAddress.valueOf(this.ovxmatch.getDataLayerDestination()).toString()
				+ "\nOutput port : " + this.ofaction.getPort()
				+"\n";

        this.log.info("Entry \n========================\n" + ret
                + "========================\n");
	}
	
}
