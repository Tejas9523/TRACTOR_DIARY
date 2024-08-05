package com.example.tractordiary;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.tractordiary.databinding.ActivitySignupBinding;
import com.google.firebase.auth.FirebaseAuth;

public class signup extends AppCompatActivity {
    ActivitySignupBinding binding;
    FirebaseAuth firebaseAuth;

    String email,pass,confirmPass;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        getWindow().setStatusBarColor(ContextCompat.getColor(signup.this,R.color.grey));

        binding.textV.setOnClickListener(view -> startActivity(new Intent(signup.this,MainActivity.class)));

        binding.button1.setOnClickListener(view -> {

            email = binding.emailEt1.getText().toString();
            pass = binding.passET1.getText().toString();
            confirmPass = binding.confirmPassEt1.getText().toString();

            if (!email.isEmpty() && !pass.isEmpty() && !confirmPass.isEmpty()){
                if (pass.equals(confirmPass)){
                    ProgressDialog mProgressDialog = new ProgressDialog(signup.this);
                    mProgressDialog.setMessage("Loading...");
                    mProgressDialog.show();
                    firebaseAuth = FirebaseAuth.getInstance();
                    firebaseAuth.createUserWithEmailAndPassword(email,pass).addOnCompleteListener(task -> {
                        if (task.isSuccessful()){
                            startActivity(new Intent(signup.this,MainActivity.class));
                        }
                        else {
                            Toast.makeText(signup.this,task.getException().toString(),Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                else {
                    Toast.makeText(signup.this,"Password is not matching...!",Toast.LENGTH_SHORT).show();
                }
            }
            else {
                Toast.makeText(signup.this,"Empty Fields Are not Allowed !!!",Toast.LENGTH_SHORT).show();
            }
        });

    }
}