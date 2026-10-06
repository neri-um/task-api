package com.example.todo.domain;

import java.util.Map;

public record TaskStats(long total, Map<TaskStatus, Long> byStatus) {}
