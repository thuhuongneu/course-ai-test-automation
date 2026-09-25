package com.anhtester.book.annotations;

import io.qameta.allure.LabelAnnotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mã manual test case — hiện trong Allure dưới label {@code testId} để map sang ma trận truy vết (RTM).
 *
 * <p>Dùng annotation thay vì gọi {@code Allure.label(...)} trong thân test: setUp hỏng thì thân test
 * không chạy và label sẽ mất — đúng lúc cần biết TC nào bị bỏ nhất.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@LabelAnnotation(name = "testId")
public @interface TcId {
    String value();
}
