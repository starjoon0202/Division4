/*
 * Copyright (c) 2015 Intel Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChain;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfcName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.service.function.chain.SfcServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.ServiceFunctionType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.service.function.type.SftServiceFunctionName;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.Icps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class implements a random SF scheduling mode.
 * <p>
 *
 * @author Johnson Li (johnson.li@intel.com)
 * @version 0.1
 *          <p>
 * @since 2015-03-04
 */
public class SfcServiceFunctionIcpsSchedulerAPI extends SfcServiceFunctionSchedulerAPI {

    private static final Logger LOG = LoggerFactory.getLogger(SfcServiceFunctionIcpsSchedulerAPI.class);

    SfcServiceFunctionIcpsSchedulerAPI() {
        super.setSfcServiceFunctionSchedulerType(Icps.class);
    }

    // TODO See similar TODO in LoadBalancer about method name.
    private SfName getServiceFunctionByType(ServiceFunctionType serviceFunctionType) {
        List<SftServiceFunctionName> sftServiceFunctionNameList = serviceFunctionType.getSftServiceFunctionName();
        int maxTries = sftServiceFunctionNameList.size();
        Random rad = new Random();
        ServiceFunction serviceFunction = null;
        SfName serviceFunctionName = null;
        int start = rad.nextInt(sftServiceFunctionNameList.size());

        while (maxTries > 0) {
            serviceFunctionName = new SfName(sftServiceFunctionNameList.get(start).getName());
            serviceFunction = SfcProviderServiceFunctionAPI.readServiceFunction(serviceFunctionName);
            if (serviceFunction != null) {
                break;
            } else {
                LOG.debug("ServiceFunction {} doesn't exist", serviceFunctionName);
                maxTries--;
                serviceFunctionName = null;
                start = (start + 1) % sftServiceFunctionNameList.size();
            }
        }
        if (serviceFunctionName == null) {
            LOG.error("Could not find an existing ServiceFunction for {}", serviceFunctionType.getType());
        }
        return serviceFunctionName;
    }

    @Override
    public List<SfName> scheduleServiceFunctions(ServiceFunctionChain chain, int serviceIndex,
                                                 ServiceFunctionPath sfp) {
        List<SfName> sfNameList = new ArrayList<>();
        List<SfcServiceFunction> sfcServiceFunctionList = new ArrayList<>();
        sfcServiceFunctionList.addAll(chain.getSfcServiceFunction());
        short index = 0;
        Map<Short, SfName> sfpMapping = getSFPHopSfMapping(sfp);

        /*
         * For each ServiceFunction type in the list of ServiceFunctions we select a specific
         * service function from the list of service functions by type.
         */
        for (SfcServiceFunction sfcServiceFunction : sfcServiceFunctionList) {
            LOG.info("ServiceFunction name: {}", sfcServiceFunction.getName());
            SfName hopSf = sfpMapping.get(index++);
            if (hopSf != null) {
                sfNameList.add(hopSf);
                continue;
            }
        }

        ServiceFunction serviceFunction = null;
        String chainName = chain.getName().getValue();

        if  (chainName.equals("iCPS-Central")) {
            serviceFunction = SfcProviderServiceFunctionAPI.readServiceFunction(new SfName("VM-Central"));
            sfNameList.add ( serviceFunction.getName());
            LOG.info("chain name: {}, {}", chain.getName(), new SfcName ("iCPS-Central") );

        } else if  (chainName.equals("iCPS-Edge")) {
            serviceFunction = SfcProviderServiceFunctionAPI.readServiceFunction(new SfName("VM-Edge"));
            sfNameList.add ( serviceFunction.getName());

        } else if  (chainName.equals("iCPS-Central-ping")){
            serviceFunction = SfcProviderServiceFunctionAPI.readServiceFunction(new SfName("VM-Central"));
            sfNameList.add ( serviceFunction.getName());
            LOG.info("chain name: {}, {}", chain.getName(), new SfcName ("iCPS-Central-ping") );

        } else if  (chainName.equals("iCPS-Edge-ping")){
            serviceFunction = SfcProviderServiceFunctionAPI.readServiceFunction(new SfName("VM-Edge"));
            sfNameList.add ( serviceFunction.getName());
            LOG.info("chain name: {}, {}", chain.getName(), new SfcName ("iCPS-Edge-ping") );
        }

        return sfNameList;
    }
}
