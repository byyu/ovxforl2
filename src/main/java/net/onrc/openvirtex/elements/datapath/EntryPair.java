package net.onrc.openvirtex.elements.datapath;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openflow.protocol.action.OFActionOutput;
import net.onrc.openvirtex.protocol.OVXMatch;
import net.onrc.openvirtex.util.MACAddress;

/**
 * This class is to store physical rule set aggregated rule.
 * Rule is matched by Match and Output Number And stored by cookie number.
 */
public class EntryPair {
	
	private Logger log = LogManager.getLogger(EntryPair.class
            .getName());
	
	/* Entry represented by Match and outaction */
	private OVXMatch ovxmatch;
	private OFActionOutput ofaction;
	
	/* Store all cookie that have same match and outaction */
	private List<Long> cookieSet = new ArrayList<Long>();
	
    /**
     * Instantiates a new EntryPair
     *
     * @param match the OVXMatch
     * @param action the output action
     * @param cookie the cookie
     */
	public EntryPair(OVXMatch match,OFActionOutput action, long cookie){
		this.ovxmatch = match;
		this.ofaction = action;
		this.cookieSet.add(cookie);
		log.info("This cookie is : {} ", cookie);
	}
	
	/**
	 * Get the OVXMatch
	 * 
	 * @return the ovxmatch
	 */
	public OVXMatch getMatch(){
		return this.ovxmatch;
	}
	
	/**
	 * Get the output action
	 * 
	 * @return the output action
	 */
	public OFActionOutput getAction(){
		return this.ofaction;
	}

	/**
	 * Add cookie to cookie set
	 * 
	 * @param cookie
	 */
	public void addCookie(long cookie){
		this.cookieSet.add(cookie);
	}
	
	/**
	 * Get the cookie set
	 * 
	 * @return the cookie set
	 */
	public List<Long> getCookieSet(){
		return this.cookieSet;
	}
	
	/**
	 * Check entry to this entry
	 * Compare with ovxmatch and outport of output action
	 * 
	 * @param entity
	 * @return if same condition, return true. otherwise return false.
	 */
	public boolean equals(EntryPair entity){
		
		if(entity == null)
			return false;
		
		if(Arrays.equals(this.ovxmatch.getDataLayerDestination(), entity.ovxmatch.getDataLayerDestination())
				&& (this.ovxmatch.getInputPort() == entity.ovxmatch.getInputPort())
				&& (this.ofaction.getPort() == entity.ofaction.getPort())){
			return true;
		}
		return false;
	}
	

	/*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
	@Override
	public String toString() {
		String ret = "";
		ret +="Cookie : ";
		for(Long c : cookieSet){
			ret += c + "\t";
		}
		ret += "\nInport : "+ this.ovxmatch.getInputPort()
				+ "\nMAC Destination Addresse: "+ MACAddress.valueOf(this.ovxmatch.getDataLayerDestination()).toString()
				+ "\nOutput port : " + this.ofaction.getPort()
				+"\n";

        ret += "Entry \n========================\n" + ret
                + "========================\n";
        
        return ret;
	}
	
}
