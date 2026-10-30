
package nl.han.ica.icss.transforms;

import nl.han.ica.icss.ast.*;
import nl.han.ica.icss.ast.literals.*;
import nl.han.ica.icss.ast.operations.*;
import nl.han.ica.datastructures.HANLinkedList;

import java.util.ArrayList;

public class Evaluator implements Transform {

    // Bewaart de namen en waarden van variabelen.
    private final HANLinkedList<VariableValue> variables = new HANLinkedList<>();

    // Koppelt een variabelenaam aan de bijbehorende waarde.
    private static class VariableValue {
        String name;
        Literal value;

        VariableValue(String name, Literal value) {
            this.name = name;
            this.value = value;
        }
    }

    @Override
    public void apply(AST ast) {
        // Begin met een lege lijst met variabelen.
        variables.clear();

        // Verwerk de AST.
        evaluateBody(ast.root.getChildren());
    }

    // Slaat een variabele op of werkt de bestaande waarde bij.
    private void setVariable(String name, Literal value) {
        for (int i = 0; i < variables.getSize(); i++) {
            VariableValue variable = variables.get(i);

            if (variable.name.equals(name)) {
                variable.value = value;
                return;
            }
        }

        variables.addFirst(new VariableValue(name, value));
    }

    // Zoekt de waarde van een variabele op.
    private Literal getVariable(String name) {
        for (int i = 0; i < variables.getSize(); i++) {
            VariableValue variable = variables.get(i);

            if (variable.name.equals(name)) {
                return variable.value;
            }
        }

        return null;
    }

    // Verwerkt alle onderdelen van een AST-body.
    private void evaluateBody(ArrayList<ASTNode> body) {
        int i = 0;

        while (i < body.size()) {
            ASTNode node = body.get(i);

            // Evalueer een variabele en sla de waarde op.
            if (node instanceof VariableAssignment) {
                VariableAssignment assignment = (VariableAssignment) node;
                Literal value = evaluate(assignment.expression);

                if (value != null) {
                    setVariable(assignment.name.name, value);
                }
                i++;
            }

            // Vervang de expressie van een declaratie door de uitkomst.
            else if (node instanceof Declaration) {
                Declaration declaration = (Declaration) node;
                declaration.expression = evaluate(declaration.expression);
                i++;
            }

            // Controleer welke tak van de if-statement uitgevoerd moet worden.
            else if (node instanceof IfClause) {
                IfClause ifClause = (IfClause) node;
                Literal condition = evaluate(ifClause.conditionalExpression);
                ArrayList<ASTNode> selectedBody = null;

                if (condition instanceof BoolLiteral) {
                    if (((BoolLiteral) condition).value) {
                        selectedBody = ifClause.body;
                    } else if (ifClause.elseClause != null) {
                        selectedBody = ifClause.elseClause.body;
                    }
                }

                // Verwijder de if-clause uit de AST.
                body.remove(i);

                // Voeg alleen de gekozen tak toe.
                if (selectedBody != null) {
                    evaluateBody(selectedBody);
                    body.addAll(i, selectedBody);
                    i += selectedBody.size();
                }
            }

            // Verwerk ook de inhoud van een CSS-regel.
            else if (node instanceof Stylerule) {
                Stylerule rule = (Stylerule) node;
                evaluateBody(rule.body);
                i++;
            } else {
                i++;
            }
        }
    }

    // Berekent een expressie en geeft het resultaat terug als Literal.
    private Literal evaluate(Expression expression) {
        if (expression == null) {
            return null;
        }

        // Een literal hoeft niet berekend te worden.
        if (expression instanceof Literal) {
            return (Literal) expression;
        }

        // Haal de waarde van een variabele op.
        if (expression instanceof VariableReference) {
            VariableReference reference = (VariableReference) expression;
            return getVariable(reference.name);
        }

        // Bereken een rekenkundige operatie.
        if (expression instanceof Operation) {
            Operation operation = (Operation) expression;

            Literal left = evaluate(operation.lhs);
            Literal right = evaluate(operation.rhs);

            if (left == null || right == null) {
                return null;
            }

            // Berekeningen met twee scalars.
            if (left instanceof ScalarLiteral &&
                    right instanceof ScalarLiteral) {

                int l = ((ScalarLiteral) left).value;
                int r = ((ScalarLiteral) right).value;

                if (operation instanceof AddOperation) {
                    return new ScalarLiteral(l + r);
                }

                if (operation instanceof SubtractOperation) {
                    return new ScalarLiteral(l - r);
                }

                if (operation instanceof MultiplyOperation) {
                    return new ScalarLiteral(l * r);
                }
            }

            // Optellen en aftrekken van pixels.
            if (left instanceof PixelLiteral && right instanceof PixelLiteral) {

                int l = ((PixelLiteral) left).value;
                int r = ((PixelLiteral) right).value;

                if (operation instanceof AddOperation) {
                    return new PixelLiteral(l + r);
                }

                if (operation instanceof SubtractOperation) {
                    return new PixelLiteral(l - r);
                }
            }

            // Vermenigvuldigen van pixels met scalars.
            if (operation instanceof MultiplyOperation) {
                if (left instanceof PixelLiteral && right instanceof ScalarLiteral) {
                    return new PixelLiteral(((PixelLiteral) left).value * ((ScalarLiteral) right).value);
                }

                if (left instanceof ScalarLiteral && right instanceof PixelLiteral) {
                    return new PixelLiteral(((ScalarLiteral) left).value * ((PixelLiteral) right).value);
                }
            }
        }

        return null;
    }
}
