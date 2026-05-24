/**
 * Rule grammar — expression language for the Rulex rule engine.
 *
 * Precedence is determined by alternative order (first = highest):
 *   Boolean : NOT > AND > OR
 *   Arith   : unary- > * / % > + -
 */
grammar Rule;

options { caseInsensitive = true; }

// ── Entry point ───────────────────────────────────────────────────────────────

program : expr EOF ;

// ── Boolean and predicate expressions ────────────────────────────────────────

expr
    : NOT expr                               # NotExpr
    | expr AND expr                          # AndExpr
    | expr OR  expr                          # OrExpr
    | '(' expr ')'                           # GroupExpr
    | arith compOp arith                     # ComparisonPred
    | arith NOT CONTAINS arith               # NotContainsPred
    | arith CONTAINS arith                   # ContainsPred
    | arith IS NOT NULL_LIT                  # IsNotNullPred
    | arith IS     NULL_LIT                  # IsNullPred
    | arith IS     NUMERIC                   # IsNumericPred
    | arith NOT IN inList                    # NotInPred
    | arith IN     inList                    # InPred
    | functionCall                           # FunctionPred
    | BOOL_LIT                               # BoolLiteralPred
    ;

compOp : EQ | NEQ | GTE | GT | LTE | LT ;

inList : '(' arith ( ',' arith )* ')' ;

// ── Arithmetic expressions ────────────────────────────────────────────────────

arith
    : MINUS arith                            # UnaryMinus
    | arith ( STAR | SLASH | PERCENT ) arith # MulExpr
    | arith ( PLUS  | MINUS )          arith # AddExpr
    | '(' arith ')'                          # ArithParen
    | functionCall                           # ArithFunc
    | fieldRef                               # ArithField
    | literal                                # ArithLiteral
    ;

// ── Calls and references ──────────────────────────────────────────────────────

functionCall : IDENTIFIER '(' argList? ')' ;

argList : arith ( ',' arith )* ;

 fieldRef : IDENTIFIER ;

// ── Literals ──────────────────────────────────────────────────────────────────

literal
    : NUMBER_LIT  # NumberLiteral
    | STRING_LIT  # StringLiteral
    | BOOL_LIT    # BoolLiteral
    | NULL_LIT    # NullLiteral
    ;

// ── Keywords (must precede IDENTIFIER) ───────────────────────────────────────

AND      : 'and' ;
OR       : 'or' ;
NOT      : 'not' ;
IS       : 'is' ;
IN       : 'in' ;
CONTAINS : 'contains' ;
NUMERIC  : 'numeric' ;
NULL_LIT : 'null' ;
BOOL_LIT : 'true' | 'false' ;

// ── Operators ─────────────────────────────────────────────────────────────────

NEQ : '!=' ;
GTE : '>=' ;
LTE : '<=' ;
GT  : '>'  ;
LT  : '<'  ;
EQ  : '='  ;

PLUS    : '+' ;
MINUS   : '-' ;
STAR    : '*' ;
SLASH   : '/' ;
PERCENT : '%' ;

// ── Literals and identifiers ──────────────────────────────────────────────────

NUMBER_LIT : [0-9]+ ( '.' [0-9]+ )? ;

STRING_LIT
    : '\'' ( ~['\r\n] | '\'\'' )* '\''
    | '"'  ( ~["\r\n]           )* '"'
    ;

IDENTIFIER : [a-z_] [a-z0-9_]* ;

WS : [ \t\r\n]+ -> skip ;
