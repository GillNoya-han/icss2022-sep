grammar ICSS;

//--- LEXER: ---

// IF support:
IF: 'if';
ELSE: 'else';
BOX_BRACKET_OPEN: '[';
BOX_BRACKET_CLOSE: ']';


//Literals
TRUE: 'TRUE';
FALSE: 'FALSE';
PIXELSIZE: [0-9]+ 'px';
PERCENTAGE: [0-9]+ '%';
SCALAR: [0-9]+;


//Color value takes precedence over id idents
COLOR: '#' [0-9a-f] [0-9a-f] [0-9a-f] [0-9a-f] [0-9a-f] [0-9a-f];

//Specific identifiers for id's and css classes
ID_IDENT: '#' [a-z0-9\-]+;
CLASS_IDENT: '.' [a-z0-9\-]+;

//General identifiers
LOWER_IDENT: [a-z] [a-z0-9\-]*;
CAPITAL_IDENT: [A-Z] [A-Za-z0-9_]*;

//All whitespace is skipped
WS: [ \t\r\n]+ -> skip;

//
OPEN_BRACE: '{';
CLOSE_BRACE: '}';
SEMICOLON: ';';
COLON: ':';
PLUS: '+';
MIN: '-';
MUL: '*';
ASSIGNMENT_OPERATOR: ':=';


//--- PARSER ---//

stylesheet
    : (variable | cssRule)* EOF
    ;

// Herkent een variabele, zoals LinkColor := #ff0000;
variable
    : CAPITAL_IDENT ASSIGNMENT_OPERATOR value SEMICOLON
    ;

// Detecteert element-, ID- en class-selectors
selector
    : LOWER_IDENT
    | ID_IDENT
    | CLASS_IDENT
    ;

// Herkent een CSS-property
property
    : LOWER_IDENT
    ;

// Herkent een declaratie tussen accolades
declaration
    : property COLON value SEMICOLON
    ;

// Herkent een volledige CSS-regel
cssRule
    : selector OPEN_BRACE declaration* CLOSE_BRACE
    ;

// Herkent letterlijke waarden
literal
    : TRUE
    | FALSE
    | PIXELSIZE
    | PERCENTAGE
    | SCALAR
    ;

// Herkent waarden die bij CSS-properties horen
value
    : literal
    | COLOR
    | LOWER_IDENT
    | CAPITAL_IDENT
    ;





