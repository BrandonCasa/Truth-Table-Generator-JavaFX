package com.example.truthtablegeneratorjavafx.model;

import com.example.truthtablegeneratorjavafx.model.Exceptions.DoubleOperandException;
import com.example.truthtablegeneratorjavafx.model.Exceptions.DoubleOperatorException;
import com.example.truthtablegeneratorjavafx.model.Exceptions.TorFException;
import com.example.truthtablegeneratorjavafx.model.Exceptions.UnknownTokenException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import javafx.application.Application;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

import com.opencsv.*;

public class ModelImpl implements Model {


    private Application application;

    /**
     * boolGrid stores all combinations of true/false for a given formula, formulaGrid stores each formula in RPN
     * (with the answer for each formula as the last element in every row) and truthTable is a grid that is the closest
     * approximation of a truth table; that is, each row is a combination of true and false, while the last element is
     * the resulting boolean value for that combination.  truthTable omits operands.
     **/
    private boolean[][] boolGrid;
    private ArrayList<String>[] formulaGrid;
    private ArrayList<String>[] truthTable;

    /**
     * variables is an arraylist of Strings containing the variables entered by the user.
     **/
    private ArrayList<String> variables;
    /**
     * formula string entered by the user
     **/
    private String formula;


    // this variable shouldn't exist.
    private String uneditedFormula;

    private String errorToken;
    private String errorMessage;

    ArrayList<String> operators = new ArrayList<String>(Arrays.asList(
    ));

    ArrayList<String> potentialChars;


    private ArrayList<ModelObserver> observers;

    public ModelImpl(Application application) {
        this.application = application;

        boolGrid = new boolean[0][0];
        formulaGrid = new ArrayList[0];
        truthTable = new ArrayList[0];
        variables = new ArrayList<>();
        formula = new String();
        uneditedFormula = new String();
        errorToken = new String();
        errorMessage = new String();
        observers = new ArrayList<>();

        potentialChars = new ArrayList<>(Arrays.asList(
                "-", "<", ">", "=", "\\", "/", "!", "~", "^", "x", "o", "r"
        ));

        operators = new ArrayList<>(Arrays.asList(
                "->", "=>", "/\\", "\\/", "<->", "!", "~", "^", "xor", "XOR"
        ));
    }
    public ArrayList<String> parse(String formula) throws UnknownTokenException, TorFException, DoubleOperatorException, DoubleOperandException {

        formula = formula.replace(" ", "");
        this.formula = formula;
        ArrayList<String> parsedTokens = new ArrayList<>(); //the actual list being returned by this function
        String stringBuilder = ""; //temp string used to build tokens that are mutliple characters long (e.g. "/\")
        Stack<String> parenStack = new Stack<>(); // stack used to validate parentheses


        for (int i = 0; i < formula.length(); i++) {
            String curr = formula.substring(i, i + 1);

            if (ShuntingYard.isOperand(curr)) {

                /* if the previous character is also an operand, throw an exception
                    e.g. 'pp', "qp"
                 */
                if (parsedTokens.size() > 0 && ShuntingYard.isOperand(parsedTokens.get(parsedTokens.size() - 1))) { // gets the previous token if there is one and ensures it is not an operand
                    throw new DoubleOperandException();
                }
                /* if we have been 'building' an operator and are interrupted, then throw an exception.
                    e.g. /p
                 */
                else if (!stringBuilder.equals("")) {
                    errorToken = stringBuilder;
                    throw new UnknownTokenException();
                }

                parsedTokens.add(curr);
            } else if (ShuntingYard.isLeftParen(curr)) {
                parsedTokens.add(curr);
                parenStack.push(curr);
            } else if (ShuntingYard.isRightParen(curr)) {
                String popped = "";
                // if this throws an EmptyStackException, it's caught by main(), as desired
                popped = parenStack.pop();
                if (!popped.equals("(")) {
                    // parenthesis error, which will be treated the same in main either way.
                    errorToken = curr;
                    throw new EmptyStackException();
                }
                parsedTokens.add(curr);
            } else { // is part of an operator
                /* the current char doesn't exist as either an operand or part of
                    a potential operator, and therefore the input is invalid (unknown token)
                 */
                if (!potentialChars.contains(curr)) {
                    errorToken = curr;
                    throw new UnknownTokenException();
                } else if (curr.equals("f") || curr.equals("F") || curr.equals("t") || curr.equals("T")) {
                    throw new TorFException();
                }
                // if the previous token is also an operator, throw an exception.
                else if (parsedTokens.size() > 0 && ShuntingYard.isOperator(parsedTokens.get(parsedTokens.size() - 1))) {
                    throw new DoubleOperatorException();
                }

                stringBuilder += curr;

                if (operators.contains(stringBuilder)) {
                    parsedTokens.add(stringBuilder);
                    stringBuilder = "";
                }
            }
        }

        if (parenStack.size() > 0) {
            // parenthesis error, which will be treated the same in main either way.
            errorToken = parenStack.peek();
            throw new EmptyStackException();
        }

        return parsedTokens;
    }

