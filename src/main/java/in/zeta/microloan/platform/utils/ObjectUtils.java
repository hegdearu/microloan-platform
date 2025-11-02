package in.zeta.microloan.platform.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Objects;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ObjectUtils {
    public static boolean anyNull(Object object1, Object object2) {
        return Objects.isNull(object1) || Objects.isNull(object2);
    }
}
