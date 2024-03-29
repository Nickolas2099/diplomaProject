package com.example.diplomaProject.domain.api.constructor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SqlFilter {

    private String field;
    private String condition;
    private String conditionField;
}