    public void main(String formula) {

        // program bugs out if it's an empty string, so we just don't do anything if that's the case.
        if (formula.equals("")){
            return;
        }

        uneditedFormula = formula;

        ArrayList<String> infixTokens = new ArrayList<>();
        boolean error = false;
        try {
            infixTokens = parse(formula);
        } catch (EmptyStackException e) {
            error = true;
            errorMessage = "Unmatched parenthesis '" + errorToken + "'.";
        } catch (UnknownTokenException e) {
            error = true;
            errorMessage = "Unknown character '" + errorToken + "'";
        } catch (DoubleOperandException e) {
            error = true;
            errorMessage = "Expecting an operator.";
        } catch (DoubleOperatorException e) {
            error = true;
            errorMessage = "Expecting an operand.";
        } catch (TorFException e) {
            error = true;
            errorMessage = "Found '" + errorToken + "'. Do not enter T or F as these are reserved.";
        }

        // only do this if there wasn't an error in parsing
        if (!error) {
            errorMessage = "";
            int numOperands = countOperands(infixTokens);
            variables = getOperands(infixTokens);

            ArrayList<String> rpnTokens = ShuntingYard.shunt(infixTokens);

            boolGrid = generateRows(numOperands);

            //Generating the 2d array of strings containing the each formula with their true/false combinations in RPN (i.e. generating formulaGrid)...
            formulaGrid = new ArrayList[boolGrid.length];

            //TODO: within this double-for loop, add the answer to each rowFormula to the end of the rowFormula List
            for (int row = 0; row < boolGrid.length; row++) {
                int col = 0;
                //visitedOperands can be made more efficient with a hashmap (index represents index in row, key represents operand)
                ArrayList<String> visitedOperands = new ArrayList<>();
                ArrayList<String> rowFormula = new ArrayList<>();
                for (int i = 0; i < rpnTokens.size(); i++) {
                    String token = rpnTokens.get(i);
                    if (ShuntingYard.isOperand(token)) {
                        if (!visitedOperands.contains(token)) {
                            rowFormula.add(Boolean.toString(boolGrid[row][col]));
                            visitedOperands.add(token);
                            col++;
                        } else {
                            int retrieve = visitedOperands.indexOf(token);
                            rowFormula.add(Boolean.toString(boolGrid[row][retrieve]));
                        }
                    } else {
                        rowFormula.add(token);
                    }
                }

                boolean rowAnswer = ShuntingYard.calculate(rowFormula);

                rowFormula.add(Boolean.toString(rowAnswer));

                formulaGrid[row] = rowFormula;
            }

            //generating truthTable, the 2d matrix of strings containing each combination of true and false and the answer for each combination in the last column...

            truthTable = new ArrayList[boolGrid.length];

            for (int row = 0; row < boolGrid.length; row++) {

                truthTable[row] = new ArrayList<>();

                for (int col = 0; col < boolGrid[0].length; col++) {
                    truthTable[row].add(Boolean.toString(boolGrid[row][col]));
                }

                String value = formulaGrid[row].get(formulaGrid[row].size() - 1); //gets the last element in the corresponding row of the formula grid, which is the actual value of the expression using that row's combination
                truthTable[row].add(value);
            }
        }
        notifyObservers();
    }

    public boolean[][] generateRows(int numOperands) {
        boolean[][] grid = new boolean[(int) Math.pow(2, numOperands)][numOperands];

        //each iteration of the for loop builds 1 row of the truth table
        for (int i = 0; i < Math.pow(2, numOperands); i++) {
            String bin = Integer.toBinaryString(i);
            while (bin.length() < numOperands) {
                bin = "0" + bin;
            }
            for (int j = 0; j < bin.length(); j++) {
                if (bin.charAt(j) == '1') {
                    grid[i][j] = true;
                }
            }
        }
        return grid;
    }


    public int countOperands(ArrayList<String> tokens) {
        ArrayList<String> uniqueOps = new ArrayList<>();
        for (int i = 0; i < tokens.size(); i++) {
            String token = tokens.get(i);
            if (!uniqueOps.contains(token) && ShuntingYard.isOperand(token)) { //if the token is an operand and it hasn't already been counted
                uniqueOps.add(token);
            }
        }
        return uniqueOps.size();
    }

    private ArrayList<String> getOperands(ArrayList<String> tokens) {
        ArrayList<String> uniqueOps = new ArrayList<>();
        for (int i = 0; i < tokens.size(); i++) {
            String token = tokens.get(i);
            if (!uniqueOps.contains(token) && ShuntingYard.isOperand(token)) { //if the token is an operand and it hasn't already been counted
                uniqueOps.add(token);
            }
        }
        return uniqueOps;
    }

    public ArrayList<String>[] getTruthTable() {
        return truthTable;
    }

    @Override
    public ArrayList<String> getVariables() {
        return variables;
    }

    @Override
    public String getFormula() {
        return formula;
    }

    @Override
    public String getUneditedFormula() {
        return uneditedFormula;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public void notifyObservers() {
        for (int i = 0; i < observers.size(); i++) {
            observers.get(i).update(this);
        }
    }

    @Override
    public void addObserver(ModelObserver observer) {
        observers.add(observer);
    }

    // save the truth table as a csv file
    @Override
    public void export(File location){
        if (location == null){
            return;
        }

        String path = location.toString() + ".csv";
        List<String[]> theRows = new ArrayList<>();
        CSVWriter writer = null;
        try {
            writer = new CSVWriter(new FileWriter(path));

            for (int row = 0; row < truthTable.length; row++){
                String[] currRow = new String[truthTable[0].size()];
                for (int col = 0; col < truthTable[0].size(); col++){
                    currRow[col] = truthTable[row].get(col);
                }
                theRows.add(currRow);
            }

            writer.writeAll(theRows);
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
    public void copy(){
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();

        String copied = "";
        for (int row = 0; row < truthTable.length; row++) {
            for (int col = 0; col < truthTable[0].size(); col++) {
                copied += truthTable[row].get(col);
                if (col != truthTable[0].size() - 1) {
                    copied += "\t";
                }
            }

            if (row != truthTable.length - 1) {
                copied += "\n";
            }
        }
        content.putString(copied);
        clipboard.setContent(content);
    }

    public void openGitHub(){
        application.getHostServices().showDocument("https://github.com/lstach/Truth-Table-Generator-JavaFX");
    }
}