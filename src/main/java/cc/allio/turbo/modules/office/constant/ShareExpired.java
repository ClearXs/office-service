package cc.allio.turbo.modules.office.constant;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Duration;

/**
 * shared expired time
 *
 * @author j.x
 * @date 2024/8/22 16:50
 */
@Getter
@AllArgsConstructor
public enum ShareExpired {

    ONE_DAY("1d", Duration.ofDays(1).toMillis()),
    SEVEN_DAYS("7d", Duration.ofDays(7).toMillis()),
    ONE_MONTH("1m", Duration.ofDays(30).toMillis()),
    UNLIMITED("unlimited", null);

    @JsonValue
    private final String value;
    private final Long time;
}
