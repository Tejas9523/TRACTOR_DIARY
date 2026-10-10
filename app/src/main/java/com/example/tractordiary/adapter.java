package com.example.tractordiary;


import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.widget.CheckBox;
import java.util.HashSet;
import java.util.Set;

public class adapter extends RecyclerView.Adapter<adapter.MyiewHolder> {

    private final Set<String> selectedKeys = new HashSet<>();
    private boolean selectionMode = false;
    ArrayList<User> list;

    public adapter(Context context, ArrayList<User> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public MyiewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item,parent,false);
        return new MyiewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull MyiewHolder holder, @SuppressLint("RecyclerView") int position) {
        
        User user = list.get(position);
        CheckBox recordCheckbox =
                holder.itemView.findViewById(R.id.recordCheckbox);

        recordCheckbox.setOnCheckedChangeListener(null);

        recordCheckbox.setVisibility(
                selectionMode ? View.VISIBLE : View.GONE
        );

        String recordKey = user.getKey();

        recordCheckbox.setChecked(
                recordKey != null && selectedKeys.contains(recordKey)
        );

        recordCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (recordKey == null) return;

            if (isChecked) {
                selectedKeys.add(recordKey);
            } else {
                selectedKeys.remove(recordKey);
            }
            if (selectionChanged != null) {
                selectionChanged.run();
            }
        });
        holder.name.setText(user.getName());
        holder.date.setText(user.getDate());
        holder.stime.setText(user.getStime());
        holder.etime.setText(user.getEtime());
        holder.description.setText(user.getDescription());
        holder.amt.setText(user.getAmt());
        holder.amt2.setText(user.getAmt2());

        if (holder.amt2.getText().toString().equals("0")){
            holder.itemView.findViewById(R.id.item123).setBackgroundResource(R.color.recieved);
        }
        else {
            holder.itemView.findViewById(R.id.item123).setBackgroundResource(R.color.pending);
        }


        holder.itemView.findViewById(R.id.item123).setOnClickListener(view -> {

            Dialog dialog = new Dialog(view.getContext());
            dialog.setContentView(R.layout.update);

            EditText nm1 = dialog.findViewById(R.id.name);
            EditText dt1 = dialog.findViewById(R.id.date);
            EditText tt1 = dialog.findViewById(R.id.time1);
            EditText tt2 = dialog.findViewById(R.id.time2);
            EditText ds = dialog.findViewById(R.id.des);
            EditText amt1 = dialog.findViewById(R.id.amont);
            EditText amt2 = dialog.findViewById(R.id.amont2);

            Button update = dialog.findViewById(R.id.ud);
            Button cle = dialog.findViewById(R.id.cancle);

            // Fill the dialog with the existing record.
            nm1.setText(user.getName());
            dt1.setText(user.getDate());
            tt1.setText(user.getStime());
            tt2.setText(user.getEtime());
            ds.setText(user.getDescription());
            amt1.setText(user.getAmt());
            amt2.setText(user.getAmt2());

            cle.setOnClickListener(v -> dialog.dismiss());

            update.setOnClickListener(v -> {

                String received = amt1.getText().toString().trim();
                String pending = amt2.getText().toString().trim();

                if (received.isEmpty() || pending.isEmpty()) {
                    Toast.makeText(view.getContext(),
                            "Please enter both amounts",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                String uid = FirebaseAuth.getInstance().getUid();

                if (uid == null) {
                    Toast.makeText(view.getContext(),
                            "Please log in again",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                Map<String, Object> map = new HashMap<>();

                map.put("name", nm1.getText().toString().trim());
                map.put("date", dt1.getText().toString().trim());
                map.put("stime", tt1.getText().toString().trim());
                map.put("etime", tt2.getText().toString().trim());
                map.put("description", ds.getText().toString().trim());
                map.put("amt", received);
                map.put("amt2", pending);

                FirebaseDatabase.getInstance()
                        .getReference("user")
                        .child(uid)
                        .child(user.getKey())
                        .updateChildren(map)
                        .addOnCompleteListener(task -> {

                            if (task.isSuccessful()) {
                                Toast.makeText(view.getContext(),
                                        "Successfully updated",
                                        Toast.LENGTH_SHORT).show();

                                dialog.dismiss();
                            } else {
                                Toast.makeText(view.getContext(),
                                        "Update failed. Please try again.",
                                        Toast.LENGTH_LONG).show();
                            }
                        });
            });

            dialog.show();

            // Make the edit dialog wider and easier to use.
            if (dialog.getWindow() != null) {
                dialog.getWindow().setLayout(
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.WRAP_CONTENT
                );
            }
        });


    }

    @Override
    public int getItemCount() {
        return list.size();
    }


    public void filterList(ArrayList<User> filteredList){
        list = filteredList;
        notifyDataSetChanged();
    }


    public void setSelectionMode(boolean enabled) {
        selectionMode = enabled;

        if (!enabled) {
            selectedKeys.clear();
        }

        notifyDataSetChanged();
    }

    public void selectAllVisible() {
        for (User user : list) {
            if (user.getKey() != null) {
                selectedKeys.add(user.getKey());
            }
        }
        notifyDataSetChanged();
    }

    public void clearSelection() {
        selectedKeys.clear();
        notifyDataSetChanged();
    }

    public Set<String> getSelectedKeys() {
        return new HashSet<>(selectedKeys);
    }

    public int getSelectedCount() {
        return selectedKeys.size();
    }


    public static class MyiewHolder extends RecyclerView.ViewHolder{

        TextView name,date,stime,etime,description,amt,amt2;

        public MyiewHolder(@NonNull View itemView) {
            super(itemView);

            name = itemView.findViewById(R.id.textview3);
            date = itemView.findViewById(R.id.textview5);
            stime = itemView.findViewById(R.id.textview7);
            etime = itemView.findViewById(R.id.textview9);
            description = itemView.findViewById(R.id.textview13);
            amt = itemView.findViewById(R.id.textview11);
            amt2 = itemView.findViewById(R.id.textview10);
        }
    }

    private Runnable selectionChanged;

    public void setSelectionChangedListener(Runnable listener) {
        selectionChanged = listener;
    }
}
