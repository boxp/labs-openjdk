/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 */
package jdk.vm.ci.hotspot.arm;

import static jdk.vm.ci.common.InitTimer.timer;

import java.util.EnumSet;

import jdk.vm.ci.arm.ARM;
import jdk.vm.ci.code.Architecture;
import jdk.vm.ci.code.RegisterConfig;
import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.code.stack.StackIntrospection;
import jdk.vm.ci.common.InitTimer;
import jdk.vm.ci.hotspot.HotSpotCodeCacheProvider;
import jdk.vm.ci.hotspot.HotSpotConstantReflectionProvider;
import jdk.vm.ci.hotspot.HotSpotJVMCIBackendFactory;
import jdk.vm.ci.hotspot.HotSpotJVMCIRuntime;
import jdk.vm.ci.hotspot.HotSpotMetaAccessProvider;
import jdk.vm.ci.hotspot.HotSpotStackIntrospection;
import jdk.vm.ci.meta.ConstantReflectionProvider;
import jdk.vm.ci.runtime.JVMCIBackend;

/** Creates the ARM32 HotSpot JVMCI backend. */
public final class HotSpotARMJVMCIBackendFactory implements HotSpotJVMCIBackendFactory {
    private static TargetDescription createTarget() {
        Architecture arch = new ARM(EnumSet.noneOf(ARM.CPUFeature.class));
        return new TargetDescription(arch, true, 8, 4096, true);
    }

    @Override
    public String getArchitecture() {
        return "arm";
    }

    @Override
    public JVMCIBackend createJVMCIBackend(HotSpotJVMCIRuntime runtime, JVMCIBackend host) {
        assert host == null;
        TargetDescription target = createTarget();
        RegisterConfig registerConfig;
        HotSpotCodeCacheProvider codeCache;
        ConstantReflectionProvider constantReflection;
        HotSpotMetaAccessProvider metaAccess;
        StackIntrospection stackIntrospection;
        try (InitTimer _ = timer("create providers")) {
            metaAccess = new HotSpotMetaAccessProvider(runtime);
            registerConfig = new ARMHotSpotRegisterConfig(target);
            codeCache = new HotSpotCodeCacheProvider(runtime, target, registerConfig);
            constantReflection = new HotSpotConstantReflectionProvider(runtime);
            stackIntrospection = new HotSpotStackIntrospection(runtime);
        }
        return new JVMCIBackend(metaAccess, codeCache, constantReflection, stackIntrospection);
    }
}
