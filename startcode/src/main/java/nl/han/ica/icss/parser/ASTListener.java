
package nl.han.ica.icss.parser;

import nl.han.ica.datastructures.HANStack;
import nl.han.ica.datastructures.IHANStack;
import nl.han.ica.icss.ast.*;
import nl.han.ica.icss.ast.literals.*;
import nl.han.ica.icss.ast.operations.*;
import nl.han.ica.icss.ast.selectors.ClassSelector;
import nl.han.ica.icss.ast.selectors.IdSelector;
import nl.han.ica.icss.ast.selectors.TagSelector;

public class ASTListener extends ICSSBaseListener {

    private final AST ast;
    private final IHANStack<ASTNode> currentContainer;

    public ASTListener() {
        ast = new AST();
        currentContainer = new HANStack<>();
        currentContainer.push(ast.root);
    }

    public AST getAST() {
        return ast;
    }

    @Override
    public void enterVariable(ICSSParser.VariableContext ctx) {
        VariableAssignment assignment = new VariableAssignment();

        // De naam van de variabele, bijvoorbeeld LinkColor
        assignment.addChild(new VariableReference(ctx.CAPITAL_IDENT().getText()));

        currentContainer.peek().addChild(assignment);
        currentContainer.push(assignment);
    }

    @Override
    public void exitVariable(ICSSParser.VariableContext ctx) {
        currentContainer.pop();
    }

    @Override
    public void enterCssRule(ICSSParser.CssRuleContext ctx) {
        Stylerule rule = new Stylerule();

        // Voeg de regel toe aan de huidige ouder.
        currentContainer.peek().addChild(rule);

        // De regel wordt de huidige container.
        currentContainer.push(rule);

        // Maak de selector aan en voeg deze toe.
        ASTNode selectorNode = createSelector(ctx.selector());
        rule.addChild(selectorNode);
    }


    @Override
    public void exitCssRule(ICSSParser.CssRuleContext ctx) {
        currentContainer.pop();
    }

    private ASTNode createSelector(ICSSParser.SelectorContext ctx) {
        if (ctx.CLASS_IDENT() != null) {
            return new ClassSelector(ctx.CLASS_IDENT().getText());
        }
        if (ctx.LOWER_IDENT() != null) {
            return new TagSelector(ctx.LOWER_IDENT().getText());
        }
        return new IdSelector(ctx.ID_IDENT().getText());
    }

    @Override
    public void enterDeclaration(ICSSParser.DeclarationContext ctx) {
        Declaration declaration = new Declaration(ctx.property().getText());
        currentContainer.peek().addChild(declaration);
        currentContainer.push(declaration);
    }

    @Override
    public void exitDeclaration(ICSSParser.DeclarationContext ctx) {
        currentContainer.pop();
    }

    // Bouwt de AST voor optellen en aftrekken.
    @Override
    public void enterExpression(ICSSParser.ExpressionContext ctx) {
        if (ctx.multiplication() == null) {
            return;
        }

        if (ctx.expression() != null) {
            Operation operation;

            if (ctx.PLUS() != null) {
                operation = new AddOperation();
            } else {
                operation = new SubtractOperation();
            }

            currentContainer.peek().addChild(operation);
            currentContainer.push(operation);
        }
    }

    @Override
    public void exitExpression(ICSSParser.ExpressionContext ctx) {
        if (ctx.expression() != null && ctx.multiplication() != null) {
            currentContainer.pop();
        }
    }

    // Bouwt de AST voor vermenigvuldigen.
    @Override
    public void enterMultiplication(ICSSParser.MultiplicationContext ctx) {
        if (ctx.multiplication() != null && ctx.MUL() != null) {
            Operation operation = new MultiplyOperation();

            currentContainer.peek().addChild(operation);
            currentContainer.push(operation);
        }
    }

    @Override
    public void exitMultiplication(ICSSParser.MultiplicationContext ctx) {
        if (ctx.multiplication() != null && ctx.MUL() != null) {
            currentContainer.pop();
        }
    }

    // Verwerkt literals, kleuren en variabelen.
    @Override
    public void enterFactor(ICSSParser.FactorContext ctx) {
        ASTNode node = null;

        if (ctx.literal() != null) {
            ICSSParser.LiteralContext literal = ctx.literal();

            if (literal.PIXELSIZE() != null) {
                node = new PixelLiteral(literal.PIXELSIZE().getText());
            } else if (literal.PERCENTAGE() != null) {
                node = new PercentageLiteral(literal.PERCENTAGE().getText());
            } else if (literal.SCALAR() != null) {
                node = new ScalarLiteral(literal.SCALAR().getText());
            } else if (literal.TRUE() != null) {
                node = new BoolLiteral(literal.TRUE().getText());
            } else if (literal.FALSE() != null) {
                node = new BoolLiteral(literal.FALSE().getText());
            }

        } else if (ctx.COLOR() != null) {
            node = new ColorLiteral(ctx.COLOR().getText());
        } else if (ctx.CAPITAL_IDENT() != null) {
            node = new VariableReference(ctx.CAPITAL_IDENT().getText());
        }

        if (node != null) {
            currentContainer.peek().addChild(node);
        }
    }
}
