package dev.hamm0000.autotrader.app.live.kis.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class KisPaperAccountPropertiesTest {

    @Test
    void 계좌번호와_상품코드가_있으면_모의계좌를_설정할_수_있다() {
        // given
        KisPaperAccountProperties properties = new KisPaperAccountProperties(
                "test-account-number",
                "test-product-code"
        );

        // when & then
        assertThatCode(properties::validate).doesNotThrowAnyException();
    }

    @Test
    void 계좌번호나_상품코드가_비어_있으면_모의계좌를_설정할_수_없다() {
        // given
        KisPaperAccountProperties emptyAccountNumber = new KisPaperAccountProperties(
                " ",
                "test-product-code"
        );
        KisPaperAccountProperties emptyProductCode = new KisPaperAccountProperties(
                "test-account-number",
                ""
        );

        // when & then
        assertThatThrownBy(emptyAccountNumber::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("KIS 모의투자 계좌번호가 필요합니다");
        assertThatThrownBy(emptyProductCode::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("KIS 모의투자 계좌 상품코드가 필요합니다");
    }

    @Test
    void 설정을_문자열로_표현해도_계좌정보는_노출하지_않는다() {
        // given
        KisPaperAccountProperties properties = new KisPaperAccountProperties(
                "sensitive-account-number",
                "sensitive-product-code"
        );

        // when
        String result = properties.toString();

        // then
        assertThat(result).doesNotContain(
                "sensitive-account-number",
                "sensitive-product-code"
        );
    }
}
