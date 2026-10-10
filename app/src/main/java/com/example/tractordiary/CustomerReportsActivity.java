package com.example.tractordiary;

import android.app.AlertDialog;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.tractordiary.databinding.ActivityCustomerReportsBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;

public class CustomerReportsActivity extends AppCompatActivity {

    private ActivityCustomerReportsBinding binding;
    private DatabaseReference customerRef;
    private ValueEventListener customerListener;

    private final ArrayList<DataSnapshot> records = new ArrayList<>();
    private final LinkedHashMap<String, CustomerSummary> customers =
            new LinkedHashMap<>();

    private static final int YELLOW = 0xFFFFC801;
    private static final int CHARCOAL = 0xFF292929;
    private static final int MUTED = 0xFF77716A;
    private static final int GREEN = 0xFF16834A;
    private static final int RED = 0xFFC64C35;

    private static class CustomerSummary {
        String name;
        double received;
        double pending;
        int jobs;
        final ArrayList<DataSnapshot> transactions = new ArrayList<>();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityCustomerReportsBinding.inflate(getLayoutInflater());
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

        binding.searchCustomer.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(
                    CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(
                    CharSequence s, int start, int before, int count) {
                renderCustomerList(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        loadCustomerRecords();
    }

    private void loadCustomerRecords() {
        customerListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                records.clear();

                for (DataSnapshot child : snapshot.getChildren()) {
                    records.add(child);
                }

                calculateSummaries();
                renderCustomerList(
                        binding.searchCustomer.getText().toString());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(CustomerReportsActivity.this,
                        "Could not load customer reports: "
                                + error.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        };

        customerRef.addValueEventListener(customerListener);
    }

    private double readAmount(DataSnapshot record, String field) {
        Object value = record.child(field).getValue();

        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }

        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }

        return 0;
    }

    private void calculateSummaries() {
        customers.clear();

        double totalReceived = 0;
        double totalPending = 0;

        for (DataSnapshot record : records) {
            String name = record.child("name").getValue(String.class);

            if (name == null || name.trim().isEmpty()) {
                name = "Unnamed Customer";
            } else {
                name = name.trim();
            }

            CustomerSummary summary = customers.get(name.toLowerCase(Locale.ROOT));

            if (summary == null) {
                summary = new CustomerSummary();
                summary.name = name;
                customers.put(name.toLowerCase(Locale.ROOT), summary);
            }

            double received = readAmount(record, "amt");
            double pending = readAmount(record, "amt2");

            summary.received += received;
            summary.pending += pending;
            summary.jobs++;
            summary.transactions.add(record);

            totalReceived += received;
            totalPending += pending;
        }

        binding.reportTotalRevenue.setText(money(totalReceived + totalPending));
        binding.reportTotalReceived.setText(money(totalReceived));
        binding.reportTotalPending.setText(money(totalPending));

        int count = customers.size();
        binding.reportCustomerCount.setText(
                count + (count == 1 ? " customer" : " customers"));
    }

    private void renderCustomerList(String query) {
        binding.customerReportList.removeAllViews();

        String search = query == null
                ? "" : query.trim().toLowerCase(Locale.ROOT);

        int shown = 0;

        for (CustomerSummary customer : customers.values()) {
            if (!customer.name.toLowerCase(Locale.ROOT).contains(search)) {
                continue;
            }

            binding.customerReportList.addView(createCustomerCard(customer));
            shown++;
        }

        binding.emptyCustomerReports.setVisibility(
                shown == 0 ? View.VISIBLE : View.GONE);
    }

    private View createCustomerCard(CustomerSummary customer) {
        // Main card: white surface, subtle border, and a small elevation.
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));
        card.setBackground(createRoundedBackground(0xFFFFFFFF, 0xFFE5E5E5, 12));
        card.setElevation(dp(2));
        card.setClickable(true);
        card.setFocusable(true);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.bottomMargin = dp(12);
        card.setLayoutParams(cardParams);

        // Header: initials avatar, customer name, and record count.
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView avatar = new TextView(this);
        String customerName = customer.name == null ? "?" : customer.name.trim();
        String initial = customerName.isEmpty()
                ? "?" : customerName.substring(0, 1).toUpperCase(Locale.ROOT);
        avatar.setText(initial);
        avatar.setTextColor(0xFF715400);
        avatar.setTextSize(18);
        avatar.setTypeface(null, android.graphics.Typeface.BOLD);
        avatar.setGravity(Gravity.CENTER);
        avatar.setBackground(createRoundedBackground(0xFFFFF1B8, 0xFFE9DDAF, 14));

        LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(
                dp(46), dp(46));
        avatarParams.setMarginEnd(dp(12));
        header.addView(avatar, avatarParams);

        LinearLayout nameBlock = new LinearLayout(this);
        nameBlock.setOrientation(LinearLayout.VERTICAL);
        nameBlock.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams nameBlockParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        header.addView(nameBlock, nameBlockParams);

