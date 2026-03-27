package com.nsia.commons.cucumber.glue.support;

import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * This class contains all utils related to this project and acts as Spring Bean.
 *
 * @author Daniel Joi Partogi Hutapea
 */
@SuppressWarnings("unused")
@Slf4j
@RequiredArgsConstructor
@Component
public class TestAppBeanUtils
{
    private final ObjectMapper objectMapper;
    private final Validator validator;

    @SneakyThrows
    public <T> T fromJson(String jsonContent, Class<T> valueType)
    {
        return objectMapper.readValue(jsonContent, valueType);
    }

    public <T> T fromJson(String jsonContent, Class<T> valueType, T defaultValueIfJsonUnparseable)
    {
        try
        {
            return jsonContent==null? defaultValueIfJsonUnparseable : fromJson(jsonContent, valueType);
        }
        catch(Exception ex)
        {
            log.warn("Failed to parse JSON to Java object '{}'. Cause: {}", valueType, ex.getMessage());
            return defaultValueIfJsonUnparseable;
        }
    }

    @SneakyThrows
    public <T> T fromJson(String jsonContent, TypeReference<T> valueTypeRef)
    {
        return objectMapper.readValue(jsonContent, valueTypeRef);
    }

    @SneakyThrows
    public String toJson(Object value)
    {
        return objectMapper.writeValueAsString(value);
    }

    @SneakyThrows
    public String toJsonPretty(Object value)
    {
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
    }

    public Map<String, Object> toMap(String jsonContent)
    {
        return fromJson(jsonContent, new TypeReference<>() {});
    }

    @SneakyThrows
    public JsonNode readTree(String jsonContent)
    {
        return objectMapper.readTree(jsonContent);
    }

    @SneakyThrows
    public <T> T treeToValue(TreeNode n, Class<T> valueType)
    {
        return objectMapper.treeToValue(n, valueType);
    }

    @SneakyThrows
    public <T> T readValue(String content, Class<T> valueType)
    {
        return objectMapper.readValue(content, valueType);
    }

    public <T> T convertValue(Object fromValue, Class<T> toValueType)
    {
        return objectMapper.convertValue(fromValue, toValueType);
    }

    public <T> T convertValue(Object fromValue, TypeReference<T> valueTypeRef)
    {
        return objectMapper.convertValue(fromValue, valueTypeRef);
    }

    public void validate(Object target, List<Class<?>> groups)
    {
        validate(target, groups==null? new Class[0] : groups.toArray(new Class[0]));
    }

    public void validate(Object target, Class<?>... groups)
    {
        var setOfConstraintViolation = validator.validate(target, groups);

        if(!setOfConstraintViolation.isEmpty())
        {
            throw new ConstraintViolationException(setOfConstraintViolation);
        }
    }
}
