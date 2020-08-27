package sqlancer.mysql.oracle;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import sqlancer.Randomly;
import sqlancer.common.oracle.PivotedQuerySynthesisBase;
import sqlancer.common.query.Query;
import sqlancer.common.query.QueryAdapter;
import sqlancer.mysql.MySQLErrors;
import sqlancer.mysql.MySQLGlobalState;
import sqlancer.mysql.MySQLSchema.MySQLColumn;
import sqlancer.mysql.MySQLSchema.MySQLRowValue;
import sqlancer.mysql.MySQLSchema.MySQLTable;
import sqlancer.mysql.MySQLSchema.MySQLTables;
import sqlancer.mysql.MySQLToStringVisitor;
import sqlancer.mysql.MySQLVisitor;
import sqlancer.mysql.ast.MySQLColumnReference;
import sqlancer.mysql.ast.MySQLConstant;
import sqlancer.mysql.ast.MySQLExpression;
import sqlancer.mysql.ast.MySQLSelect;
import sqlancer.mysql.ast.MySQLTableReference;
import sqlancer.mysql.ast.MySQLUnaryPostfixOperation;
import sqlancer.mysql.ast.MySQLUnaryPostfixOperation.UnaryPostfixOperator;
import sqlancer.mysql.ast.MySQLUnaryPrefixOperation;
import sqlancer.mysql.ast.MySQLUnaryPrefixOperation.MySQLUnaryPrefixOperator;
import sqlancer.mysql.gen.MySQLExpressionGenerator;

public class MySQLPivotedQuerySynthesisOracle
        extends PivotedQuerySynthesisBase<MySQLGlobalState, MySQLRowValue, MySQLExpression> {

    private List<MySQLExpression> fetchColumns;
    private List<MySQLColumn> columns;

    public MySQLPivotedQuerySynthesisOracle(MySQLGlobalState globalState) throws SQLException {
        super(globalState);
        MySQLErrors.addExpressionErrors(errors);
        errors.add("in 'order clause'"); // e.g., Unknown column '2067708013' in 'order clause'
    }

    @Override
    public Query getQueryThatContainsAtLeastOneRow() throws SQLException {
        MySQLTables randomFromTables = globalState.getSchema().getRandomTableNonEmptyTables();
        List<MySQLTable> tables = randomFromTables.getTables();

        MySQLSelect selectStatement = new MySQLSelect();
        selectStatement.setSelectType(Randomly.fromOptions(MySQLSelect.SelectType.values()));
        columns = randomFromTables.getColumns();
        pivotRow = randomFromTables.getRandomRowValue(globalState.getConnection());

        selectStatement.setFromList(tables.stream().map(t -> new MySQLTableReference(t)).collect(Collectors.toList()));

        fetchColumns = columns.stream().map(c -> new MySQLColumnReference(c, null)).collect(Collectors.toList());
        selectStatement.setFetchColumns(fetchColumns);
        MySQLExpression whereClause = generateWhereClauseThatContainsRowValue(columns, pivotRow);
        selectStatement.setWhereClause(whereClause);
        List<MySQLExpression> groupByClause = generateGroupByClause(columns, pivotRow);
        selectStatement.setGroupByExpressions(groupByClause);
        MySQLExpression limitClause = generateLimit();
        selectStatement.setLimitClause(limitClause);
        if (limitClause != null) {
            MySQLExpression offsetClause = generateOffset();
            selectStatement.setOffsetClause(offsetClause);
        }
        List<String> modifiers = Randomly.subset("STRAIGHT_JOIN", "SQL_SMALL_RESULT", "SQL_BIG_RESULT", "SQL_NO_CACHE"); // "SQL_BUFFER_RESULT",
                                                                                                                         // "SQL_CALC_FOUND_ROWS",
                                                                                                                         // "HIGH_PRIORITY"
        // TODO: Incorrect usage/placement of 'SQL_BUFFER_RESULT'
        selectStatement.setModifiers(modifiers);
        List<MySQLExpression> orderBy = new MySQLExpressionGenerator(globalState).setColumns(columns)
                .generateOrderBys();
        selectStatement.setOrderByExpressions(orderBy);

        MySQLToStringVisitor visitor = new MySQLToStringVisitor();
        visitor.visit(selectStatement);
        return new QueryAdapter(visitor.get(), errors);
    }

    private List<MySQLExpression> generateGroupByClause(List<MySQLColumn> columns, MySQLRowValue rw) {
        if (Randomly.getBoolean()) {
            return columns.stream().map(c -> MySQLColumnReference.create(c, rw.getValues().get(c)))
                    .collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    private MySQLConstant generateLimit() {
        if (Randomly.getBoolean()) {
            return MySQLConstant.createIntConstant(Integer.MAX_VALUE);
        } else {
            return null;
        }
    }

    private MySQLExpression generateOffset() {
        if (Randomly.getBoolean()) {
            // OFFSET 0
            return MySQLConstant.createIntConstantNotAsBoolean(0);
        } else {
            return null;
        }
    }

    private MySQLExpression generateWhereClauseThatContainsRowValue(List<MySQLColumn> columns, MySQLRowValue rw) {
        MySQLExpression expression = new MySQLExpressionGenerator(globalState).setRowVal(rw).setColumns(columns)
                .generateExpression();
        MySQLConstant expectedValue = expression.getExpectedValue();
        MySQLExpression result;
        if (expectedValue.isNull()) {
            result = new MySQLUnaryPostfixOperation(expression, UnaryPostfixOperator.IS_NULL, false);
        } else if (expectedValue.asBooleanNotNull()) {
            result = expression;
        } else {
            result = new MySQLUnaryPrefixOperation(expression, MySQLUnaryPrefixOperator.NOT);
        }
        rectifiedPredicates.add(result);
        return result;
    }

    @Override
    protected Query getContainedInQuery(Query query) throws SQLException {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT * FROM ("); // ANOTHER SELECT TO USE ORDER BY without restrictions
        if (query.getQueryString().endsWith(";")) {
            sb.append(query.getQueryString().substring(0, query.getQueryString().length() - 1));
        } else {
            sb.append(query.getQueryString());
        }
        sb.append(") as result WHERE ");
        int i = 0;
        for (MySQLColumn c : columns) {
            if (i++ != 0) {
                sb.append(" AND ");
            }
            sb.append("result.");
            sb.append("ref");
            sb.append(i - 1);
            if (pivotRow.getValues().get(c).isNull()) {
                sb.append(" IS NULL");
            } else {
                sb.append(" = ");
                sb.append(pivotRow.getValues().get(c).getTextRepresentation());
            }
        }

        String resultingQueryString = sb.toString();
        return new QueryAdapter(resultingQueryString, query.getExpectedErrors());
    }

    @Override
    protected String asString(MySQLExpression expr) {
        return MySQLVisitor.asExpectedValues(expr);
    }
}
