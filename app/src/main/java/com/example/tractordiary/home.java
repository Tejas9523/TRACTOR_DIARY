package com.example.tractordiary;

import static android.app.PendingIntent.getActivity;

import static java.lang.Integer.parseInt;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.DatePicker;
import android.widget.ProgressBar;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.tractordiary.databinding.ActivityHomeBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;

public class home extends AppCompatActivity {
    ActivityHomeBinding binding;
    DatabaseReference reference;

    FirebaseAuth auth = FirebaseAuth.getInstance();
    String id = auth.getCurrentUser().getUid();

    String uname,date,stime,etime,amt,amt2,description;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        getWindow().setStatusBarColor(ContextCompat.getColor(home.this,R.color.purple_200));

        binding.eml.setText(auth.getCurrentUser().getEmail().toString());

        reference = FirebaseDatabase.getInstance().getReference("user").child(id);
        reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                int counter = (int) dataSnapshot.getChildrenCount();
                String userCounter = String.valueOf(counter);
                binding.d3.setText(userCounter);

                int s1 = 0;
                int s2 = 0;
                for(DataSnapshot snapshot: dataSnapshot.getChildren()){
                    String a1 = snapshot.child("amt").getValue().toString();
                    s1 += Integer.parseInt(a1);
                    binding.d1.setText(String.valueOf(s1));

                    String a2 = snapshot.child("amt2").getValue().toString();
                    s2 = s2 + Integer.parseInt(a2);
                    binding.d2.setText(String.valueOf(s2));
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });

        binding.history.setOnClickListener((View view) ->{
                startActivity(new Intent(home.this,history.class));
        });

        binding.date.setOnClickListener(view -> {
            final Calendar c = Calendar.getInstance();

            // on below line we are getting
            // our day, month and year.
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);

            // on below line we are creating a variable for date picker dialog.
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    // on below line we are passing context.
                    home.this,
                    (view1, year1, monthOfYear, dayOfMonth) -> {
                        // on below line we are setting date to our text view.
                        binding.date.setText(dayOfMonth + "/" + (monthOfYear + 1) + "/" + year1);
                    },
                    // on below line we are passing year,
                    // month and day for selected date in our date picker.
                    year, month, day);
            // at last we are calling show to
            // display our date picker dialog.
            datePickerDialog.show();
        });

        binding.time1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Calendar c = Calendar.getInstance();

                // on below line we are getting our hour, minute.
                int hour = c.get(Calendar.HOUR_OF_DAY);
                int minute = c.get(Calendar.MINUTE);

                // on below line we are initializing our Time Picker Dialog
                TimePickerDialog timePickerDialog = new TimePickerDialog(home.this,
                        (view12, hourOfDay, minute1) -> {
                            // on below line we are setting selected time
                            // in our text view.
                            binding.time1.setText(hourOfDay + ":" + minute1);
                        }, hour, minute, false);
                // at last we are calling show to
                // display our time picker dialog.
                timePickerDialog.show();
            }
        });

        binding.time2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Calendar c = Calendar.getInstance();

                // on below line we are getting our hour, minute.
                int hour = c.get(Calendar.HOUR_OF_DAY);
                int minute = c.get(Calendar.MINUTE);

                // on below line we are initializing our Time Picker Dialog
                TimePickerDialog timePickerDialog = new TimePickerDialog(home.this,
                        new TimePickerDialog.OnTimeSetListener() {
                            @Override
                            public void onTimeSet(TimePicker view, int hourOfDay,
                                                  int minute) {
                                // on below line we are setting selected time
                                // in our text view.
                                binding.time2.setText(hourOfDay + ":" + minute);
                            }
                        }, hour, minute, false);
                // at last we are calling show to
                // display our time picker dialog.
                timePickerDialog.show();
            }
        });

        binding.add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                uname = binding.name.getText().toString();
                date = binding.date.getText().toString();
                stime = binding.time1.getText().toString();
                etime = binding.time2.getText().toString();
                amt = binding.amont.getText().toString();
                amt2 = binding.amont2.getText().toString();
                description = binding.des.getText().toString();

                if (!uname.isEmpty() && !date.isEmpty() && !stime.isEmpty() && !etime.isEmpty() && !amt.isEmpty() && !amt2.isEmpty() && !description.isEmpty()){
                    User use = new User(uname,date,stime,etime,amt,amt2,description);
                    FirebaseDatabase db = FirebaseDatabase.getInstance();
                    reference = db.getReference("user");
                    reference.child(id).child(reference.child(id).push().getKey()).setValue(use).addOnCompleteListener(task -> {
                        Toast.makeText(home.this,"Successfully Uploaded ...",Toast.LENGTH_SHORT).show();

                        startActivity(new Intent(home.this,home.class));

                    });
                }
                else {
                    Toast.makeText(home.this,"Invalid Deatils !!!",Toast.LENGTH_SHORT).show();
                }
            }
        });

        binding.imageView.setOnClickListener(view -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(home.this);
            builder.setMessage("Do you want to LogOut ...! ")
                    .setPositiveButton("Yes", (dialog, id) -> {
                        auth.signOut();
                        ProgressDialog mProgressDialog = new ProgressDialog(home.this);
                        mProgressDialog.setMessage("Loading...");
                        mProgressDialog.show();
                        startActivity(new Intent(home.this,MainActivity.class));
                        finish();
                    })
                    .setNegativeButton("No", (dialog, id) -> {
                        // User cancels the dialog.
                        dialog.cancel();
                    });
            // Create the AlertDialog object and return it.
            builder.create().show();
        });
    }
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finishAffinity();
    }
}