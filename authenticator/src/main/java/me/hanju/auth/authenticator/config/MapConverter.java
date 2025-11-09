package me.hanju.auth.authenticator.config;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Map을 JSON 문자열로 변환하는 JPA Converter */
@Slf4j
@Converter
@RequiredArgsConstructor
public class MapConverter implements AttributeConverter<Map<String, Object>, String> {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Override
  public String convertToDatabaseColumn(Map<String, Object> attribute) {
    if (attribute == null || attribute.isEmpty()) {
      return "{}";
    }
    try {
      return MAPPER.writeValueAsString(attribute);
    } catch (JsonProcessingException e) {
      log.error("Error converting map to JSON", e);
      return "{}";
    }
  }

  @Override
  public Map<String, Object> convertToEntityAttribute(String dbData) {
    if (dbData == null || dbData.isEmpty()) {
      return new HashMap<>();
    }
    try {
      return MAPPER.readValue(
          dbData,
          new TypeReference<Map<String, Object>>() {
          });
    } catch (JsonProcessingException e) {
      log.error("Error converting JSON to map", e);
      return new HashMap<>();
    }
  }
}
