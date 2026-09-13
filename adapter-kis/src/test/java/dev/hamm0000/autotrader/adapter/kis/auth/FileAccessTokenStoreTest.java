package dev.hamm0000.autotrader.adapter.kis.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileAccessTokenStoreTest {

    private static final Instant EXPIRES_AT = Instant.parse("2026-08-02T00:00:00Z");

    @TempDir
    Path directory;

    @Test
    void 캐시_파일이_없으면_저장된_토큰이_없다() {
        // given
        FileAccessTokenStore store = new FileAccessTokenStore(directory.resolve("token.json"));

        // when & then
        assertThat(store.find()).isEmpty();
    }

    @Test
    void 저장한_토큰과_만료_시각을_다음_기동에서_읽을_수_있다() {
        // given
        Path path = directory.resolve("nested/token.json");
        AccessToken token = new AccessToken("stored-value", EXPIRES_AT);
        FileAccessTokenStore writer = new FileAccessTokenStore(path);

        // when
        writer.save(token);
        FileAccessTokenStore reader = new FileAccessTokenStore(path);

        // then
        assertThat(reader.find()).contains(token);
    }

    @Test
    void 손상된_캐시_파일은_토큰이_없는_것으로_처리한다() throws IOException {
        // given
        Path path = directory.resolve("token.json");
        Files.writeString(path, "not-json");
        FileAccessTokenStore store = new FileAccessTokenStore(path);

        // when & then
        assertThat(store.find()).isEmpty();
    }
}
