package com.example.sivaram.cranes;

import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import dmax.dialog.SpotsDialog;

public class MainActivity extends AppCompatActivity {
    FirebaseAuth firebaseAuth;
    RelativeLayout rootlayout;
    FirebaseDatabase firebaseDatabase;
    DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button login = findViewById(R.id.login);
        rootlayout=(RelativeLayout)findViewById(R.id.root_layout);
        final Button signup =findViewById(R.id.sign_up);
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseDatabase=FirebaseDatabase.getInstance();
        databaseReference=firebaseDatabase.getReference("Users");
        signup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showRegistrationDialogue();
            }
        });
        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLoginDialogue();
            }
        });

    }

    private void showLoginDialogue() {

        final Button signup =findViewById(R.id.sign_up);
        final AlertDialog.Builder dialog =new AlertDialog.Builder(this);
        dialog.setTitle("SIGN IN");
        dialog.setMessage("Please use email for sign in");
        LayoutInflater inflater = LayoutInflater.from(this);
        View login_layout = inflater.inflate(R.layout.login,null);
        final EditText email = login_layout.findViewById(R.id.email);
        final EditText password = login_layout.findViewById(R.id.password);
        dialog.setView(login_layout);
        dialog.setPositiveButton("SIGN IN", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                        signup.setEnabled(false);

                        if (TextUtils.isEmpty(email.getText().toString())) {
                            Snackbar.make(rootlayout, "please enter email address ", Snackbar.LENGTH_SHORT).show();
                            return;

                        }
                        if (password.getText().toString().length() < 6) {
                            Snackbar.make(rootlayout, "password is too short ", Snackbar.LENGTH_SHORT).show();
                            return;
                        }
                        final SpotsDialog waitingDialog = new SpotsDialog(MainActivity.this);
                        waitingDialog.show();
                        firebaseAuth.signInWithEmailAndPassword(email.getText().toString(),password.getText().toString()).addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                            @Override
                            public void onSuccess(AuthResult authResult) {
                                waitingDialog.dismiss();
                                startActivity(new Intent(MainActivity.this,Welcome.class));
                                finish();

                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                waitingDialog.dismiss();
                                Snackbar.make(rootlayout, "Failed "+e.getMessage(), Snackbar.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
        dialog.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        dialog.show();

    }

    private void showRegistrationDialogue() {
        final AlertDialog.Builder dialog =new AlertDialog.Builder(this);
        dialog.setTitle("REGISTER");
        dialog.setMessage("Please use email for register");
        LayoutInflater inflater = LayoutInflater.from(this);
        View register_layout = inflater.inflate(R.layout.register,null);
        final EditText email = register_layout.findViewById(R.id.email);
        final EditText password = register_layout.findViewById(R.id.password);
        final EditText name = register_layout.findViewById(R.id.Name);
        final EditText phone = register_layout.findViewById(R.id.phone);
        dialog.setView(register_layout);
        dialog.setPositiveButton("REGISTER", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
                if(TextUtils.isEmpty(email.getText().toString())){
                    Snackbar.make(rootlayout,"please enter email address ",Snackbar.LENGTH_SHORT).show();
                    return;

                }
                if(password.getText().toString().length() <6){
                    Snackbar.make(rootlayout,"password is too short ",Snackbar.LENGTH_SHORT).show();
                    return;

                }
                if(TextUtils.isEmpty(name.getText().toString())){
                    Snackbar.make(rootlayout,"please enter name ",Snackbar.LENGTH_SHORT).show();
                    return;

                }
                if(TextUtils.isEmpty(phone.getText().toString())){
                    Snackbar.make(rootlayout,"please enter phone number ",Snackbar.LENGTH_SHORT).show();
                    return;

                }
                firebaseAuth.createUserWithEmailAndPassword(email.getText().toString(),password.getText().toString()).addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        User user = new User();
                        user.setEmail(email.getText().toString());
                        user.setPassword(password.getText().toString());
                        user.setName(name.getText().toString());
                        user.setPhone(phone.getText().toString());
                        databaseReference.child(FirebaseAuth.getInstance().getCurrentUser().getUid()).setValue(user).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Snackbar.make(rootlayout,"Register successfully",Snackbar.LENGTH_SHORT).show();

                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Snackbar.make(rootlayout,"Register failed ",Snackbar.LENGTH_SHORT).show();
                            }
                        })
                        ;
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        final Button signup =findViewById(R.id.sign_up);
                        Snackbar.make(rootlayout,"Failed ",Snackbar.LENGTH_SHORT).show();
                        signup.setEnabled(true);

                    }
                })
                ;

            }
        });

        dialog.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        dialog.show();
    }
}
