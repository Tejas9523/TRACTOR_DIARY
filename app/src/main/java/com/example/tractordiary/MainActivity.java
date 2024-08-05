package com.example.tractordiary;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.tractordiary.databinding.ActivityMainBinding;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

ActivityMainBinding binding;
FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();

String email,pass;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        getWindow().setStatusBarColor(ContextCompat.getColor(MainActivity.this,R.color.yellow));

        binding.textView.setOnClickListener(view -> startActivity(new Intent(MainActivity.this, signup.class)));

        binding.button.setOnClickListener(view -> {
            email = binding.emailEt.getText().toString();
            pass = binding.passET.getText().toString();

            if(!email.isEmpty() && !pass.isEmpty()){
                ProgressDialog mProgressDialog = new ProgressDialog(MainActivity.this);
                mProgressDialog.setMessage("Loading...");
                mProgressDialog.show();
                firebaseAuth.signInWithEmailAndPassword(email,pass).addOnCompleteListener(task -> {
                    if (task.isSuccessful()){
                        startActivity(new Intent(MainActivity.this, home.class));
                    }
                    else {
                        Toast.makeText(MainActivity.this,task.getException().toString(),Toast.LENGTH_SHORT).show();
                    }
                });
            }
            else {
                Toast.makeText(MainActivity.this,"Empty Fields Are not Allowed !!",Toast.LENGTH_SHORT).show();
            }
        });
        if (firebaseAuth.getCurrentUser()!= null){
            Intent intent = new Intent(MainActivity.this, home.class);
            startActivity(intent);
        }
    }
}