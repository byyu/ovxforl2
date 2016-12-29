package net.onrc.openvirtex.elements.datapath;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.Wildcards.Flag;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionOutput;
import org.openflow.protocol.action.OFActionType;

import net.onrc.openvirtex.messages.OVXFlowMod;
import net.onrc.openvirtex.protocol.OVXMatch;

/**
 * 
 * 
 * @author byyu
 *
 */
//byyu
public class PhysicalFlowEntry {
	
	private static Logger log = LogManager.getLogger(PhysicalFlowEntry.class.getName());
	private Set<EntryPair> entry = new HashSet<EntryPair>();
	private PhysicalSwitch physw;
	
	public PhysicalFlowEntry(PhysicalSwitch sw){
		this.physw = sw;
	}

	private void addEntry(OVXMatch match, OFActionOutput action){
		EntryPair entity = new EntryPair(match, action, match.getCookie());
		entry.add(entity);
	}
	
	private void addEntry(OVXFlowMod fm, OVXMatch match, OFActionOutput action){
		short prio = fm.getPriority();
		fm.setPriority(++prio);
		match.setWildcards((OFMatch.OFPFW_ALL) & (~OFMatch.OFPFW_IN_PORT)
								& (~OFMatch.OFPFW_DL_SRC)
								& (~OFMatch.OFPFW_DL_DST)
								& (~OFMatch.OFPFW_DL_TYPE)
								& (~OFMatch.OFPFW_NW_DST_MASK)
								& (~OFMatch.OFPFW_NW_SRC_MASK));
		fm.setMatch(match);
		addEntry(match, action);
	}
	
	public List<Long> removeEntry(OVXMatch match, OFActionOutput action, long cookie){
		EntryPair newEntity = new EntryPair(match, action, cookie);
		for(EntryPair entity : entry){
			if(entity.equals(newEntity)){
				List<Long> cookieList = entity.getCookieSet();
				entry.remove(entity);
				return cookieList;
			}
		}
		return null;
	}

	public boolean checkduplicate(OVXFlowMod fm){
		log.info("Start checking duplicate at {}", this.physw.toString());
		OVXMatch match = new OVXMatch(fm.getMatch());
		OFActionOutput outaction = null;
		match.setCookie(fm.getCookie());
		OVXMatch oldMatch;

		short oldoutport, outport=0;
		
		for(OFAction action : fm.getActions()){
			if(action.getType()==OFActionType.OUTPUT){
	    		outaction = (OFActionOutput) action;
	    		outport = outaction.getPort();
			}
		}
		
		if(outport==0){
			return false;
		}
		
		for(EntryPair entity : entry){
			oldMatch = entity.getMatch();
			oldoutport = entity.getAction().getPort();
			if(oldMatch.getInputPort() == match.getInputPort()){
				if(outport == oldoutport){
					if(oldMatch.getWildcards() == match.getWildcards()){
						if(!oldMatch.getWildcardObj().isWildcarded(Flag.NW_DST)){
							if(oldMatch.getNetworkDestination() == match.getNetworkDestination()){
								entity.addCookie(match.getCookie());
								return true;
							}
						}else{
							entity.addCookie(match.getCookie());
							return true;
						}
					}
				}else{
					if(oldMatch.getWildcards() == match.getWildcards()){
						addEntry(fm, match, outaction);
					}
				}
			}
		}
		
		addEntry(match, outaction);
		return false;
	}
}

