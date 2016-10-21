package net.onrc.openvirtex.elements.sla;

import org.openflow.protocol.OFMatch;
import org.openflow.protocol.Wildcards.Flag;

public class SLAManager {
	final public static int Hop_no_isolation = 0;
	final public static int Hop_isolation = 1;
	final public static int Host_isolation = 2;
	final public static int Service_isolation = 3;
	final public static int isolation = 4;
	
	
	public void SLArewriteMatch(OFMatch ofmatch, int sla_level){
		switch(sla_level){
			case Hop_no_isolation :
		        ofmatch.setWildcards((~OFMatch.OFPFW_IN_PORT) & (~OFMatch.OFPFW_DL_DST));
		        return ;
		        
			case Hop_isolation :
					ofmatch.setWildcards(OFMatch.OFPFW_ALL & (~OFMatch.OFPFW_DL_DST) & (~OFMatch.OFPFW_IN_PORT) & (~OFMatch.OFPFW_DL_SRC));
				return ;
				
			case Host_isolation :
				ofmatch.setWildcards(OFMatch.OFPFW_ALL & (~OFMatch.OFPFW_DL_DST) & (~OFMatch.OFPFW_IN_PORT) 
						& (~OFMatch.OFPFW_DL_SRC) & (~OFMatch.OFPFW_NW_DST_MASK)
						& (~OFMatch.OFPFW_NW_SRC_MASK) & (~OFMatch.OFPFW_DL_TYPE));
				
				return;
			case Service_isolation :
				ofmatch.setWildcards(OFMatch.OFPFW_ALL & (~OFMatch.OFPFW_DL_DST) & (~OFMatch.OFPFW_IN_PORT) 
						&(~OFMatch.OFPFW_DL_SRC) & (~OFMatch.OFPFW_NW_PROTO) & (~OFMatch.OFPFW_DL_TYPE)
						& (~OFMatch.OFPFW_NW_DST_MASK) & (~OFMatch.OFPFW_NW_SRC_MASK) & (~OFMatch.OFPFW_TP_DST)
						& (~OFMatch.OFPFW_TP_SRC));
				return;
			case isolation :
				if(ofmatch.getWildcardObj().isWildcarded(Flag.DL_SRC)){
	    			ofmatch.setWildcards(ofmatch.getWildcards() & (~OFMatch.OFPFW_DL_SRC));
	    		}
	    		if(ofmatch.getWildcardObj().isWildcarded(Flag.IN_PORT)){
	    			ofmatch.setWildcards(ofmatch.getWildcards() & (~OFMatch.OFPFW_IN_PORT));
	    		}
	    		return ;
			
		}
			
	}
	
	public short SLAresettingPriority(short priority, int sla_level){
		switch(sla_level){
		case Hop_no_isolation :
			return priority;
		case Hop_isolation :
			return priority;
		case Host_isolation :
			priority = (short) (priority+2);
			return priority;
		case Service_isolation :
			priority = (short) (priority+5);
			return priority;
		case isolation :
			priority = (short) (priority+1);
			return priority;
		}
		return priority;
	}
}
