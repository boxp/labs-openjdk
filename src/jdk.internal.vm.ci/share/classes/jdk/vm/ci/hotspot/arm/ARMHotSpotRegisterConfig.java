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

/**
 * ARM EABI/VFPv3 register and calling-convention configuration for HotSpot.
 *
 * Implements AAPCS (ARM Procedure Call Standard) for ARM32:
 * - Integer/pointer args: r0-r3; 64-bit longs use an even-aligned register pair
 * - Floating-point args: VFP s0-s15 for float, d0-d7 for double
 */
public final class ARMHotSpotRegisterConfig implements RegisterConfig {
    private static final List<Register> GENERAL_ARGUMENTS = List.of(r0, r1, r2, r3);
    // Single-precision VFP argument registers s0-s15
    private static final List<Register> FP_SINGLE_ARGUMENTS = List.of(
        s0, s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12, s13, s14, s15);
    // Double-precision VFP argument registers d0-d7 (d0=s0:s1, d1=s2:s3, ...)
    private static final List<Register> FP_DOUBLE_ARGUMENTS = List.of(
        d0, d1, d2, d3, d4, d5, d6, d7);
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
        // general: next available integer register index (r0-r3)
        // fpSlot: next available VFP single-register slot (float=1 slot, double=2 slots)
        // stack: bytes of stack argument area consumed so far
        int general = 0, fpSlot = 0, stack = 0;
        for (int i = 0; i < parameterTypes.length; i++) {
            JavaKind kind = parameterTypes[i].getJavaKind().getStackKind();
            ValueKind<?> valueKind = valueKindFactory.getValueKind(kind);
            if (kind == JavaKind.Long) {
                // AAPCS: 64-bit integers must start on an even-numbered register (r0 or r2).
                // If the current index is odd, skip it — it is wasted per spec.
                if (general % 2 != 0) {
                    general++;
                }
                if (general + 1 < GENERAL_ARGUMENTS.size()) {
                    // Use the lower register of the even-aligned pair as the value location.
                    // The upper word occupies general+1; code generation must treat them as a pair.
                    locations[i] = GENERAL_ARGUMENTS.get(general).asValue(valueKind);
                    general += 2;
                } else {
                    // No room in registers; spill to 8-byte-aligned stack slot.
                    general = GENERAL_ARGUMENTS.size();
                    if (stack % 8 != 0) stack += 4;
                    locations[i] = StackSlot.get(valueKind, stack, !convention.out);
                    stack += 8;
                }
            } else if (kind == JavaKind.Double) {
                // AAPCS VFP: doubles must start on an even s-slot boundary (= a d register).
                if (fpSlot % 2 != 0) {
                    fpSlot++;
                }
                int dIdx = fpSlot / 2;
                if (dIdx < FP_DOUBLE_ARGUMENTS.size()) {
                    locations[i] = FP_DOUBLE_ARGUMENTS.get(dIdx).asValue(valueKind);
                    fpSlot += 2;
                } else {
                    if (stack % 8 != 0) stack += 4;
                    locations[i] = StackSlot.get(valueKind, stack, !convention.out);
                    stack += 8;
                }
            } else if (kind == JavaKind.Float) {
                // AAPCS VFP: floats occupy a single s-slot.
                if (fpSlot < FP_SINGLE_ARGUMENTS.size()) {
                    locations[i] = FP_SINGLE_ARGUMENTS.get(fpSlot).asValue(valueKind);
                    fpSlot++;
                } else {
                    locations[i] = StackSlot.get(valueKind, stack, !convention.out);
                    stack += 4;
                }
            } else {
                // Boolean, Byte, Short, Char, Int, Object — single general register.
                if (general < GENERAL_ARGUMENTS.size()) {
                    locations[i] = GENERAL_ARGUMENTS.get(general).asValue(valueKind);
                } else {
                    locations[i] = StackSlot.get(valueKind, stack, !convention.out);
                    stack += Math.max(valueKind.getPlatformKind().getSizeInBytes(), target.wordSize);
                }
                general++;
            }
        }
        JavaKind returnKind = returnType == null ? JavaKind.Void : returnType.getJavaKind();
        AllocatableValue result = returnKind == JavaKind.Void ? Value.ILLEGAL
            : getReturnRegister(returnKind).asValue(valueKindFactory.getValueKind(returnKind.getStackKind()));
        return new CallingConvention(stack, result, locations);
    }

    @Override
    public List<Register> getCallingConventionRegisters(Type type, JavaKind kind) {
        return switch (kind) {
            case Boolean, Byte, Short, Char, Int, Long, Object -> GENERAL_ARGUMENTS;
            case Float -> FP_SINGLE_ARGUMENTS;
            case Double -> FP_DOUBLE_ARGUMENTS;
            default -> throw JVMCIError.shouldNotReachHere();
        };
    }

    @Override
    public Register getReturnRegister(JavaKind kind) {
        return switch (kind) {
            case Boolean, Byte, Short, Char, Int, Long, Object -> r0;
            case Float -> s0;
            case Double -> d0;
            case Void, Illegal -> null;
            default -> throw new UnsupportedOperationException("no return register for type " + kind);
        };
    }

    @Override public Register getFrameRegister() { return sp; }
}
