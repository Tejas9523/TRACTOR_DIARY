
package com.example.tractordiary;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.tractordiary.databinding.ActivityExpensesBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class ExpensesActivity extends AppCompatActivity {

    private ActivityExpensesBinding binding;
    private DatabaseReference expensesRef;
    private ValueEventListener expensesListener;

    private String selectedDate;

    private int selectedYear =
            Calendar.getInstance().get(Calendar.YEAR);

    private boolean updatingSelectionControls = false;
    private boolean updatingYearFilter = false;
    private boolean deletingExpenses = false;

    private final ArrayList<DataSnapshot> expenseRecords =
            new ArrayList<>();

    private final Set<String> selectedExpenseIds =
            new HashSet<>();

    private static final int YELLOW = 0xFFFFC801;
    private static final int CHARCOAL = 0xFF212121;
    private static final int MUTED = 0xFF757575;
    private static final int RED = 0xFFB3261E;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int BORDER = 0xFFE5E5E5;


    private ArrayList<DataSnapshot> getFilteredExpenseRecords() {
        ArrayList<DataSnapshot> filteredRecords = new ArrayList<>();

        for (DataSnapshot record : expenseRecords) {
            String date = record.child("date").getValue(String.class);
            int[] parts = parseMonthYear(date);

            if (parts != null && parts[1] == selectedYear) {
                filteredRecords.add(record);
            }
        }

        return filteredRecords;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityExpensesBinding.inflate(
                getLayoutInflater());
        setContentView(binding.getRoot());

        String uid = FirebaseAuth.getInstance().getUid();

        if (uid == null) {
            Toast.makeText(this, "Please log in again",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        expensesRef = FirebaseDatabase.getInstance()
                .getReference("expenses")
                .child(uid);

        selectedDate = new SimpleDateFormat(
                "dd/MM/yyyy", Locale.getDefault()
        ).format(new Date());

        binding.expenseDate.setText(selectedDate);

        setupDatePicker();
        setupYearFilter();

        binding.saveExpense.setOnClickListener(
                v -> saveExpense());

        binding.selectAllExpenses.setOnCheckedChangeListener(
                (buttonView, isChecked) -> {
                    if (updatingSelectionControls) {
                        return;
                    }

                    selectedExpenseIds.clear();

                    if (isChecked) {

                        for (DataSnapshot record : getFilteredExpenseRecords()) {
                            String id = record.getKey();

                            if (id != null) {
                                selectedExpenseIds.add(id);
                            }
                        }
                    }

                    renderExpenses();
                });

        binding.deleteSelectedExpenses.setOnClickListener(
                v -> confirmDeleteSelected());

        loadExpenses();
    }

    private void setupDatePicker() {
        binding.expenseDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();

            DatePickerDialog picker = new DatePickerDialog(
                    this,
                    (view, year, month, day) -> {
                        selectedDate = String.format(
                                Locale.getDefault(),
                                "%02d/%02d/%04d",
                                day, month + 1, year
                        );

                        binding.expenseDate.setText(selectedDate);
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );

            picker.show();
        });
    }


    private void setupYearFilter() {
        binding.expenseYearFilter.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            View view,
                            int position,
                            long id) {

                        if (updatingYearFilter) {
                            return;
                        }

                        Object item = parent.getItemAtPosition(position);

                        if (item != null) {
                            try {
                                int newYear = Integer.parseInt(item.toString());

                                if (selectedYear != newYear) {
                                    selectedYear = newYear;

                                    // Clear old selections when changing years.
                                    selectedExpenseIds.clear();

                                    // Refresh chart AND recent expense list.
                                    renderExpenses();
                                }
                            } catch (NumberFormatException ignored) {
                                // Ignore invalid year labels.
                            }
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                });
    }

    private void saveExpense() {
        String amountText =
                binding.expenseAmount.getText()
                        .toString().trim();

        if (amountText.isEmpty()) {
            binding.expenseAmount.setError("Enter an amount");
            return;
        }

        double amount;

        try {
            amount = Double.parseDouble(amountText);
        } catch (NumberFormatException e) {
            binding.expenseAmount.setError(
                    "Enter a valid amount");
            return;
        }

        if (!Double.isFinite(amount) || amount <= 0) {
            binding.expenseAmount.setError(
                    "Amount must be greater than zero");
            return;
        }

        Object selectedCategory =
                binding.expenseCategory.getSelectedItem();

        if (selectedCategory == null) {
            Toast.makeText(this,
                    "Select an expense category",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        String category = selectedCategory.toString();
        String recordId = expensesRef.push().getKey();

        if (recordId == null) {
            Toast.makeText(this,
                    "Could not create expense",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> expense = new HashMap<>();
        expense.put("date", selectedDate);
        expense.put("category", category);
        expense.put("amount", amount);
        expense.put("createdAt", System.currentTimeMillis());

        binding.saveExpense.setEnabled(false);

        expensesRef.child(recordId).setValue(expense)
                .addOnCompleteListener(task -> {
                    binding.saveExpense.setEnabled(true);

                    if (task.isSuccessful()) {
                        binding.expenseAmount.setText("");

                        Toast.makeText(this,
                                "Expense saved successfully",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this,
                                "Could not save expense. "
                                        + "Check connection.",
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void loadExpenses() {
        expensesListener = new ValueEventListener() {
            @Override
            public void onDataChange(
                    @NonNull DataSnapshot snapshot) {

                expenseRecords.clear();

                Set<String> existingIds = new HashSet<>();

                for (DataSnapshot child : snapshot.getChildren()) {
                    expenseRecords.add(child);

                    String id = child.getKey();

                    if (id != null) {
                        existingIds.add(id);
                    }
                }

                // Remove selections for deleted records.
                selectedExpenseIds.retainAll(existingIds);

                renderExpenses();
            }

            @Override
            public void onCancelled(
                    @NonNull DatabaseError error) {

                Toast.makeText(ExpensesActivity.this,
                        "Could not load expenses: "
                                + error.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        };

        expensesRef.addValueEventListener(expensesListener);
    }


    private void renderExpenses() {
        binding.expenseHistoryList.removeAllViews();

        refreshYearFilter();

        ArrayList<DataSnapshot> filteredRecords =
                getFilteredExpenseRecords();

        // Keep the chart and recent list synchronized.
        renderMonthlyExpenseChart();

        if (filteredRecords.isEmpty()) {
            if (expenseRecords.isEmpty()) {
                binding.expenseHistory.setText(
                        "No expenses recorded yet.");
            } else {
                binding.expenseHistory.setText(
                        "No expenses recorded for " + selectedYear + ".");
            }

            updateSelectionControls();
            return;
        }

        binding.expenseHistory.setText(
                filteredRecords.size()
                        + (filteredRecords.size() == 1
                        ? " expense for " : " expenses for ")
                        + selectedYear);

        // Display only the selected year's expenses, newest first.
        for (int i = filteredRecords.size() - 1; i >= 0; i--) {
            DataSnapshot record = filteredRecords.get(i);

            binding.expenseHistoryList.addView(
                    createExpenseCard(record));
        }

        updateSelectionControls();
    }


    // Populate years from dates that exist in Firebase.
    private void refreshYearFilter() {
        TreeSet<Integer> years =
                new TreeSet<>(Collections.reverseOrder());

        for (DataSnapshot record : expenseRecords) {
            String date = record.child("date")
                    .getValue(String.class);

            int[] parts = parseMonthYear(date);

            if (parts != null) {
                years.add(parts[1]);
            }
        }

        // Show the current year when there are no saved expenses.
        if (years.isEmpty()) {
            years.add(Calendar.getInstance()
                    .get(Calendar.YEAR));
        }

        // Keep the current filter if that year still exists.
        if (!years.contains(selectedYear)) {
            selectedYear = years.first();
        }

        ArrayList<String> yearLabels = new ArrayList<>();

        for (Integer year : years) {
            yearLabels.add(String.valueOf(year));
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                yearLabels
        );

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);

        int selectedPosition =
                yearLabels.indexOf(String.valueOf(selectedYear));

        updatingYearFilter = true;

        binding.expenseYearFilter.setAdapter(adapter);

        if (selectedPosition >= 0) {
            binding.expenseYearFilter.setSelection(
                    selectedPosition, false);
        }

        updatingYearFilter = false;
    }

    // Returns [month, year] for dates formatted as dd/MM/yyyy.
    private int[] parseMonthYear(String date) {
        if (date == null) {
            return null;
        }

        String[] parts = date.trim().split("/");

        if (parts.length != 3) {
            return null;
        }

        try {
            int day = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            int year = Integer.parseInt(parts[2]);

            if (day < 1 || day > 31
                    || month < 1 || month > 12
                    || year < 1) {
                return null;
            }

            return new int[]{month, year};

        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void renderMonthlyExpenseChart() {
        if (binding == null) {
            return;
        }

        double[] monthlyTotals = new double[12];
        double yearlyTotal = 0.0;

        for (DataSnapshot record : expenseRecords) {
            String date = record.child("date")
                    .getValue(String.class);

            int[] parts = parseMonthYear(date);

            if (parts == null) {
                continue;
            }

            int month = parts[0];
            int year = parts[1];

            if (year != selectedYear) {
                continue;
            }

            double amount = readAmount(record, "amount");

            monthlyTotals[month - 1] += amount;
            yearlyTotal += amount;
        }

        binding.monthlyExpenseYearTotal.setText(
                "Total for " + selectedYear + ": "
                        + money(yearlyTotal));

        binding.monthlyExpenseChartContainer.removeAllViews();

        MonthlyExpenseChartView chart =
                new MonthlyExpenseChartView(
                        this, monthlyTotals);

        LinearLayout.LayoutParams chartParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(250));

        binding.monthlyExpenseChartContainer.addView(
                chart, chartParams);
    }


    private View createExpenseCard(DataSnapshot record) {
        String recordId = record.getKey();

        String date = record.child("date").getValue(String.class);
        String category = record.child("category").getValue(String.class);
        double amount = readAmount(record, "amount");

        if (date == null) date = "Date not recorded";
        if (category == null) category = "Other";

        final String finalRecordId = recordId;

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        card.setBackground(createRoundedBackground(
                WHITE, BORDER, 6));
        card.setElevation(dp(1));

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);

        cardParams.bottomMargin = dp(10);
        card.setLayoutParams(cardParams);

        // Checkbox, category, date and amount.
        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        CheckBox checkBox = new CheckBox(this);

        checkBox.setButtonTintList(new ColorStateList(
                new int[][]{
                        new int[]{android.R.attr.state_checked},
                        new int[]{}
                },
                new int[]{YELLOW, 0xFF9E9E9E}
        ));

        checkBox.setChecked(
                finalRecordId != null
                        && selectedExpenseIds.contains(finalRecordId));

        checkBox.setEnabled(!deletingExpenses);

        LinearLayout.LayoutParams checkParams =
                new LinearLayout.LayoutParams(dp(42), dp(44));

        topRow.addView(checkBox, checkParams);

        LinearLayout details = new LinearLayout(this);
        details.setOrientation(LinearLayout.VERTICAL);

        LinearLayout.LayoutParams detailsParams =
                new LinearLayout.LayoutParams(0, -2, 1);

        topRow.addView(details, detailsParams);

        TextView categoryView = new TextView(this);
        categoryView.setText(category);
        categoryView.setTextColor(CHARCOAL);
        categoryView.setTextSize(16);
        categoryView.setTypeface(null, Typeface.BOLD);

        details.addView(categoryView);

        TextView dateView = new TextView(this);
        dateView.setText(date);
        dateView.setTextColor(MUTED);
        dateView.setTextSize(13);

        LinearLayout.LayoutParams dateParams =
                new LinearLayout.LayoutParams(-1, -2);

        dateParams.topMargin = dp(4);
        details.addView(dateView, dateParams);

        TextView amountView = new TextView(this);
        amountView.setText(money(amount));
        amountView.setTextColor(CHARCOAL);
        amountView.setTextSize(16);
        amountView.setTypeface(null, Typeface.BOLD);

        topRow.addView(amountView);
        card.addView(topRow);

        checkBox.setOnCheckedChangeListener(
                (buttonView, isChecked) -> {
                    if (updatingSelectionControls
                            || finalRecordId == null) {
                        return;
                    }

                    if (isChecked) {
                        selectedExpenseIds.add(finalRecordId);
                    } else {
                        selectedExpenseIds.remove(finalRecordId);
                    }

                    updateSelectionControls();
                });

        return card;
    }


    private void updateSelectionControls() {
        ArrayList<DataSnapshot> visibleRecords =
                getFilteredExpenseRecords();

        int selectedCount = 0;

        for (DataSnapshot record : visibleRecords) {
            String id = record.getKey();

            if (id != null && selectedExpenseIds.contains(id)) {
                selectedCount++;
            }
        }

        int totalCount = visibleRecords.size();

        updatingSelectionControls = true;

        binding.selectAllExpenses.setChecked(
                totalCount > 0 && selectedCount == totalCount);

        binding.selectAllExpenses.setEnabled(
                totalCount > 0 && !deletingExpenses);

        binding.selectedExpenseCount.setText(
                selectedCount
                        + (selectedCount == 1
                        ? " expense selected"
                        : " expenses selected"));

        binding.deleteSelectedExpenses.setEnabled(
                selectedCount > 0 && !deletingExpenses);

        if (deletingExpenses) {
            binding.deleteSelectedExpenses.setText("Deleting...");
        } else if (selectedCount > 0) {
            binding.deleteSelectedExpenses.setText(
                    "Delete Selected (" + selectedCount + ")");
        } else {
            binding.deleteSelectedExpenses.setText("Delete Selected");
        }

        updatingSelectionControls = false;
    }

    private void confirmDeleteSelected() {
        if (selectedExpenseIds.isEmpty() || deletingExpenses) {
            Toast.makeText(this, "Select expenses to delete",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        int count = selectedExpenseIds.size();

        new AlertDialog.Builder(this)
                .setTitle("Delete selected expenses?")
                .setMessage(
                        "You are about to permanently delete "
                                + count
                                + (count == 1
                                ? " expense."
                                : " expenses.")
                                + "\n\nAnalytics totals will update "
                                + "when Firebase synchronizes.")
                .setNegativeButton("Cancel",
                        (dialog, which) -> dialog.dismiss())
                .setPositiveButton("Delete",
                        (dialog, which) ->
                                deleteSelectedExpenses())
                .show();
    }

    private void deleteSelectedExpenses() {
        if (selectedExpenseIds.isEmpty() || deletingExpenses) {
            return;
        }

        Map<String, Object> updates = new HashMap<>();

        for (String id : new HashSet<>(selectedExpenseIds)) {
            updates.put(id, null);
        }

        deletingExpenses = true;
        renderExpenses();

        expensesRef.updateChildren(updates)
                .addOnCompleteListener(task -> {
                    deletingExpenses = false;

                    if (task.isSuccessful()) {
                        selectedExpenseIds.clear();

                        Toast.makeText(this,
                                "Selected expenses deleted successfully",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this,
                                "Could not delete selected expenses. "
                                        + "Check your connection.",
                                Toast.LENGTH_LONG).show();
                    }

                    renderExpenses();
                });
    }

    private double readAmount(
            DataSnapshot record, String field) {

        Object value = record.child(field).getValue();

        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }

        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException ignored) {
                return 0.0;
            }
        }

        return 0.0;
    }

    private String money(double amount) {
        return String.format(
                Locale.getDefault(), "₹%,.2f", amount);
    }

    private GradientDrawable createRoundedBackground(
            int fillColor, int strokeColor, int radiusDp) {

        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fillColor);
        drawable.setCornerRadius(dp(radiusDp));
        drawable.setStroke(dp(1), strokeColor);

        return drawable;
    }

    private int dp(int value) {
        return (int) (value
                * getResources().getDisplayMetrics().density
                + 0.5f);
    }

    // Native Android bar chart. No external chart library required.
    private class MonthlyExpenseChartView extends View {

        private final double[] values;
        private final Paint paint =
                new Paint(Paint.ANTI_ALIAS_FLAG);

        private final String[] monthNames = {
                "Jan", "Feb", "Mar", "Apr",
                "May", "Jun", "Jul", "Aug",
                "Sep", "Oct", "Nov", "Dec"
        };

        MonthlyExpenseChartView(
                Context context, double[] monthlyValues) {

            super(context);

            values = monthlyValues.clone();
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            float width = getWidth();
            float height = getHeight();

            float left = dp(4);
            float right = width - dp(4);
            float top = dp(28);
            float bottom = height - dp(34);

            if (right <= left || bottom <= top) {
                return;
            }

            double maxValue = 0.0;

            for (double value : values) {
                if (value > maxValue) {
                    maxValue = value;
                }
            }

            // Leave space above the tallest bar for its value label.
            double scaleMaximum =
                    maxValue > 0 ? maxValue * 1.30 : 1.0;

            float chartHeight = bottom - top;
            float chartWidth = right - left;

            // Horizontal grid lines.
            paint.setColor(0xFFEAEAEA);
            paint.setStrokeWidth(dp(1));
            paint.setStyle(Paint.Style.STROKE);

            for (int i = 0; i <= 4; i++) {
                float y = bottom
                        - chartHeight * i / 4f;

                canvas.drawLine(left, y, right, y, paint);
            }

            paint.setStyle(Paint.Style.FILL);

            float slotWidth = chartWidth / 12f;
            float barWidth = Math.max(
                    dp(5), slotWidth * 0.52f);

            for (int i = 0; i < 12; i++) {
                float centerX = left
                        + slotWidth * (i + 0.5f);

                double value = values[i];

                if (value > 0) {
                    float barHeight = (float)
                            (value / scaleMaximum * chartHeight);

                    float barTop = bottom - barHeight;

                    paint.setColor(YELLOW);
                    paint.setStyle(Paint.Style.FILL);

                    canvas.drawRoundRect(
                            centerX - barWidth / 2f,
                            barTop,
                            centerX + barWidth / 2f,
                            bottom,
                            dp(3),
                            dp(3),
                            paint
                    );

                    // Compact value label above each bar.
                    paint.setColor(CHARCOAL);
                    paint.setTextSize(dp(9));
                    paint.setTypeface(Typeface.DEFAULT);
                    paint.setTextAlign(Paint.Align.CENTER);

                    String label = compactAmount(value);

                    float labelY = Math.max(
                            dp(11), barTop - dp(4));

                    canvas.drawText(
                            label, centerX, labelY, paint);
                }

                // Month names along the bottom.
                paint.setColor(MUTED);
                paint.setTextSize(dp(10));
                paint.setTextAlign(Paint.Align.CENTER);
                paint.setTypeface(Typeface.DEFAULT);

                canvas.drawText(
                        monthNames[i],
                        centerX,
                        height - dp(8),
                        paint
                );
            }
        }

        private String compactAmount(double amount) {
            if (amount >= 10000000) {
                return String.format(
                        Locale.getDefault(),
                        "%.1fCr", amount / 10000000.0);
            }

            if (amount >= 100000) {
                return String.format(
                        Locale.getDefault(),
                        "%.1fL", amount / 100000.0);
            }

            if (amount >= 1000) {
                return String.format(
                        Locale.getDefault(),
                        "%.1fk", amount / 1000.0);
            }

            return String.format(
                    Locale.getDefault(), "%.0f", amount);
        }
    }

    @Override
    protected void onDestroy() {
        if (expensesRef != null && expensesListener != null) {
            expensesRef.removeEventListener(expensesListener);
        }

        super.onDestroy();
    }
}