        TextView name = new TextView(this);
        name.setText(customer.name);
        name.setTextColor(CHARCOAL);
        name.setTextSize(16);
        name.setMaxLines(2);
        name.setTypeface(null, android.graphics.Typeface.BOLD);
        nameBlock.addView(name);

        TextView jobs = new TextView(this);
        jobs.setText(customer.jobs + (customer.jobs == 1 ? " work record" : " work records"));
        jobs.setTextColor(MUTED);
        jobs.setTextSize(12);
        LinearLayout.LayoutParams jobsParams = new LinearLayout.LayoutParams(
                -2, -2);
        jobsParams.topMargin = dp(3);
        nameBlock.addView(jobs, jobsParams);

        TextView arrow = new TextView(this);
        arrow.setText("›");
        arrow.setTextColor(MUTED);
        arrow.setTextSize(28);
        arrow.setGravity(Gravity.CENTER);
        header.addView(arrow, new LinearLayout.LayoutParams(dp(24), dp(40)));

        card.addView(header);

        // Show collections and outstanding balance without repeating revenue.
        // Separate received and pending amount tiles.
        LinearLayout amounts = new LinearLayout(this);
        amounts.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams amountsParams = new LinearLayout.LayoutParams(
                -1, -2);
        amountsParams.topMargin = dp(16);
        card.addView(amounts, amountsParams);

        LinearLayout receivedBox = new LinearLayout(this);
        receivedBox.setOrientation(LinearLayout.VERTICAL);
        receivedBox.setPadding(dp(11), dp(10), dp(8), dp(10));
        receivedBox.setBackground(createRoundedBackground(0xFFEDF7EF, 0xFFD7EBDD, 8));
        amounts.addView(receivedBox, new LinearLayout.LayoutParams(0, -2, 1f));

        TextView receivedLabel = new TextView(this);
        receivedLabel.setText("RECEIVED");
        receivedLabel.setTextColor(0xFF34734A);
        receivedLabel.setTextSize(10);
        receivedLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        receivedBox.addView(receivedLabel);

        TextView received = new TextView(this);
        received.setText(money(customer.received));
        received.setTextColor(GREEN);
        received.setTextSize(16);
        received.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams receivedValueParams = new LinearLayout.LayoutParams(
                -1, -2);
        receivedValueParams.topMargin = dp(4);
        receivedBox.addView(received, receivedValueParams);

        LinearLayout pendingBox = new LinearLayout(this);
        pendingBox.setOrientation(LinearLayout.VERTICAL);
        pendingBox.setPadding(dp(11), dp(10), dp(8), dp(10));
        pendingBox.setBackground(createRoundedBackground(0xFFFFF5D9, 0xFFEFE0B2, 8));
        LinearLayout.LayoutParams pendingBoxParams = new LinearLayout.LayoutParams(
                0, -2, 1f);
        pendingBoxParams.setMarginStart(dp(8));
        amounts.addView(pendingBox, pendingBoxParams);

        TextView pendingLabel = new TextView(this);
        pendingLabel.setText("PENDING");
        pendingLabel.setTextColor(0xFF8A6800);
        pendingLabel.setTextSize(10);
        pendingLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        pendingBox.addView(pendingLabel);

        TextView pending = new TextView(this);
        pending.setText(money(customer.pending));
        pending.setTextColor(customer.pending > 0 ? RED : MUTED);
        pending.setTextSize(16);
        pending.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams pendingValueParams = new LinearLayout.LayoutParams(
                -1, -2);
        pendingValueParams.topMargin = dp(4);
        pendingBox.addView(pending, pendingValueParams);

        // Low-key action row; tapping anywhere on the card opens its history.
        LinearLayout actionRow = new LinearLayout(this);
        actionRow.setOrientation(LinearLayout.HORIZONTAL);
        actionRow.setGravity(Gravity.CENTER_VERTICAL);
        actionRow.setPadding(dp(12), dp(9), dp(12), dp(9));
        actionRow.setBackground(createRoundedBackground(0xFFFFF9E5, 0x00FFFFFF, 8));
        LinearLayout.LayoutParams actionParams = new LinearLayout.LayoutParams(
                -1, -2);
        actionParams.topMargin = dp(12);
        card.addView(actionRow, actionParams);

        TextView details = new TextView(this);
        details.setText("View transaction history");
        details.setTextColor(0xFF755A00);
        details.setTextSize(13);
        details.setTypeface(null, android.graphics.Typeface.BOLD);
        actionRow.addView(details, new LinearLayout.LayoutParams(0, -2, 1f));

        TextView actionArrow = new TextView(this);
        actionArrow.setText("→");
        actionArrow.setTextColor(0xFF755A00);
        actionArrow.setTextSize(18);
        actionRow.addView(actionArrow);

        card.setOnClickListener(v -> showTransactionHistory(customer));
        actionRow.setOnClickListener(v -> showTransactionHistory(customer));

