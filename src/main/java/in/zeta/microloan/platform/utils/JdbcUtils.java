package in.zeta.microloan.platform.utils;

import in.zeta.microloan.platform.exception.DatabaseException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

import static in.zeta.microloan.platform.utils.ObjectUtils.anyNull;


@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class JdbcUtils {

    private static final String DATABASE_ERROR_LOG_STRING = "Database error";

    public static String getString(ResultSet resultSet, String keyName) {
        if (anyNull(resultSet, keyName)) {
            return null;
        }
        try {
            return resultSet.getString(keyName);
        } catch (SQLException e) {
            logSqlException(resultSet, keyName, e);
            throw new DatabaseException("Database error");
        }
    }

    public static Long getLong(ResultSet resultSet, String keyName) {
        if (anyNull(resultSet, keyName)) {
            return null;
        }
        try {
            long aLong = resultSet.getLong(keyName);
            return resultSet.wasNull() ? null : aLong;
        } catch (SQLException e) {
            logSqlException(resultSet, keyName, e);
            throw new DatabaseException("Database error");
        }
    }

    public static BigDecimal getBigDecimal(ResultSet resultSet, String keyName) {
        if (anyNull(resultSet, keyName)) {
            return null;
        }
        try {
            BigDecimal bigDecimal = resultSet.getBigDecimal(keyName);
            return resultSet.wasNull() ? null : bigDecimal;
        } catch (SQLException e) {
            logSqlException(resultSet, keyName, e);
            throw new DatabaseException("Database error");
        }
    }

    public static LocalDateTime getLocalDateTime(ResultSet resultSet, String keyName) {
        if (anyNull(resultSet, keyName)) {
            return null;
        }
        try {
            Timestamp timestamp = resultSet.getTimestamp(keyName);
            return Objects.isNull(timestamp) ? null : timestamp.toLocalDateTime();
        } catch (SQLException e) {
            logSqlException(resultSet, keyName, e);
            throw new DatabaseException("Database error");
        }
    }

    public static Integer getInteger(ResultSet resultSet, String keyName) {
        if (anyNull(resultSet, keyName)) {
            return null;
        }
        try {
            int anInt = resultSet.getInt(keyName);
            return resultSet.wasNull() ? null : anInt;
        } catch (SQLException e) {
            logSqlException(resultSet, keyName, e);
            throw new DatabaseException("Database error");
        }
    }

    private static void logSqlException(ResultSet resultSet, String keyName, SQLException sqlException) throws DatabaseException {

    }
}
