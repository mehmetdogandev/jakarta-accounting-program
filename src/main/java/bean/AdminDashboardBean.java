package bean;

import entity.BankAccount;
import entity.BankTransaction;
import entity.CashAccount;
import entity.CashTransaction;
import entity.Cheque;
import entity.Expense;
import facadeLocal.BankAccountFacadeLocal;
import facadeLocal.CashAccountFacadeLocal;
import facadeLocal.ChequeFacadeLocal;
import facadeLocal.CurrentAccountFacadeLocal;
import facadeLocal.ExpenseFacadeLocal;
import facadeLocal.ProductFacadeLocal;
import facadeLocal.ReportFacadeLocal;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import org.primefaces.model.charts.ChartData;
import org.primefaces.model.charts.line.LineChartDataSet;
import org.primefaces.model.charts.line.LineChartModel;

import java.io.Serializable;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Named
@ViewScoped
public class AdminDashboardBean implements Serializable {

    private static final Locale TURKISH = Locale.forLanguageTag("tr-TR");
    private static final DateTimeFormatter LABEL_FMT = DateTimeFormatter.ofPattern("dd MMM", TURKISH);

    @EJB
    private CashAccountFacadeLocal cashAccountFacade;
    @EJB
    private BankAccountFacadeLocal bankAccountFacade;
    @EJB
    private CurrentAccountFacadeLocal currentAccountFacade;
    @EJB
    private ExpenseFacadeLocal expenseFacade;
    @EJB
    private ProductFacadeLocal productFacade;
    @EJB
    private ChequeFacadeLocal chequeFacade;
    @EJB
    private ReportFacadeLocal reportFacade;

    private BigDecimal totalCashBalance = BigDecimal.ZERO;
    private BigDecimal totalBankBalance = BigDecimal.ZERO;
    private int totalCurrentAccounts;
    private long pendingExpenseCount;
    private long pendingIncomeCount;
    private int totalProducts;
    private int lowStockCount;
    private BigDecimal stockValuationTotal = BigDecimal.ZERO;
    private int dueSoonChequeCount;
    private long depositedChequeCount;
    private long issuedChequeCount;
    private LineChartModel cashFlowChart;
    private LineChartModel incomeExpenseChart;
    private boolean hasCashFlowData;
    private boolean hasIncomeExpenseData;
    private List<String> alertMessages = List.of();

    @PostConstruct
    public void init() {
        refresh();
    }

    public void refresh() {
        loadKpis();
        buildCharts();
    }

    public String formatCurrency(BigDecimal amount) {
        return NumberFormat.getCurrencyInstance(TURKISH).format(amount == null ? BigDecimal.ZERO : amount);
    }

    public BigDecimal getTotalCashBalance() {
        return totalCashBalance;
    }

    public BigDecimal getTotalBankBalance() {
        return totalBankBalance;
    }

    public int getTotalCurrentAccounts() {
        return totalCurrentAccounts;
    }

    public long getPendingExpenseCount() {
        return pendingExpenseCount;
    }

    public long getPendingIncomeCount() {
        return pendingIncomeCount;
    }

    public int getTotalProducts() {
        return totalProducts;
    }

    public int getLowStockCount() {
        return lowStockCount;
    }

    public BigDecimal getStockValuationTotal() {
        return stockValuationTotal;
    }

    public int getDueSoonChequeCount() {
        return dueSoonChequeCount;
    }

    public long getDepositedChequeCount() {
        return depositedChequeCount;
    }

    public long getIssuedChequeCount() {
        return issuedChequeCount;
    }

    public LineChartModel getCashFlowChart() {
        return cashFlowChart;
    }

    public LineChartModel getIncomeExpenseChart() {
        return incomeExpenseChart;
    }

    public boolean isHasCashFlowData() {
        return hasCashFlowData;
    }

    public boolean isHasIncomeExpenseData() {
        return hasIncomeExpenseData;
    }

    public List<String> getAlertMessages() {
        return alertMessages;
    }

    public boolean isAllClear() {
        return alertMessages.isEmpty();
    }

    private void loadKpis() {
        totalCashBalance = cashAccountFacade.findAll().stream()
                .map(CashAccount::getBalance)
                .reduce(BigDecimal.ZERO, (a, b) -> a.add(b == null ? BigDecimal.ZERO : b));

        totalBankBalance = bankAccountFacade.findAll().stream()
                .map(BankAccount::getBalance)
                .reduce(BigDecimal.ZERO, (a, b) -> a.add(b == null ? BigDecimal.ZERO : b));

        totalCurrentAccounts = currentAccountFacade.findAll(false).size();

        List<Expense> expenses = expenseFacade.findAll(false);
        pendingExpenseCount = expenses.stream()
                .filter(e -> "EXPENSE".equalsIgnoreCase(e.getTransactionType()) && "PENDING".equalsIgnoreCase(e.getStatus()))
                .count();
        pendingIncomeCount = expenses.stream()
                .filter(e -> "INCOME".equalsIgnoreCase(e.getTransactionType()) && "PENDING".equalsIgnoreCase(e.getStatus()))
                .count();

        totalProducts = productFacade.findAll(false).size();
        lowStockCount = productFacade.findLowStock().size();

        stockValuationTotal = reportFacade.getStockValuation().stream()
                .map(x -> x.get("stockValue"))
                .filter(BigDecimal.class::isInstance)
                .map(BigDecimal.class::cast)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Cheque> cheques = chequeFacade.findAll();
        dueSoonChequeCount = chequeFacade.findDueWithin(7).size();
        depositedChequeCount = cheques.stream().filter(c -> "DEPOSITED".equalsIgnoreCase(c.getStatus())).count();
        issuedChequeCount = cheques.stream().filter(c -> "ISSUED".equalsIgnoreCase(c.getChequeType())).count();

        List<String> alerts = new ArrayList<>();
        if (lowStockCount > 0) {
            alerts.add(lowStockCount + " urunde dusuk stok var.");
        }
        if (dueSoonChequeCount > 0) {
            alerts.add(dueSoonChequeCount + " cekin vadesi 7 gun icinde doluyor.");
        }
        if (pendingExpenseCount + pendingIncomeCount > 0) {
            alerts.add((pendingExpenseCount + pendingIncomeCount) + " gelir/gider kaydi onay bekliyor.");
        }
        alertMessages = alerts;
    }

