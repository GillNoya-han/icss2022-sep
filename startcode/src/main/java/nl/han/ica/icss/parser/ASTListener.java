
package nl.han.ica.icss.parser;

import nl.han.ica.datastructures.HANStack;
import nl.han.ica.datastructures.IHANStack;
import nl.han.ica.icss.ast.*;
import nl.han.ica.icss.ast.literals.*;
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
		assignment.addChild(
				new VariableReference(ctx.CAPITAL_IDENT().getText())
		);

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
		// Verwijder alleen de huidige regel van de stack.
		// De regel is bij enterCssRule al aan de ouder toegevoegd.
		currentContainer.pop();
	}

	private ASTNode createSelector(ICSSParser.SelectorContext ctx) {
		if (ctx.ID_IDENT() != null) {
			return new IdSelector(ctx.ID_IDENT().getText());
		}
		if (ctx.CLASS_IDENT() != null) {
			return new ClassSelector(ctx.CLASS_IDENT().getText());
		}
		if (ctx.LOWER_IDENT() != null) {
			return new TagSelector(ctx.LOWER_IDENT().getText());
		}
		throw new IllegalArgumentException(
				"Onbekend selectortype: " + ctx.getText()
		);
	}

	@Override
	public void enterValue(ICSSParser.ValueContext ctx) {
		ASTNode node = null;

		if (ctx.COLOR() != null) {
			node = new ColorLiteral(ctx.COLOR().getText());
		} else if (ctx.literal() != null) {
			ICSSParser.LiteralContext literal = ctx.literal();

			if (literal.PIXELSIZE() != null) {
				node = new PixelLiteral(literal.PIXELSIZE().getText());
			} else if (literal.PERCENTAGE() != null) {
				node = new PercentageLiteral(literal.PERCENTAGE().getText());
			}else if (literal.TRUE() != null) {
				node = new BoolLiteral(literal.TRUE().getText());

			} else if (literal.FALSE() != null) {
				node = new BoolLiteral(literal.FALSE().getText());
			}
		} else if (ctx.CAPITAL_IDENT() != null) {
			node = new VariableReference(ctx.CAPITAL_IDENT().getText());
		}

		if (node != null) {
			currentContainer.peek().addChild(node);
		}
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
}
