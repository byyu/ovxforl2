package net.onrc.openvirtex.api.service.handlers.tenant;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2ParamsType;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;

import net.onrc.openvirtex.api.service.handlers.ApiHandler;
import net.onrc.openvirtex.api.service.handlers.HandlerUtils;
import net.onrc.openvirtex.api.service.handlers.TenantHandler;
import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.host.Host;
import net.onrc.openvirtex.elements.sla.SLAHandler;
import net.onrc.openvirtex.exceptions.DroppedMessageException;
import net.onrc.openvirtex.exceptions.InvalidHostException;
import net.onrc.openvirtex.exceptions.MissingRequiredField;
import net.onrc.openvirtex.exceptions.NetworkMappingException;
import net.onrc.openvirtex.exceptions.SLAValueException;

public class SetFlowSLA extends ApiHandler<Map<String, Object>>{
	Logger log = LogManager.getLogger(SetFlowSLA.class.getName());
	
	@Override
	public JSONRPC2Response process(final Map<String, Object> params){
		JSONRPC2Response resp = null;
		

			try {
				final Number tenantId = HandlerUtils.<Number>fetchField(
						TenantHandler.TENANT, params, true, null);
				final Number host1 = HandlerUtils.<Number>fetchField(
						TenantHandler.HOST, params, true, null);
				final Number host2 = HandlerUtils.<Number>fetchField(
						TenantHandler.HOST1, params, true, null);
				final Number sla = HandlerUtils.<Number>fetchField(
						TenantHandler.SLA, params, true, null);
				
				HandlerUtils.isValidTenantId(tenantId.intValue());
				HandlerUtils.isValidHostId(tenantId.intValue(), host1.intValue());
				HandlerUtils.isValidHostId(tenantId.intValue(), host2.intValue());
				HandlerUtils.isValidSLA(sla.intValue());
				
				Host h1 = OVXMap.getInstance().getVirtualNetwork(tenantId.intValue()).getHost(host1.intValue());
				Host h2 = OVXMap.getInstance().getVirtualNetwork(tenantId.intValue()).getHost(host2.intValue());
				int flowId = OVXMap.getInstance().getVirtualNetwork(tenantId.intValue()).getFlowManager().getFlowId(h1.getMac().getAddress(),h2.getMac().getAddress());
				
				SLAHandler slaHandler = SLAHandler.getInstance();
				slaHandler.setFlowSLA(flowId, sla.intValue());
				
				Map<String, Object> reply = new HashMap<String, Object>();
				reply.put(TenantHandler.SLA, sla.intValue());
				reply.put(TenantHandler.HOST, host1.intValue());
				reply.put(TenantHandler.HOST, host2.intValue());
				resp = new JSONRPC2Response(reply, 0);

			} catch (MissingRequiredField e) {
	            resp = new JSONRPC2Response(
	                    new JSONRPC2Error(
	                            JSONRPC2Error.INVALID_PARAMS.getCode(),
	                            this.cmdName()
	                                    + ": Unable to set this SLA in the virtual network : "
	                                    + e.getMessage()), 0);
			} catch (SLAValueException e) {
				 resp = new JSONRPC2Response(new JSONRPC2Error(
						 JSONRPC2Error.INVALID_PARAMS.getCode(), this.cmdName()
						 + ": Invalid virtual SLA : " + e.getMessage()), 0);
			} catch (InvalidHostException e) {
				resp = new JSONRPC2Response(new JSONRPC2Error(
						 JSONRPC2Error.INVALID_PARAMS.getCode(), this.cmdName()
						 + ": Invalid Host id : " + e.getMessage()), 0);
			} catch (NetworkMappingException e) {
				resp = new JSONRPC2Response(new JSONRPC2Error(
						 JSONRPC2Error.INVALID_PARAMS.getCode(), this.cmdName()
						 + ": Invalid virtual network : " + e.getMessage()), 0);
			} catch (DroppedMessageException e) {
				resp = new JSONRPC2Response(new JSONRPC2Error(
						 JSONRPC2Error.INVALID_PARAMS.getCode(), this.cmdName()
						 + ": Invalid Hosts id : " + e.getMessage()), 0);
			}
			
			return resp;
			

	}

	@Override
	public JSONRPC2ParamsType getType() {
		return JSONRPC2ParamsType.OBJECT;
	}
}
