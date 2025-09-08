package com.cryptomessage.server.model.persistance.message;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Converter
public class ContentByUserConverter implements AttributeConverter<Map<Long, String>, String> {

    private static final String ENTRY_DELIMITER = ";";
    private static final String KEY_VALUE_DELIMITER = ":";

    @Override
    public String convertToDatabaseColumn(Map<Long, String> contentByUser) {
        if (contentByUser == null || contentByUser.isEmpty()) {
            return null;
        }

        // Convert Map<Long, String> to a single String
        return contentByUser.entrySet().stream()
                .map(entry -> entry.getKey() + KEY_VALUE_DELIMITER + entry.getValue())
                .collect(Collectors.joining(ENTRY_DELIMITER));
    }

    @Override
    public Map<Long, String> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return new HashMap<>();
        }

        // Convert String to Map<Long, String>
        return Stream.of(dbData.split(ENTRY_DELIMITER))
                .map(entry -> entry.split(KEY_VALUE_DELIMITER, 2))
                .collect(Collectors.toMap(
                        entry -> Long.parseLong(entry[0]),
                        entry -> entry[1]
                ));
    }
}
