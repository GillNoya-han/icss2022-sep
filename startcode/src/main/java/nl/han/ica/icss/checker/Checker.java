package nl.han.ica.icss.checker;

import nl.han.ica.icss.ast.*;
import nl.han.ica.icss.ast.literals.*;
import nl.han.ica.icss.ast.operations.*;
import nl.han.ica.datastructures.HANLinkedList;

public class Checker {

    // Houdt per variabele de naam en het type bij.
    private static class VariableInfo {
        String name;
        Class<?> type;

        VariableInfo(String name, Class<?> type) {
            this.name = name;
            this.type = type;
        }
    }

    // Bevat alle scopes. De meest recente (binnenste) scope staat vooraan.
    private final HANLinkedList<HANLinkedList<VariableInfo>> scopes =
            new HANLinkedList<>();

    // Start de semantische controle met een lege globale scope.
    public void check(AST ast) {
        scopes.clear();
        enterScope();
        checkNode(ast.root);
        leaveScope();
    }

    // Maakt een nieuwe scope aan.
    private void enterScope() {
        scopes.addFirst(new HANLinkedList<>());
    }

    // Verwijdert de scope waarin we klaar zijn met controleren.
    private void leaveScope() {
        scopes.removeFirst();
    }

    // Controleert een AST-node en verwerkt scopes waar nodig.
    private void checkNode(ASTNode node) {

        // Elke stylerule krijgt een eigen scope.
        if (node instanceof Stylerule) {
            enterScope();

            for (ASTNode child : node.getChildren()) {
                checkNode(child);
            }

            leaveScope();
            return;
        }

        // Controleert de conditie en de inhoud van een if-statement.
        if (node instanceof IfClause) {
            IfClause ifClause = (IfClause) node;

            checkExpression(ifClause.conditionalExpression);
            Class<?> type = getExpressionType(ifClause.conditionalExpression);

            // Een if-conditie moet een boolean opleveren.
            if (type != BoolLiteral.class) {
                node.setError("If-conditie moet een boolean zijn.");
            }

            // De if-body krijgt een eigen scope.
            enterScope();

            for (ASTNode child : node.getChildren()) {
                // De else-body wordt apart gecontroleerd.
                if (!(child instanceof ElseClause)) {
                    checkNode(child);
                }
            }

            leaveScope();

            // De else-body krijgt een eigen, onafhankelijke scope.
            for (ASTNode child : node.getChildren()) {
                if (child instanceof ElseClause) {
                    checkNode(child);
                }
            }

            return;
        }

        // De else-body krijgt een eigen scope.
        if (node instanceof ElseClause) {
            enterScope();

            for (ASTNode child : node.getChildren()) {
                checkNode(child);
            }

            leaveScope();
            return;
        }

        // Controleert een variabeletoekenning en slaat het type op.
        if (node instanceof VariableAssignment) {
            VariableAssignment assignment = (VariableAssignment) node;

            checkExpression(assignment.expression);

            Class<?> type = getExpressionType(assignment.expression);

            // Alleen variabelen met een geldig type worden opgeslagen.
            if (type != null) {
                addVariable(assignment.name.name, type);
            }

            return;
        }

        // Controleert een CSS-declaratie, inclusief het type van de waarde.
        if (node instanceof Declaration) {
            Declaration declaration = (Declaration) node;

            checkExpression(declaration.expression);
            checkPropertyType(declaration);
        }

        // Controleert eventuele onderliggende AST-nodes.
        for (ASTNode child : node.getChildren()) {
            checkNode(child);
        }
    }

    // Voegt een variabele toe aan de huidige scope.
    private void addVariable(String name, Class<?> type) {
        scopes.getFirst().addFirst(new VariableInfo(name, type));
    }

    // Zoekt een variabele op naam en controleert alle zichtbare scopes.
    private Class<?> getVariableType(String name) {
        for (int i = 0; i < scopes.getSize(); i++) {
            HANLinkedList<VariableInfo> scope = scopes.get(i);

            for (int j = 0; j < scope.getSize(); j++) {
                VariableInfo variable = scope.get(j);

                // De eerste gevonden variabele bepaalt het type.
                if (variable.name.equals(name)) {
                    return variable.type;
                }
            }
        }

        // De variabele bestaat niet in een zichtbare scope.
        return null;
    }

