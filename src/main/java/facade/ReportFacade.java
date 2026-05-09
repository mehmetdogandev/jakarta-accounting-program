package facade;

import facadeLocal.ReportFacadeLocal;
import jakarta.ejb.Stateless;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Stateless
public class ReportFacade extends AbstractFacade implements ReportFacadeLocal {

    @Override
    public List<Map<String, Object>> getTrialBalance(LocalDate asOf) {
        @SuppressWarnings("unchecked")
        List<Object> rows = entityManager.createNativeQuery(
                        "SELECT l.account_code, l.account_name, "
                                + "COALESCE(SUM(l.debit), 0) AS total_debit, "
                                + "COALESCE(SUM(l.credit), 0) AS total_credit, "
                                + "COALESCE(SUM(l.debit - l.credit), 0) AS balance "
                                + "FROM journal_entry_line l "
                                + "JOIN journal_entry j ON j.id = l.journal_entry_id "
                                + "WHERE j.status = 'POSTED' "
                                + "AND j.deleted_at IS NULL "
                                + "AND j.entry_date <= ? "
                                + "GROUP BY l.account_code, l.account_name "
                                + "ORDER BY l.account_code")
                .setParameter(1, Date.valueOf(asOf))
                .getResultList();
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object rowObj : rows) {
            Object[] r = (Object[]) rowObj;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("accountCode", r[0]);
            m.put("accountName", r[1]);
            m.put("totalDebit", asBigDecimal(r[2]));
            m.put("totalCredit", asBigDecimal(r[3]));
            m.put("balance", asBigDecimal(r[4]));
            out.add(m);
        }
        return out;
    }

    @Override
    public Map<String, Object> getProfitLoss(LocalDate from, LocalDate to) {
        Map<String, Object> out = new LinkedHashMap<>();

        BigDecimal otherIncome = singleAmount(
                "SELECT COALESCE(SUM(e.amount), 0) "
                        + "FROM expense e "
                        + "WHERE e.deleted_at IS NULL "
                        + "AND e.status = 'APPROVED' "
                        + "AND e.transaction_type = 'INCOME' "
                        + "AND e.transaction_date BETWEEN ? AND ?",
                from, to);
        BigDecimal expenses = singleAmount(
                "SELECT COALESCE(SUM(e.amount), 0) "
                        + "FROM expense e "
                        + "WHERE e.deleted_at IS NULL "
                        + "AND e.status = 'APPROVED' "
                        + "AND e.transaction_type = 'EXPENSE' "
                        + "AND e.transaction_date BETWEEN ? AND ?",
                from, to);
        BigDecimal salesRevenue = hasInvoiceTable()
                ? singleAmount(
                "SELECT COALESCE(SUM(i.grand_total), 0) "
                        + "FROM invoice i "
                        + "WHERE i.deleted_at IS NULL "
                        + "AND i.status = 'POSTED' "
                        + "AND i.invoice_type = 'SALES' "
                        + "AND i.invoice_date BETWEEN ? AND ?",
                from, to)
                : BigDecimal.ZERO;
        BigDecimal cogs = singleAmount(
                "SELECT COALESCE(SUM(sm.quantity * sm.unit_cost), 0) "
                        + "FROM stock_movement sm "
                        + "WHERE sm.movement_type = 'OUT' "
                        + "AND DATE(sm.movement_date) BETWEEN ? AND ?",
                from, to);

        BigDecimal grossProfit = salesRevenue.subtract(cogs);
        BigDecimal netProfit = grossProfit.add(otherIncome).subtract(expenses);

        out.put("salesRevenue", salesRevenue);
        out.put("costOfGoodsSold", cogs);
        out.put("grossProfit", grossProfit);
        out.put("otherIncome", otherIncome);
        out.put("expenses", expenses);
        out.put("netProfit", netProfit);
        return out;
    }

    @Override
    public Map<String, Object> getCashFlowSummary(LocalDate from, LocalDate to) {
        Map<String, Object> out = new LinkedHashMap<>();
        BigDecimal cashIn = singleAmount(
                "SELECT COALESCE(SUM(ct.amount), 0) "
                        + "FROM cash_transaction ct "
                        + "WHERE ct.transaction_type = 'IN' "
                        + "AND ct.transaction_date BETWEEN ? AND ?",
                from, to);
        BigDecimal cashOut = singleAmount(
                "SELECT COALESCE(SUM(ct.amount), 0) "
                        + "FROM cash_transaction ct "
                        + "WHERE ct.transaction_type = 'OUT' "
                        + "AND ct.transaction_date BETWEEN ? AND ?",
                from, to);
        BigDecimal bankIn = singleAmount(
                "SELECT COALESCE(SUM(bt.amount), 0) "
                        + "FROM bank_transaction bt "
                        + "WHERE bt.transaction_type = 'IN' "
                        + "AND bt.transaction_date BETWEEN ? AND ?",
                from, to);
        BigDecimal bankOut = singleAmount(
                "SELECT COALESCE(SUM(bt.amount), 0) "
                        + "FROM bank_transaction bt "
                        + "WHERE bt.transaction_type = 'OUT' "
                        + "AND bt.transaction_date BETWEEN ? AND ?",
                from, to);
        BigDecimal net = cashIn.subtract(cashOut).add(bankIn.subtract(bankOut));
        out.put("cashIn", cashIn);
        out.put("cashOut", cashOut);
        out.put("bankIn", bankIn);
        out.put("bankOut", bankOut);
        out.put("netCashFlow", net);
        return out;
    }

    @Override
    public List<Map<String, Object>> getReceivables() {
        if (!hasInvoiceTable()) {
            return List.of();
        }
        @SuppressWarnings("unchecked")
        List<Object> rows = entityManager.createNativeQuery(
                        "SELECT ca.code, ca.name, i.invoice_no, i.due_date, "
                                + "COALESCE(i.grand_total,0) - COALESCE(i.paid_amount,0) AS open_amount, "
                                + "CASE WHEN i.due_date < CURRENT_DATE THEN TRUE ELSE FALSE END AS overdue "
                                + "FROM invoice i "
                                + "LEFT JOIN current_account ca ON ca.id = i.current_account_id "
                                + "WHERE i.deleted_at IS NULL "
                                + "AND i.status IN ('APPROVED', 'POSTED') "
                                + "AND i.invoice_type = 'SALES' "
                                + "AND (COALESCE(i.grand_total,0) - COALESCE(i.paid_amount,0)) > 0 "
                                + "ORDER BY i.due_date")
                .getResultList();
        return receivableLike(rows);
    }

    @Override
    public List<Map<String, Object>> getPayables() {
        if (!hasInvoiceTable()) {
            return List.of();
        }
        @SuppressWarnings("unchecked")
        List<Object> rows = entityManager.createNativeQuery(
                        "SELECT ca.code, ca.name, i.invoice_no, i.due_date, "
                                + "COALESCE(i.grand_total,0) - COALESCE(i.paid_amount,0) AS open_amount, "
                                + "CASE WHEN i.due_date < CURRENT_DATE THEN TRUE ELSE FALSE END AS overdue "
                                + "FROM invoice i "
                                + "LEFT JOIN current_account ca ON ca.id = i.current_account_id "
                                + "WHERE i.deleted_at IS NULL "
                                + "AND i.status IN ('APPROVED', 'POSTED') "
                                + "AND i.invoice_type = 'PURCHASE' "
                                + "AND (COALESCE(i.grand_total,0) - COALESCE(i.paid_amount,0)) > 0 "
                                + "ORDER BY i.due_date")
                .getResultList();
        return receivableLike(rows);
    }

    @Override
    public List<Map<String, Object>> getAgedReceivables() {
        if (!hasInvoiceTable()) {
            return List.of();
        }
        @SuppressWarnings("unchecked")
        List<Object> rows = entityManager.createNativeQuery(
                        "SELECT "
                                + "SUM(CASE WHEN CURRENT_DATE - i.due_date BETWEEN 0 AND 30 THEN COALESCE(i.grand_total,0)-COALESCE(i.paid_amount,0) ELSE 0 END) AS bucket_0_30, "
                                + "SUM(CASE WHEN CURRENT_DATE - i.due_date BETWEEN 31 AND 60 THEN COALESCE(i.grand_total,0)-COALESCE(i.paid_amount,0) ELSE 0 END) AS bucket_31_60, "
                                + "SUM(CASE WHEN CURRENT_DATE - i.due_date BETWEEN 61 AND 90 THEN COALESCE(i.grand_total,0)-COALESCE(i.paid_amount,0) ELSE 0 END) AS bucket_61_90, "
                                + "SUM(CASE WHEN CURRENT_DATE - i.due_date > 90 THEN COALESCE(i.grand_total,0)-COALESCE(i.paid_amount,0) ELSE 0 END) AS bucket_90_plus "
                                + "FROM invoice i "
                                + "WHERE i.deleted_at IS NULL "
                                + "AND i.status IN ('APPROVED', 'POSTED') "
                                + "AND i.invoice_type = 'SALES' "
                                + "AND (COALESCE(i.grand_total,0) - COALESCE(i.paid_amount,0)) > 0")
                .getResultList();
        if (rows.isEmpty()) {
            return List.of();
        }
        Object[] r = (Object[]) rows.get(0);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("bucket0to30", asBigDecimal(r[0]));
        m.put("bucket31to60", asBigDecimal(r[1]));
        m.put("bucket61to90", asBigDecimal(r[2]));
        m.put("bucket90plus", asBigDecimal(r[3]));
        return List.of(m);
    }

    @Override
    public List<Map<String, Object>> getStockValuation() {
        @SuppressWarnings("unchecked")
        List<Object> rows = entityManager.createNativeQuery(
                        "SELECT p.code, p.name, p.stock_quantity, "
                                + "COALESCE((SELECT AVG(sm.unit_cost) FROM stock_movement sm WHERE sm.product_id = p.id), 0) AS avg_cost, "
                                + "COALESCE(p.stock_quantity, 0) * COALESCE((SELECT AVG(sm.unit_cost) FROM stock_movement sm WHERE sm.product_id = p.id), 0) AS stock_value "
                                + "FROM product p "
                                + "WHERE p.deleted_at IS NULL "
                                + "ORDER BY p.code")
                .getResultList();
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object rowObj : rows) {
            Object[] r = (Object[]) rowObj;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("productCode", r[0]);
            m.put("productName", r[1]);
            m.put("stockQuantity", asBigDecimal(r[2]));
            m.put("avgCost", asBigDecimal(r[3]));
            m.put("stockValue", asBigDecimal(r[4]));
            out.add(m);
        }
        return out;
    }

    private List<Map<String, Object>> receivableLike(List<Object> rows) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object rowObj : rows) {
            Object[] r = (Object[]) rowObj;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("currentAccountCode", r[0]);
            m.put("currentAccountName", r[1]);
            m.put("invoiceNo", r[2]);
            m.put("dueDate", r[3]);
            m.put("openAmount", asBigDecimal(r[4]));
            m.put("overdue", r[5] instanceof Boolean b && b);
            out.add(m);
        }
        return out;
    }

    private BigDecimal singleAmount(String sql, LocalDate from, LocalDate to) {
        Object value = entityManager.createNativeQuery(sql)
                .setParameter(1, Date.valueOf(from))
                .setParameter(2, Date.valueOf(to))
                .getSingleResult();
        return asBigDecimal(value);
    }

    private boolean hasInvoiceTable() {
        Object count = entityManager.createNativeQuery(
                        "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'invoice'")
                .getSingleResult();
        return asBigDecimal(count).compareTo(BigDecimal.ZERO) > 0;
    }

    private static BigDecimal asBigDecimal(Object v) {
        if (v == null) {
            return BigDecimal.ZERO;
        }
        if (v instanceof BigDecimal b) {
            return b;
        }
        if (v instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        }
        try {
            return new BigDecimal(v.toString());
        } catch (NumberFormatException ex) {
            return BigDecimal.ZERO;
        }
    }
}
