/*
 * Copyright (c) 2014, 2015 Ericsson Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.l2renderer;

import java.util.Iterator;

import org.opendaylight.sfc.l2renderer.SffGraph.SffGraphEntry;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.entry.SfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.sff.data.plane.locator.DataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.DataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.MacAddressLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.MacBuilder;

public class SfcRspProcessorVlan extends SfcRspTransportProcessorBase {
    private static final int VLAN_ID_INCR_HOP = 1;
    private static final int VLAN_ID_INCR_RSP = 100;
    private static int lastVlanId = 0;

    /**
     * Set the RSP path egress DPL and SFF Hop Ingress DPLs for the VLAN transport type.
     */
    @Override
    public void setRspTransports() {
        int hopIncrement = VLAN_ID_INCR_HOP;
        int transportData = lastVlanId + VLAN_ID_INCR_RSP;
        lastVlanId = transportData;

        Iterator<SffGraph.SffGraphEntry> sffGraphIter = sffGraph.getGraphEntryIterator();
        while (sffGraphIter.hasNext()) {
            SffGraph.SffGraphEntry entry = sffGraphIter.next();
            LOG.debug("RspTransport entry: {}", entry);

            if (entry.getSrcSff().equals(entry.getDstSff())) {
                // It may be that multiple SFs are on the same SFF
                // If so, we dont need to set the transports again
                // Otherwise the SFF ingress DPL will be overwritten
                continue;
            }

            DataPlaneLocatorBuilder dpl = new DataPlaneLocatorBuilder();
            dpl.setTransport(rsp.getTransportType());
            MacBuilder macBuilder = new MacBuilder();
            macBuilder.setVlanId(transportData);
            dpl.setLocatorType(macBuilder.build());

            if (entry.getDstSff().equals(SffGraph.EGRESS)) {
                sffGraph.setPathEgressDpl(entry.getPathId(), dpl.build());
            } else {
                sffGraph.setHopIngressDpl(entry.getDstSff(), entry.getPathId(), dpl.build());
            }
            transportData += hopIncrement;
        }
    }

    //
    // TransportIngress methods
    //

    /**
     * Configure the Transport Ingress flow for SFs
     *
     * @param entry - RSP hop info used to create the flow
     */
    @Override
    public void configureSfTransportIngressFlow(SffGraphEntry entry, SfDataPlaneLocator sfDpl) {
        String sffNodeName = sfcProviderUtils.getSffOpenFlowNodeName(entry.getDstSff(), entry.getPathId());
        this.sfcFlowProgrammer.configureVlanTransportIngressFlow(sffNodeName);
    }

    /**
     * Configure the Transport Ingress flow for SFFs
     *
     * @param entry - RSP hop info used to create the flow
     */
    @Override
    public void configureSffTransportIngressFlow(SffGraphEntry entry, SffDataPlaneLocator dstSffDpl) {
        String sffNodeName = sfcProviderUtils.getSffOpenFlowNodeName(entry.getDstSff(), entry.getPathId());
        this.sfcFlowProgrammer.configureVlanTransportIngressFlow(sffNodeName);
    }

    //
    // PathMapper methods
    //

    /**
     * Configure the Path Mapper flow for SFs
     *
     * @param entry - RSP hop info used to create the flow
     * @param sfDpl - the particular SF DPL used to create the flow
     */
    @Override
    public void configureSfPathMapperFlow(SffGraph.SffGraphEntry entry, SfDataPlaneLocator sfDpl) {
        String sffNodeName = sfcProviderUtils.getSffOpenFlowNodeName(entry.getDstSff(), entry.getPathId());
        Integer vlanTag = ((MacAddressLocator) sfDpl.getLocatorType()).getVlanId();
        if (vlanTag != null) {
            this.sfcFlowProgrammer.configureVlanPathMapperFlow(sffNodeName, vlanTag, entry.getPathId(), true);
        }
    }

    /**
     * Configure the Path Mapper flow for SFFs
     *
     * @param entry - RSP hop info used to create the flow
     * @param hopDpl - the particular SFF Hop DPL used to create the flow
     */
    @Override
    public void configureSffPathMapperFlow(SffGraph.SffGraphEntry entry, DataPlaneLocator hopDpl) {
        String sffNodeName = sfcProviderUtils.getSffOpenFlowNodeName(entry.getDstSff(), entry.getPathId());
        Integer vlanTag = ((MacAddressLocator) hopDpl.getLocatorType()).getVlanId();
        if (vlanTag != null) {
            this.sfcFlowProgrammer.configureVlanPathMapperFlow(sffNodeName, vlanTag, entry.getPathId(), false);
        }
    }

    //
    // NextHop methods
    //

    /**
     * Configure the Next Hop flow from an SFF to an SF
     *
     * @param entry - RSP hop info used to create the flow
     * @param srcSffDpl - the particular SFF DPL used to create the flow
     * @param dstSfDpl - the particular SF DPL used to create the flow
     */
    @Override
    public void configureNextHopFlow(SffGraph.SffGraphEntry entry, SffDataPlaneLocator srcSffDpl, SfDataPlaneLocator dstSfDpl) {
        String sffNodeName = sfcProviderUtils.getSffOpenFlowNodeName(entry.getDstSff(), entry.getPathId());
        String srcMac = sfcProviderUtils.getDplPortInfoMac(srcSffDpl);
        String dstMac = sfcProviderUtils.getSfDplMac(dstSfDpl);
        this.sfcFlowProgrammer.configureMacNextHopFlow(sffNodeName, entry.getPathId(), srcMac, dstMac);
    }

    /**
     * Configure the Next Hop flow from an SF to an SFF
     *
     * @param entry - RSP hop info used to create the flow
     * @param srcSfDpl - the particular SF DPL used to create the flow
     * @param dstSffDpl - the particular SFF DPL used to create the flow
     */
    @Override
    public void configureNextHopFlow(SffGraph.SffGraphEntry entry, SfDataPlaneLocator srcSfDpl, SffDataPlaneLocator dstSffDpl) {
        // in this case, we use the SrcSff instead of the typical DstSff
        String sffNodeName = sfcProviderUtils.getSffOpenFlowNodeName(entry.getSrcSff(), entry.getPathId());
        String srcMac = sfcProviderUtils.getSfDplMac(srcSfDpl);
        String dstMac = sfcProviderUtils.getDplPortInfoMac(dstSffDpl);
        this.sfcFlowProgrammer.configureMacNextHopFlow(sffNodeName, entry.getPathId(), srcMac, dstMac);
    }

    /**
     * Configure the Next Hop flow from an SF to an SF
     *
     * @param entry - RSP hop info used to create the flow
     * @param srcSfDpl - the particular SF DPL used to create the flow
     * @param dstSfDpl - the particular SF DPL used to create the flow
     */
    @Override
    public void configureNextHopFlow(SffGraph.SffGraphEntry entry, SfDataPlaneLocator srcSfDpl, SfDataPlaneLocator dstSfDpl) {
        // in this case, we use the SrcSff instead of the typical DstSff
        String sffNodeName = sfcProviderUtils.getSffOpenFlowNodeName(entry.getSrcSff(), entry.getPathId());
        String srcMac = sfcProviderUtils.getSfDplMac(srcSfDpl);
        String dstMac = sfcProviderUtils.getSfDplMac(dstSfDpl);
        this.sfcFlowProgrammer.configureMacNextHopFlow(sffNodeName, entry.getPathId(), srcMac, dstMac);
    }

    /**
     * Configure the Next Hop flow from an SFF to an SFF
     *
     * @param entry - RSP hop info used to create the flow
     * @param srcSffDpl - the particular SFF DPL used to create the flow
     * @param dstSffDpl - the particular SFF DPL used to create the flow
     */
    @Override
    public void configureNextHopFlow(SffGraph.SffGraphEntry entry, SffDataPlaneLocator srcSffDpl, SffDataPlaneLocator dstSffDpl) {
        String sffNodeName = sfcProviderUtils.getSffOpenFlowNodeName(entry.getDstSff(), entry.getPathId());
        String srcMac = sfcProviderUtils.getDplPortInfoMac(srcSffDpl);
        String dstMac = sfcProviderUtils.getDplPortInfoMac(dstSffDpl);
        this.sfcFlowProgrammer.configureMacNextHopFlow(sffNodeName, entry.getPathId(), srcMac, dstMac);
    }

    //
    // TransportEgress methods
    //

    /**
     * Configure the Transport Egress flow from an SFF to an SF
     *
     * @param entry - RSP hop info used to create the flow
     * @param srcSffDpl - the particular SFF DPL used to create the flow
     * @param dstSfDpl - the particular SF DPL used to create the flow
     * @param hopDpl - Hop DPL used to create the flow
     */
    @Override
    public void configureSfTransportEgressFlow(
            SffGraph.SffGraphEntry entry, SffDataPlaneLocator srcSffDpl, SfDataPlaneLocator dstSfDpl, DataPlaneLocator hopDpl) {
        Integer vlanTag = ((MacAddressLocator) hopDpl.getLocatorType()).getVlanId();
        if (vlanTag == null) {
            return;
        }

        String srcOfsPortStr = sfcProviderUtils.getDplPortInfoPort(srcSffDpl);
        if (srcOfsPortStr == null) {
            throw new RuntimeException("configureSffTransportEgressFlow OFS port not avail for SFF ["
                    + entry.getDstSff() + "] sffDpl [" + srcSffDpl.getName().getValue() + "]");
        }

        // For the SF transport Egress flow, we'll write to the Dst SFF as opposed to typically writing to the Src SFF
        String sffNodeName = sfcProviderUtils.getSffOpenFlowNodeName(entry.getDstSff(), entry.getPathId());
        String srcMac = sfcProviderUtils.getDplPortInfoMac(srcSffDpl);
        String dstMac = sfcProviderUtils.getSfDplMac(dstSfDpl);

        boolean doPktIn = false;
        ServiceFunction sf = sfcProviderUtils.getServiceFunction(entry.getSf(), entry.getPathId());
        // FIXME: I would caution against this approach. Instead you may want to see if
        // ServiceFunctionType has "bidirectional" = True in future.
        if (sf.getType().getValue().equals("tcp-proxy")) {
            LOG.info("SfcRspProcessorVlan::configureSfTransportEgressFlow TCP PROXY");
            // If the SF is a TCP Proxy, we need this additional flow for the SF:
            // - a flow that will also check for TCP Syn and do a PktIn
            this.sfcFlowProgrammer.configureArpTransportIngressFlow(sffNodeName, srcMac);
            doPktIn = true;
        }

        this.sfcFlowProgrammer.configureVlanSfTransportEgressFlow(
                sffNodeName, srcMac, dstMac, vlanTag,
                srcOfsPortStr, entry.getPathId(), doPktIn);
    }

    /**
     * Configure the Transport Egress flow from an SFF to an SFF
     *
     * @param entry - RSP hop info used to create the flow
     * @param srcSffDpl - the particular SFF DPL used to create the flow
     * @param dstSffDpl - the particular SFF DPL used to create the flow
     * @param hopDpl - Hop DPL used to create the flow
     */
    @Override
    public void configureSffTransportEgressFlow(
            SffGraph.SffGraphEntry entry, SffDataPlaneLocator srcSffDpl, SffDataPlaneLocator dstSffDpl, DataPlaneLocator hopDpl) {
        Integer vlanTag = ((MacAddressLocator) hopDpl.getLocatorType()).getVlanId();
        if (vlanTag == null) {
            return;
        }

        String srcOfsPortStr = sfcProviderUtils.getDplPortInfoPort(srcSffDpl);
        if (srcOfsPortStr == null) {
            throw new RuntimeException("configureSffTransportEgressFlow OFS port not avail for SFF ["
                    + entry.getDstSff() + "] sffDpl [" + srcSffDpl.getName().getValue() + "]");
        }

        String sffNodeName = sfcProviderUtils.getSffOpenFlowNodeName(entry.getSrcSff(), entry.getPathId());
        String srcMac = sfcProviderUtils.getDplPortInfoMac(srcSffDpl);
        String dstMac = sfcProviderUtils.getDplPortInfoMac(dstSffDpl);
        if (entry.getDstSff().equals(SffGraph.EGRESS)) {
            this.sfcFlowProgrammer.configureVlanLastHopTransportEgressFlow(
                    sffNodeName, srcMac, dstMac, vlanTag,
                    srcOfsPortStr, entry.getPathId());
        } else {
            this.sfcFlowProgrammer.configureVlanTransportEgressFlow(
                    sffNodeName, srcMac, dstMac, vlanTag,
                    srcOfsPortStr, entry.getPathId());
        }
    }
}
