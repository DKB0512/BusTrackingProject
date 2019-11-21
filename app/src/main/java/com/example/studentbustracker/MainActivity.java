package com.example.studentbustracker;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private EditText mEmail, mPassword;

    private LocationManager locationManager;

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getLocationPermission();

        mAuth = FirebaseAuth.getInstance();

        if (Build.VERSION.SDK_INT >= 24) {
            PermissionCheck();
        }

        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser User = FirebaseAuth.getInstance().getCurrentUser();
                if (User != null) {
                    Intent intent = new Intent(MainActivity.this, SelectionActivity.class);
                    startActivity(intent);
                    finish();
                }
            }
        };

        mEmail = findViewById(R.id.User);
        mPassword = findViewById(R.id.Pass);
        Button mLogin = findViewById(R.id.Login);
        Button mRegistration = findViewById(R.id.Registration);

        mRegistration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String Email = mEmail.getText().toString();
                final String Password = mPassword.getText().toString();
                mAuth.createUserWithEmailAndPassword(Email, Password).addOnCompleteListener(MainActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (!task.isSuccessful()) {
                            Toast.makeText(getApplicationContext(), "User Already Exist", Toast.LENGTH_LONG).show();
                        } else {
                            String u_id = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
                            DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("Users").child("Students").child(u_id);
                            reference.setValue(true);
                        }
                    }
                });
            }
        });

        mLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String Email = mEmail.getText().toString();
                final String Password = mPassword.getText().toString();
                if(!Email.equals("") && !Password.equals("")) {
                    mAuth.signInWithEmailAndPassword(Email, Password).addOnCompleteListener(MainActivity.this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (!task.isSuccessful()) {
                                Toast.makeText(getApplicationContext(), "Sign-In Error/Failed", Toast.LENGTH_LONG).show();
                            } else {
                                String user_id = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
                                DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("Users").child("Students").child(user_id);
                                reference.setValue(true);
                            }
                        }
                    });
                }else{
                    Toast.makeText(getApplicationContext(), "Please fill all the Fields", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void getLocationPermission() {
        //adding permission as a String
        String[] permission = {Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION};

        //Checking both permission are granted or not
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(this.getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {

            } else {
                ActivityCompat.requestPermissions(this, permission, 1234);
            }
        } else {
            ActivityCompat.requestPermissions(this, permission, 1234);
        }
    }

    private void PermissionCheck() {


        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Location Services Not Active");
            builder.setMessage("Please enable Location Services and GPS");
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialogInterface, int i) {
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(intent);
                }
            });
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthStateListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mAuth.removeAuthStateListener(mAuthStateListener);
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Closing Activity")
                .setMessage("Are you sure you want to close this Application?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        moveTaskToBack(true);
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }
}
