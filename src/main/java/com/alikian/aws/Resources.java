package com.alikian.aws;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class Resources {
    String type;
    Map<String, Object> properties;

    public String getStringProperty(String name){
        return (String) properties.get(name);
    }
    public List<Map<String,String>> getListMap(String name){
        return (List<Map<String, String>>) properties.get(name);
    }
}
