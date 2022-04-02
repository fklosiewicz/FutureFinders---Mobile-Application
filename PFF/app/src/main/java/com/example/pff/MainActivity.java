package com.example.pff;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.pff.design.User;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.UnknownHostException;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class MainActivity<color> extends AppCompatActivity {

    Button Register;
    public ArrayList<User> users;//List of Users
    public ArrayList<Integer> states;//List of selected states
    public ArrayList<Integer> indicators;//List of selected indicators
    public User activeUser;//Logged in member
    public final int CAP_GUEST = 2;//State cap for guest
    public final int CAP_MEMBER = 3;//State cap for member

    AlertDialog.Builder continueBuilder;

    private static final String URL = "jdbc:mysql://172.16.122.19:3306/future_finders";
    private static final String USER = "finder";
    private static final String PASS = "1234abcd";

    @SuppressLint("StaticFieldLeak")
    public class InfoAsyncTask extends AsyncTask<String, Void, Map<String, String>> {
        protected Map<String, String> doInBackground(String... strings) {
            Map<String, String> info = new HashMap<>();
            System.out.println("Connecting to the database...");
            try (Connection connection = DriverManager.getConnection(URL, USER, PASS)) {
                System.out.println("Connection valid: " + connection.isValid(5));

                String sql = "SELECT Wage FROM Indicators WHERE StateABBR = '" + strings[0] + "';";
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery();

                if(resultSet.next()) {
                    info.put("Wage", resultSet.getString("Wage"));
                }
            } catch (Exception e) {
                Log.e("InfoAsyncTask", "Error reading information", e);
            }
            return info;
        }

        @Override
        protected void onPostExecute(Map<String, String> result) {
            if (!result.isEmpty()) {
            }
        }
    }

    public void establishConnection(View view) throws InterruptedException {
        new InfoAsyncTask().execute("CA");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        activeUser = null;//Start main with no active user
        String p = this.getApplicationInfo().dataDir + "/appdata.dat";
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = getIntent();
        if(intent.hasExtra("States")) {
            states = intent.getExtras().getIntegerArrayList("States");
            for(int id : states) {
                Button b = findViewById(id);
                b.setBackgroundColor(Color.parseColor("#800080"));
            }
            indicators = intent.getExtras().getIntegerArrayList("Indicators");
            if(intent.hasExtra("activeUser")){//so that we remain logged in when coming back from indicators/results
                activeUser = (User)intent.getExtras().getSerializable("activeUser");
                findViewById(R.id.Account).setVisibility(View.VISIBLE);
                findViewById(R.id.Logout).setVisibility(View.VISIBLE);
            }
        }
        else {
            states = new ArrayList<Integer>();
            indicators = new ArrayList<Integer>();
        }
        if(intent.hasExtra("User")) {
            activeUser = (User)intent.getExtras().getSerializable("User");
            users = (ArrayList<User>)intent.getExtras().getSerializable("Users");
            findViewById(R.id.Account).setVisibility(View.VISIBLE);
            findViewById(R.id.Logout).setVisibility(View.VISIBLE);
        }

        File data = new File(p);
        boolean de = true;
        if(!data.exists() || !data.isFile()) {
            de = false;
            try {
                data.createNewFile();
                users = new ArrayList<User>();
                User stock = new User("username", "password");
                users.add(stock);

                FileOutputStream fos = new FileOutputStream(p);
                ObjectOutputStream oos = new ObjectOutputStream(fos);
                oos.writeObject(users);
                fos.close();
                oos.close();


            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else if (de) {
            try {
                FileInputStream fis = new FileInputStream(p);
                ObjectInputStream ois = new ObjectInputStream(fis);
                users = (ArrayList<User>) ois.readObject();
                fis.close();
                ois.close();

            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

    }

    public void register(View view) {
        Bundle bundle = new Bundle();
        bundle.putSerializable("Users", users);
        Intent intent = new Intent(this, RegistrationActivity.class);
        intent.putExtras(bundle);
        startActivity(intent);
    }

    public void selectState(View view) {
        Button b = (Button)view;
        if(activeUser==null){//If no User is logged in
            if(!states.contains(b.getId())) {
                if(!(states.size()<CAP_GUEST)){//If the cap is reached, prompt user to register
                    AlertDialog.Builder cap_reached = new AlertDialog.Builder(this);
                    cap_reached.setMessage("Maximum " + CAP_GUEST + " states for guest.\nTo " +
                            "select up to "+ CAP_MEMBER + " states, " +
                            "please register.").setPositiveButton("Okay", null);
                    cap_reached.show();
                    return;
                }
                states.add(b.getId());
                //Change background color to indicate selected
                b.setBackgroundColor(Color.parseColor("#800080"));
            }
            else {
                states.remove((Integer)b.getId());
                //Change background color back to default to indicate deselected
                b.setBackgroundColor(Color.parseColor("#FF6200EE"));
            }
        }
        else{//If User is logged in
            if(!states.contains(b.getId())) {
                if(!(states.size()<CAP_MEMBER)){//If the cap is reached, extort
                    AlertDialog.Builder cap_reached = new AlertDialog.Builder(this);
                    cap_reached.setMessage("Maximum " + CAP_MEMBER + " states for members.").setPositiveButton("Okay", null);
                    cap_reached.show();
                    return;
                }
                states.add(b.getId());
                //Change background color to indicate selected
                b.setBackgroundColor(Color.parseColor("#800080"));
            }
            else {
                states.remove((Integer)b.getId());
                //Change background color back to default to indicate deselected
                b.setBackgroundColor(Color.parseColor("#FF6200EE"));
            }
        }
    }

    public void indicators(View view) {
        if(states.isEmpty()) {//Make sure at least one state is selected
            AlertDialog.Builder noStates = new AlertDialog.Builder(view.getContext());
            noStates.setMessage("Please select at least one state").setPositiveButton("Okay", null);
            noStates.show();
            return;
        }

        Bundle bundle = new Bundle();
        bundle.putIntegerArrayList("States", states);
        bundle.putIntegerArrayList("Indicators", indicators);
        if(activeUser != null){
            bundle.putSerializable("activeUser", activeUser);
        }
//        Intent intent = new Intent(this, IndicatorActivity.class);
//        intent.putExtras(bundle);
//        startActivity(intent);
//        bundle.putStringArrayList("States", states);
//        bundle.putStringArrayList("Indicators", indicators);

        if (activeUser != null){
            Intent intent = new Intent(this, IndicatorActivity.class);
            intent.putExtras(bundle);
            startActivity(intent);
        }
        else if (activeUser == null){
            continueBuilder = new AlertDialog.Builder(MainActivity.this, R.style.AlertDialog_AppCompat);
            View continue_popup = getLayoutInflater().inflate(R.layout.continue_popup,
                    (ConstraintLayout)findViewById(R.id.Dialog_Container));
            continueBuilder.setView(continue_popup);
            continueBuilder.setTitle("Don't miss your chance to choose your own indicators!");
            final AlertDialog continueDialog = continueBuilder.create();
            Button continue_popup_login = (Button)continue_popup.findViewById(R.id.reminder_login);
            Button continue_popup_continue = (Button)continue_popup.findViewById(R.id.reminder_continue);

            continue_popup_login.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Go back to Main landing page
                    continueDialog.dismiss();
                    Toast.makeText(MainActivity.this, "Back",Toast.LENGTH_SHORT).show();
                }
            });

            continue_popup_continue.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Go to Results Page
                    indicators.add(R.id.wage);
                    indicators.add(R.id.happy);
                    indicators.add(R.id.tax_rate);

                    System.out.println(indicators);

                    continueDialog.dismiss();
                    Toast.makeText(MainActivity.this, "Continue",Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(MainActivity.this, ResultsActivity.class);
                    intent.putExtras(bundle);
                    startActivity(intent);
                }
            });
            continueDialog.show();

        }
    }


    public void logout(View view) {
        if(activeUser != null) {
            Toast.makeText(this, "User: " + activeUser.username + " successfully logged out!", Toast.LENGTH_LONG).show();
            activeUser = null;
            findViewById(R.id.Account).setVisibility(View.INVISIBLE);
            findViewById(R.id.Logout).setVisibility(View.INVISIBLE);
            for(int st : states){//Change background color of each selected state back to default to indicate deselected
                Button b = (Button)findViewById(st);
                b.setBackgroundColor(Color.parseColor("#FF6200EE"));
            }
            states.clear();//clear selections when logging out
            indicators.clear();//clear selections when logging out
            return;
        }
        else if(activeUser == null) {
            Toast.makeText(this, "Not currently logged in.", Toast.LENGTH_LONG).show();
            return;
        }
    }

    public void login(View view) {
        String p = this.getApplicationInfo().dataDir + "/appdata.dat";
        try {
            FileInputStream fis = new FileInputStream(p);
            ObjectInputStream ois = new ObjectInputStream(fis);
            users = (ArrayList<User>) ois.readObject();
            fis.close();
            ois.close();

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Please enter your username and password:");

        final EditText login = new EditText(this);
        login.setHint("User Name");
        final EditText pass = new EditText(this);
        pass.setHint("Password");

        login.setInputType(InputType.TYPE_CLASS_TEXT);
        pass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        LinearLayout lay = new LinearLayout(this);
        lay.setOrientation(LinearLayout.VERTICAL);
        lay.addView(login);
        lay.addView(pass);
        builder.setView(lay);
        final boolean[] success = {false};

        builder.setPositiveButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.cancel();
            }
        });

        builder.setNegativeButton("Login", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                for (User t : users) {
                    if(login.getText().toString().equals(t.username) && pass.getText().toString().equals(t.password)) {
                        AlertDialog.Builder no_delete = new AlertDialog.Builder(view.getContext());
                        no_delete.setMessage("Successful Login!").setPositiveButton("Okay", null);
                        no_delete.show();
                        activeUser = t;//Capture the logged in user
                        findViewById(R.id.Account).setVisibility(View.VISIBLE);
                        findViewById(R.id.Logout).setVisibility(View.VISIBLE);
                        success[0] = true;
                        return;
                    }
                }
                if(success[0] == false) {
                    AlertDialog.Builder no_delete = new AlertDialog.Builder(view.getContext());
                    no_delete.setMessage("This user does not exist.").setPositiveButton("Okay", null);
                    no_delete.show();
                    return;
                }
            }
        });
        builder.show().getButton(AlertDialog.BUTTON_NEGATIVE).requestFocus();
    }

    public void account(View view) {
        Bundle bundle = new Bundle();
        bundle.putSerializable("User", activeUser);
        bundle.putSerializable("Users", users);
        Intent intent = new Intent(this, AccountActivity.class);
        intent.putExtras(bundle);
        startActivity(intent);
    }
}