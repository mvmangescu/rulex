package com.rulex.engine;

import org.antlr.v4.runtime.tree.ParseTree;

public record CompiledRule(String expression, ParseTree tree) {}
