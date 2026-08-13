/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 */
package jdk.vm.ci.hotspot.arm;

import static jdk.vm.ci.arm.ARM.*;

import java.util.ArrayList;
import java.util.List;

import jdk.vm.ci.arm.ARM;
import jdk.vm.ci.code.CallingConvention;
import jdk.vm.ci.code.CallingConvention.Type;
import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.RegisterAttributes;
import jdk.vm.ci.code.RegisterConfig;
import jdk.vm.ci.code.StackSlot;
import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.code.ValueKindFactory;
import jdk.vm.ci.common.JVMCIError;
import jdk.vm.ci.hotspot.HotSpotCallingConventionType;
import jdk.vm.ci.meta.AllocatableValue;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.JavaType;
import jdk.vm.ci.meta.PlatformKind;
import jdk.vm.ci.meta.Value;
import jdk.vm.ci.meta.ValueKind;

/** ARM EABI/VFP register and calling-convention configuration for HotSpot. */
public final class ARMHotSpotRegisterConfig implements RegisterConfig {
    private static final List<Register> GENERAL_ARGUMENTS = List.of(r0, r1, r2, r3);
    private static final List<Register> FP_ARGUMENTS = List.of(s0, s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12, s13, s14, s15);
    private static final List<Register> RESERVED = List.of(r9, r10, r11, r12, sp, lr, pc);
    private final TargetDescription target;
    private final List<Register> allocatable;
    private final List<RegisterAttributes> attributes;

    public ARMHotSpotRegisterConfig(TargetDescription target) {
        this.target = target;
        ArrayList<Register> registers = new ArrayList<>();
        for (Register reg : ARM.allRegisters) {
            if (!RESERVED.contains(reg)) registers.add(reg);
        }
        allocatable = List.copyOf(registers);
        attributes = RegisterAttributes.createMap(this, ARM.allRegisters);
    }

    @Override public List<Register> getAllocatableRegisters() { return allocatable; }
    @Override public List<Register> getCallerSaveRegisters() { return allocatable; }
    @Override public List<Register> getCalleeSaveRegisters() { return null; }
    @Override public boolean areAllAllocatableRegistersCallerSaved() { return true; }
    @Override public List<RegisterAttributes> getAttributesMap() { return attributes; }

    @Override
    public List<Register> filterAllocatableRegisters(PlatformKind kind, List<Register> registers) {
        ArrayList<Register> result = new ArrayList<>();
        for (Register reg : registers) {
            if (target.arch.canStoreValue(reg.getRegisterCategory(), kind)) result.add(reg);
        }
        return List.copyOf(result);
    }

    @Override
    public CallingConvention getCallingConvention(Type type, JavaType returnType, JavaType[] parameterTypes, ValueKindFactory<?> valueKindFactory) {
        HotSpotCallingConventionType convention = (HotSpotCallingConventionType) type;
        AllocatableValue[] locations = new AllocatableValue[parameterTypes.length];
        int general = 0, fp = 0, stack = 0;
        for (int i = 0; i < parameterTypes.length; i++) {
            JavaKind kind = parameterTypes[i].getJavaKind().getStackKind();
            boolean floating = kind == JavaKind.Float || kind == JavaKind.Double;
            List<Register> registers = floating ? FP_ARGUMENTS : GENERAL_ARGUMENTS;
            int index = floating ? fp++ : general++;
            ValueKind<?> valueKind = valueKindFactory.getValueKind(kind);
            if (index < registers.size()) locations[i] = registers.get(index).asValue(valueKind);
            else {
                locations[i] = StackSlot.get(valueKind, stack, !convention.out);
                stack += Math.max(valueKind.getPlatformKind().getSizeInBytes(), target.wordSize);
            }
        }
        JavaKind returnKind = returnType == null ? JavaKind.Void : returnType.getJavaKind();
        AllocatableValue result = returnKind == JavaKind.Void ? Value.ILLEGAL : getReturnRegister(returnKind).asValue(valueKindFactory.getValueKind(returnKind.getStackKind()));
        return new CallingConvention(stack, result, locations);
    }

    @Override
    public List<Register> getCallingConventionRegisters(Type type, JavaKind kind) {
        return switch (kind) {
            case Boolean, Byte, Short, Char, Int, Long, Object -> GENERAL_ARGUMENTS;
            case Float, Double -> FP_ARGUMENTS;
            default -> throw JVMCIError.shouldNotReachHere();
        };
    }

    @Override
    public Register getReturnRegister(JavaKind kind) {
        return switch (kind) {
            case Boolean, Byte, Short, Char, Int, Long, Object -> r0;
            case Float, Double -> s0;
            case Void, Illegal -> null;
            default -> throw new UnsupportedOperationException("no return register for type " + kind);
        };
    }

    @Override public Register getFrameRegister() { return sp; }
}
