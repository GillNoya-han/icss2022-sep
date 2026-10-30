
grammar ICSS;

//--- LEXER ---

IF: 'if';
ELSE: 'else';
BOX_BRACKET_OPEN: '[';
BOX_BRACKET_CLOSE: ']';

TRUE: 'TRUE';
FALSE: 'FALSE';
PIXELSIZE: [0-9]+ 'px';
PERCENTAGE: [0-9]+ '%';
SCALAR: [0-9]+;

COLOR: '#' [0-9a-f] [0-9a-f] [0-9a-f]
             [0-9a-f] [0-9a-f] [0-9a-f];

ID_IDENT: '#' [a-z0-9\-]+;
CLASS_IDENT: '.' [a-z0-9\-]+;

LOWER_IDENT: [a-z] [a-z0-9\-]*;
CAPITAL_IDENT: [A-Z] [A-Za-z0-9_]*;

WS: [ \t\r\n]+ -> skip;

OPEN_BRACE: '{';
CLOSE_BRACE: '}';
OPEN_PAREN: '(';
CLOSE_PAREN: ')';
SEMICOLON: ';';
COLON: ':';
PLUS: '+';
MIN: '-';
MUL: '*';
ASSIGNMENT_OPERATOR: ':=';


//--- PARSER ---
stylesheet
    : (variable | cssRule)* EOF
    ;

// Variabelen, in formaat: Naam := berekening/waarde;
variable
    : CAPITAL_IDENT ASSIGNMENT_OPERATOR expression SEMICOLON
    ;

// Selectors voor elementen, ID's en classes
selector
    : LOWER_IDENT
    | ID_IDENT
    | CLASS_IDENT
    ;

// CSS-property
property
    : LOWER_IDENT
    ;

// CSS-regel met declarations
cssRule
    : selector OPEN_BRACE statementBody* CLOSE_BRACE
    ;

// CSS-declaratie, bijvoorbeeld: width: ParWidth;
declaration
    : property COLON expression SEMICOLON
    ;

// Optellen en aftrekken
expression
    : expression (PLUS | MIN) multiplication
    | multiplication
    ;

// Vermenigvuldige
multiplication
    : multiplication MUL factor
    | factor
    ;

// Waarden, variabelen en haakjes
factor
    : literal
    | COLOR
    | LOWER_IDENT
    | CAPITAL_IDENT
    | OPEN_PAREN expression CLOSE_PAREN
    ;

// Letterlijke waarden
literal
    : TRUE
    | FALSE
    | PIXELSIZE
    | PERCENTAGE
    | SCALAR
    ;

// Declareert statements of waarden
statementBody
    : variable
    | declaration
    | ifStatement
    ;

// If- en else-statements
ifStatement
    : IF BOX_BRACKET_OPEN expression BOX_BRACKET_CLOSE
      OPEN_BRACE statementBody* CLOSE_BRACE
      elseStatement?
    ;

elseStatement
    : ELSE OPEN_BRACE statementBody* CLOSE_BRACE
    ;

// Voorwaarde van een if-statement
condition
    : CAPITAL_IDENT
    ;
