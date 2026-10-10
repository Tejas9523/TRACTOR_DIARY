
package com.example.tractordiary;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.tractordiary.databinding.ActivityHistoryBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class history extends AppCompatActivity {

    private ActivityHistoryBinding binding;
    private DatabaseReference db;
    private adapter adapt;

    private final ArrayList<User> allRecords = new ArrayList<>();
    private String currentQuery = "";
    private ValueEventListener recordsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        getWindow().setStatusBarColor(
                ContextCompat.getColor(this, R.color.purple_200)
        );

        String userId = FirebaseAuth.getInstance()
                .getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        if (userId == null) {
            Toast.makeText(this, "Please log in again",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        db = FirebaseDatabase.getInstance()
                .getReference("user").child(userId);

        binding.his.setLayoutManager(new LinearLayoutManager(this));

        adapt = new adapter(this, new ArrayList<>());
        binding.his.setAdapter(adapt);

        adapt.setSelectionMode(true);
        adapt.setSelectionChangedListener(this::updateSelectionCount);

        binding.selectedCount.setText("0 selected");
        binding.deleteSelected.setEnabled(false);

        binding.selectAll.setOnCheckedChangeListener((button, checked) -> {
            adapt.setSelectionMode(true);

            if (checked) {
                adapt.selectAllVisible();
            } else {
                adapt.clearSelection();
            }

            updateSelectionCount();
        });

        binding.deleteSelected.setOnClickListener(view -> {
            int count = adapt.getSelectedCount();

            if (count == 0) {
                Toast.makeText(this, "Select records to delete",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            new AlertDialog.Builder(this)
                    .setTitle("Delete records?")
                    .setMessage("Permanently delete " + count
                            + " selected record(s)? This cannot be undone.")
                    .setNegativeButton("Cancel", (dialog, which) ->
                            dialog.dismiss())
                    .setPositiveButton("Delete", (dialog, which) ->
                            deleteSelectedRecords())
                    .show();
        });

        binding.search.setOnQueryTextListener(
                new SearchView.OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextSubmit(String query) {
                        applySearch(query);
                        return true;
                    }

                    @Override
                    public boolean onQueryTextChange(String newText) {
                        applySearch(newText);
                        return true;
                    }
                }
        );

        recordsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allRecords.clear();

                for (DataSnapshot child : snapshot.getChildren()) {
                    User user = child.getValue(User.class);

                    if (user != null) {
                        user.setKey(child.getKey());
                        allRecords.add(user);
                    }
                }

                applySearch(currentQuery);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(history.this,
                        "Could not load records: " + error.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        };

        db.addValueEventListener(recordsListener);
    }

    private void applySearch(String query) {
        currentQuery = query == null ? "" : query.trim();

        ArrayList<User> filtered = new ArrayList<>();

        for (User user : allRecords) {
            String name = user.getName() == null ? "" : user.getName();

            if (name.toLowerCase().contains(
                    currentQuery.toLowerCase())) {
                filtered.add(user);
            }
        }

        adapt.filterList(filtered);
        updateSelectionCount();
    }

    private void updateSelectionCount() {
        int count = adapt.getSelectedCount();

        binding.selectedCount.setText(count + " selected");
        binding.deleteSelected.setEnabled(count > 0);
    }

    private void deleteSelectedRecords() {
        Map<String, Object> updates = new HashMap<>();

        for (String key : adapt.getSelectedKeys()) {
            updates.put(key, null);
        }

        if (updates.isEmpty()) {
            Toast.makeText(this, "No records selected",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        binding.deleteSelected.setEnabled(false);

        db.updateChildren(updates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        adapt.clearSelection();
                        binding.selectAll.setOnCheckedChangeListener(null);
                        binding.selectAll.setChecked(false);
                        binding.selectAll.setOnCheckedChangeListener(
                                (button, checked) -> {
                                    adapt.setSelectionMode(true);
                                    if (checked) {
                                        adapt.selectAllVisible();
                                    } else {
                                        adapt.clearSelection();
                                    }
                                    updateSelectionCount();
                                }
                        );

                        Toast.makeText(this,
                                "Selected records deleted",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        updateSelectionCount();
                        Toast.makeText(this,
                                "Delete failed. Please try again.",
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    @Override
    protected void onDestroy() {
        if (db != null && recordsListener != null) {
            db.removeEventListener(recordsListener);
        }
        super.onDestroy();
    }
}