        return card;
    }

    private GradientDrawable createRoundedBackground(
            int fillColor, int strokeColor, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fillColor);
        drawable.setCornerRadius(dp(radiusDp));
        if (strokeColor != 0x00FFFFFF) {
            drawable.setStroke(dp(1), strokeColor);
        }
        return drawable;
    }

    private void showTransactionHistory(CustomerSummary customer) {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(8), dp(16), dp(8));

        // Customer-level totals shown here exclude the repeated revenue total.
        LinearLayout totalsCard = new LinearLayout(this);
        totalsCard.setOrientation(LinearLayout.VERTICAL);
        totalsCard.setPadding(dp(14), dp(12), dp(14), dp(12));
        totalsCard.setBackground(createRoundedBackground(
                0xFFFFF9E5, 0xFFE9DDAF, 10));

        TextView totalsHeading = new TextView(this);
        totalsHeading.setText("PAYMENT SUMMARY");
        totalsHeading.setTextColor(0xFF806300);
        totalsHeading.setTextSize(11);
        totalsHeading.setTypeface(null, android.graphics.Typeface.BOLD);
        totalsCard.addView(totalsHeading);

        addTransactionField(totalsCard, "Total received", money(customer.received));
        addTransactionField(totalsCard, "Total pending", money(customer.pending));
        content.addView(totalsCard);

        TextView historyHeading = new TextView(this);
        historyHeading.setText("ALL TRANSACTION DETAILS");
        historyHeading.setTextColor(MUTED);
        historyHeading.setTextSize(12);
        historyHeading.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams headingParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        headingParams.topMargin = dp(18);
        headingParams.bottomMargin = dp(8);
        content.addView(historyHeading, headingParams);

        for (int i = customer.transactions.size() - 1; i >= 0; i--) {
            DataSnapshot record = customer.transactions.get(i);

            String customerName = readText(record, "name");
            String date = readText(record, "date");
            String startTime = readFirstText(record, "stime", "time1");
            String endTime = readFirstText(record, "etime", "time2");
            String description = readFirstText(record, "description", "des");
            double received = readAmount(record, "amt");
            double pending = readAmount(record, "amt2");

            LinearLayout transactionCard = new LinearLayout(this);
            transactionCard.setOrientation(LinearLayout.VERTICAL);
            transactionCard.setPadding(dp(14), dp(12), dp(14), dp(12));
            transactionCard.setBackground(createRoundedBackground(
                    0xFFFFFFFF, 0xFFE5E5E5, 9));

            LinearLayout.LayoutParams transactionParams =
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT);
            transactionParams.bottomMargin = dp(10);
            content.addView(transactionCard, transactionParams);

            TextView transactionTitle = new TextView(this);
            transactionTitle.setText("Work record " + (customer.transactions.size() - i));
            transactionTitle.setTextColor(CHARCOAL);
            transactionTitle.setTextSize(15);
            transactionTitle.setTypeface(null, android.graphics.Typeface.BOLD);
            transactionCard.addView(transactionTitle);

            addTransactionField(transactionCard, "Customer", emptyFallback(customerName, customer.name));
            addTransactionField(transactionCard, "Date", emptyFallback(date, "Date not recorded"));
            addTransactionField(transactionCard, "Start time", emptyFallback(startTime, "Not recorded"));
            addTransactionField(transactionCard, "End time", emptyFallback(endTime, "Not recorded"));
            addTransactionField(transactionCard, "Description", emptyFallback(description, "No description"));
            addTransactionField(transactionCard, "Received amount", money(received));
            addTransactionField(transactionCard, "Pending amount", money(pending));
        }

        android.widget.ScrollView scrollView =
                new android.widget.ScrollView(this);
        scrollView.setFillViewport(false);
        scrollView.addView(content);

        new AlertDialog.Builder(this)
                .setTitle(customer.name + " • Transaction History")
                .setView(scrollView)
                .setPositiveButton("Close", null)
                .show();
    }

    private void addTransactionField(
            LinearLayout container, String label, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.TOP);

        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        rowParams.topMargin = dp(7);
        row.setLayoutParams(rowParams);

        TextView labelView = new TextView(this);
        labelView.setText(label);
        labelView.setTextColor(MUTED);
        labelView.setTextSize(12);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.42f);
        row.addView(labelView, labelParams);

        TextView valueView = new TextView(this);
        valueView.setText(value);
        valueView.setTextColor(CHARCOAL);
        valueView.setTextSize(13);
        valueView.setGravity(Gravity.END);
        valueView.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams valueParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.58f);
        row.addView(valueView, valueParams);

        container.addView(row);
    }

    private String readText(DataSnapshot record, String field) {
        Object value = record.child(field).getValue();
        return value == null ? "" : value.toString().trim();
    }

    private String readFirstText(
            DataSnapshot record, String firstField, String secondField) {
        String value = readText(record, firstField);
        if (value.isEmpty()) {
            value = readText(record, secondField);
        }
        return value;
    }

    private String emptyFallback(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private String money(double amount) {
        return String.format(Locale.getDefault(), "₹%,.2f", amount);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onDestroy() {
        if (customerRef != null && customerListener != null) {
            customerRef.removeEventListener(customerListener);
        }

        super.onDestroy();
    }
}
