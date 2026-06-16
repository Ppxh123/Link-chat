package com.linkchat.server.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * BusinessException 测试
 */
@DisplayName("业务异常 BusinessException 测试")
class BusinessExceptionTest {

    @Test
    @DisplayName("通过ResultCode创建异常 - 应包含正确的code和message")
    void shouldCreateFromResultCode() {
        BusinessException ex = new BusinessException(ResultCode.USER_NOT_FOUND);
        assertThat(ex.getCode()).isEqualTo(1001);
        assertThat(ex.getMessage()).isEqualTo("用户不存在");
    }

    @Test
    @DisplayName("通过ResultCode+自定义消息创建异常")
    void shouldCreateWithCustomMessage() {
        BusinessException ex = new BusinessException(ResultCode.USER_NOT_FOUND, "用户ID=99999不存在");
        assertThat(ex.getCode()).isEqualTo(1001);
        assertThat(ex.getMessage()).isEqualTo("用户ID=99999不存在");
    }

    @Test
    @DisplayName("通过code+message创建异常")
    void shouldCreateFromCodeAndMessage() {
        BusinessException ex = new BusinessException(8888, "自定义业务异常");
        assertThat(ex.getCode()).isEqualTo(8888);
        assertThat(ex.getMessage()).isEqualTo("自定义业务异常");
    }

    @Test
    @DisplayName("BusinessException应继承RuntimeException")
    void shouldBeRuntimeException() {
        BusinessException ex = new BusinessException(ResultCode.INTERNAL_ERROR);
        assertThat(ex).isInstanceOf(RuntimeException.class);
    }
}