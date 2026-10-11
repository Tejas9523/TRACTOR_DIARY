package com.example.tractordiary;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.tractordiary.databinding.ActivityAnalyticsBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import java.util.Map;
import java.util.TreeMap;

import android.app.DatePickerDialog;
public class AnalyticsActivity extends AppCompatActivity {

    private ActivityAnalyticsBinding binding;

    private DatabaseReference customerRef;
    private DatabaseReference expenseRef;

    private ValueEventListener customerListener;
    private ValueEventListener expenseListener;

    private final ArrayList<DataSnapshot> customerRecords =
            new ArrayList<>();

    private final ArrayList<DataSnapshot> expenseRecords =
            new ArrayList<>();

    private final String[] months = {
            "All Months", "January", "February", "March",
            "April", "May", "June", "July", "August",
            "September", "October", "November", "December"
    };

    private final ArrayList<String> years = new ArrayList<>();

    private boolean filtersReady = false;
    private String customStartDate = null;
    private String customEndDate = null;
    private static final class Totals {
        double revenue;
        double received;
        double pending;
        double expenses;
        double diesel;
        double maintenance;
    }

    private void setupCustomFilters() {
        binding.customStartDate.setOnClickListener(v ->
                showDatePicker(true));

        binding.customEndDate.setOnClickListener(v ->
                showDatePicker(false));

        binding.resetCustomFilters.setOnClickListener(v -> {
            customStartDate = null;
            customEndDate = null;

            binding.customStartDate.setText("From Date");
            binding.customEndDate.setText("To Date");

            refreshDashboard();
        });
    }

