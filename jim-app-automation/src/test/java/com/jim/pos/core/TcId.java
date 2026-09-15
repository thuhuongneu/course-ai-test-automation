package com.jim.pos.core;

import io.qameta.allure.LabelAnnotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Ma manual test case, hien trong Allure duoi label {@code testId}.
 *
 * <p>Phai la <b>annotation</b> chu khong phai goi {@code Allure.label(...)} trong than test: khi
 * test bi skip hoac hong o {@code setUp}, than test khong bao gio chay nen label se mat - dung luc
 * can biet TC nao bi bo nhat. Mat {@code testId} la dut moi noi sang cot TC ID cua ma tran truy vet
 * (RTM) va cac execution report cu.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@LabelAnnotation(name = "testId")
public @interface TcId {
  String value();
}
