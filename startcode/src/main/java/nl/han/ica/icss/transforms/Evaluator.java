
package nl.han.ica.icss.transforms;

import nl.han.ica.icss.ast.*;
import nl.han.ica.icss.ast.literals.*;
import nl.han.ica.icss.ast.operations.*;

import java.util.HashMap;
import java.util.Map;

public class Evaluator implements Transform {
    private final Map<String, Literal> variableValues = new HashMap<>();

    @Override
    public void apply(AST ast) {
        variableValues.clear();
        collectVariables(ast.root);
        evaluateTree(ast.root);
    }

    private void collectVariables(ASTNode node) {
        // Bereken de waarde van een variabele en sla deze op.
        if (node instanceof VariableAssignment) {
            VariableAssignment assignment = (VariableAssignment) node;

            Literal value = evaluate(assignment.expression);
            if (value != null) {
                variableValues.put(assignment.name.name, value);
            }
        }

        // Doorloopt ook alle onderliggende AST-nodes.
        for (ASTNode child : node.getChildren()) {
            collectVariables(child);
        }
    }

    private void evaluateTree(ASTNode node) {
        // Vervangt de expressie van een declaration door de berekende waarde.
        if (node instanceof Declaration) {
            Declaration declaration = (Declaration) node;
            declaration.expression = evaluate(declaration.expression);
        }

        // Evalueert ook de onderliggende nodes.
        for (ASTNode child : node.getChildren()) {
            evaluateTree(child);
        }
    }

    private Literal evaluate(Expression expression) {
        // Een ontbrekende expressie heeft geen waarde.
        if (expression == null) {
            return null;
        }

        // Een literal heeft al een concrete waarde.
        if (expression instanceof Literal) {
            return (Literal) expression;
        }

        // Haalt de opgeslagen waarde van een variabele op.
        if (expression instanceof VariableReference) {
            VariableReference reference = (VariableReference) expression;
            return variableValues.get(reference.name);
        }

        // Berekent een rekenkundige bewerking.
        if (expression instanceof Operation) {
            Operation operation = (Operation) expression;

            Literal left = evaluate(operation.lhs);
            Literal right = evaluate(operation.rhs);

            // Voert de bewerking uit als beide waarden scalars zijn.
            if (left instanceof ScalarLiteral && right instanceof ScalarLiteral) {
                int lhs = ((ScalarLiteral) left).value;
                int rhs = ((ScalarLiteral) right).value;

                if (operation instanceof AddOperation) {
                    return new ScalarLiteral(lhs + rhs);
                }
                if (operation instanceof SubtractOperation) {
                    return new ScalarLiteral(lhs - rhs);
                }
                if (operation instanceof MultiplyOperation) {
                    return new ScalarLiteral(lhs * rhs);
                }
            }

            // Scalar * Pixel = Pixel
            if (operation instanceof MultiplyOperation) {
                if (left instanceof ScalarLiteral && right instanceof PixelLiteral) {
                    int scalar = ((ScalarLiteral) left).value;
                    int pixels = ((PixelLiteral) right).value;
                    return new PixelLiteral(scalar * pixels);
                }

                if (left instanceof PixelLiteral && right instanceof ScalarLiteral) {
                    int pixels = ((PixelLiteral) left).value;
                    int scalar = ((ScalarLiteral) right).value;
                    return new PixelLiteral(pixels * scalar);
                }
            }

            // Pixel + Pixel = Pixel
            if (left instanceof PixelLiteral && right instanceof PixelLiteral) {
                int lhs = ((PixelLiteral) left).value;
                int rhs = ((PixelLiteral) right).value;

                if (operation instanceof AddOperation) {
                    return new PixelLiteral(lhs + rhs);
                }

                if (operation instanceof SubtractOperation) {
                    return new PixelLiteral(lhs - rhs);
                }
            }
        }
        // Geeft null terug als de expressie niet verwerkt kan worden.
        return null;
    }
}