    private void showDatePicker(boolean isStartDate) {
        Calendar calendar = Calendar.getInstance();

        String currentValue = isStartDate
                ? customStartDate : customEndDate;

        Date existingDate = parseDate(currentValue);
        if (existingDate != null) {
            calendar.setTime(existingDate);
        }

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    Calendar selected = Calendar.getInstance();
                    selected.set(year, month, dayOfMonth, 0, 0, 0);
                    selected.set(Calendar.MILLISECOND, 0);

                    String selectedDate = new SimpleDateFormat(
                            "dd/MM/yyyy", Locale.US)
                            .format(selected.getTime());

                    if (isStartDate) {
                        customStartDate = selectedDate;
                        binding.customStartDate.setText(selectedDate);
                    } else {
                        customEndDate = selectedDate;
                        binding.customEndDate.setText(selectedDate);
                    }

                    if (customStartDate != null && customEndDate != null) {
                        Date start = parseDate(customStartDate);
                        Date end = parseDate(customEndDate);

                        if (start != null && end != null && start.after(end)) {
                            Toast.makeText(this,
                                    "From Date cannot be after To Date",
                                    Toast.LENGTH_LONG).show();

                            if (isStartDate) {
                                customStartDate = null;
                                binding.customStartDate.setText("From Date");
                            } else {
                                customEndDate = null;
                                binding.customEndDate.setText("To Date");
                            }
                        }
                    }

                    refreshDashboard();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));

        dialog.show();
    }

    private boolean matchesCustomDateRange(String dateText) {
        // If no custom dates are set, use the existing month/year filters.
        if (customStartDate == null && customEndDate == null) {
            return matchesPeriod(dateText);
        }

        Date recordDate = parseDate(dateText);
        if (recordDate == null) {
            return false;
        }

        Calendar recordCalendar = Calendar.getInstance();
        recordCalendar.setTime(recordDate);
        recordCalendar.set(Calendar.HOUR_OF_DAY, 0);
        recordCalendar.set(Calendar.MINUTE, 0);
        recordCalendar.set(Calendar.SECOND, 0);
        recordCalendar.set(Calendar.MILLISECOND, 0);
        recordDate = recordCalendar.getTime();

        Date start = parseDate(customStartDate);
        Date end = parseDate(customEndDate);

        if (start != null && recordDate.before(start)) {
            return false;
        }

        return end == null || !recordDate.after(end);
    }

    private void updateCharts() {
        // Monthly comparison for the currently applied date/customer filters.
        Map<Integer, Double> monthlyRevenue = new TreeMap<>();
        Map<Integer, Double> monthlyExpenses = new TreeMap<>();

        int selectedMonth = binding.monthFilter.getSelectedItemPosition();
        boolean customRangeActive =
                customStartDate != null || customEndDate != null;

        for (DataSnapshot record : customerRecords) {
            String dateText = record.child("date").getValue(String.class);

            if (!matchesCustomDateRange(dateText)) {
                continue;
            }

            Date date = parseDate(dateText);
            if (date == null) {
                continue;
            }

            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            int month = cal.get(Calendar.MONTH);

            monthlyRevenue.merge(
                    month,
                    readAmount(record, "amt") + readAmount(record, "amt2"),
                    Double::sum);
        }

        for (DataSnapshot record : expenseRecords) {
            String dateText = record.child("date").getValue(String.class);

            if (!matchesCustomDateRange(dateText)) {
                continue;
            }

            Date date = parseDate(dateText);
            if (date == null) {
                continue;
            }

            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            int month = cal.get(Calendar.MONTH);

            monthlyExpenses.merge(
                    month,
                    readAmount(record, "amount"),
                    Double::sum);
        }

        ArrayList<BarEntry> revenueEntries = new ArrayList<>();
        ArrayList<BarEntry> expenseEntries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();

        String[] shortMonths =
                new java.text.DateFormatSymbols(Locale.getDefault())
                        .getShortMonths();

        for (int month = 0; month < 12; month++) {
            // Without a custom date range, the month spinner controls the
            // visible period. When "All Months" is selected, show all 12.
            if (!customRangeActive
                    && selectedMonth != 0
                    && month != selectedMonth - 1) {
                continue;
            }

            revenueEntries.add(new BarEntry(
                    labels.size(),
                    monthlyRevenue.getOrDefault(month, 0.0).floatValue()));

            expenseEntries.add(new BarEntry(
                    labels.size(),
                    monthlyExpenses.getOrDefault(month, 0.0).floatValue()));

            labels.add(shortMonths[month]);
        }

        BarDataSet revenueSet = new BarDataSet(revenueEntries, "Revenue");
        BarDataSet expenseSet = new BarDataSet(expenseEntries, "Expenses");

        // Keep the app's yellow theme and use charcoal for contrast.
        int yellow = androidx.core.content.ContextCompat.getColor(
                this, R.color.yellow);
        int charcoal = 0xFF424242;

        revenueSet.setColor(yellow);
        expenseSet.setColor(charcoal);

        // Avoid crowded currency labels on the bars.
        revenueSet.setDrawValues(false);
        expenseSet.setDrawValues(false);

        BarData barData = new BarData(revenueSet, expenseSet);
        barData.setBarWidth(0.36f);

        BarChart chart = binding.revenueExpenseChart;
        chart.setData(barData);
        chart.setBackgroundColor(android.graphics.Color.WHITE);
        chart.setDrawGridBackground(false);
        chart.setDrawBarShadow(false);
        chart.setHighlightFullBarEnabled(false);
        chart.setDoubleTapToZoomEnabled(false);
        chart.setPinchZoom(false);
        chart.setScaleEnabled(false);
        chart.setFitBars(false);
        chart.setExtraBottomOffset(8f);
        chart.setExtraTopOffset(12f);
        chart.setNoDataText("No records for the selected filters");
        chart.setNoDataTextColor(0xFF757575);
        chart.getDescription().setEnabled(false);

        XAxis xAxis = chart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setCenterAxisLabels(true);
        xAxis.setTextColor(0xFF616161);
        xAxis.setTextSize(11f);
        xAxis.setDrawGridLines(false);
        xAxis.setAxisLineColor(0xFFE0E0E0);
        xAxis.setDrawAxisLine(true);
        xAxis.setAvoidFirstLastClipping(true);

        chart.getAxisLeft().setAxisMinimum(0f);
        chart.getAxisLeft().setTextColor(0xFF757575);
        chart.getAxisLeft().setTextSize(10f);
        chart.getAxisLeft().setGridColor(0xFFEEEEEE);
        chart.getAxisLeft().setDrawAxisLine(false);
        chart.getAxisLeft().setGranularityEnabled(true);
        chart.getAxisRight().setEnabled(false);

        chart.getLegend().setEnabled(true);
        chart.getLegend().setTextColor(0xFF424242);
        chart.getLegend().setTextSize(12f);
        chart.getLegend().setForm(
                com.github.mikephil.charting.components.Legend.LegendForm.SQUARE);
        chart.getLegend().setXEntrySpace(16f);
        chart.getLegend().setWordWrapEnabled(true);

        if (!labels.isEmpty()) {
            // Each month occupies one group containing Revenue and Expenses.
            chart.getXAxis().setAxisMinimum(0f);
            chart.getXAxis().setAxisMaximum((float) labels.size());
            chart.groupBars(0f, 0.16f, 0.06f);
        }

        chart.animateY(450);
        chart.invalidate();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityAnalyticsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String uid = FirebaseAuth.getInstance().getUid();

        if (uid == null) {
            Toast.makeText(this, "Please log in again",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        customerRef = FirebaseDatabase.getInstance()
                .getReference("user").child(uid);

        expenseRef = FirebaseDatabase.getInstance()
                .getReference("expenses").child(uid);

        setupFilters();
        setupCustomFilters();
        listenForRecords();
    }

    private void setupFilters() {
        ArrayAdapter<String> monthAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, months);

        monthAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);

        binding.monthFilter.setAdapter(monthAdapter);

        int currentYear = Calendar.getInstance()
                .get(Calendar.YEAR);

        Set<String> foundYears = new HashSet<>();
        foundYears.add(String.valueOf(currentYear));
        foundYears.add(String.valueOf(currentYear - 1));

        years.clear();
        years.add("All Years");

        // Keep all years available, including years in older records.
        for (int year = currentYear; year >= 2000; year--) {
            years.add(String.valueOf(year));
        }

        ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, years);

        yearAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);

        binding.yearFilter.setAdapter(yearAdapter);

        binding.monthFilter.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            android.view.View view,
                            int position,
                            long id) {
                        if (filtersReady) refreshDashboard();
                    }

                    @Override
                    public void onNothingSelected(
                            AdapterView<?> parent) { }
                });

        binding.yearFilter.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            android.view.View view,
                            int position,
                            long id) {
                        if (filtersReady) refreshDashboard();
                    }

                    @Override
                    public void onNothingSelected(
                            AdapterView<?> parent) { }
                });

        binding.monthFilter.setSelection(
                Calendar.getInstance().get(Calendar.MONTH) + 1);

        binding.yearFilter.setSelection(1);

        filtersReady = true;
    }

    private void listenForRecords() {
        customerListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                customerRecords.clear();

                for (DataSnapshot child : snapshot.getChildren()) {
                    customerRecords.add(child);
                }
                refreshDashboard();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AnalyticsActivity.this,
                        "Could not load customer records",
                        Toast.LENGTH_SHORT).show();
            }
        };

        expenseListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                expenseRecords.clear();

                for (DataSnapshot child : snapshot.getChildren()) {
                    expenseRecords.add(child);
                }

                refreshDashboard();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AnalyticsActivity.this,
                        "Could not load expenses",
                        Toast.LENGTH_SHORT).show();
            }
        };

        customerRef.addValueEventListener(customerListener);
        expenseRef.addValueEventListener(expenseListener);
    }

    private Date parseDate(String text) {
        if (text == null || text.trim().isEmpty()) return null;

        String[] formats = {
                "dd/MM/yyyy", "d/M/yyyy",
                "dd-MM-yyyy", "d-M-yyyy"
        };

        for (String format : formats) {
            try {
                SimpleDateFormat sdf =
                        new SimpleDateFormat(format, Locale.US);
                sdf.setLenient(false);
                return sdf.parse(text.trim());
            } catch (Exception ignored) {
            }
        }

        return null;
    }

    private boolean matchesPeriod(String dateText) {
        // All-time means include records regardless of date format.
        if (binding.yearFilter.getSelectedItemPosition() == 0
                && binding.monthFilter.getSelectedItemPosition() == 0) {
            return true;
        }

        Date date = parseDate(dateText);

        // Do not silently include undated records in a filtered period.
        if (date == null) return false;

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        String selectedYear =
                binding.yearFilter.getSelectedItem().toString();

        if (!selectedYear.equals("All Years")
                && calendar.get(Calendar.YEAR)
                != Integer.parseInt(selectedYear)) {
            return false;
        }

        int selectedMonth = binding.monthFilter.getSelectedItemPosition();

        return selectedMonth == 0
                || calendar.get(Calendar.MONTH) + 1 == selectedMonth;
    }

    private double readAmount(DataSnapshot snapshot, String field) {
        Object value = snapshot.child(field).getValue();

        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }

        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException ignored) {
            }
        }

        return 0.0;
    }

    private void refreshDashboard() {
        if (!filtersReady) return;

        Totals totals = new Totals();

        for (DataSnapshot record : customerRecords) {
            String date = record.child("date").getValue(String.class);

            if (!matchesCustomDateRange(date)) continue;

            double received = readAmount(record, "amt");
            double pending = readAmount(record, "amt2");

            totals.received += received;
            totals.pending += pending;
            totals.revenue += received + pending;
        }

        for (DataSnapshot expense : expenseRecords) {
            String date = expense.child("date").getValue(String.class);

            if (!matchesCustomDateRange(date)) continue;

            double amount = readAmount(expense, "amount");
            String category =
                    expense.child("category").getValue(String.class);

            totals.expenses += amount;

            if ("Diesel".equalsIgnoreCase(category)) {
                totals.diesel += amount;
            } else if ("Maintenance".equalsIgnoreCase(category)) {
                totals.maintenance += amount;
            }
        }

        double profit = totals.revenue - totals.expenses;
        double cashAfterExpenses = totals.received - totals.expenses;

        binding.totalRevenue.setText(money(totals.revenue));
        binding.totalExpenses.setText(money(totals.expenses));
        binding.estimatedProfit.setText(money(profit));
        binding.cashAfterExpenses.setText(money(cashAfterExpenses));
        binding.outstandingPayments.setText(money(totals.pending));

        binding.dieselTotal.setText(money(totals.diesel));
        binding.maintenanceTotal.setText(money(totals.maintenance));

        String month = binding.monthFilter.getSelectedItem().toString();
        String year = binding.yearFilter.getSelectedItem().toString();

        binding.periodLabel.setText(
                month + " • " + year);

        binding.revenueExpenseSummary.setText(
                "Revenue: " + money(totals.revenue)
                        + "\nExpenses: " + money(totals.expenses)
                        + "\nEstimated profit: " + money(profit));
        updateCharts();
    }

    private String money(double amount) {
        return String.format(Locale.getDefault(), "₹%,.2f", amount);
    }

    @Override
    protected void onDestroy() {
        if (customerRef != null && customerListener != null) {
            customerRef.removeEventListener(customerListener);
        }

        if (expenseRef != null && expenseListener != null) {
            expenseRef.removeEventListener(expenseListener);
        }

        super.onDestroy();
    }
}

