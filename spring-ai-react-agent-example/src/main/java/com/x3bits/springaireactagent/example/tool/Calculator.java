package com.x3bits.springaireactagent.example.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class Calculator {

    @Tool(description = "计算两数之和")
    public int add(int a, int b) {
        return a + b;
    }
} 