    // Controleert variabeleverwijzingen en rekenkundige expressies.
    private void checkExpression(Expression expression) {
        if (expression == null) {
            return;
        }

        // Controleert of een gebruikte variabele bestaat en zichtbaar is.
        if (expression instanceof VariableReference) {
            String name = ((VariableReference) expression).name;

            if (getVariableType(name) == null) {
                expression.setError("Variabele '" + name + "' bestaat niet of valt buiten de scope.");
            }

            return;
        }

        // Controleert de types van beide kanten van een berekening.
        if (expression instanceof Operation) {
            Operation operation = (Operation) expression;

            checkExpression(operation.lhs);
            checkExpression(operation.rhs);

            Class<?> left = getExpressionType(operation.lhs);
            Class<?> right = getExpressionType(operation.rhs);

            // Zonder bekende types kunnen we de berekening niet verder controleren.
            if (left == null || right == null) {
                return;
            }

            // Kleuren mogen niet worden gebruikt in berekeningen.
            if (left == ColorLiteral.class || right == ColorLiteral.class) {
                expression.setError("Kleuren mogen niet in berekeningen worden gebruikt.");
            }

            // Bij optellen en aftrekken moeten beide operands hetzelfde type hebben.
            if (operation instanceof AddOperation || operation instanceof SubtractOperation) {
                if (left != right) {
                    expression.setError("Optellen en aftrekken vereisen dezelfde types.");
                }
            }

            // Bij vermenigvuldigen moet minimaal één operand een Scalar zijn.
            if (operation instanceof MultiplyOperation) {
                if (left != ScalarLiteral.class && right != ScalarLiteral.class) {
                    expression.setError("Minimaal één operand moet een Scalar zijn.");
                }
            }
        }
    }

    // Bepaalt welk type een expressie oplevert.
    private Class<?> getExpressionType(Expression expression) {

        // Een literal heeft als type zijn eigen klasse.
        if (expression instanceof Literal) {
            return expression.getClass();
        }

        // Het type van een variabeleverwijzing wordt opgezocht in de scopes.
        if (expression instanceof VariableReference) {
            return getVariableType(((VariableReference) expression).name);
        }

        // Bepaalt het resultaattype van een berekening.
        if (expression instanceof Operation) {
            Operation operation = (Operation) expression;

            Class<?> left = getExpressionType(operation.lhs);
            Class<?> right = getExpressionType(operation.rhs);

            if (left == null || right == null) {
                return null;
            }

            // Kleuren hebben geen geldig resultaat als onderdeel van een berekening.
            if (left == ColorLiteral.class || right == ColorLiteral.class) {
                return null;
            }

            // Optellen en aftrekken leveren hetzelfde type op als hun operands.
            if (operation instanceof AddOperation || operation instanceof SubtractOperation) {
                if (left != right) {
                    return null;
                }

                return left;
            }

            // Vermenigvuldigen met een Scalar behoudt het type van de andere operand.
            if (operation instanceof MultiplyOperation) {
                if (left == ScalarLiteral.class) {
                    return right;
                }

                if (right == ScalarLiteral.class) {
                    return left;
                }

                return null;
            }
        }

        return null;
    }

    // Controleert of de waarde van een CSS-property het juiste type heeft.
    private void checkPropertyType(Declaration declaration) {
        String property = declaration.property.name;
        Class<?> type = getExpressionType(declaration.expression);

        // Width en height accepteren alleen pixels of percentages.
        if (property.equals("width") || property.equals("height")) {
            if (type != PixelLiteral.class && type != PercentageLiteral.class) {
                declaration.setError(property + " moet een pixel- of percentagewaarde hebben.");
            }
        }

        // Color en background-color accepteren alleen kleurwaarden.
        if (property.equals("color") || property.equals("background-color")) {
            if (type != ColorLiteral.class) {
                declaration.setError(property + " moet een kleur hebben.");
            }
        }
    }
}