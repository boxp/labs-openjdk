/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 */
package jdk.vm.ci.arm;

import jdk.vm.ci.code.Register;

/** Named ARM32 register aliases for clients that need the ARM EABI register set. */
public final class ARMRegister {
    private ARMRegister() {
    }

    public static final Register r0 = ARM.r0, r1 = ARM.r1, r2 = ARM.r2, r3 = ARM.r3;
    public static final Register r4 = ARM.r4, r5 = ARM.r5, r6 = ARM.r6, r7 = ARM.r7;
    public static final Register r8 = ARM.r8, r9 = ARM.r9, r10 = ARM.r10, r11 = ARM.r11;
    public static final Register r12 = ARM.r12, r13 = ARM.r13, r14 = ARM.r14, r15 = ARM.r15;
    public static final Register sp = ARM.sp, lr = ARM.lr, pc = ARM.pc;

    public static final Register s0 = ARM.s0, s1 = ARM.s1, s2 = ARM.s2, s3 = ARM.s3;
    public static final Register s4 = ARM.s4, s5 = ARM.s5, s6 = ARM.s6, s7 = ARM.s7;
    public static final Register s8 = ARM.s8, s9 = ARM.s9, s10 = ARM.s10, s11 = ARM.s11;
    public static final Register s12 = ARM.s12, s13 = ARM.s13, s14 = ARM.s14, s15 = ARM.s15;
    public static final Register s16 = ARM.s16, s17 = ARM.s17, s18 = ARM.s18, s19 = ARM.s19;
    public static final Register s20 = ARM.s20, s21 = ARM.s21, s22 = ARM.s22, s23 = ARM.s23;
    public static final Register s24 = ARM.s24, s25 = ARM.s25, s26 = ARM.s26, s27 = ARM.s27;
    public static final Register s28 = ARM.s28, s29 = ARM.s29, s30 = ARM.s30, s31 = ARM.s31;

    public static final Register d0 = ARM.d0, d1 = ARM.d1, d2 = ARM.d2, d3 = ARM.d3;
    public static final Register d4 = ARM.d4, d5 = ARM.d5, d6 = ARM.d6, d7 = ARM.d7;
    public static final Register d8 = ARM.d8, d9 = ARM.d9, d10 = ARM.d10, d11 = ARM.d11;
    public static final Register d12 = ARM.d12, d13 = ARM.d13, d14 = ARM.d14, d15 = ARM.d15;
}
