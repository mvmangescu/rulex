package com.rulex.engine;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ParseTreeNode(String type, String text, List<ParseTreeNode> children) {}
