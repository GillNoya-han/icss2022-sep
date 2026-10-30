
package nl.han.ica.icss.generator;

import nl.han.ica.icss.ast.*;
import nl.han.ica.icss.ast.literals.*;
import nl.han.ica.icss.ast.operations.*;
import nl.han.ica.icss.ast.selectors.*;

public class Generator {

	public String generate(AST ast) {
		StringBuilder css = new StringBuilder();

		// Genereer CSS voor iedere stylerule.
		for (ASTNode node : ast.root.getChildren()) {
			if (node instanceof Stylerule) {
				generateRule((Stylerule) node, css);
			}
		}

		return css.toString();
	}

	private void generateRule(Stylerule rule, StringBuilder css) {
		// Schrijf de selectors.
		for (int i = 0; i < rule.selectors.size(); i++) {
			if (i > 0) {
				css.append(", ");
			}
			css.append(generateSelector(rule.selectors.get(i)));
		}

		css.append(" {\n");

		// Schrijf alle declarations in de regel.
		for (ASTNode node : rule.body) {
			if (node instanceof Declaration) {
				Declaration declaration = (Declaration) node;

				css.append("    ")
						.append(declaration.property.name)
						.append(": ")
						.append(generateExpression(declaration.expression))
						.append(";\n");
			}
		}

		css.append("}\n\n");
	}

	private String generateSelector(Selector selector) {
		// Gebruik de echte CSS-selector, niet het AST-label.
		if (selector instanceof TagSelector) {
			return selector.getNodeLabel().replace("TagSelector ", "");
		}
		if (selector instanceof IdSelector) {
			return selector.getNodeLabel().replace("IdSelector ", "");
		}
		if (selector instanceof ClassSelector) {
			return selector.getNodeLabel().replace("ClassSelector ", "");
		}

		return selector.getNodeLabel();
	}

	private String generateExpression(Expression expression) {
		if (expression == null) {
			return "";
		}

		// Zet literals om naar geldige CSS-waarden.
		if (expression instanceof PixelLiteral) {
			return ((PixelLiteral) expression).value + "px";
		}
		if (expression instanceof PercentageLiteral) {
			return ((PercentageLiteral) expression).value + "%";
		}
		if (expression instanceof ScalarLiteral) {
			return String.valueOf(((ScalarLiteral) expression).value);
		}
		if (expression instanceof ColorLiteral) {
			return expression.getNodeLabel()
					.replace("Color literal (", "")
					.replace(")", "");
		}
		if (expression instanceof BoolLiteral) {
			return expression.getNodeLabel()
					.replace("Bool Literal (", "")
					.replace(")", "");
		}
		if (expression instanceof VariableReference) {
			return ((VariableReference) expression).name;
		}

		// Genereer rekenkundige expressies.
		if (expression instanceof Operation) {
			Operation operation = (Operation) expression;

			String operator;
			if (operation instanceof AddOperation) {
				operator = " + ";
			} else if (operation instanceof SubtractOperation) {
				operator = " - ";
			} else if (operation instanceof MultiplyOperation) {
				operator = " * ";
			} else {
				return "";
			}

			return generateExpression(operation.lhs)
					+ operator
					+ generateExpression(operation.rhs);
		}

		return expression.getNodeLabel();
	}
}
