package io.github.kotlerdev.frap.core.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record GenerateOptions(
    @JsonProperty("language") String language,
    @JsonProperty("class_name") String className,
    @JsonProperty("package_name") String packageName,
    @JsonProperty("include_signatures") Boolean includeSignatures
) {
    public static GenerateOptions javaPlaywright(String className, String packageName) {
        return new GenerateOptions("java_playwright", className, packageName, true);
    }
}