    private void buildCharts() {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(29);

        Map<LocalDate, BigDecimal> cashFlowSeries = new LinkedHashMap<>();
        Map<LocalDate, BigDecimal> incomeSeries = new LinkedHashMap<>();
        Map<LocalDate, BigDecimal> expenseSeries = new LinkedHashMap<>();
        for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) {
            cashFlowSeries.put(day, BigDecimal.ZERO);
            incomeSeries.put(day, BigDecimal.ZERO);
            expenseSeries.put(day, BigDecimal.ZERO);
        }

        for (CashAccount account : cashAccountFacade.findActive()) {
            for (CashTransaction tx : cashAccountFacade.listTransactions(account.getId())) {
                if (tx.getTransactionDate() == null || tx.getTransactionDate().isBefore(start) || tx.getTransactionDate().isAfter(end)) {
                    continue;
                }
                BigDecimal sign = "IN".equalsIgnoreCase(tx.getTransactionType()) ? tx.getAmount() : tx.getAmount().negate();
                cashFlowSeries.computeIfPresent(tx.getTransactionDate(), (k, v) -> v.add(sign == null ? BigDecimal.ZERO : sign));
            }
        }
        for (BankAccount account : bankAccountFacade.findActive()) {
            for (BankTransaction tx : bankAccountFacade.listTransactions(account.getId())) {
                if (tx.getTransactionDate() == null || tx.getTransactionDate().isBefore(start) || tx.getTransactionDate().isAfter(end)) {
                    continue;
                }
                BigDecimal sign = "IN".equalsIgnoreCase(tx.getTransactionType()) ? tx.getAmount() : tx.getAmount().negate();
                cashFlowSeries.computeIfPresent(tx.getTransactionDate(), (k, v) -> v.add(sign == null ? BigDecimal.ZERO : sign));
            }
        }

        for (Expense expense : expenseFacade.findByDateRange(start, end)) {
            if (!"APPROVED".equalsIgnoreCase(expense.getStatus()) || expense.getTransactionDate() == null) {
                continue;
            }
            if ("INCOME".equalsIgnoreCase(expense.getTransactionType())) {
                incomeSeries.computeIfPresent(expense.getTransactionDate(), (k, v) -> v.add(nvl(expense.getAmount())));
            } else if ("EXPENSE".equalsIgnoreCase(expense.getTransactionType())) {
                expenseSeries.computeIfPresent(expense.getTransactionDate(), (k, v) -> v.add(nvl(expense.getAmount())));
            }
        }

        cashFlowChart = new LineChartModel();
        cashFlowChart.setData(singleSeriesChartData("Net Nakit Akisi", "#0ea5e9", cashFlowSeries));
        hasCashFlowData = cashFlowSeries.values().stream().anyMatch(v -> v.compareTo(BigDecimal.ZERO) != 0);

        incomeExpenseChart = new LineChartModel();
        ChartData ieData = new ChartData();
        ieData.setLabels(cashFlowSeries.keySet().stream().map(d -> d.format(LABEL_FMT)).toList());
        ieData.addChartDataSet(series("Gelir", "#16a34a", incomeSeries.values().stream().toList()));
        ieData.addChartDataSet(series("Gider", "#dc2626", expenseSeries.values().stream().toList()));
        incomeExpenseChart.setData(ieData);
        hasIncomeExpenseData = incomeSeries.values().stream().anyMatch(v -> v.compareTo(BigDecimal.ZERO) != 0)
                || expenseSeries.values().stream().anyMatch(v -> v.compareTo(BigDecimal.ZERO) != 0);
    }

    private ChartData singleSeriesChartData(String label, String color, Map<LocalDate, BigDecimal> map) {
        ChartData data = new ChartData();
        List<String> labels = new ArrayList<>();
        List<BigDecimal> values = new ArrayList<>();
        for (Map.Entry<LocalDate, BigDecimal> e : map.entrySet()) {
            labels.add(e.getKey().format(LABEL_FMT));
            values.add(e.getValue());
        }
        data.setLabels(labels);
        data.addChartDataSet(series(label, color, values));
        return data;
    }

    private LineChartDataSet series(String label, String color, List<BigDecimal> values) {
        LineChartDataSet ds = new LineChartDataSet();
        ds.setLabel(label);
        ds.setData(new ArrayList<>(values));
        ds.setBorderColor(color);
        ds.setBackgroundColor("rgba(0,0,0,0)");
        ds.setFill(false);
        ds.setTension(0.3);
        return ds;
    }

    private static BigDecimal nvl(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
