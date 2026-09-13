package dev.hamm0000.autotrader.adapter.kis.auth;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import tools.jackson.databind.ObjectMapper;

/**
 * 로컬 파일에 KIS Access Token과 만료 시각을 저장합니다.
 */
public final class FileAccessTokenStore implements AccessTokenStore {

    private static final System.Logger log = System.getLogger(FileAccessTokenStore.class.getName());
    private static final Set<PosixFilePermission> OWNER_READ_WRITE = Set.of(
            PosixFilePermission.OWNER_READ,
            PosixFilePermission.OWNER_WRITE
    );

    private final Path path;
    private final ObjectMapper objectMapper;

    public FileAccessTokenStore(Path path) {
        this(path, new ObjectMapper());
    }

    FileAccessTokenStore(Path path, ObjectMapper objectMapper) {
        this.path = Objects.requireNonNull(path, "path must not be null")
                .toAbsolutePath()
                .normalize();
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    @Override
    public Optional<AccessToken> find() {
        if (Files.notExists(path)) {
            return Optional.empty();
        }

        try {
            StoredAccessToken stored = objectMapper.readValue(
                    Files.readAllBytes(path),
                    StoredAccessToken.class
            );
            return Optional.of(stored.toAccessToken());
        } catch (Exception failure) {
            log.log(
                    System.Logger.Level.WARNING,
                    "KIS AccessToken 캐시 파일을 읽을 수 없어 사용하지 않습니다 (path={0})",
                    path
            );
            return Optional.empty();
        }
    }

    @Override
    public void save(AccessToken token) {
        Objects.requireNonNull(token, "token must not be null");

        Path temporary = null;
        try {
            Path parent = path.getParent();
            Files.createDirectories(parent);
            temporary = Files.createTempFile(parent, ".kis-access-token-", ".tmp");
            restrictToOwner(temporary);
            Files.write(
                    temporary,
                    objectMapper.writeValueAsBytes(StoredAccessToken.from(token))
            );
            moveReplacing(temporary, path);
            restrictToOwner(path);
        } catch (Exception failure) {
            log.log(
                    System.Logger.Level.WARNING,
                    "KIS AccessToken 캐시 파일을 저장할 수 없습니다 (path={0})",
                    path
            );
        } finally {
            deleteTemporary(temporary);
        }
    }

    private static void moveReplacing(Path source, Path target) throws IOException {
        try {
            Files.move(
                    source,
                    target,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING
            );
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void restrictToOwner(Path target) throws IOException {
        try {
            Files.setPosixFilePermissions(target, OWNER_READ_WRITE);
        } catch (UnsupportedOperationException ignored) {
            // POSIX 권한을 지원하지 않는 파일 시스템에서는 플랫폼 기본 권한을 사용합니다.
        }
    }

    private static void deleteTemporary(Path temporary) {
        if (temporary == null) {
            return;
        }
        try {
            Files.deleteIfExists(temporary);
        } catch (IOException ignored) {
            // 캐시 저장 실패 정리 과정은 원래 KIS 요청 결과에 영향을 주지 않습니다.
        }
    }

    private record StoredAccessToken(String value, String expiresAt) {

        private static StoredAccessToken from(AccessToken token) {
            return new StoredAccessToken(token.value(), token.expiresAt().toString());
        }

        private AccessToken toAccessToken() {
            if (value == null || value.isBlank()) {
                throw new IllegalStateException("stored token value must not be blank");
            }
            return new AccessToken(
                    value,
                    Instant.parse(Objects.requireNonNull(
                            expiresAt,
                            "stored token expiration must not be null"
                    ))
            );
        }
    }
}
