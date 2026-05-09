package bean;

import enums.Permission;
import enums.Scope;
import facadeLocal.ReportFacadeLocal;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import org.primefaces.PrimeFaces;
import procedure.RbacProcedureBean;

import java.io.Serializable;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Named
@ViewScoped
public class AdminReportBean implements Serializable {

    private static final Locale TURKISH = Locale.forLanguageTag("tr-TR");

    @EJB
    private ReportFacadeLocal reportFacade;

    @EJB
    private RbacProcedureBean rbacProcedure;

    private String reportType = "TRIAL_BALANCE";
    private LocalDate fromDate;
    private LocalDate toDate;
    private Object reportData = Collections.emptyList();

    @PostConstruct
    public void init() {
        if (!rbacProcedure.require(Scope.REPORT, Permission.ACCESS)) {
            return;
        }
        LocalDate now = LocalDate.now();
        fromDate = now.withDayOfMonth(1);
        toDate = now.withDayOfMonth(now.lengthOfMonth());
        generateReport();
    }

    public void generateReport() {
        if (!rbacProcedure.require(Scope.REPORT, Permission.READ)) {
            reportData = Collections.emptyList();
            return;
        }
        reportData = switch (reportType) {
            case "TRIAL_BALANCE" -> reportFacade.getTrialBalance(toDate == null ? LocalDate.now() : toDate);
            case "PROFIT_LOSS" -> reportFacade.getProfitLoss(safeFrom(), safeTo());
            case "CASH_FLOW" -> reportFacade.getCashFlowSummary(safeFrom(), safeTo());
            case "RECEIVABLES" -> reportFacade.getReceivables();
            case "PAYABLES" -> reportFacade.getPayables();
            case "AGED" -> reportFacade.getAgedReceivables();
            case "STOCK" -> reportFacade.getStockValuation();
            default -> Collections.emptyList();
        };
    }

    public void exportToPdf() {
        triggerExport("pdf");
    }

    public void exportToExcel() {
        triggerExport("xlsx");
    }

    public List<Map<String, Object>> getRows() {
        if (reportData instanceof List<?> list) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> cast = (List<Map<String, Object>>) list;
            return cast;
        }
        return Collections.emptyList();
    }

    public Map<String, Object> getSummaryMap() {
        if (reportData instanceof Map<?, ?> map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> cast = (Map<String, Object>) map;
            return cast;
        }
        return Collections.emptyMap();
    }

    public List<String> getReportTypeOptions() {
        return List.of("TRIAL_BALANCE", "PROFIT_LOSS", "CASH_FLOW", "RECEIVABLES", "PAYABLES", "AGED", "STOCK");
    }

    public String getReportTypeLabel(String type) {
        return switch (type) {
            case "TRIAL_BALANCE" -> "Mizan";
            case "PROFIT_LOSS" -> "Kar/Zarar";
            case "CASH_FLOW" -> "Nakit Akisi";
            case "RECEIVABLES" -> "Alacaklar";
            case "PAYABLES" -> "Borclar";
            case "AGED" -> "Vade Analizi";
            case "STOCK" -> "Stok Degeri";
            default -> type;
        };
    }

    public String statusClass(Object overdue) {
        return Boolean.TRUE.equals(overdue) ? "text-danger fw-semibold" : "";
    }

    public String formatCurrency(Object amount) {
        BigDecimal safe = amount instanceof BigDecimal b ? b : BigDecimal.ZERO;
        return NumberFormat.getCurrencyInstance(TURKISH).format(safe);
    }

    public String getReportType() {
        return reportType;
    }

    public void setReportType(String reportType) {
        this.reportType = reportType;
    }

    public LocalDate getFromDate() {
        return fromDate;
    }

    public void setFromDate(LocalDate fromDate) {
        this.fromDate = fromDate;
    }

    public LocalDate getToDate() {
        return toDate;
    }

    public void setToDate(LocalDate toDate) {
        this.toDate = toDate;
    }

    public Object getReportData() {
        return reportData;
    }

    private LocalDate safeFrom() {
        return fromDate == null ? LocalDate.now().withDayOfMonth(1) : fromDate;
    }

    private LocalDate safeTo() {
        return toDate == null ? LocalDate.now() : toDate;
    }

    private void triggerExport(String type) {
        if (reportType.equals("PROFIT_LOSS") || reportType.equals("CASH_FLOW")) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Export", "Bu rapor ozet kutularla gosteriliyor."));
            return;
        }
        String buttonId = "mainForm:" + switch (reportType) {
            case "TRIAL_BALANCE" -> type.equals("pdf") ? "tbPdfBtn" : "tbXlsxBtn";
            case "RECEIVABLES" -> type.equals("pdf") ? "recPdfBtn" : "recXlsxBtn";
            case "PAYABLES" -> type.equals("pdf") ? "payPdfBtn" : "payXlsxBtn";
            case "AGED" -> type.equals("pdf") ? "agedPdfBtn" : "agedXlsxBtn";
            case "STOCK" -> type.equals("pdf") ? "stockPdfBtn" : "stockXlsxBtn";
            default -> "";
        };
        if (buttonId.isBlank()) {
            return;
        }
        PrimeFaces.current().executeScript("document.getElementById('" + buttonId + "').click();");
    }
}
