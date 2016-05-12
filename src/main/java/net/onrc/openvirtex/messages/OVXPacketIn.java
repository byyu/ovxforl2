/*******************************************************************************
 * Copyright 2014 Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package net.onrc.openvirtex.messages;

import java.util.Arrays;
import java.util.LinkedList;

import net.onrc.openvirtex.core.OpenVirteXController;
import net.onrc.openvirtex.elements.Mappable;
import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.address.IPMapper;
import net.onrc.openvirtex.elements.address.PhysicalIPAddress;
import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.elements.link.OVXLinkUtils;
import net.onrc.openvirtex.elements.link.OVXLink;
import net.onrc.openvirtex.elements.link.OVXLinkField;
import net.onrc.openvirtex.elements.port.OVXPort;
import net.onrc.openvirtex.elements.port.PhysicalPort;
import net.onrc.openvirtex.exceptions.AddressMappingException;
import net.onrc.openvirtex.exceptions.DroppedMessageException;
import net.onrc.openvirtex.exceptions.IndexOutOfBoundException;
import net.onrc.openvirtex.exceptions.NetworkMappingException;
import net.onrc.openvirtex.exceptions.SwitchMappingException;
import net.onrc.openvirtex.packet.ARP;
import net.onrc.openvirtex.packet.Ethernet;
import net.onrc.openvirtex.packet.IPv4;
import net.onrc.openvirtex.util.MACAddress;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFPacketIn;
import org.openflow.protocol.OFPacketOut;
import org.openflow.protocol.Wildcards.Flag;
import org.openflow.util.U16;

public class OVXPacketIn extends OFPacketIn implements Virtualizable {

    private final Logger log = LogManager
            .getLogger(OVXPacketIn.class.getName());
    private PhysicalPort port = null;
    private OVXPort ovxPort = null;
    private Integer tenantId = null;

    @Override
    public void virtualize(final PhysicalSwitch sw) {
    	this.log.info("Start packetIn virtualization");
    	
        OVXSwitch vSwitch = OVXMessageUtil.untranslateXid(this, sw);
        /*
         * Fetching port from the physical switch
         */
        short inport = this.getInPort();  //패킷으로부터 포트번호를 받아온다.
        port = sw.getPort(inport);			//패킷의 포트번호로 스위치의 포트를 찾는다.
        Mappable map = sw.getMap();		//맵 클래스를 받는다.
        
        final OFMatch match = new OFMatch();
        match.loadFromPacket(this.getPacketData(), inport);		//팻킷의 맷치를 받아 온다.
        this.log.info("srcMAC and destMAC - scr : {}, dst : {}", match.getDataLayerSource(),match.getDataLayerDestination());
        /*
         * Check whether this packet arrived on an edge port.
         *
         * if it did we do not need to rewrite anything, but just find which
         * controller this should be send to.
         */
        if (this.port.isEdge()) {														//팻킷의 포트가 엣지 포트일때
            this.tenantId = this.fetchTenantId(match, map, true);						//테넌트 아이디를 받아온다. - 데스티네이션 맥주소를 통해 받아온다.
            if (this.tenantId == null) {												//테넌트 아이디가 없으면 패킷을 드랍한다.
                this.log.warn(
                        "PacketIn {} does not belong to any virtual network; "
                                + "dropping and installing a temporary drop rule",
                        this);
                this.installDropRule(sw, match);
                return;
            }

            /*
             * Checks on vSwitch and the virtual port done in swndPkt.
             */
            vSwitch = this.fetchOVXSwitch(sw, vSwitch, map);							//해당 테넌트의 버츄얼스위치를 받아온다.
            this.ovxPort = this.port.getOVXPort(this.tenantId, 0);						//해당 테넌트의 버츄얼포트를 받아온다.
            this.sendPkt(vSwitch, match, sw);											//버츄얼스위치로 패킷을 보낸다.
            this.learnHostIP(match, map);												//아이피주소를 맵에 저장한다.
            this.learnAddresses(match, map);											//맥주소를 맵에 저장한다.
            this.log.info("Edge PacketIn {} sent to virtual network {}", this,
                    this.tenantId);
            return;
        }

        /*
         * Below handles packets traveling in the core.
         *
         *
         * The idea here si to rewrite the packets such that the controller is
         * able to recognize them.
         *
         * For IPv4 packets and ARP packets this means rewriting the IP fields
         * and possibly the mac address fields if these packets are at the
         * egress point of a virtual link.
         */

        if (match.getDataLayerType() == Ethernet.TYPE_IPV4					//코어에서 
                || match.getDataLayerType() == Ethernet.TYPE_ARP) {
            PhysicalIPAddress srcIP = new PhysicalIPAddress(
                    match.getNetworkSource());
            PhysicalIPAddress dstIP = new PhysicalIPAddress(
                    match.getNetworkDestination());

            Ethernet eth = new Ethernet();
            eth.deserialize(this.getPacketData(), 0,
                    this.getPacketData().length);
           this.log.info("Ethernet SrcMAC : {} \n DstMAC : {}",eth.getSourceMAC().toString(), eth.getDestinationMAC().toString());
         //byyu
           Integer flowId = null;
           try {
           	//tenantId = this.fetchTenantId(match, map, true);
        	   long linkId = MACAddress.valueOf(match.getDataLayerDestination()).toLong()-MACAddress.valueOf(match.getDataLayerSource()).toLong();
        	   tenantId = OVXMap.getInstance().gettenantIdbyLinkId(linkId);
           	if(tenantId!=null)
           		flowId = map.getVirtualNetwork(tenantId).getFlowManager().getFlowId(match.getDataLayerSource(), match.getDataLayerDestination());

           } catch (NetworkMappingException | DroppedMessageException e1) {
				this.log.error("We can't find network or other error");
				//e1.printStackTrace();
				return ;
			}catch(NullPointerException e2){
				//e2.printStackTrace();
			};

			OVXLinkUtils lUtils = new OVXLinkUtils(tenantId, flowId, eth.getSourceMAC(), eth.getDestinationMAC());
//           OVXLinkUtils lUtils = new OVXLinkUtils(eth.getSourceMAC(),
//                   eth.getDestinationMAC());
			
            // rewrite the OFMatch with the values of the link
            if (lUtils.isValid()) {
            	
                OVXPort srcPort = port.getOVXPort(lUtils.getTenantId(),
                        lUtils.getLinkId());
                if (srcPort == null) {
                    this.log.error(
                            "Virtual Src Port Unknown: {}, port {} with this match {}; dropping packet",
                            sw.getName(), match.getInputPort(), match);
                    return;
                }
                this.setInPort(srcPort.getPortNumber());
                OVXLink link;
                try {
                    OVXPort dstPort = map.getVirtualNetwork(
                            lUtils.getTenantId()).getNeighborPort(srcPort);
                    link = map.getVirtualSwitch(sw, lUtils.getTenantId())
                            .getMap().getVirtualNetwork(lUtils.getTenantId())
                            .getLink(dstPort, srcPort);
                } catch (SwitchMappingException | NetworkMappingException e) {
                    return; // same as (link == null)
                }
                this.ovxPort = this.port.getOVXPort(lUtils.getTenantId(),
                        link.getLinkId());
                OVXLinkField linkField = OpenVirteXController.getInstance()
                        .getOvxLinkField();
                // TODO: Need to check that the values in linkId and flowId
                // don't exceed their space
                if (linkField == OVXLinkField.MAC_ADDRESS) {
                    try {
                        LinkedList<MACAddress> macList = sw.getMap()
                                .getVirtualNetwork(this.ovxPort.getTenantId())
                                .getFlowManager()
                                .getFlowValues(lUtils.getFlowId());
                        eth.setSourceMACAddress(macList.get(0).toBytes())
                                .setDestinationMACAddress(
                                        macList.get(1).toBytes());
                        match.setDataLayerSource(eth.getSourceMACAddress())
                                .setDataLayerDestination(
                                        eth.getDestinationMACAddress());
                    } catch (NetworkMappingException e) {
                        log.warn(e);
                    }
                } else if (linkField == OVXLinkField.VLAN) {
                    // TODO
                    log.warn("VLAN virtual links not yet implemented.");
                    return;
                }

            }

            if (match.getDataLayerType() == Ethernet.TYPE_ARP) {
                // ARP packet
                final ARP arp = (ARP) eth.getPayload();
                this.tenantId = this.fetchTenantId(match, map, true);
                try {
                    if (map.hasVirtualIP(srcIP)) {
                        arp.setSenderProtocolAddress(map.getVirtualIP(srcIP)
                                .getIp());
                    }
                    if (map.hasVirtualIP(dstIP)) {
                        arp.setTargetProtocolAddress(map.getVirtualIP(dstIP)
                                .getIp());
                    }
                } catch (AddressMappingException e) {
                    log.warn("Inconsistency in OVXMap? : {}", e);
                }
            } else if (match.getDataLayerType() == Ethernet.TYPE_IPV4) {
                try {
                    final IPv4 ip = (IPv4) eth.getPayload();
                    ip.setDestinationAddress(map.getVirtualIP(dstIP).getIp());
                    ip.setSourceAddress(map.getVirtualIP(srcIP).getIp());
                    // TODO: Incorporate below into fetchTenantId
                    if (this.tenantId == null) {
                        this.tenantId = dstIP.getTenantId();
                    }
                } catch (AddressMappingException e) {
                    log.warn("Could not rewrite IP fields : {}", e);
                }
            } else {
                this.log.info("{} handling not yet implemented; dropping",
                        match.getDataLayerType());
                this.installDropRule(sw, match);
                return;
            }
            this.setPacketData(eth.serialize());

            vSwitch = this.fetchOVXSwitch(sw, vSwitch, map);

            this.sendPkt(vSwitch, match, sw);
            this.log.debug("IPv4 PacketIn {} sent to virtual network {}", this,
                    this.tenantId);
            return;
        }

        this.tenantId = this.fetchTenantId(match, map, true);
        if (this.tenantId == null || this.tenantId == 0) {
            this.log.warn(
                    "PacketIn {} does not belong to any virtual network; "
                            + "dropping and installing a temporary drop rule",
                    this);
            this.installDropRule(sw, match);
            return;
        }
        vSwitch = this.fetchOVXSwitch(sw, vSwitch, map);
        this.sendPkt(vSwitch, match, sw);
        this.log.debug("Layer2 PacketIn {} sent to virtual network {}", this,
                this.tenantId);
    }

    private void learnHostIP(OFMatch match, Mappable map) {
        if (!match.getWildcardObj().isWildcarded(Flag.NW_SRC)) {

            try {
                map.getVirtualNetwork(tenantId).getHost(ovxPort)				//호스트를 불러와 호스트객체의 아이피주소에 셋
                        .setIPAddress(match.getNetworkSource());
            } catch (NetworkMappingException e) {
                log.warn("Failed to lookup virtual network {}", this.tenantId);
                return;
            } catch (NullPointerException npe) {
                log.warn("No host attached at {} port {}", this.ovxPort
                        .getParentSwitch().getSwitchName(), this.ovxPort
                        .getPhysicalPortNumber());
            }
        }

    }

    private void sendPkt(final OVXSwitch vSwitch, final OFMatch match,
            final PhysicalSwitch sw) {
        if (vSwitch == null || !vSwitch.isActive()) {								//버츄얼 스위치가 없거나 죽어있을 
            this.log.warn(
                    "Controller for virtual network {} has not yet connected "
                            + "or is down", this.tenantId);
            this.installDropRule(sw, match);
            return;
        }
        this.setBufferId(vSwitch.addToBufferMap(this));								//버퍼아이디를 지정한다.
        if (this.port != null && this.ovxPort != null								//리얼 포트, 버추얼 포트가 있고 포트가 살아 있을 
                && this.ovxPort.isActive()) {
            this.setInPort(this.ovxPort.getPortNumber());							//인포트를 셋
            if ((this.packetData != null)											//패킷데이터가 있고 메시지 길이가 최대가 아닐 때 
                    && (vSwitch.getMissSendLen() != OVXSetConfig.MSL_FULL)) {		
                this.packetData = Arrays.copyOf(this.packetData,					//페킷데이터를 카피함 
                        U16.f(vSwitch.getMissSendLen()));
                this.setLengthU(OFPacketIn.MINIMUM_LENGTH							//패킷 데이터의 길이를 지정 
                        + this.packetData.length);
            }
            vSwitch.sendMsg(this, sw);												//스위치로 패킷을 전송 
        } else if (this.port == null) {												//리얼 포트가 없을 때 에러 
            log.error("The port {} doesn't belong to the physical switch {}",
                    this.getInPort(), sw.getName());
        } else if (this.ovxPort == null || !this.ovxPort.isActive()) {				//버츄얼포트가 없거나 포트가 죽어있을 때 
            log.error(
                    "Virtual port associated to physical port {} in physical switch {} for "
                            + "virtual network {} is not defined or inactive",
                    this.getInPort(), sw.getName(), this.tenantId);
        }
    }

    private void learnAddresses(final OFMatch match, final Mappable map) {
        if (match.getDataLayerType() == Ethernet.TYPE_IPV4
                || match.getDataLayerType() == Ethernet.TYPE_ARP) {						//데이터레이어 타입이 아이피브이4거나 압일때 
            if (!match.getWildcardObj().isWildcarded(Flag.NW_SRC)) {
                IPMapper.getPhysicalIp(this.tenantId, match.getNetworkSource());		//아이피맵에 없으면 추가 
            }
            if (!match.getWildcardObj().isWildcarded(Flag.NW_DST)) {
                IPMapper.getPhysicalIp(this.tenantId,
                        match.getNetworkDestination());
            }
        }
    }

    private void installDropRule(final PhysicalSwitch sw, final OFMatch match) {	//룰에 해당하는 패킷을 드랍하는 룰을 만들어 보낸다.
        final OVXFlowMod fm = new OVXFlowMod();
        fm.setMatch(match);
        fm.setBufferId(this.getBufferId());
        fm.setHardTimeout((short) 1);
        sw.sendMsg(fm, sw);
    }

    private Integer fetchTenantId(final OFMatch match, final Mappable map,
            final boolean useMAC) {
        MACAddress mac = MACAddress.valueOf(match.getDataLayerSource());		//목적지의 맥주소를 가져온다.
        if (useMAC && map.hasMAC(mac)) {
            try {
                return map.getMAC(mac);											//맵에서 맥주로를 통해 테넌트 아이디를 받아온다.
            } catch (AddressMappingException e) {
                log.warn("Tried to return non-mapped MAC address : {}", e);
            }
        }
        return null;
    }

    private OVXSwitch fetchOVXSwitch(PhysicalSwitch psw, OVXSwitch vswitch,			//해당 테넌트의 버츄얼 스위치를 받아온다.
            Mappable map) {
        if (vswitch == null) {
            try {
                vswitch = map.getVirtualSwitch(psw, this.tenantId);
            } catch (SwitchMappingException e) {
                log.warn("Cannot fetch non-mapped OVXSwitch: {}", e);
            }
        }
        return vswitch;
    }

    public OVXPacketIn(final OVXPacketIn pktIn) {
        this.bufferId = pktIn.bufferId;
        this.inPort = pktIn.inPort;
        this.length = pktIn.length;
        this.packetData = pktIn.packetData;
        this.reason = pktIn.reason;
        this.totalLength = pktIn.totalLength;
        this.type = pktIn.type;
        this.version = pktIn.version;
        this.xid = pktIn.xid;
    }

    public OVXPacketIn() {
        super();
    }

    public OVXPacketIn(final byte[] data, final short portNumber) {
        this();
        this.setInPort(portNumber);
        this.setBufferId(OFPacketOut.BUFFER_ID_NONE);
        this.setReason(OFPacketIn.OFPacketInReason.NO_MATCH);
        this.setPacketData(data);
        this.setTotalLength((short) (OFPacketIn.MINIMUM_LENGTH + this
                .getPacketData().length));
        this.setLengthU(OFPacketIn.MINIMUM_LENGTH + this.getPacketData().length);
    }

}
