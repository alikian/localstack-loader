package io.github.alikian.aws;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * AWS Cloudformation Resource
 */
@Data
public class Resources {
    String type;
    Map<String, Object> properties;

    /**
     * Get Property Value
     * @param name property name to get
     * @return Property
     */
    public String getStringProperty(String name){
        return (String) properties.get(name);
    }

    /**
     * Get List Map
     * @param name map to get
     * @return List Map
     */
    public List<Map<String,String>> getListMap(String name){
        return (List<Map<String, String>>) properties.get(name);
    }
}
