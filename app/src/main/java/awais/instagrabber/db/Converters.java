package awais.instagrabber.db;

import androidx.room.TypeConverter;

import java.util.Date;

import awais.instagrabber.models.enums.FavoriteType;

public class Converters {
    @TypeConverter
    public static Date fromTimestamp(Long value) {
        return value == null ? null : new Date(value);
    }

    @TypeConverter
    public static Long dateToTimestamp(Date date) {
        return date == null ? null : date.getTime();
    }

    @TypeConverter
    public static FavoriteType fromFavoriteTypeString(String value) {
        try {
            return FavoriteType.valueOf(value);
        } catch (Exception e) {
            return null;
        }
    }

    @TypeConverter
    public static String favoriteTypeToString(FavoriteType favoriteType) {
        return favoriteType == null ? null : favoriteType.toString();
    }
}
