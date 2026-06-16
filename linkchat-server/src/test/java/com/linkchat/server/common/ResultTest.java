package com.linkchat.server.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Result 统一返回对象测试
 */
@DisplayName("统一返回对象 Result 测试")
class ResultTest {

    @Test
    @DisplayName("success() - 无数据时应返回code=200")
    void successWithoutDataShouldReturn200() {
        Result<Void> result = Result.success();
        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getMessage()).isEqualTo("操作成功");
        assertThat(result.getData()).isNull();
    }

    @Test
    @DisplayName("success(data) - 应返回包含数据的Result")
    void successWithDataShouldWrapData() {
        String data = "test data";
        Result<String> result = Result.success(data);
        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isEqualTo("test data");
    }

    @Test
    @DisplayName("error(ResultCode) - 应返回对应错误码")
    void errorWithResultCodeShouldReturnCorrectCode() {
        Result<Void> result = Result.error(ResultCode.USER_NOT_FOUND);
        assertThat(result.getCode()).isEqualTo(1001);
        assertThat(result.getMessage()).isEqualTo("用户不存在");
        assertThat(result.getData()).isNull();
    }

    @Test
    @DisplayName("error(ResultCode, message) - 应使用自定义消息")
    void errorWithCustomMessageShouldOverride() {
        Result<Void> result = Result.error(ResultCode.UNAUTHORIZED, "Token已过期");
        assertThat(result.getCode()).isEqualTo(401);
        assertThat(result.getMessage()).isEqualTo("Token已过期");
    }

    @Test
    @DisplayName("error(code, message) - 自定义code和message")
    void errorWithCodeAndMessage() {
        Result<Void> result = Result.error(9999, "自定义错误");
        assertThat(result.getCode()).isEqualTo(9999);
        assertThat(result.getMessage()).isEqualTo("自定义错误");
    }
}